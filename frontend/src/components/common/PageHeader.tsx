import type { ReactNode } from "react";

export function PageHeader({
  title,
  description,
  actions,
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3 border-b border-border/60 pb-4">
      <div>
        <div className="font-mono-retro mb-1 flex items-center gap-2 text-[10px] uppercase tracking-[0.35em] text-primary-foreground/70">
          <span className="inline-block h-1.5 w-1.5 rounded-full bg-primary shadow-[0_0_8px_oklch(0.52_0.16_18/0.8)]" />
          ProdWatch · Module
        </div>
        <h1 className="font-display text-2xl font-semibold uppercase tracking-wide vw-text-glow">
          {title}
        </h1>
        {description && <p className="mt-1 text-sm text-muted-foreground">{description}</p>}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  );
}
