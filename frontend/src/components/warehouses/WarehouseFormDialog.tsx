import { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import type { Warehouse, WarehouseInput } from "@/types";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initial?: Warehouse | null;
  onSubmit: (input: WarehouseInput) => void;
  pending?: boolean;
}

const empty: WarehouseInput = { name: "", location: "" };

export function WarehouseFormDialog({ open, onOpenChange, initial, onSubmit, pending }: Props) {
  const [form, setForm] = useState<WarehouseInput>(empty);

  useEffect(() => {
    if (open) {
      setForm(initial ? { name: initial.name, location: initial.location } : empty);
    }
  }, [open, initial]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{initial ? "Edit warehouse" : "Add warehouse"}</DialogTitle>
        </DialogHeader>
        <form
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            onSubmit({ name: form.name.trim(), location: form.location.trim() });
          }}
        >
          <div className="space-y-1.5">
            <Label htmlFor="wh-name">Name</Label>
            <Input
              id="wh-name"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="wh-location">Location</Label>
            <Input
              id="wh-location"
              value={form.location}
              onChange={(e) => setForm((f) => ({ ...f, location: e.target.value }))}
              required
            />
          </div>
          <DialogFooter className="pt-2">
            <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={pending}>
              {initial ? "Save changes" : "Create warehouse"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
