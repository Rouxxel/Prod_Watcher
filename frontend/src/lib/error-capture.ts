import { isApiError } from "@/services/api";

export type ApiErrorContext = {
  method?: string;
  path?: string;
  source?: string;
};

// ---------------------------------------------------------------------------
// SSR — recover swallowed h3 errors (server.ts)
// ---------------------------------------------------------------------------

let lastCapturedError: { error: unknown; at: number } | undefined;
const TTL_MS = 5_000;

function recordUnhandled(error: unknown) {
  lastCapturedError = { error, at: Date.now() };
}

if (typeof globalThis.addEventListener === "function") {
  globalThis.addEventListener("error", (event) =>
    recordUnhandled((event as ErrorEvent).error ?? event),
  );
  globalThis.addEventListener("unhandledrejection", (event) =>
    recordUnhandled((event as PromiseRejectionEvent).reason),
  );
}

export function consumeLastCapturedError(): unknown {
  if (!lastCapturedError) return undefined;
  if (Date.now() - lastCapturedError.at > TTL_MS) {
    lastCapturedError = undefined;
    return undefined;
  }
  const { error } = lastCapturedError;
  lastCapturedError = undefined;
  return error;
}

// ---------------------------------------------------------------------------
// Browser API — dev console logging (api.ts, toastApiError)
// ---------------------------------------------------------------------------

/** Log API failures to the console in development. No-op in production builds. */
export function captureApiError(err: unknown, context?: ApiErrorContext): void {
  if (!import.meta.env.DEV) return;

  if (isApiError(err)) {
    console.warn("[ProdWatch API]", {
      status: err.status,
      error: err.error,
      detail: err.message,
      ...context,
    });
    return;
  }

  if (err instanceof Error) {
    console.warn("[ProdWatch API]", err.message, context ?? {});
    return;
  }

  console.warn("[ProdWatch API]", err, context ?? {});
}
