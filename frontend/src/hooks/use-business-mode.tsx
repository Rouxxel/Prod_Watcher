import { createContext, useContext, useEffect, useMemo, useState } from "react";

export type BusinessMode = "auto" | "single" | "multi";

interface Ctx {
  /** User preference. */
  preference: BusinessMode;
  setPreference: (v: BusinessMode) => void;
  /** Resolved effective mode for the UI, factoring auto-detection. */
  isSingleLocation: boolean;
  /** Inform the provider of the current warehouse count for "auto". */
  setDetectedWarehouseCount: (n: number) => void;
}

const Ctx = createContext<Ctx | null>(null);
const KEY = "prodwatch:business-mode";

export function BusinessModeProvider({ children }: { children: React.ReactNode }) {
  const [preference, setPreferenceState] = useState<BusinessMode>("auto");
  const [detected, setDetected] = useState<number>(1);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const stored = window.localStorage.getItem(KEY) as BusinessMode | null;
    if (stored === "auto" || stored === "single" || stored === "multi") {
      setPreferenceState(stored);
    }
  }, []);

  const setPreference = (v: BusinessMode) => {
    setPreferenceState(v);
    if (typeof window !== "undefined") window.localStorage.setItem(KEY, v);
  };

  const isSingleLocation = useMemo(() => {
    if (preference === "single") return true;
    if (preference === "multi") return false;
    return detected <= 1;
  }, [preference, detected]);

  return (
    <Ctx.Provider
      value={{ preference, setPreference, isSingleLocation, setDetectedWarehouseCount: setDetected }}
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
