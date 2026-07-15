import { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { roleLabel } from "@/lib/format";
import type { Role } from "@/types";

const STEP_DOWN_ROLES: Role[] = [
  "warehouse_manager",
  "warehouse_worker",
  "inspector",
  "cashier",
];

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (role: Role) => void;
  pending?: boolean;
}

export function StepDownAdminDialog({ open, onOpenChange, onSubmit, pending }: Props) {
  const [role, setRole] = useState<Role>("warehouse_manager");

  useEffect(() => {
    if (open) setRole("warehouse_manager");
  }, [open]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Step down as admin</DialogTitle>
          <DialogDescription>
            Choose a new role for your account. You will lose admin access immediately, including this
            users page. At least one other active admin must remain.
          </DialogDescription>
        </DialogHeader>
        <form
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            onSubmit(role);
          }}
        >
          <div className="space-y-1.5">
            <Label htmlFor="step-down-role">New role</Label>
            <Select value={role} onValueChange={(value) => setRole(value as Role)}>
              <SelectTrigger id="step-down-role">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {STEP_DOWN_ROLES.map((r) => (
                  <SelectItem key={r} value={r}>
                    {roleLabel(r)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <DialogFooter className="pt-2">
            <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="destructive" disabled={pending}>
              Confirm step down
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
