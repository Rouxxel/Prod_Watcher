import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import logoUrl from "@/assets/prodwatch-logo.png";
import { Button } from "@/components/ui/button";
import { extractSignupConfirmationToken } from "@/lib/auth-callback";
import { isApiError } from "@/services/api";
import { authService } from "@/services/auth.service";

export const Route = createFileRoute("/confirm-email")({
  component: ConfirmEmailPage,
  validateSearch: (search: Record<string, unknown>) => ({
    token_hash: typeof search.token_hash === "string" ? search.token_hash : undefined,
    token: typeof search.token === "string" ? search.token : undefined,
    type: typeof search.type === "string" ? search.type : undefined,
  }),
});

function ConfirmEmailPage() {
  const search = Route.useSearch();
  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    const token =
      search.token_hash ??
      search.token ??
      extractSignupConfirmationToken(window.location.search, window.location.hash);

    if (!token) {
      setStatus("error");
      setMessage(
        "Missing confirmation token. Open the link from your email or request a new sign-up.",
      );
      return;
    }

    let cancelled = false;

    authService
      .confirmEmail(token)
      .then(() => {
        if (cancelled) return;
        setStatus("success");
        setMessage("Email confirmed — you can now log in with your new account.");
      })
      .catch((err) => {
        if (cancelled) return;
        setStatus("error");
        setMessage(isApiError(err) ? err.message : "Unable to confirm your email.");
      });

    return () => {
      cancelled = true;
    };
  }, [search.token, search.token_hash]);

  if (status === "loading") {
    return (
      <AuthShell title="Confirming email">
        <p className="text-sm text-muted-foreground">Confirming your account…</p>
      </AuthShell>
    );
  }

  return (
    <AuthShell title={status === "success" ? "Email confirmed" : "Confirmation failed"}>
      <div
        className={
          status === "success"
            ? "rounded-md border border-border bg-background/50 px-4 py-3 text-sm text-foreground"
            : "rounded-md border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive"
        }
      >
        {message}
      </div>
      <Button asChild className="mt-4 w-full">
        <Link to="/login">{status === "success" ? "Go to login" : "Back to login"}</Link>
      </Button>
    </AuthShell>
  );
}

function AuthShell({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-background px-4 py-10">
      <div
        aria-hidden
        className="pointer-events-none absolute left-1/2 top-[10%] h-72 w-72 -translate-x-1/2 rounded-full opacity-80 blur-[1px]"
        style={{ background: "var(--gradient-sun)" }}
      />
      <div
        className="vw-scanlines pointer-events-none absolute inset-0 opacity-[0.08]"
        aria-hidden
      />

      <div className="relative w-full max-w-md rounded-xl border border-border bg-card/80 p-6 backdrop-blur sm:p-8">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="flex h-16 w-16 items-center justify-center overflow-hidden rounded-xl bg-[oklch(0.92_0.01_20)] shadow-[var(--shadow-neon)] ring-1 ring-border">
            <img src={logoUrl} alt="ProdWatch logo" className="h-14 w-14 object-contain" />
          </div>
          <h1 className="font-display vw-text-glow mt-4 text-2xl font-semibold uppercase tracking-wide">
            {title}
          </h1>
        </div>
        {children}
      </div>
    </div>
  );
}
