import { getAccessToken } from "@/lib/auth-token";

export const PRODUCT_IMAGES_BUCKET = "product-images";

const ALLOWED_MIME_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "image/gif",
]);

const MAX_BYTES = 5 * 1024 * 1024;

function supabaseUrl(): string {
  const url = import.meta.env.VITE_SUPABASE_URL;
  if (!url) {
    throw new Error("VITE_SUPABASE_URL is not configured");
  }
  return url.replace(/\/$/, "");
}

function supabaseAnonKey(): string {
  const key = import.meta.env.VITE_SUPABASE_ANON_KEY;
  if (!key) {
    throw new Error("VITE_SUPABASE_ANON_KEY is not configured");
  }
  return key;
}

/** True when Supabase Storage env vars are set (upload enabled). URL paste always works. */
export function isProductImageUploadEnabled(): boolean {
  return !!(import.meta.env.VITE_SUPABASE_URL && import.meta.env.VITE_SUPABASE_ANON_KEY);
}

export function productImagePublicUrl(objectPath: string): string {
  const normalized = objectPath.replace(/^\/+/, "");
  return `${supabaseUrl()}/storage/v1/object/public/${PRODUCT_IMAGES_BUCKET}/${normalized}`;
}

function extensionFor(file: File): string {
  const fromName = file.name.split(".").pop()?.toLowerCase();
  if (fromName && ["jpg", "jpeg", "png", "webp", "gif"].includes(fromName)) {
    return fromName === "jpeg" ? "jpg" : fromName;
  }
  switch (file.type) {
    case "image/jpeg":
      return "jpg";
    case "image/png":
      return "png";
    case "image/webp":
      return "webp";
    case "image/gif":
      return "gif";
    default:
      return "jpg";
  }
}

function sanitizePathSegment(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9-_]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 64);
}

export function buildProductImagePath(file: File, sku?: string): string {
  const ext = extensionFor(file);
  const index = Date.now();
  if (sku?.trim()) {
    return `${sanitizePathSegment(sku)}/${index}.${ext}`;
  }
  return `${crypto.randomUUID()}.${ext}`;
}

export function validateProductImageFile(file: File): string | null {
  if (!ALLOWED_MIME_TYPES.has(file.type)) {
    return "Use JPEG, PNG, WebP, or GIF images only.";
  }
  if (file.size > MAX_BYTES) {
    return "Image must be 5 MB or smaller.";
  }
  return null;
}

/**
 * Upload a product image to Supabase Storage using the logged-in user's JWT.
 * Requires VITE_SUPABASE_URL + VITE_SUPABASE_ANON_KEY and an active session token.
 */
export async function uploadProductImage(file: File, objectPath?: string): Promise<string> {
  const validationError = validateProductImageFile(file);
  if (validationError) {
    throw new Error(validationError);
  }

  const token = getAccessToken();
  if (!token) {
    throw new Error("You must be logged in to upload images.");
  }

  const path = (objectPath ?? buildProductImagePath(file)).replace(/^\/+/, "");
  const encodedPath = path
    .split("/")
    .map((segment) => encodeURIComponent(segment))
    .join("/");

  const response = await fetch(
    `${supabaseUrl()}/storage/v1/object/${PRODUCT_IMAGES_BUCKET}/${encodedPath}`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        apikey: supabaseAnonKey(),
        "Content-Type": file.type,
        "x-upsert": "true",
      },
      body: file,
    },
  );

  if (!response.ok) {
    let detail = "Upload failed";
    try {
      const body = (await response.json()) as { message?: string; error?: string };
      detail = body.message ?? body.error ?? detail;
    } catch {
      detail = (await response.text()) || detail;
    }
    throw new Error(detail);
  }

  return productImagePublicUrl(path);
}
