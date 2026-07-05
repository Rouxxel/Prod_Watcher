import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { users } from "@/mock/seed";
import type { Role, User } from "@/types";

interface LoginResult {
  ok: boolean;
  error?: string;
}

interface CurrentUserCtx {
  user: User | null;
  login: (email: string, password: string, role: Role) => LoginResult;
  logout: () => void;
}

const Ctx = createContext<CurrentUserCtx | null>(null);
const KEY = "prodwatch:current-user";
// Mock shared password for all seed users.
const MOCK_PASSWORD = "demo1234";

export function CurrentUserProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const raw = window.localStorage.getItem(KEY);
    if (!raw) return;
    try {
      const parsed = JSON.parse(raw) as User;
      const match = users.find((u) => u.id === parsed.id && u.active);
      if (match) setUser(match);
    } catch {
      /* ignore */
    }
  }, []);

  const value = useMemo<CurrentUserCtx>(
    () => ({
      user,
      login: (email, password, role) => {
        const normalized = email.trim().toLowerCase();
        const match = users.find((u) => u.email.toLowerCase() === normalized);
        if (!match) return { ok: false, error: "No account found for that email." };
        if (!match.active) return { ok: false, error: "This account is disabled." };
        if (password !== MOCK_PASSWORD) return { ok: false, error: "Incorrect password." };
        if (match.role !== role) {
          return { ok: false, error: "Selected role doesn't match this account." };
        }
        setUser(match);
        if (typeof window !== "undefined") {
          window.localStorage.setItem(KEY, JSON.stringify(match));
          // Force landing/mode picker after a fresh login.
          window.localStorage.removeItem("prodwatch:app-mode");
        }
        return { ok: true };
      },
      logout: () => {
        setUser(null);
        if (typeof window !== "undefined") {
          window.localStorage.removeItem(KEY);
          window.localStorage.removeItem("prodwatch:app-mode");
        }
      },
    }),
    [user],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useCurrentUser() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useCurrentUser must be used inside CurrentUserProvider");
  return ctx;
}

export const MOCK_LOGIN_PASSWORD = MOCK_PASSWORD;
