import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { dateTime } from "@/lib/format";
import type { Product, StockMovement, Warehouse } from "@/types";

interface Props {
  movement: StockMovement | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  products: Product[];
  warehouses: Warehouse[];
}

const typeColor: Record<string, string> = {
  IN: "bg-success/15 text-success border-success/30",
  OUT: "bg-destructive/15 text-destructive border-destructive/30",
  TRANSFER: "bg-info/15 text-info border-info/30",
  ADJUSTMENT: "bg-warning/15 text-warning border-warning/30",
};

function DetailField({
  label,
  children,
  className,
}: {
  label: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={className}>
      <dt className="text-xs font-medium text-muted-foreground">{label}</dt>
      <dd className="mt-1 text-sm">{children}</dd>
    </div>
  );
}

export function MovementDetailDialog({
  movement,
  open,
  onOpenChange,
  products,
  warehouses,
}: Props) {
  if (!movement) return null;

  const productName =
    movement.productName ??
    products.find((p) => p.id === movement.productId)?.name ??
    movement.productId;
  const whName = (id?: string) =>
    id ? warehouses.find((w) => w.id === id)?.name ?? "—" : "—";
  const fromLabel =
    movement.type === "IN" ? movement.provider ?? "—" : whName(movement.fromWarehouseId);
  const toLabel =
    movement.type === "OUT" ? movement.recipient ?? "—" : whName(movement.toWarehouseId);
  const userName = movement.userName ?? "—";
  const qtyDisplay = movement.qty > 0 ? `+${movement.qty}` : String(movement.qty);
  const note = movement.note?.trim();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Stock movement</DialogTitle>
        </DialogHeader>
        <dl className="relative z-[1] grid grid-cols-2 gap-x-4 gap-y-4">
          <DetailField label="Product">{productName}</DetailField>
          <DetailField label="Type">
            <Badge variant="outline" className={typeColor[movement.type]}>
              {movement.type}
            </Badge>
          </DetailField>
          <DetailField label="Qty">
            <span className="tabular-nums">{qtyDisplay}</span>
          </DetailField>
          <DetailField label="From">{fromLabel}</DetailField>
          <DetailField label="To">{toLabel}</DetailField>
          <DetailField label="User">{userName}</DetailField>
          <DetailField label="When" className="col-span-2">
            {dateTime(movement.timestamp)}
          </DetailField>
          {note && (
            <DetailField label="Note" className="col-span-2">
              <p className="whitespace-pre-wrap text-muted-foreground">{note}</p>
            </DetailField>
          )}
        </dl>
        <DialogFooter>
          <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
