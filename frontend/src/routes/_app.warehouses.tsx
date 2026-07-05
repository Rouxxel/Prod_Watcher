import { createFileRoute } from "@tanstack/react-router";
import { useEffect } from "react";
import { Warehouse as WarehouseIcon, MapPin, Package, AlertTriangle, Store } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useProducts, useWarehouses } from "@/hooks/queries";
import { useBusinessMode } from "@/hooks/use-business-mode";
import { currency } from "@/lib/format";

export const Route = createFileRoute("/_app/warehouses")({
  component: WarehousesPage,
});

function WarehousesPage() {
  const warehouses = useWarehouses();
  const products = useProducts();
  const { isSingleLocation, setDetectedWarehouseCount } = useBusinessMode();

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

  return (
    <div>
      <PageHeader
        title={isSingleLocation ? "Location" : "Warehouses"}
        description={
          isSingleLocation
            ? "You're running in single-location mode — perfect for a small shop or studio."
            : "Stock distribution across your locations."
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
          {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-44" />)}
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {warehouses.data?.map((w) => {
            const s = stats(w.id);
            return (
              <Card key={w.id} className="overflow-hidden">
                <div className="h-1.5" style={{ background: "var(--gradient-primary)" }} />
                <CardContent className="p-5">
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <WarehouseIcon className="h-4 w-4 text-primary-foreground" />
                        <h3 className="font-semibold">{w.name}</h3>
                      </div>
                      <div className="mt-1 flex items-center gap-1 text-xs text-muted-foreground">
                        <MapPin className="h-3 w-3" /> {w.location}
                      </div>
                    </div>
                    {s.low > 0 && (
                      <div className="flex items-center gap-1 rounded-full bg-warning/15 px-2 py-0.5 text-xs text-warning">
                        <AlertTriangle className="h-3 w-3" /> {s.low} low
                      </div>
                    )}
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
    </div>
  );
}
