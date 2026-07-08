import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Pencil, Plus, Trash2, Warehouse as WarehouseIcon, MapPin, Package, AlertTriangle, Store } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/common/EmptyState";
import { WarehouseFormDialog } from "@/components/warehouses/WarehouseFormDialog";
import {
  useCreateWarehouse,
  useDeleteWarehouse,
  useProducts,
  useUpdateWarehouse,
  useWarehouses,
} from "@/hooks/queries";
import { useBusinessMode } from "@/hooks/use-business-mode";
import { currency } from "@/lib/format";
import { toast } from "sonner";
import type { Warehouse, WarehouseInput } from "@/types";
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

export const Route = createFileRoute("/_app/warehouses")({
  component: WarehousesPage,
});

function WarehousesPage() {
  const warehouses = useWarehouses();
  const products = useProducts();
  const createMut = useCreateWarehouse();
  const updateMut = useUpdateWarehouse();
  const deleteMut = useDeleteWarehouse();
  const { isSingleLocation, setDetectedWarehouseCount } = useBusinessMode();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Warehouse | null>(null);
  const [toDelete, setToDelete] = useState<Warehouse | null>(null);

  useEffect(() => {
    if (warehouses.data) setDetectedWarehouseCount(warehouses.data.length);
  }, [warehouses.data, setDetectedWarehouseCount]);

  const stats = (warehouseId: string) => {
    const items = products.data?.filter((p) => p.warehouseId === warehouseId) ?? [];
    return {
      count: items.length,
      units: items.reduce((s, p) => s + p.stock, 0),
      value: items.reduce((s, p) => s + p.stock * p.price, 0),
      low: items.filter((p) => p.stock <= p.lowStockThreshold).length,
    };
  };

  const handleSubmit = (input: WarehouseInput) => {
    if (editing) {
      updateMut.mutate(
        { id: editing.id, input },
        {
          onSuccess: () => {
            toast.success("Warehouse updated");
            setDialogOpen(false);
            setEditing(null);
          },
        },
      );
    } else {
      createMut.mutate(input, {
        onSuccess: () => {
          toast.success("Warehouse created");
          setDialogOpen(false);
        },
      });
    }
  };

  return (
    <div>
      <PageHeader
        title={isSingleLocation ? "Location" : "Warehouses"}
        description={
          isSingleLocation
            ? "You're running in single-location mode — perfect for a small shop or studio."
            : "Stock distribution across your locations."
        }
        actions={
          <Button onClick={() => { setEditing(null); setDialogOpen(true); }}>
            <Plus className="mr-2 h-4 w-4" /> Add warehouse
          </Button>
        }
      />
      {isSingleLocation && (
        <Card className="mb-4 border-dashed">
          <CardContent className="flex items-start gap-3 p-4 text-sm">
            <Store className="mt-0.5 h-4 w-4 text-primary-foreground" />
            <div>
              <div className="font-medium">Single-location mode is active</div>
              <p className="text-muted-foreground">
                Warehouse selectors are hidden across the app. Switch to multi-location in Settings when you open
                a second site.
              </p>
            </div>
          </CardContent>
        </Card>
      )}
      {warehouses.isLoading || products.isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-44" />
          ))}
        </div>
      ) : (warehouses.data?.length ?? 0) === 0 ? (
        <EmptyState
          icon={WarehouseIcon}
          title="No warehouses"
          description="Add your first location to start tracking inventory."
          action={
            <Button onClick={() => setDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" /> Add warehouse
            </Button>
          }
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {warehouses.data?.map((w) => {
            const s = stats(w.id);
            return (
              <Card key={w.id} className="overflow-hidden">
                <div className="h-1.5" style={{ background: "var(--gradient-primary)" }} />
                <CardContent className="p-5">
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <WarehouseIcon className="h-4 w-4 shrink-0 text-primary-foreground" />
                        <h3 className="truncate font-semibold">{w.name}</h3>
                      </div>
                      <div className="mt-1 flex items-center gap-1 text-xs text-muted-foreground">
                        <MapPin className="h-3 w-3 shrink-0" /> <span className="truncate">{w.location}</span>
                      </div>
                    </div>
                    <div className="flex shrink-0 gap-1">
                      {s.low > 0 && (
                        <div className="flex items-center gap-1 rounded-full bg-warning/15 px-2 py-0.5 text-xs text-warning">
                          <AlertTriangle className="h-3 w-3" /> {s.low} low
                        </div>
                      )}
                      <Button size="icon" variant="ghost" className="h-8 w-8" onClick={() => { setEditing(w); setDialogOpen(true); }}>
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button size="icon" variant="ghost" className="h-8 w-8" onClick={() => setToDelete(w)}>
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                  <div className="mt-5 grid grid-cols-3 gap-2 text-center">
                    {[
                      { label: "SKUs", value: String(s.count) },
                      { label: "Units", value: String(s.units) },
                      { label: "Value", value: currency(s.value) },
                    ].map(({ label, value }) => {
                      const len = value.length;
                      const size =
                        len <= 4 ? "text-lg" : len <= 7 ? "text-base" : len <= 10 ? "text-sm" : "text-xs";
                      return (
                        <div key={label} className="min-w-0 px-1">
                          <div className="text-xs text-muted-foreground">{label}</div>
                          <div
                            className={`mt-1 block w-full truncate font-semibold tabular-nums ${size}`}
                            title={value}
                          >
                            {value}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                  <div className="mt-4 flex items-center gap-2 text-xs text-muted-foreground">
                    <Package className="h-3 w-3" /> active inventory
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      <WarehouseFormDialog
        open={dialogOpen}
        onOpenChange={(o) => { setDialogOpen(o); if (!o) setEditing(null); }}
        initial={editing}
        onSubmit={handleSubmit}
        pending={createMut.isPending || updateMut.isPending}
      />

      <AlertDialog open={!!toDelete} onOpenChange={(o) => !o && setToDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete {toDelete?.name}?</AlertDialogTitle>
            <AlertDialogDescription>
              Deletion is blocked if this warehouse has stock, movements, or is a product default location.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                if (!toDelete) return;
                deleteMut.mutate(toDelete.id, {
                  onSuccess: () => {
                    toast.success("Warehouse deleted");
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
    </div>
  );
}
