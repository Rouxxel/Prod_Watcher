import { createFileRoute, Link, Navigate } from "@tanstack/react-router";
import { useEffect, useState, type FormEvent } from "react";
import logoUrl from "@/assets/prodwatch-logo.png";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PasswordInput } from "@/components/ui/password-input";
import { Label } from "@/components/ui/label";
import { useCurrentUser } from "@/hooks/use-current-user";
import { isApiError } from "@/services/api";
import { authService } from "@/services/auth.service";
import { validation } from "@/lib/notify";

export const Route = createFileRoute("/signup")({
  component: SignupPage,
});

function SignupPage() {
  const { user, isLoading } = useCurrentUser();
  const [checking, setChecking] = useState(true);
  const [allowed, setAllowed] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    authService
      .bootstrapStatus()
      .then((status) => setAllowed(status.signupAllowed))
      .catch(() => setAllowed(false))
      .finally(() => setChecking(false));
  }, []);

  if (isLoading || checking) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <p className="text-sm text-muted-foreground">Loading…</p>
      </div>
    );
  }

  if (user) return <Navigate to="/" />;

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }
    if (password.length < 8) {
      validation.passwordTooShort();
      return;
    }

    setSubmitting(true);
    try {
      const response = await authService.signup({
        name: name.trim(),
        email: email.trim(),
        password,
      });
      setSuccessMessage(response.message);
    } catch (err) {
      setError(isApiError(err) ? err.message : "Unable to create account.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-background px-4 py-10">
      <div
        aria-hidden
        className="pointer-events-none absolute left-1/2 top-[10%] h-72 w-72 -translate-x-1/2 rounded-full opacity-80 blur-[1px]"
        style={{ background: "var(--gradient-sun)" }}
      />
      <div className="vw-scanlines pointer-events-none absolute inset-0 opacity-[0.08]" aria-hidden />

      <div className="relative w-full max-w-md rounded-xl border border-border bg-card/80 p-6 backdrop-blur sm:p-8">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="flex h-16 w-16 items-center justify-center overflow-hidden rounded-xl bg-[oklch(0.92_0.01_20)] shadow-[var(--shadow-neon)] ring-1 ring-border">
            <img src={logoUrl} alt="ProdWatch logo" className="h-14 w-14 object-contain" />
          </div>
          <h1 className="font-display vw-text-glow mt-4 text-2xl font-semibold uppercase tracking-wide">
            {allowed ? "Owner sign-up" : "Sign up"}
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {allowed
              ? "Create the first admin account for this workspace."
              : "Owner registration is already complete for this workspace."}
          </p>
        </div>

        {!allowed ? (
          <div className="space-y-4 text-center">
            <div className="rounded-md border border-border bg-background/50 px-4 py-3 text-sm text-muted-foreground">
              If you need access, ask your administrator or log in with your existing account.
            </div>
            <Button asChild className="w-full">
              <Link to="/login">Back to login</Link>
            </Button>
          </div>
        ) : successMessage ? (
          <div className="space-y-4 text-center">
            <div className="rounded-md border border-border bg-background/50 px-4 py-3 text-sm text-foreground">
              {successMessage}
            </div>
            <Button asChild className="w-full">
              <Link to="/login">Back to login</Link>
            </Button>
          </div>
        ) : (
          <>
            <form className="space-y-4" onSubmit={onSubmit}>
              <div className="space-y-1.5">
                <Label htmlFor="name">Name</Label>
                <Input
                  id="name"
                  autoComplete="name"
                  placeholder="Alex Owner"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  placeholder="alex@acme.co"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="password">Password</Label>
                <PasswordInput
                  id="password"
                  autoComplete="new-password"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="confirm-password">Confirm password</Label>
                <PasswordInput
                  id="confirm-password"
                  autoComplete="new-password"
                  placeholder="••••••••"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
              </div>
              {error && (
                <div className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
                  {error}
                </div>
              )}
              <Button type="submit" className="w-full" disabled={submitting}>
                {submitting ? "Creating account…" : "Create account"}
              </Button>
            </form>

            <p className="mt-6 text-center text-sm text-muted-foreground">
              Already have an account?{" "}
              <Link to="/login" className="font-medium text-foreground underline-offset-4 hover:underline">
                Log in
              </Link>
            </p>
          </>
        )}
      </div>
    </div>
  );
}
