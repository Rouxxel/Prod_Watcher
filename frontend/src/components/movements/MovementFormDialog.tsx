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

const empty = (): StockMovementInput => ({
  type: "IN",
  productId: "",
  qty: 1,
  fromWarehouseId: null,
  toWarehouseId: null,
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

  useEffect(() => {
    if (open) {
      setForm({
        ...empty(),
        productId: products[0]?.id ?? "",
        toWarehouseId: warehouses[0]?.id ?? null,
      });
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
    if (form.type === "OUT" && !form.fromWarehouseId) {
      notify.error("Source warehouse required for OUT movements");
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
      const hasFrom = !!form.fromWarehouseId;
      const hasTo = !!form.toWarehouseId;
      if (hasFrom === hasTo) {
        notify.error("Adjustment requires exactly one warehouse (source or destination)");
        return false;
      }
    }
    return true;
  };

  const showFrom = form.type === "OUT" || form.type === "TRANSFER" || form.type === "ADJUSTMENT";
  const showTo = form.type === "IN" || form.type === "TRANSFER" || form.type === "ADJUSTMENT";

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
                setForm((f) => ({
                  ...f,
                  type,
                  fromWarehouseId: type === "IN" ? null : f.fromWarehouseId ?? warehouses[0]?.id ?? null,
                  toWarehouseId: type === "OUT" ? null : f.toWarehouseId ?? warehouses[0]?.id ?? null,
                }));
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
          <div className="space-y-1.5">
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
