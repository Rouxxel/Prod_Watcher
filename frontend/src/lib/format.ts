export const currency = (n: number) =>
  new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(n);

export const dateTime = (iso: string) =>
  new Date(iso).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

export const roleLabel = (role: string) =>
  role
    .split("_")
    .map((w) => w[0].toUpperCase() + w.slice(1))
    .join(" ");

export function shortId(id: string | null | undefined) {
  if (!id) return "—";
  return id.length > 8 ? `${id.slice(0, 8)}…` : id;
}

export function auditUserLabel(userName?: string | null, userId?: string | null) {
  if (userName?.trim()) return userName.trim();
  if (userId) return shortId(userId);
  return "Deleted user";
}
