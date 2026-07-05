import { createFileRoute, useNavigate, Navigate } from "@tanstack/react-router";
import { useState, type FormEvent } from "react";
import logoUrl from "@/assets/prodwatch-logo.png";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { MOCK_LOGIN_PASSWORD, useCurrentUser } from "@/hooks/use-current-user";
import { roleLabel } from "@/lib/format";
import { users } from "@/mock/seed";
import type { Role } from "@/types";

export const Route = createFileRoute("/login")({
  component: LoginPage,
});

const ROLES: Role[] = ["admin", "warehouse_manager", "warehouse_worker", "inspector", "cashier"];

function LoginPage() {
  const { user, login } = useCurrentUser();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<Role>("admin");
  const [error, setError] = useState<string | null>(null);

  if (user) return <Navigate to="/" />;

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    const result = login(email, password, role);
    if (!result.ok) {
      setError(result.error ?? "Unable to Log in.");
      return;
    }
    navigate({ to: "/" });
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-background px-4 py-10">
      <div
        aria-hidden
        className="pointer-events-none absolute left-1/2 top-[10%] h-72 w-72 -translate-x-1/2 rounded-full opacity-80 blur-[1px]"
        style={{ background: "var(--gradient-sun)" }}
      />
      <div
        aria-hidden
        className="vw-grid pointer-events-none absolute inset-x-0 bottom-0 h-[55%]"
        style={{
          transform: "perspective(600px) rotateX(60deg)",
          transformOrigin: "center top",
          maskImage: "linear-gradient(to bottom, transparent 0%, black 35%, transparent 100%)",
        }}
      />
      <div className="vw-scanlines pointer-events-none absolute inset-0 opacity-[0.08]" aria-hidden />

      <div className="relative w-full max-w-md rounded-xl border border-border bg-card/80 p-6 backdrop-blur sm:p-8">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="flex h-16 w-16 items-center justify-center overflow-hidden rounded-xl bg-[oklch(0.92_0.01_20)] shadow-[var(--shadow-neon)] ring-1 ring-border">
            <img src={logoUrl} alt="ProdWatch logo" className="h-14 w-14 object-contain" />
          </div>
          <h1 className="font-display vw-text-glow mt-4 text-2xl font-semibold uppercase tracking-wide">
            Log in
          </h1>
          <p className="font-mono-retro mt-1 text-[10px] uppercase tracking-[0.3em] text-muted-foreground">
            ProdWatch · Inventory · POS
          </p>
        </div>

        <form className="space-y-4" onSubmit={onSubmit}>
          <div className="space-y-1.5">
            <Label htmlFor="role">Role</Label>
            <Select value={role} onValueChange={(v) => setRole(v as Role)}>
              <SelectTrigger id="role" className="h-10">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {ROLES.map((r) => (
                  <SelectItem key={r} value={r}>
                    {roleLabel(r)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="you@acme.co"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          {error && (
            <div className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
              {error}
            </div>
          )}
          <Button type="submit" className="w-full">
            Log in
          </Button>
        </form>

        <div className="mt-6 rounded-md border border-border bg-background/50 p-3 text-xs text-muted-foreground">
          <div className="font-mono-retro mb-1 text-[10px] uppercase tracking-[0.25em] text-foreground/80">
            Demo accounts · password{" "}
            <span className="text-primary-foreground">{MOCK_LOGIN_PASSWORD}</span>
          </div>
          <ul className="space-y-0.5">
            {users
              .filter((u) => u.active)
              .map((u) => (
                <li key={u.id} className="flex justify-between gap-2">
                  <span className="truncate">{u.email}</span>
                  <span className="text-foreground/60">{roleLabel(u.role)}</span>
                </li>
              ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
