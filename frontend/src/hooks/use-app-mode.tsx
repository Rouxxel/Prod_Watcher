import { createContext, useContext, useEffect, useState } from "react";

export type AppMode = "inventory" | "selling";

interface Ctx {
  mode: AppMode | null;
  setMode: (m: AppMode | null) => void;
}

const Ctx = createContext<Ctx | null>(null);
const KEY = "prodwatch:app-mode";

export function AppModeProvider({ children }: { children: React.ReactNode }) {
  const [mode, setModeState] = useState<AppMode | null>(null);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const stored = window.localStorage.getItem(KEY);
    if (stored === "inventory" || stored === "selling") setModeState(stored);
    setHydrated(true);
  }, []);

  const setMode = (m: AppMode | null) => {
    setModeState(m);
    if (typeof window !== "undefined") {
      if (m) window.localStorage.setItem(KEY, m);
      else window.localStorage.removeItem(KEY);
    }
  };

  return <Ctx.Provider value={{ mode: hydrated ? mode : null, setMode }}>{children}</Ctx.Provider>;
}

export function useAppMode() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useAppMode must be used within AppModeProvider");
  return ctx;
}
