import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { clearAccessToken, getAccessToken, setAccessToken } from "@/lib/auth-token";
import { isApiError } from "@/services/api";
import { authService } from "@/services/auth.service";
import type { User } from "@/types";

interface LoginResult {
  ok: boolean;
  error?: string;
}

interface CurrentUserCtx {
  user: User | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<LoginResult>;
  logout: () => Promise<void>;
}

const Ctx = createContext<CurrentUserCtx | null>(null);
const APP_MODE_KEY = "prodwatch:app-mode";

export function CurrentUserProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (typeof window === "undefined") {
      setIsLoading(false);
      return;
    }

    const token = getAccessToken();
    if (!token) {
      setIsLoading(false);
      return;
    }

    authService
      .getMe()
      .then(setUser)
      .catch(() => {
        clearAccessToken();
        setUser(null);
      })
      .finally(() => setIsLoading(false));
  }, []);

  const login = useCallback(async (email: string, password: string): Promise<LoginResult> => {
    try {
      const session = await authService.login({
        email: email.trim(),
        password,
      });
      setAccessToken(session.accessToken);
      const me = await authService.getMe();
      setUser(me);
      if (typeof window !== "undefined") {
        window.localStorage.removeItem(APP_MODE_KEY);
      }
      return { ok: true };
    } catch (err) {
      clearAccessToken();
      setUser(null);
      if (isApiError(err)) {
        return { ok: false, error: err.message };
      }
      return { ok: false, error: "Unable to log in." };
    }
  }, []);

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } catch {
      /* best-effort */
    }
    clearAccessToken();
    setUser(null);
    if (typeof window !== "undefined") {
      window.localStorage.removeItem(APP_MODE_KEY);
    }
  }, []);

  const value = useMemo<CurrentUserCtx>(
    () => ({ user, isLoading, login, logout }),
    [user, isLoading, login, logout],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useCurrentUser() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useCurrentUser must be used inside CurrentUserProvider");
  return ctx;
}
