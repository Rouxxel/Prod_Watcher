import type { LucideIcon } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export function StatCard({
  label,
  value,
  icon: Icon,
  hint,
  accent,
}: {
  label: string;
  value: string;
  icon: LucideIcon;
  hint?: string;
  accent?: boolean;
}) {
  // Auto-fit value font size based on length so long numbers stay readable without truncation.
  const len = value.length;
  const valueSize =
    len <= 6
      ? "text-3xl"
      : len <= 9
        ? "text-2xl"
        : len <= 12
          ? "text-xl"
          : len <= 16
            ? "text-lg"
            : "text-base";

  return (
    <Card
      className={cn(
        "relative overflow-hidden border-border",
        accent ? "border-primary/40 vw-border-neon" : "hover:border-primary/30 transition-colors",
      )}
      style={accent ? { background: "var(--gradient-primary)" } : undefined}
    >
      {accent && (
        <div
          aria-hidden
          className="vw-grid pointer-events-none absolute inset-0 opacity-25"
          style={{ maskImage: "linear-gradient(to bottom right, black, transparent 70%)" }}
        />
      )}
      <CardContent className="relative flex min-h-[148px] flex-col justify-between gap-3 p-5">
        {/* Top row: (label + value column) ──── (icon top-right) */}
        <div className="flex items-start gap-3">
          <div className="flex min-w-0 flex-1 flex-col gap-2">
            <p
              className={cn(
                "font-mono-retro text-[10px] uppercase tracking-[0.25em]",
                accent ? "text-primary-foreground/75" : "text-muted-foreground",
              )}
            >
              {label}
            </p>
            <p
              className={cn(
                "font-display block w-full font-semibold tabular-nums leading-tight break-all",
                valueSize,
                accent ? "text-primary-foreground vw-text-glow" : "",
              )}
              title={value}
            >
              {value}
            </p>
          </div>

          <div
            className={cn(
              "grid h-10 w-10 shrink-0 place-items-center rounded-md",
              accent ? "bg-background/15 text-primary-foreground" : "bg-primary/15 text-primary-foreground",
            )}
          >
            <Icon className="h-5 w-5" />
          </div>
        </div>

        {/* Bottom: hint */}
        <p
          className={cn(
            "text-xs",
            accent ? "text-primary-foreground/70" : "text-muted-foreground",
            !hint && "invisible",
          )}
        >
          {hint ?? "—"}
        </p>
      </CardContent>
    </Card>
  );
}
