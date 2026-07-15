import { getAccessToken } from "@/lib/auth-token";

export const PRODUCT_IMAGES_BUCKET = "product-images";

const ALLOWED_MIME_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "image/gif",
]);

const MAX_BYTES = 5 * 1024 * 1024;

const UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

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

export interface ProductImagePathOptions {
  ecosystemId: string;
  /** Set when editing an existing product; omitted on create uses draft-{sku|uuid} */
  productId?: string;
  sku?: string;
}

/**
 * Object path under product-images bucket.
 * Must match storage RLS (V26): first segment = ecosystem_id.
 */
export function buildProductImagePath(file: File, options: ProductImagePathOptions): string {
  const ecosystemId = options.ecosystemId.trim();
  if (!UUID_RE.test(ecosystemId)) {
    throw new Error("Invalid workspace id for image upload.");
  }

  const ext = extensionFor(file);
  const filename = `${Date.now()}.${ext}`;

  if (options.productId?.trim()) {
    const productId = options.productId.trim();
    if (!UUID_RE.test(productId)) {
      throw new Error("Invalid product id for image upload.");
    }
    return `${ecosystemId}/${productId}/${filename}`;
  }

  const draftKey = options.sku?.trim()
    ? `draft-${sanitizePathSegment(options.sku)}`
    : `draft-${crypto.randomUUID()}`;
  return `${ecosystemId}/${draftKey}/${filename}`;
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
 * Path must start with the user's ecosystem_id (enforced by storage RLS V26).
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

  const path = (objectPath ?? "").replace(/^\/+/, "");
  if (!path) {
    throw new Error("Image path is required.");
  }

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
