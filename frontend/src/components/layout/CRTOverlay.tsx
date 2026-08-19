// Subtle animated CRT overlay — fixed full-viewport, ignores pointer events.
// Combines: moving scanlines, gentle flicker, vignette, and a slow sweep beam.
export function CRTOverlay() {
  return (
    <div
      aria-hidden
      className="pointer-events-none fixed inset-0 z-[60] overflow-hidden mix-blend-screen"
    >
      {/* Static scanlines (very subtle) */}
      <div className="absolute inset-0 vw-scanlines opacity-[0.18]" />
      {/* Animated scanlines (slow drift) */}
      <div className="absolute inset-0 crt-scanlines-anim opacity-[0.12]" />
      {/* Slow sweep beam, like a CRT refresh */}
      <div className="absolute inset-x-0 -top-1/3 h-1/3 crt-sweep" />
      {/* Vignette */}
      <div
        className="absolute inset-0"
        style={{
          background:
            "radial-gradient(ellipse at center, transparent 55%, oklch(0 0 0 / 0.45) 100%)",
        }}
      />
      {/* Flicker layer */}
      <div className="absolute inset-0 crt-flicker bg-[oklch(0.52_0.16_18/0.05)]" />
    </div>
  );
}
