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
