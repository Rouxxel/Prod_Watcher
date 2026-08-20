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
import { PasswordInput } from "@/components/ui/password-input";
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
import { notify, validation } from "@/lib/notify";
import type { Role, UserProvisionInput } from "@/types";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (input: UserProvisionInput) => void;
  pending?: boolean;
}

const ROLES: { value: Role; label: string }[] = [
  { value: "warehouse_manager", label: "Warehouse manager" },
  { value: "warehouse_worker", label: "Warehouse worker" },
  { value: "inspector", label: "Inspector" },
  { value: "cashier", label: "Cashier" },
];

const empty: UserProvisionInput = {
  name: "",
  email: "",
  password: "",
  role: "warehouse_worker",
  active: true,
};

export function ProvisionUserDialog({ open, onOpenChange, onSubmit, pending }: Props) {
  const [form, setForm] = useState<UserProvisionInput>(empty);
  const [confirmPassword, setConfirmPassword] = useState("");

  useEffect(() => {
    if (open) {
      setForm(empty);
      setConfirmPassword("");
    }
  }, [open]);

  const set = <K extends keyof UserProvisionInput>(key: K, value: UserProvisionInput[K]) =>
    setForm((f) => ({ ...f, [key]: value }));

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Provision user</DialogTitle>
          <DialogDescription>
            Create a confirmed account — the employee can log in immediately with the password you
            set.
          </DialogDescription>
        </DialogHeader>
        <form
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            if (form.password !== confirmPassword) {
              notify.error("Passwords do not match");
              return;
            }
            if (form.password.length < 8) {
              validation.passwordTooShort();
              return;
            }
            onSubmit({
              ...form,
              name: form.name.trim(),
              email: form.email.trim(),
            });
          }}
        >
          <div className="space-y-1.5">
            <Label htmlFor="provision-name">Name</Label>
            <Input
              id="provision-name"
              value={form.name}
              onChange={(e) => set("name", e.target.value)}
              placeholder="Jane Doe"
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="provision-email">Email</Label>
            <Input
              id="provision-email"
              type="email"
              value={form.email}
              onChange={(e) => set("email", e.target.value)}
              placeholder="jane@company.com"
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="provision-password">Password</Label>
            <PasswordInput
              id="provision-password"
              autoComplete="new-password"
              value={form.password}
              onChange={(e) => set("password", e.target.value)}
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="provision-confirm">Confirm password</Label>
            <PasswordInput
              id="provision-confirm"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
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
              <Label htmlFor="provision-active" className="cursor-pointer">
                Active
              </Label>
              <p className="text-xs text-muted-foreground">
                Inactive users cannot log in until reactivated.
              </p>
            </div>
            <Switch
              id="provision-active"
              checked={form.active}
              onCheckedChange={(v) => set("active", v)}
            />
          </div>
          <DialogFooter className="pt-2">
            <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={pending}>
              Create user
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
