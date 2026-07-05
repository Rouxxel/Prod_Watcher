import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { Search, Plus, Minus, Trash2, ShoppingCart, ScanBarcode } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useProducts } from "@/hooks/queries";
import { useCart } from "@/hooks/use-cart";
import { currency, dateTime } from "@/lib/format";
import { toast } from "sonner";

export const Route = createFileRoute("/_app/cashier")({
  component: CashierPage,
});

function CashierPage() {
  const products = useProducts();
  const cart = useCart();
  const [query, setQuery] = useState("");
  const [receiptOpen, setReceiptOpen] = useState(false);
  const [lastTotal, setLastTotal] = useState(0);
  const [lastTime, setLastTime] = useState("");

  const filtered = useMemo(() => {
    const q = query.toLowerCase().trim();
    return (products.data ?? [])
      .filter((p) => p.stock > 0)
      .filter((p) => !q || p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q));
  }, [products.data, query]);

  const checkout = () => {
    if (cart.items.length === 0) return;
    setLastTotal(cart.total);
    setLastTime(new Date().toISOString());
    setReceiptOpen(true);
  };

  return (
    <div>
      <PageHeader
        title="Cashier"
        description="Scan or search products, build the cart, and complete the sale."
        actions={
          <Button asChild variant="outline">
            <Link to="/cart">
              <ShoppingCart className="mr-2 h-4 w-4" /> View cart ({cart.items.length})
            </Link>
          </Button>
        }
      />

      <div className="grid gap-4 lg:grid-cols-[1fr_360px]">
        <Card className="p-4">
          <div className="relative mb-4">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              autoFocus
              placeholder="Scan SKU or search product…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="pl-9"
            />
          </div>

          {products.isLoading ? (
            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-24" />)}
            </div>
          ) : filtered.length === 0 ? (
            <div className="grid place-items-center py-16 text-sm text-muted-foreground">
              <ScanBarcode className="mb-2 h-8 w-8" />
              No products match.
            </div>
          ) : (
            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
              {filtered.map((p) => (
                <button
                  key={p.id}
                  onClick={() => { cart.add(p); toast.success(`Added ${p.name}`); }}
                  className="group flex flex-col gap-1 rounded-md border border-border bg-card p-3 text-left transition hover:border-primary/50 hover:shadow-[var(--shadow-soft)]"
                >
                  <div className="text-sm font-medium">{p.name}</div>
                  <div className="text-xs text-muted-foreground">{p.sku} · {p.category}</div>
                  <div className="mt-1 flex items-center justify-between">
                    <span className="text-sm font-semibold tabular-nums">{currency(p.price)}</span>
                    <Badge variant="outline" className="text-[10px]">{p.stock} in stock</Badge>
                  </div>
                </button>
              ))}
            </div>
          )}
        </Card>

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
                <li key={i.productId} className="py-3">
                  <div className="flex justify-between gap-2">
                    <div className="min-w-0">
                      <div className="truncate text-sm font-medium">{i.name}</div>
                      <div className="text-xs text-muted-foreground">{i.sku} · {currency(i.unitPrice)}</div>
                    </div>
                    <div className="text-right text-sm tabular-nums">{currency(i.qty * i.unitPrice)}</div>
                  </div>
                  <div className="mt-2 flex items-center gap-1">
                    <Button size="icon" variant="outline" className="h-7 w-7" onClick={() => cart.setQty(i.productId, i.qty - 1)}>
                      <Minus className="h-3 w-3" />
                    </Button>
                    <span className="w-8 text-center text-sm tabular-nums">{i.qty}</span>
                    <Button size="icon" variant="outline" className="h-7 w-7" onClick={() => cart.setQty(i.productId, i.qty + 1)}>
                      <Plus className="h-3 w-3" />
                    </Button>
                    <Button size="icon" variant="ghost" className="ml-auto h-7 w-7" onClick={() => cart.remove(i.productId)}>
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          )}

          <div className="mt-4 space-y-1 border-t border-border pt-3 text-sm">
            <div className="flex justify-between text-muted-foreground"><span>Subtotal</span><span className="tabular-nums">{currency(cart.subtotal)}</span></div>
            <div className="flex justify-between text-muted-foreground"><span>Tax ({Math.round(cart.taxRate * 100)}%)</span><span className="tabular-nums">{currency(cart.tax)}</span></div>
            <div className="flex justify-between pt-1 text-base font-semibold"><span>Total</span><span className="tabular-nums">{currency(cart.total)}</span></div>
          </div>

          <Button className="mt-4 w-full" disabled={cart.items.length === 0} onClick={checkout}>
            Checkout
          </Button>
        </Card>
      </div>

      <Dialog open={receiptOpen} onOpenChange={setReceiptOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Receipt</DialogTitle>
          </DialogHeader>
          <div className="space-y-2 text-sm">
            <div className="text-xs text-muted-foreground">{lastTime && dateTime(lastTime)}</div>
            <div className="rounded-md border border-border bg-muted/30 p-3 text-center">
              <div className="text-xs uppercase tracking-wide text-muted-foreground">Total charged</div>
              <div className="mt-1 text-2xl font-semibold tabular-nums">{currency(lastTotal)}</div>
            </div>
            <p className="text-xs text-muted-foreground">This is a mock receipt — no payment was actually processed.</p>
          </div>
          <DialogFooter>
            <Button onClick={() => { cart.clear(); setReceiptOpen(false); toast.success("Sale recorded (mock)"); }}>
              Done
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
