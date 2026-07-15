import type { Role } from "@/types";

export function isAdminRole(role: Role): boolean {
  return role === "admin";
}

/** POS checkout and cart mutations — not for read-only oversight roles. */
export function canUsePosCheckout(role: Role): boolean {
  return role === "admin" || role === "cashier";
}

/** Read completed sales (transactions list/detail). */
export function canViewTransactions(role: Role): boolean {
  return (
    role === "admin" ||
    role === "cashier" ||
    role === "warehouse_manager" ||
    role === "inspector"
  );
}

/** Browse the cashier product catalog (add-to-cart requires canUsePosCheckout). */
export function canViewCashierCatalog(role: Role): boolean {
  return canUsePosCheckout(role) || role === "inspector";
}

/** Create/update/delete products, warehouses, and stock movements. */
export function canMutateInventory(role: Role): boolean {
  return role !== "inspector";
}

/** Admins may reset their own password or any non-admin user — not other admins. */
export function canAdminResetPassword(
  actorId: string,
  target: { id: string; role: Role },
): boolean {
  return !isAdminRole(target.role) || target.id === actorId;
}

export const ADMIN_ONLY_PATHS = ["/users", "/settings"] as const;

export function isAdminOnlyPath(path: string): boolean {
  return ADMIN_ONLY_PATHS.some((p) => path === p || path.startsWith(`${p}/`));
}
