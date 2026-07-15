import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { Search, Plus, Minus, Trash2, ShoppingCart, ScanBarcode } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useProducts, useWarehouses } from "@/hooks/queries";
import { cartLineKey, useCart } from "@/hooks/use-cart";
import { useCurrentUser } from "@/hooks/use-current-user";
import { canUsePosCheckout } from "@/lib/role-access";
import { currency, dateTime } from "@/lib/format";
import { validation } from "@/lib/notify";
import { toast } from "sonner";
import type { Transaction } from "@/types";

export const Route = createFileRoute("/_app/cashier")({
  component: CashierPage,
});

function CashierPage() {
  const { user } = useCurrentUser();
  const readOnly = user ? !canUsePosCheckout(user.role) : false;
  const warehouses = useWarehouses();
  const [warehouseId, setWarehouseId] = useState<string>("");
  const products = useProducts(warehouseId || undefined);
  const cart = useCart();
  const [query, setQuery] = useState("");
  const [receiptOpen, setReceiptOpen] = useState(false);
  const [lastTransaction, setLastTransaction] = useState<Transaction | null>(null);

  useEffect(() => {
    if (!warehouseId && warehouses.data?.length) {
      setWarehouseId(warehouses.data[0].id);
    }
  }, [warehouseId, warehouses.data]);

  const selectedWarehouse = warehouses.data?.find((w) => w.id === warehouseId);

  const filtered = useMemo(() => {
    const q = query.toLowerCase().trim();
    return (products.data ?? [])
      .filter((p) => p.stock > 0)
      .filter((p) => !q || p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q));
  }, [products.data, query]);

  const handleAdd = (product: (typeof filtered)[number]) => {
    if (!selectedWarehouse || product.stock <= 0) return;
    const added = cart.add(product, {
      warehouseId: selectedWarehouse.id,
      warehouseName: selectedWarehouse.name,
      maxStock: product.stock,
    });
    if (added) {
      toast.success(`Added ${product.name} from ${selectedWarehouse.name}`);
    } else {
      validation.quantityExceedsStock(product.stock);
    }
  };

  const handleCheckout = async () => {
    if (cart.items.length === 0) return;
    const result = await cart.checkout();
    if (!result.ok) return;
    setLastTransaction(result.transaction);
    setReceiptOpen(true);
    toast.success("Sale completed");
  };

  return (
    <div>
      <PageHeader
        title="Cashier"
        description={
          readOnly
            ? "Read-only product catalog and pricing at each warehouse."
            : "Scan or search products, build the cart, and complete the sale."
        }
        actions={
          !readOnly ? (
            <Button asChild variant="outline">
              <Link to="/cart">
                <ShoppingCart className="mr-2 h-4 w-4" /> View cart ({cart.items.length})
              </Link>
            </Button>
          ) : undefined
        }
      />

      <div className={`grid gap-4 ${readOnly ? "" : "lg:grid-cols-[1fr_360px]"}`}>
        <Card className="p-4">
          <div className="mb-4 flex flex-wrap items-center gap-2">
            <div className="relative min-w-[220px] flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                autoFocus
                placeholder="Scan SKU or search product…"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                className="pl-9"
              />
            </div>
            <Select value={warehouseId} onValueChange={setWarehouseId}>
              <SelectTrigger className="w-[200px]">
                <SelectValue placeholder="Warehouse" />
              </SelectTrigger>
              <SelectContent>
                {warehouses.data?.map((w) => (
                  <SelectItem key={w.id} value={w.id}>
                    {w.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {selectedWarehouse && (
            <p className="mb-3 text-xs text-muted-foreground">
              {readOnly ? (
                <>Viewing stock at <span className="font-medium text-foreground">{selectedWarehouse.name}</span>.</>
              ) : (
                <>
                  Showing stock available at{" "}
                  <span className="font-medium text-foreground">{selectedWarehouse.name}</span>. Switch warehouse to
                  add items from another location.
                </>
              )}
            </p>
          )}

          {products.isLoading || warehouses.isLoading ? (
            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={i} className="h-24" />
              ))}
            </div>
          ) : filtered.length === 0 ? (
            <div className="grid place-items-center py-16 text-sm text-muted-foreground">
              <ScanBarcode className="mb-2 h-8 w-8" />
              No in-stock products match at this warehouse.
            </div>
          ) : (
            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
              {filtered.map((p) =>
                readOnly ? (
                  <div
                    key={p.id}
                    className="flex flex-col gap-1 rounded-md border border-border bg-card p-3 text-left"
                  >
                    <div className="text-sm font-medium">{p.name}</div>
                    <div className="text-xs text-muted-foreground">
                      {p.sku} · {p.category}
                    </div>
                    <div className="mt-1 flex items-center justify-between">
                      <span className="text-sm font-semibold tabular-nums">{currency(p.price)}</span>
                      <Badge variant="outline" className="text-[10px]">
                        {p.stock} in stock
                      </Badge>
                    </div>
                  </div>
                ) : (
                  <button
                    key={p.id}
                    type="button"
                    disabled={p.stock <= 0}
                    onClick={() => handleAdd(p)}
                    className="group flex flex-col gap-1 rounded-md border border-border bg-card p-3 text-left transition hover:border-primary/50 hover:shadow-[var(--shadow-soft)] disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <div className="text-sm font-medium">{p.name}</div>
                    <div className="text-xs text-muted-foreground">
                      {p.sku} · {p.category}
                    </div>
                    <div className="mt-1 flex items-center justify-between">
                      <span className="text-sm font-semibold tabular-nums">{currency(p.price)}</span>
                      <Badge variant="outline" className="text-[10px]">
                        {p.stock} in stock
                      </Badge>
                    </div>
                  </button>
                ),
              )}
            </div>
          )}
        </Card>

        {!readOnly && (
        <Card className="flex flex-col p-4">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">Cart</h2>
          {cart.items.length === 0 ? (
            <div className="grid flex-1 place-items-center py-10 text-center text-sm text-muted-foreground">
              <div>
                <ShoppingCart className="mx-auto mb-2 h-8 w-8" />
                Cart is empty
              </div>
            </div>
          ) : (
            <ul className="flex-1 divide-y divide-border">
              {cart.items.map((i) => (
                <li key={cartLineKey(i.productId, i.warehouseId)} className="py-3">
                  <div className="flex justify-between gap-2">
                    <div className="min-w-0">
                      <div className="truncate text-sm font-medium">{i.name}</div>
                      <div className="text-xs text-muted-foreground">
                        {i.sku} · {currency(i.unitPrice)}
                      </div>
                      {i.warehouseName && (
                        <div className="text-xs text-muted-foreground">{i.warehouseName}</div>
                      )}
                    </div>
                    <div className="text-right text-sm tabular-nums">
                      {currency(i.qty * i.unitPrice)}
                    </div>
                  </div>
                  <div className="mt-2 flex items-center gap-1">
                    <Button
                      size="icon"
                      variant="outline"
                      className="h-7 w-7"
                      onClick={() => cart.setQty(i.productId, i.warehouseId, i.qty - 1, i.maxStock)}
                    >
                      <Minus className="h-3 w-3" />
                    </Button>
                    <span className="w-8 text-center text-sm tabular-nums">{i.qty}</span>
                    <Button
                      size="icon"
                      variant="outline"
                      className="h-7 w-7"
                      disabled={i.qty >= (i.maxStock ?? i.qty)}
                      onClick={() => {
                        const maxStock = i.maxStock ?? i.qty;
                        if (i.qty >= maxStock) {
                          validation.quantityExceedsStock(maxStock);
                          return;
                        }
                        cart.setQty(i.productId, i.warehouseId, i.qty + 1, maxStock);
                      }}
                    >
                      <Plus className="h-3 w-3" />
                    </Button>
                    <Button
                      size="icon"
                      variant="ghost"
                      className="ml-auto h-7 w-7"
                      onClick={() => cart.remove(i.productId, i.warehouseId)}
                    >
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          )}

          <div className="mt-4 space-y-1 border-t border-border pt-3 text-sm">
            <div className="flex justify-between text-muted-foreground">
              <span>Subtotal</span>
              <span className="tabular-nums">{currency(cart.subtotal)}</span>
            </div>
            <div className="flex justify-between text-muted-foreground">
              <span>Tax ({Math.round(cart.taxRate * 100)}%)</span>
              <span className="tabular-nums">{currency(cart.tax)}</span>
            </div>
            <div className="flex justify-between pt-1 text-base font-semibold">
              <span>Total</span>
              <span className="tabular-nums">{currency(cart.total)}</span>
            </div>
          </div>

          <Button
            className="mt-4 w-full"
            disabled={cart.items.length === 0 || cart.isCheckingOut}
            onClick={handleCheckout}
          >
            {cart.isCheckingOut ? "Processing…" : "Checkout"}
          </Button>
        </Card>
        )}
      </div>

      <Dialog open={receiptOpen} onOpenChange={setReceiptOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Receipt</DialogTitle>
          </DialogHeader>
          {lastTransaction && (
            <div className="space-y-3 text-sm">
              <div className="text-xs text-muted-foreground">
                {dateTime(lastTransaction.timestamp)} · #{lastTransaction.id.slice(0, 8).toUpperCase()}
              </div>
              <ul className="divide-y divide-border rounded-md border border-border">
                {lastTransaction.items.map((item) => (
                  <li key={cartLineKey(item.productId, item.warehouseId ?? item.productId)} className="flex justify-between gap-2 px-3 py-2">
                    <span className="min-w-0 truncate">
                      {item.name} × {item.qty}
                    </span>
                    <span className="shrink-0 tabular-nums">{currency(item.qty * item.unitPrice)}</span>
                  </li>
                ))}
              </ul>
              <div className="space-y-1 text-xs text-muted-foreground">
                <div className="flex justify-between">
                  <span>Subtotal</span>
                  <span className="tabular-nums">{currency(lastTransaction.subtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Tax</span>
                  <span className="tabular-nums">{currency(lastTransaction.tax)}</span>
                </div>
              </div>
              <div className="rounded-md border border-border bg-muted/30 p-3 text-center">
                <div className="text-xs uppercase tracking-wide text-muted-foreground">Total charged</div>
                <div className="mt-1 text-2xl font-semibold tabular-nums">
                  {currency(lastTransaction.total)}
                </div>
              </div>
              <Badge variant="outline" className="uppercase">
                {lastTransaction.status}
              </Badge>
            </div>
          )}
          <DialogFooter>
            <Button onClick={() => setReceiptOpen(false)}>Done</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
