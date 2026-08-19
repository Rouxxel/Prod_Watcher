import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useCurrentUser } from "@/hooks/use-current-user";
import { useSettings } from "@/hooks/use-settings";
import type { BusinessModePreference } from "@/types";

/** @deprecated Use `BusinessModePreference` from `@/types` — kept for existing imports. */
export type BusinessMode = BusinessModePreference;

interface Ctx {
  /** User preference — server-synced from workspace settings when authenticated. */
  preference: BusinessMode;
  setPreference: (v: BusinessMode) => void;
  /** Resolved effective mode for the UI, factoring auto-detection. */
  isSingleLocation: boolean;
  /** Inform the provider of the current warehouse count for "auto". */
  setDetectedWarehouseCount: (n: number) => void;
  /** True once workspace settings have been applied (or user is logged out). */
  isSynced: boolean;
}

const Ctx = createContext<Ctx | null>(null);
const CACHE_KEY = "prodwatch:business-mode";

function isBusinessMode(value: string | null): value is BusinessMode {
  return value === "auto" || value === "single" || value === "multi";
}

function readCachedMode(): BusinessMode {
  if (typeof window === "undefined") return "auto";
  const stored = window.localStorage.getItem(CACHE_KEY);
  return isBusinessMode(stored) ? stored : "auto";
}

function writeCachedMode(mode: BusinessMode) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(CACHE_KEY, mode);
}

export function BusinessModeProvider({ children }: { children: React.ReactNode }) {
  const { user } = useCurrentUser();
  const { data: settings } = useSettings(!!user);
  const [preference, setPreferenceState] = useState<BusinessMode>(readCachedMode);
  const [detected, setDetected] = useState(1);
  const [isSynced, setIsSynced] = useState(!user);

  // Server is source of truth — sync on login and after settings refresh.
  useEffect(() => {
    if (!user) {
      setIsSynced(true);
      return;
    }
    if (!settings?.businessMode) return;
    setPreferenceState(settings.businessMode);
    writeCachedMode(settings.businessMode);
    setIsSynced(true);
  }, [user, settings?.businessMode, settings?.updatedAt]);

  const setPreference = useCallback((v: BusinessMode) => {
    setPreferenceState(v);
    writeCachedMode(v);
  }, []);

  const isSingleLocation = useMemo(() => {
    if (preference === "single") return true;
    if (preference === "multi") return false;
    return detected <= 1;
  }, [preference, detected]);

  return (
    <Ctx.Provider
      value={{
        preference,
        setPreference,
        isSingleLocation,
        setDetectedWarehouseCount: setDetected,
        isSynced,
      }}
    >
      {children}
    </Ctx.Provider>
  );
}

export function useBusinessMode() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useBusinessMode must be used within BusinessModeProvider");
  return ctx;
}
