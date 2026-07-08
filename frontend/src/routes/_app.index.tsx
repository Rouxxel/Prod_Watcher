import { createFileRoute } from "@tanstack/react-router";
import { Package, DollarSign, AlertTriangle, ShoppingBag, Activity } from "lucide-react";
import { StatCard } from "@/components/common/StatCard";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useProducts, useMovements, useTransactions } from "@/hooks/queries";
import { currency, dateTime } from "@/lib/format";
import { Skeleton } from "@/components/ui/skeleton";
import type { StockMovement } from "@/types";

export const Route = createFileRoute("/_app/")({
  component: DashboardPage,
});

const movementColor: Record<string, string> = {
  IN: "bg-success/15 text-success border-success/30",
  OUT: "bg-destructive/15 text-destructive border-destructive/30",
  TRANSFER: "bg-info/15 text-info border-info/30",
  ADJUSTMENT: "bg-warning/15 text-warning border-warning/30",
};

function DashboardPage() {
  const products = useProducts();
  const movements = useMovements();
  const transactions = useTransactions();

  const totalProducts = products.data?.length ?? 0;
  const stockValue = products.data?.reduce((s, p) => s + p.price * p.stock, 0) ?? 0;
  const lowStock = products.data?.filter((p) => p.stock <= p.lowStockThreshold).length ?? 0;
  const today = new Date().toDateString();
  const salesToday = transactions.data
    ?.filter((t) => t.status === "completed" && new Date(t.timestamp).toDateString() === today)
    .reduce((s, t) => s + t.total, 0) ?? 0;

  const productName = (m: StockMovement) =>
    m.productName ?? products.data?.find((p) => p.id === m.productId)?.name ?? m.productId;
  const userName = (m: StockMovement) => m.userName ?? "—";

  return (
    <div>
      <PageHeader title="Dashboard" description="At-a-glance view of inventory health and recent activity." />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total Products" value={String(totalProducts)} icon={Package} hint="across all warehouses" accent />
        <StatCard label="Stock Value" value={currency(stockValue)} icon={DollarSign} hint="current valuation" />
        <StatCard label="Low Stock Alerts" value={String(lowStock)} icon={AlertTriangle} hint="below threshold" />
        <StatCard label="Sales Today" value={currency(salesToday)} icon={ShoppingBag} hint="completed transactions" />
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="text-base">Recent stock movements</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {movements.isLoading ? (
              <div className="space-y-2">
                {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
              </div>
            ) : (
              <div className="overflow-hidden rounded-md border border-border/70">
                {/* Header */}
                <div className="grid grid-cols-[1fr_120px_80px] items-center gap-2 border-b border-border/70 bg-muted/30 px-3 py-2 font-mono-retro text-[10px] uppercase tracking-[0.2em] text-muted-foreground">
                  <div>Product</div>
                  <div className="text-center">Type</div>
                  <div className="text-right">Qty</div>
                </div>
                {/* Rows */}
                <ul className="divide-y divide-border/70 [&>li:nth-child(even)]:bg-muted/10">
                  {movements.data?.slice(0, 8).map((m) => (
                    <li
                      key={m.id}
                      className="grid grid-cols-[1fr_120px_80px] items-center gap-2 px-3 py-2.5"
                    >
                      <div className="min-w-0 border-r border-border/40 pr-2">
                        <div className="truncate text-sm font-medium">{productName(m)}</div>
                        <div className="truncate text-xs text-muted-foreground">
                          by {userName(m)} · {dateTime(m.timestamp)}
                        </div>
                      </div>
                      <div className="flex justify-center border-r border-border/40">
                        <Badge variant="outline" className={movementColor[m.type]}>{m.type}</Badge>
                      </div>
                      <div className="text-right text-sm font-semibold tabular-nums text-muted-foreground">
                        {m.qty > 0 ? `+${m.qty}` : m.qty}
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Latest transactions</CardTitle>
          </CardHeader>
          <CardContent>
            {transactions.isLoading ? (
              <div className="space-y-2">
                {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
              </div>
            ) : (
              <ul className="divide-y divide-border">
                {transactions.data?.slice(0, 5).map((t) => (
                  <li key={t.id} className="flex items-center justify-between py-3">
                    <div>
                      <div className="text-sm font-medium">#{t.id.toUpperCase()}</div>
                      <div className="text-xs text-muted-foreground">{dateTime(t.timestamp)}</div>
                    </div>
                    <div className="text-right">
                      <div className="text-sm font-semibold tabular-nums">{currency(t.total)}</div>
                      <Badge variant="outline" className="text-[10px] uppercase">{t.status}</Badge>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
