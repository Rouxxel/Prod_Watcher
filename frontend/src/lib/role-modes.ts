import type { AppMode } from "@/hooks/use-app-mode";
import type { Role } from "@/types";

/** App modes a role may use. Single-mode roles cannot switch in the top bar. */
export function allowedModesForRole(role: Role): AppMode[] {
  switch (role) {
    case "warehouse_worker":
      return ["inventory"];
    case "cashier":
      return ["selling"];
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

/** Landing route after login or when blocking a disallowed area. */
export function defaultPathForRole(role: Role): "/" | "/cashier" {
  return defaultAppModeForRole(role) === "selling" ? "/cashier" : "/";
}

export function canSwitchAppMode(role: Role): boolean {
  return allowedModesForRole(role).length > 1;
}
