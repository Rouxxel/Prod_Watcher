import { Boxes, ScanBarcode, ArrowRight } from "lucide-react";
import logoUrl from "@/assets/prodwatch-logo.png";
import { useAppMode, type AppMode } from "@/hooks/use-app-mode";
import { useCurrentUser } from "@/hooks/use-current-user";
import { Button } from "@/components/ui/button";
import { roleLabel } from "@/lib/format";
import { allowedModesForRole } from "@/lib/role-modes";

const cards: Array<{
  mode: AppMode;
  title: string;
  tagline: string;
  description: string;
  icon: typeof Boxes;
  points: string[];
}> = [
  {
    mode: "inventory",
    title: "Inventory Mode",
    tagline: "Track stock, locations, and movements.",
    description:
      "For managers, warehouse workers and inspectors. Focused on what's in stock, where it lives, and how it moves.",
    icon: Boxes,
    points: ["Products & photos", "Warehouses / single location", "Stock movements", "Inventory audit"],
  },
  {
    mode: "selling",
    title: "Selling Mode",
    tagline: "Ring up sales and manage the cart.",
    description:
      "For cashiers and the storefront. A streamlined POS view with cart, checkout, and transaction history.",
    icon: ScanBarcode,
    points: ["Cashier (POS)", "Cart", "Receipts & transactions"],
  },
];

export function ModeSelectScreen() {
  const { setMode } = useAppMode();
  const { user } = useCurrentUser();
  const visibleCards = cards.filter((c) => user && allowedModesForRole(user.role).includes(c.mode));

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-background px-4 py-10">
      {/* Vaporwave sun */}
      <div
        aria-hidden
        className="pointer-events-none absolute left-1/2 top-[12%] h-64 w-64 -translate-x-1/2 rounded-full opacity-80 blur-[1px] sm:h-80 sm:w-80"
        style={{ background: "var(--gradient-sun)" }}
      />
      <div
        aria-hidden
        className="pointer-events-none absolute left-1/2 top-[12%] h-64 w-64 -translate-x-1/2 rounded-full sm:h-80 sm:w-80"
        style={{
          background:
            "repeating-linear-gradient(to bottom, transparent 0 8px, oklch(0.145 0.005 20) 8px 12px)",
          maskImage: "radial-gradient(circle, black 60%, transparent 70%)",
        }}
      />
      {/* Perspective neon grid floor */}
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

      <div className="relative w-full max-w-4xl">
        <div className="mb-10 flex flex-col items-center text-center">
          <div className="flex h-32 w-32 items-center justify-center overflow-hidden rounded-2xl bg-[oklch(0.92_0.01_20)] shadow-[var(--shadow-neon)] ring-1 ring-border sm:h-40 sm:w-40">
            <img src={logoUrl} alt="ProdWatch logo" className="h-full w-full object-contain" />
          </div>
          <div className="font-mono-retro mt-4 text-[10px] uppercase tracking-[0.4em] text-primary-foreground/70">
            ▸ Session online
          </div>
          <h1 className="font-display vw-text-glow mt-2 text-3xl font-semibold uppercase tracking-wide sm:text-4xl">
            Welcome back, {user?.name.split(" ")[0] ?? "there"}
          </h1>
          <p className="mt-3 max-w-md text-sm text-muted-foreground">
            Choose how you want to work today. You can switch modes anytime from the top bar.
          </p>
          <p className="font-mono-retro mt-2 text-[11px] uppercase tracking-widest text-muted-foreground">
            Signed in as <span className="text-foreground/80">{user ? roleLabel(user.role) : "—"}</span>
          </p>
        </div>

        <div className={`grid gap-4 ${visibleCards.length > 1 ? "sm:grid-cols-2" : "max-w-md mx-auto"}`}>
          {visibleCards.map((c) => (
            <button
              key={c.mode}
              onClick={() => setMode(c.mode)}
              className="group relative flex flex-col rounded-xl border border-border bg-card p-6 text-left transition hover:border-primary/60 hover:bg-card/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <div
                className="absolute inset-x-0 top-0 h-1 rounded-t-xl opacity-70 transition group-hover:opacity-100"
                style={{ background: "var(--gradient-primary)" }}
              />
              <div className="flex items-center gap-3">
                <div
                  className="grid h-11 w-11 place-items-center rounded-md text-primary-foreground"
                  style={{ background: "var(--gradient-primary)" }}
                >
                  <c.icon className="h-5 w-5" />
                </div>
                <div>
                  <div className="font-display text-lg font-semibold tracking-tight">{c.title}</div>
                  <div className="text-xs text-muted-foreground">{c.tagline}</div>
                </div>
              </div>
              <p className="mt-4 text-sm text-muted-foreground">{c.description}</p>
              <ul className="mt-4 space-y-1.5 text-sm">
                {c.points.map((p) => (
                  <li key={p} className="flex items-center gap-2">
                    <span className="h-1.5 w-1.5 rounded-full bg-primary" />
                    <span className="text-foreground/85">{p}</span>
                  </li>
                ))}
              </ul>
              <div className="mt-6 flex items-center justify-end text-sm font-medium text-primary-foreground/90">
                Enter
                <ArrowRight className="ml-1.5 h-4 w-4 transition group-hover:translate-x-0.5" />
              </div>
            </button>
          ))}
        </div>

        <div className="mt-8 flex justify-center">
          <Button variant="ghost" size="sm" onClick={() => setMode(null)} className="text-muted-foreground">
            (Demo) Stay on this screen
          </Button>
        </div>
      </div>
    </div>
  );
}
