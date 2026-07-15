import {
  createContext,
  useContext,
  useMemo,
  type ReactNode,
} from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCurrentUser } from "@/hooks/use-current-user";
import { settingsService } from "@/services/settings.service";
import type { WorkspaceSettings, WorkspaceSettingsUpdate } from "@/types";

const DEFAULT_TAX_RATE = 0.16;
const DEFAULT_TAX_LABEL = "Tax";
const STALE_TIME_MS = 5 * 60 * 1000;

export const useSettings = (enabled = true) =>
  useQuery({
    queryKey: ["settings"],
    queryFn: () => settingsService.get(),
    staleTime: STALE_TIME_MS,
    enabled,
  });

export const useUpdateSettings = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: WorkspaceSettingsUpdate) => settingsService.update(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["settings"] }),
  });
};

interface SettingsCtx {
  settings: WorkspaceSettings | undefined;
  isLoading: boolean;
  taxRate: number;
  taxLabel: string;
  receiptFooter: string | null;
  receiptLogoUrl: string | null;
  businessName: string;
}

const Ctx = createContext<SettingsCtx | null>(null);

export function SettingsProvider({ children }: { children: ReactNode }) {
  const { user } = useCurrentUser();
  const { data: settings, isLoading } = useSettings(!!user);

  const value = useMemo<SettingsCtx>(
    () => ({
      settings,
      isLoading: !!user && isLoading,
      taxRate: settings?.taxRate ?? DEFAULT_TAX_RATE,
      taxLabel: settings?.taxLabel ?? DEFAULT_TAX_LABEL,
      receiptFooter: settings?.receiptFooter ?? null,
      receiptLogoUrl: settings?.receiptLogoUrl ?? null,
      businessName: settings?.businessName ?? "",
    }),
    [settings, isLoading, user],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useSettingsContext() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useSettingsContext must be used within SettingsProvider");
  return ctx;
}

export { DEFAULT_TAX_RATE, DEFAULT_TAX_LABEL };
