import { captureApiError, type ApiErrorContext } from "@/lib/error-capture";
import { notify, validation } from "@/lib/notify";
import { isApiError } from "@/services/api";

export function toastApiError(
  err: unknown,
  fallback = "Request failed",
  context?: ApiErrorContext,
): void {
  captureApiError(err, context);

  if (isApiError(err)) {
    if (err.status === 401) {
      if (typeof window !== "undefined") {
        const path = window.location.pathname;
        if (path !== "/login" && path !== "/signup" && path !== "/confirm-email") {
          notify.warning("Session expired", "Please sign in again.");
        }
      }
      return;
    }
    if (err.status === 403) {
      validation.unauthorized();
      return;
    }
    if (err.status === 409) {
      notify.error("Stock conflict", err.message);
      return;
    }
    if (err.status === 429) {
      validation.rateLimit(err.message);
      return;
    }
    if (err.status === 0) {
      validation.networkError();
      return;
    }
    notify.error(fallback, err.message);
    return;
  }

  if (err instanceof Error && err.message) {
    notify.error(fallback, err.message);
    return;
  }

  notify.error(fallback);
}
