import { useState } from "react";
import { ChevronLeft, ChevronRight, ImageIcon } from "lucide-react";
import { cn } from "@/lib/utils";

interface Props {
  images: string[];
  alt: string;
  className?: string;
  rounded?: string;
  aspect?: string;
  showCounter?: boolean;
}

export function ProductImageCarousel({
  images,
  alt,
  className,
  rounded = "rounded-md",
  aspect = "aspect-square",
  showCounter = true,
}: Props) {
  const [i, setI] = useState(0);
  const count = images.length;

  if (count === 0) {
    return (
      <div
        className={cn(
          "relative grid place-items-center overflow-hidden border border-border bg-muted/40",
          aspect,
          rounded,
          className,
        )}
      >
        <ImageIcon className="h-6 w-6 text-muted-foreground/60" aria-hidden />
        <span className="sr-only">No image</span>
      </div>
    );
  }

  const prev = (e: React.MouseEvent) => {
    e.stopPropagation();
    setI((v) => (v - 1 + count) % count);
  };
  const next = (e: React.MouseEvent) => {
    e.stopPropagation();
    setI((v) => (v + 1) % count);
  };

  return (
    <div
      className={cn(
        "group relative overflow-hidden border border-border bg-muted/40",
        aspect,
        rounded,
        className,
      )}
    >
      <div
        className="flex h-full w-full transition-transform duration-300 ease-out"
        style={{ transform: `translateX(-${i * 100}%)` }}
      >
        {images.map((src, idx) => (
          <img
            key={idx}
            src={src}
            alt={`${alt} — image ${idx + 1}`}
            loading="lazy"
            draggable={false}
            className="h-full w-full shrink-0 object-cover"
          />
        ))}
      </div>

      {count > 1 && (
        <>
          <button
            type="button"
            onClick={prev}
            aria-label="Previous image"
            className="absolute left-1 top-1/2 -translate-y-1/2 grid h-6 w-6 place-items-center rounded-full bg-background/70 text-foreground opacity-0 transition group-hover:opacity-100 hover:bg-background"
          >
            <ChevronLeft className="h-3.5 w-3.5" />
          </button>
          <button
            type="button"
            onClick={next}
            aria-label="Next image"
            className="absolute right-1 top-1/2 -translate-y-1/2 grid h-6 w-6 place-items-center rounded-full bg-background/70 text-foreground opacity-0 transition group-hover:opacity-100 hover:bg-background"
          >
            <ChevronRight className="h-3.5 w-3.5" />
          </button>

          <div className="pointer-events-none absolute bottom-1 left-1/2 flex -translate-x-1/2 items-center gap-1">
            {images.map((_, idx) => (
              <span
                key={idx}
                className={cn(
                  "h-1 w-1 rounded-full transition-all",
                  idx === i ? "w-3 bg-primary-foreground" : "bg-primary-foreground/40",
                )}
              />
            ))}
          </div>

          {showCounter && (
            <div className="pointer-events-none absolute right-1 top-1 rounded bg-background/70 px-1.5 py-0.5 text-[10px] font-medium tabular-nums">
              {i + 1}/{count}
            </div>
          )}
        </>
      )}
    </div>
  );
}
