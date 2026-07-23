import { clearAccessToken, getAccessToken } from "@/lib/auth-token";
import { captureApiError } from "@/lib/error-capture";

export class ApiError extends Error {
  readonly status: number;
  readonly error: string;

  constructor(status: number, error: string, detail: string) {
    super(detail);
    this.name = "ApiError";
    this.status = status;
    this.error = error;
  }
}

export function isApiError(value: unknown): value is ApiError {
  return value instanceof ApiError;
}

type RequestOptions = {
  /** Attach Authorization header (default true). Set false for public auth routes. */
  auth?: boolean;
};

function getBaseUrl(): string {
  const base = import.meta.env.VITE_API_BASE_URL;
  if (!base) {
    throw new Error("VITE_API_BASE_URL is not configured");
  }
  return base.replace(/\/$/, "");
}

function buildUrl(path: string): string {
  const normalized = path.startsWith("/") ? path : `/${path}`;
  return `${getBaseUrl()}${normalized}`;
}

async function parseErrorBody(res: Response): Promise<{ error: string; detail: string }> {
  try {
    const data = (await res.json()) as { error?: string; detail?: string };
    return {
      error: data.error ?? res.statusText,
      detail: data.detail ?? res.statusText,
    };
  } catch {
    return { error: res.statusText, detail: res.statusText };
  }
}

function redirectToLogin(): void {
  if (typeof window === "undefined") return;
  const path = window.location.pathname;
  if (path === "/login" || path === "/signup" || path === "/confirm-email") return;
  window.location.assign("/login");
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
  options: RequestOptions = {},
): Promise<T> {
  const withAuth = options.auth !== false;
  const headers: Record<string, string> = {
    Accept: "application/json",
  };

  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  if (withAuth) {
    const token = getAccessToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }

  let res: Response;
  const errorContext = { method, path };
  try {
    res = await fetch(buildUrl(path), {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    const networkError = new ApiError(0, "Network Error", "Could not reach the server");
    captureApiError(networkError, errorContext);
    throw networkError;
  }

  if (res.status === 401 && withAuth) {
    clearAccessToken();
    redirectToLogin();
  }

  if (!res.ok) {
    const { error, detail } = await parseErrorBody(res);
    const apiError = new ApiError(res.status, error, detail);
    captureApiError(apiError, errorContext);
    throw apiError;
  }

  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  if (!text) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}

export function apiGet<T>(path: string, options?: RequestOptions): Promise<T> {
  return request<T>("GET", path, undefined, options);
}

export type HealthStatus = {
  status: string;
  database?: string;
};

/**
 * Ping the public backend health endpoint (VITE_API_BASE_URL + /health).
 * Returns true only when the backend responds with a healthy status.
 * Any network error, non-2xx response, or non-"ok" status resolves to false.
 */
export async function checkBackendHealth(): Promise<boolean> {
  try {
    const health = await apiGet<HealthStatus>("/health", { auth: false });
    return health.status?.toLowerCase() === "ok";
  } catch {
    return false;
  }
}

export function apiPost<T>(path: string, body?: unknown, options?: RequestOptions): Promise<T> {
  return request<T>("POST", path, body, options);
}

export function apiPatch<T>(path: string, body?: unknown, options?: RequestOptions): Promise<T> {
  return request<T>("PATCH", path, body, options);
}

export function apiDelete(path: string, options?: RequestOptions): Promise<void> {
  return request<void>("DELETE", path, undefined, options);
}
