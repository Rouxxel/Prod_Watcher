import { createFileRoute, Link } from "@tanstack/react-router";
import { ShoppingCart, Trash2, Plus, Minus } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/common/EmptyState";
import { cartLineKey, useCart } from "@/hooks/use-cart";
import { currency } from "@/lib/format";
import { validation } from "@/lib/notify";

export const Route = createFileRoute("/_app/cart")({
  component: CartPage,
});

function CartPage() {
  const cart = useCart();

  return (
    <div>
      <PageHeader title="Cart" description="Review the active sale before checkout." />
      {cart.items.length === 0 ? (
        <EmptyState
          icon={ShoppingCart}
          title="Cart is empty"
          description="Head to the cashier screen to add products."
          action={<Button asChild><Link to="/cashier">Open cashier</Link></Button>}
        />
      ) : (
        <div className="grid gap-4 lg:grid-cols-[1fr_360px]">
          <Card className="divide-y divide-border p-2">
            {cart.items.map((i) => (
              <div key={cartLineKey(i.productId, i.warehouseId)} className="flex items-center gap-3 p-3">
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-medium">{i.name}</div>
                  <div className="text-xs text-muted-foreground">{i.sku} · {currency(i.unitPrice)}</div>
                  {i.warehouseName && (
                    <div className="text-xs text-muted-foreground">{i.warehouseName}</div>
                  )}
                </div>
                <div className="flex items-center gap-1">
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
                </div>
                <div className="w-24 text-right text-sm tabular-nums">{currency(i.qty * i.unitPrice)}</div>
                <Button
                  size="icon"
                  variant="ghost"
                  onClick={() => cart.remove(i.productId, i.warehouseId)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            ))}
          </Card>

          <Card className="p-4">
            <div className="space-y-1 text-sm">
              <div className="flex justify-between text-muted-foreground"><span>Subtotal</span><span className="tabular-nums">{currency(cart.subtotal)}</span></div>
              <div className="flex justify-between text-muted-foreground"><span>{cart.taxLineLabel}</span><span className="tabular-nums">{currency(cart.tax)}</span></div>
              <div className="flex justify-between pt-2 text-base font-semibold"><span>Total</span><span className="tabular-nums">{currency(cart.total)}</span></div>
            </div>
            <Button asChild className="mt-4 w-full">
              <Link to="/cashier">Continue to checkout</Link>
            </Button>
            <Button variant="ghost" className="mt-2 w-full" onClick={cart.clear}>Clear cart</Button>
          </Card>
        </div>
      )}
    </div>
  );
}
