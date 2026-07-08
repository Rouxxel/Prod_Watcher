import { isApiError } from "@/services/api";
import { notify, validation } from "@/lib/notify";

export function toastApiError(err: unknown, fallback = "Request failed"): void {
  if (isApiError(err)) {
    if (err.status === 403) {
      validation.unauthorized();
      return;
    }
    if (err.status === 409) {
      notify.error("Stock conflict", err.message);
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
