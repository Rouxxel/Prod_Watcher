import type { AppMode } from "@/hooks/use-app-mode";
import type { Role } from "@/types";

/** App modes a role may use. Warehouse workers are inventory-only (no POS access). */
export function allowedModesForRole(role: Role): AppMode[] {
  switch (role) {
    case "warehouse_worker":
      return ["inventory"];
    default:
      return ["inventory", "selling"];
  }
}

export function canUseAppMode(role: Role, mode: AppMode): boolean {
  return allowedModesForRole(role).includes(mode);
}

export function defaultAppModeForRole(role: Role): AppMode {
  return allowedModesForRole(role)[0];
}

export function canSwitchAppMode(role: Role): boolean {
  return allowedModesForRole(role).length > 1;
}
