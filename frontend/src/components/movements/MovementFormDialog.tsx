import { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { notify } from "@/lib/notify";
import type { Product, StockMovementInput, StockMovementType, Warehouse } from "@/types";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  products: Product[];
  warehouses: Warehouse[];
  onSubmit: (input: StockMovementInput) => void;
  pending?: boolean;
}

const TYPES: StockMovementType[] = ["IN", "OUT", "TRANSFER", "ADJUSTMENT"];

type AdjustmentDirection = "add" | "remove";

const empty = (): StockMovementInput => ({
  type: "IN",
  productId: "",
  qty: 1,
  fromWarehouseId: null,
  toWarehouseId: null,
  provider: "",
  recipient: "",
  note: null,
});

export function MovementFormDialog({
  open,
  onOpenChange,
  products,
  warehouses,
  onSubmit,
  pending,
}: Props) {
  const [form, setForm] = useState<StockMovementInput>(empty());
  const [adjustmentDirection, setAdjustmentDirection] = useState<AdjustmentDirection>("add");
  const [adjustmentWarehouseId, setAdjustmentWarehouseId] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      const defaultWarehouseId = warehouses[0]?.id ?? null;
      setForm({
        ...empty(),
        productId: products[0]?.id ?? "",
        toWarehouseId: defaultWarehouseId,
      });
      setAdjustmentDirection("add");
      setAdjustmentWarehouseId(defaultWarehouseId);
    }
  }, [open, products, warehouses]);

  const set = <K extends keyof StockMovementInput>(key: K, value: StockMovementInput[K]) =>
    setForm((f) => ({ ...f, [key]: value }));

  const validate = (): boolean => {
    if (!form.productId) {
      notify.error("Select a product");
      return false;
    }
    if (form.qty <= 0) {
      notify.error("Invalid quantity", "Quantity must be greater than 0.");
      return false;
    }
    if (form.type === "IN" && !form.toWarehouseId) {
      notify.error("Destination warehouse required for IN movements");
      return false;
    }
    if (form.type === "IN" && !form.provider?.trim()) {
      notify.error("Provider required", "Enter the supplier or company the stock came from.");
      return false;
    }
    if (form.type === "OUT" && !form.fromWarehouseId) {
      notify.error("Source warehouse required for OUT movements");
      return false;
    }
    if (form.type === "OUT" && !form.recipient?.trim()) {
      notify.error("Recipient required", "Enter the customer or company the stock is going to.");
      return false;
    }
    if (form.type === "TRANSFER") {
      if (!form.fromWarehouseId || !form.toWarehouseId) {
        notify.error("Both warehouses required for transfers");
        return false;
      }
      if (form.fromWarehouseId === form.toWarehouseId) {
        notify.error("Transfer warehouses must differ");
        return false;
      }
    }
    if (form.type === "ADJUSTMENT") {
      if (!adjustmentWarehouseId) {
        notify.error("Warehouse required for adjustments");
        return false;
      }
    }
    return true;
  };

  const showFrom = form.type === "OUT" || form.type === "TRANSFER";
  const showTo = form.type === "IN" || form.type === "TRANSFER";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Record stock movement</DialogTitle>
          <DialogDescription>
            Movements are immutable — submit a new adjustment to correct mistakes.
          </DialogDescription>
        </DialogHeader>
        <form
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            if (!validate()) return;
            onSubmit({
              ...form,
              fromWarehouseId:
                form.type === "ADJUSTMENT"
                  ? adjustmentDirection === "remove"
                    ? adjustmentWarehouseId
                    : null
                  : form.fromWarehouseId,
              toWarehouseId:
                form.type === "ADJUSTMENT"
                  ? adjustmentDirection === "add"
                    ? adjustmentWarehouseId
                    : null
                  : form.toWarehouseId,
              provider: form.type === "IN" ? form.provider?.trim() || null : null,
              recipient: form.type === "OUT" ? form.recipient?.trim() || null : null,
              note: form.note?.trim() || null,
            });
          }}
        >
          <div className="space-y-1.5">
            <Label>Type</Label>
            <Select
              value={form.type}
              onValueChange={(v) => {
                const type = v as StockMovementType;
                const defaultWarehouseId = warehouses[0]?.id ?? null;
                setForm((f) => ({
                  ...f,
                  type,
                  fromWarehouseId:
                    type === "IN" || type === "ADJUSTMENT"
                      ? null
                      : (f.fromWarehouseId ?? defaultWarehouseId),
                  toWarehouseId:
                    type === "OUT" || type === "ADJUSTMENT"
                      ? null
                      : (f.toWarehouseId ?? defaultWarehouseId),
                  provider: type === "IN" ? (f.provider ?? "") : null,
                  recipient: type === "OUT" ? (f.recipient ?? "") : null,
                }));
                if (type === "ADJUSTMENT") {
                  setAdjustmentDirection("add");
                  setAdjustmentWarehouseId(defaultWarehouseId);
                }
              }}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {TYPES.map((t) => (
                  <SelectItem key={t} value={t}>
                    {t}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label>Product</Label>
            <Select value={form.productId} onValueChange={(v) => set("productId", v)}>
              <SelectTrigger>
                <SelectValue placeholder="Select product" />
              </SelectTrigger>
              <SelectContent>
                {products.map((p) => (
                  <SelectItem key={p.id} value={p.id}>
                    {p.name} ({p.sku})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="movement-qty">Quantity</Label>
            <Input
              id="movement-qty"
              type="number"
              min={1}
              value={form.qty}
              onChange={(e) => set("qty", Number(e.target.value))}
              required
            />
          </div>
          {form.type === "IN" && (
            <div className="space-y-1.5">
              <Label htmlFor="movement-provider">Provider</Label>
              <Input
                id="movement-provider"
                value={form.provider ?? ""}
                onChange={(e) => set("provider", e.target.value)}
                placeholder="e.g. Acme Steel Co."
                required
              />
            </div>
          )}
          {form.type === "OUT" && (
            <div className="space-y-1.5">
              <Label htmlFor="movement-recipient">Recipient</Label>
              <Input
                id="movement-recipient"
                value={form.recipient ?? ""}
                onChange={(e) => set("recipient", e.target.value)}
                placeholder="e.g. BuildRight Contractors"
                required
              />
            </div>
          )}
          {form.type === "ADJUSTMENT" && (
            <>
              <div className="space-y-1.5">
                <Label>Direction</Label>
                <Select
                  value={adjustmentDirection}
                  onValueChange={(v) => setAdjustmentDirection(v as AdjustmentDirection)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="add">Add stock</SelectItem>
                    <SelectItem value="remove">Remove stock</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>Warehouse</Label>
                <Select
                  value={adjustmentWarehouseId ?? ""}
                  onValueChange={(v) => setAdjustmentWarehouseId(v || null)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select warehouse" />
                  </SelectTrigger>
                  <SelectContent>
                    {warehouses.map((w) => (
                      <SelectItem key={w.id} value={w.id}>
                        {w.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </>
          )}
          {showFrom && (
            <div className="space-y-1.5">
              <Label>From warehouse</Label>
              <Select
                value={form.fromWarehouseId ?? ""}
                onValueChange={(v) => set("fromWarehouseId", v || null)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select warehouse" />
                </SelectTrigger>
                <SelectContent>
                  {warehouses.map((w) => (
                    <SelectItem key={w.id} value={w.id}>
                      {w.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}
          {showTo && (
            <div className="space-y-1.5">
              <Label>To warehouse</Label>
              <Select
                value={form.toWarehouseId ?? ""}
                onValueChange={(v) => set("toWarehouseId", v || null)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select warehouse" />
                </SelectTrigger>
                <SelectContent>
                  {warehouses.map((w) => (
                    <SelectItem key={w.id} value={w.id}>
                      {w.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}
          <div className="space-y-1.5 pt-6">
            <Label htmlFor="movement-note">Note (optional)</Label>
            <Textarea
              id="movement-note"
              value={form.note ?? ""}
              onChange={(e) => set("note", e.target.value)}
              rows={2}
            />
          </div>
          <DialogFooter className="pt-2">
            <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={pending}>
              Record movement
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
