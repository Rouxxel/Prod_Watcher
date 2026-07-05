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
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { Role } from "@/types";

interface InviteUserInput {
  name: string;
  email: string;
  role: Role;
  active: boolean;
}

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (input: InviteUserInput) => void;
  pending?: boolean;
}

const ROLES: { value: Role; label: string }[] = [
  { value: "admin", label: "Admin" },
  { value: "warehouse_manager", label: "Warehouse manager" },
  { value: "warehouse_worker", label: "Warehouse worker" },
  { value: "inspector", label: "Inspector" },
  { value: "cashier", label: "Cashier" },
];

const empty: InviteUserInput = {
  name: "",
  email: "",
  role: "warehouse_worker",
  active: true,
};

export function InviteUserDialog({ open, onOpenChange, onSubmit, pending }: Props) {
  const [form, setForm] = useState<InviteUserInput>(empty);

  useEffect(() => {
    if (open) setForm(empty);
  }, [open]);

  const set = <K extends keyof InviteUserInput>(key: K, value: InviteUserInput[K]) =>
    setForm((f) => ({ ...f, [key]: value }));

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Invite user</DialogTitle>
          <DialogDescription>
            Send an invite so they can join the workspace with the chosen role.
          </DialogDescription>
        </DialogHeader>
        <form
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            onSubmit(form);
          }}
        >
          <div className="space-y-1.5">
            <Label htmlFor="invite-name">Name</Label>
            <Input
              id="invite-name"
              value={form.name}
              onChange={(e) => set("name", e.target.value)}
              placeholder="Jane Doe"
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="invite-email">Email</Label>
            <Input
              id="invite-email"
              type="email"
              value={form.email}
              onChange={(e) => set("email", e.target.value)}
              placeholder="jane@company.com"
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label>Role</Label>
            <Select value={form.role} onValueChange={(v) => set("role", v as Role)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {ROLES.map((r) => (
                  <SelectItem key={r.value} value={r.value}>
                    {r.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="flex items-center justify-between rounded-md border border-border bg-muted/30 px-3 py-2">
            <div>
              <Label htmlFor="invite-active" className="cursor-pointer">
                Activate immediately
              </Label>
              <p className="text-xs text-muted-foreground">
                If off, the account will be created but disabled.
              </p>
            </div>
            <Switch
              id="invite-active"
              checked={form.active}
              onCheckedChange={(v) => set("active", v)}
            />
          </div>
          <DialogFooter className="pt-2">
            <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={pending}>
              Send invite
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
