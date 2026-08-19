import { Toaster as Sonner } from "sonner";

type ToasterProps = React.ComponentProps<typeof Sonner>;

const Toaster = ({ ...props }: ToasterProps) => {
  return (
    <Sonner
      className="toaster group"
      toastOptions={{
        classNames: {
          toast:
            "group toast vw-panel group-[.toaster]:bg-card/95 group-[.toaster]:backdrop-blur group-[.toaster]:text-foreground group-[.toaster]:border-primary/40 group-[.toaster]:shadow-[var(--shadow-neon)] group-[.toaster]:font-mono-retro",
          title: "font-display uppercase tracking-wider text-sm vw-text-glow",
          description: "group-[.toast]:text-muted-foreground !font-sans",
          actionButton:
            "group-[.toast]:bg-primary group-[.toast]:text-primary-foreground group-[.toast]:shadow-[var(--shadow-neon)]",
          cancelButton: "group-[.toast]:bg-muted group-[.toast]:text-muted-foreground",
          error:
            "group-[.toaster]:!border-destructive/60 group-[.toaster]:shadow-[0_0_22px_-4px_oklch(0.6_0.22_27/0.6)]",
          success:
            "group-[.toaster]:!border-[oklch(0.65_0.16_150/0.6)] group-[.toaster]:shadow-[0_0_22px_-4px_oklch(0.65_0.16_150/0.5)]",
          warning:
            "group-[.toaster]:!border-[oklch(0.78_0.15_75/0.6)] group-[.toaster]:shadow-[0_0_22px_-4px_oklch(0.78_0.15_75/0.5)]",
          info: "group-[.toaster]:!border-[oklch(0.65_0.13_230/0.6)] group-[.toaster]:shadow-[0_0_22px_-4px_oklch(0.65_0.13_230/0.5)]",
        },
      }}
      {...props}
    />
  );
};

export { Toaster };
