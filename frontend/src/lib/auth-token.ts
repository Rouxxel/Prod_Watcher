const STORAGE_KEY = "prodwatch:access-token";

/** Supabase JWT from POST /auth/login — stored for the browser session only. */
export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.sessionStorage.getItem(STORAGE_KEY);
}

export function setAccessToken(token: string): void {
  if (typeof window === "undefined") return;
  window.sessionStorage.setItem(STORAGE_KEY, token);
}

export function clearAccessToken(): void {
  if (typeof window === "undefined") return;
  window.sessionStorage.removeItem(STORAGE_KEY);
}
