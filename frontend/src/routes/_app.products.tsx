import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { Plus, Search, Pencil, Trash2, Package } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { TableSkeleton } from "@/components/common/TableSkeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { ProductFormDialog } from "@/components/products/ProductFormDialog";
import { ProductImageCarousel } from "@/components/products/ProductImageCarousel";
import { useBusinessMode } from "@/hooks/use-business-mode";
import {
  useProducts,
  useWarehouses,
  useCreateProduct,
  useUpdateProduct,
  useDeleteProduct,
} from "@/hooks/queries";
import { useCurrentUser } from "@/hooks/use-current-user";
import { canMutateInventory } from "@/lib/role-access";
import { currency } from "@/lib/format";
import type { Product, ProductInput } from "@/types";
import { toast } from "sonner";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";

export const Route = createFileRoute("/_app/products")({
  component: ProductsPage,
});

function ProductsPage() {
  const { user } = useCurrentUser();
  const readOnly = user ? !canMutateInventory(user.role) : false;
  const warehouses = useWarehouses();
  const [warehouseFilter, setWarehouseFilter] = useState("all");
  const stockWarehouseId = warehouseFilter === "all" ? undefined : warehouseFilter;
  const products = useProducts(stockWarehouseId);
  const createMut = useCreateProduct();
  const updateMut = useUpdateProduct();
  const deleteMut = useDeleteProduct();
  const { isSingleLocation, setDetectedWarehouseCount } = useBusinessMode();

  useEffect(() => {
    if (warehouses.data) setDetectedWarehouseCount(warehouses.data.length);
  }, [warehouses.data, setDetectedWarehouseCount]);

  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("all");
  const [stockLevel, setStockLevel] = useState("all");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Product | null>(null);
  const [toDelete, setToDelete] = useState<Product | null>(null);

  const categories = useMemo(
    () => Array.from(new Set(products.data?.map((p) => p.category) ?? [])),
    [products.data],
  );

  const filtered = useMemo(() => {
    return (products.data ?? []).filter((p) => {
      const q = query.toLowerCase();
      if (q && !p.name.toLowerCase().includes(q) && !p.sku.toLowerCase().includes(q)) return false;
      if (category !== "all" && p.category !== category) return false;
      if (warehouseFilter !== "all") {
        const stockedHere = p.stock > 0;
        const defaultHere = p.warehouseId === warehouseFilter;
        if (!stockedHere && !defaultHere) return false;
      }
      if (stockLevel === "out" && p.stock !== 0) return false;
      if (stockLevel === "low" && (p.stock === 0 || p.stock > p.lowStockThreshold)) return false;
      if (stockLevel === "ok" && p.stock <= p.lowStockThreshold) return false;
      return true;
    });
  }, [products.data, query, category, stockLevel, warehouseFilter]);

  const handleSubmit = (input: ProductInput) => {
    if (editing) {
      updateMut.mutate(
        { id: editing.id, input },
        {
          onSuccess: () => {
            toast.success("Product updated");
            setDialogOpen(false);
            setEditing(null);
          },
        },
      );
    } else {
      createMut.mutate(input, {
        onSuccess: () => {
          toast.success("Product created");
          setDialogOpen(false);
        },
      });
    }
  };

  const warehouseName = (id: string) => warehouses.data?.find((w) => w.id === id)?.name ?? "—";

  const stockBadge = (p: Product) => {
    if (p.stock === 0)
      return (
        <Badge
          variant="outline"
          className="bg-destructive/15 text-destructive border-destructive/30"
        >
          Out
        </Badge>
      );
    if (p.stock <= p.lowStockThreshold)
      return (
        <Badge variant="outline" className="bg-warning/15 text-warning border-warning/30">
          Low
        </Badge>
      );
    return (
      <Badge variant="outline" className="bg-success/15 text-success border-success/30">
        OK
      </Badge>
    );
  };

  return (
    <div>
      <PageHeader
        title="Products"
        description={
          readOnly
            ? "Read-only catalog view — pricing and stock levels across locations."
            : "Manage your catalog, pricing, and stock levels."
        }
        actions={
          !readOnly ? (
            <Button
              onClick={() => {
                setEditing(null);
                setDialogOpen(true);
              }}
            >
              <Plus className="mr-2 h-4 w-4" /> Add product
            </Button>
          ) : undefined
        }
      />

      <Card className="p-4">
        <div className="mb-4 flex flex-wrap items-center gap-2">
          <div className="relative flex-1 min-w-[220px]">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search by name or SKU…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="pl-9"
            />
          </div>
          <Select value={category} onValueChange={setCategory}>
            <SelectTrigger className="w-[160px]">
              <SelectValue placeholder="Category" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All categories</SelectItem>
              {categories.map((c) => (
                <SelectItem key={c} value={c}>
                  {c}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {!isSingleLocation && (
            <Select value={warehouseFilter} onValueChange={setWarehouseFilter}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="Warehouse" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All warehouses</SelectItem>
                {warehouses.data?.map((w) => (
                  <SelectItem key={w.id} value={w.id}>
                    {w.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
          <Select value={stockLevel} onValueChange={setStockLevel}>
            <SelectTrigger className="w-[160px]">
              <SelectValue placeholder="Stock level" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All stock</SelectItem>
              <SelectItem value="out">Out of stock</SelectItem>
              <SelectItem value="low">Low stock</SelectItem>
              <SelectItem value="ok">In stock</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {products.isLoading ? (
          <TableSkeleton rows={8} cols={6} />
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={Package}
            title={products.data?.length === 0 ? "No products yet" : "No products found"}
            description={
              products.data?.length === 0
                ? "Add your first product to start tracking inventory."
                : "Try adjusting filters or add a new product."
            }
            action={
              !readOnly ? (
                <Button
                  onClick={() => {
                    setEditing(null);
                    setDialogOpen(true);
                  }}
                >
                  <Plus className="mr-2 h-4 w-4" /> Add product
                </Button>
              ) : undefined
            }
          />
        ) : (
          <div className="overflow-hidden rounded-md border border-border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[88px]">Photos</TableHead>
                  <TableHead>Name</TableHead>
                  <TableHead>SKU</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead className="text-right">Price/Unit</TableHead>
                  <TableHead className="text-right">Stock</TableHead>
                  <TableHead>Status</TableHead>
                  {!isSingleLocation && <TableHead>Warehouse</TableHead>}
                  {!readOnly && <TableHead className="w-[100px]" />}
                </TableRow>
              </TableHeader>
              <TableBody>
                {filtered.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell>
                      <ProductImageCarousel
                        images={p.images}
                        alt={p.name}
                        className="h-16 w-16"
                        aspect=""
                        showCounter={false}
                      />
                    </TableCell>
                    <TableCell className="font-medium">{p.name}</TableCell>
                    <TableCell className="text-muted-foreground">{p.sku}</TableCell>
                    <TableCell>{p.category}</TableCell>
                    <TableCell className="text-right tabular-nums">{currency(p.price)}</TableCell>
                    <TableCell className="text-right tabular-nums">{p.stock}</TableCell>
                    <TableCell>{stockBadge(p)}</TableCell>
                    {!isSingleLocation && (
                      <TableCell className="text-muted-foreground">
                        {warehouseName(p.warehouseId)}
                      </TableCell>
                    )}
                    {!readOnly && (
                      <TableCell>
                        <div className="flex justify-end gap-1">
                          <Button
                            size="icon"
                            variant="ghost"
                            onClick={() => {
                              setEditing(p);
                              setDialogOpen(true);
                            }}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button size="icon" variant="ghost" onClick={() => setToDelete(p)}>
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </Card>

      {!readOnly && (
        <>
          <ProductFormDialog
            open={dialogOpen}
            onOpenChange={(o) => {
              setDialogOpen(o);
              if (!o) setEditing(null);
            }}
            initial={editing}
            warehouses={warehouses.data ?? []}
            onSubmit={handleSubmit}
            pending={createMut.isPending || updateMut.isPending}
          />

          <AlertDialog open={!!toDelete} onOpenChange={(o) => !o && setToDelete(null)}>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Delete this product?</AlertDialogTitle>
                <AlertDialogDescription>
                  {toDelete?.name} ({toDelete?.sku}) will be permanently removed from the catalog.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction
                  onClick={() => {
                    if (!toDelete) return;
                    deleteMut.mutate(toDelete.id, {
                      onSuccess: () => {
                        toast.success("Product deleted");
                        setToDelete(null);
                      },
                    });
                  }}
                >
                  Delete
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </>
      )}
    </div>
  );
}
