import type { WorkspaceSettings, WorkspaceSettingsUpdate } from "@/types";
import { apiGet, apiPatch } from "./api";

export const settingsService = {
  async get(): Promise<WorkspaceSettings> {
    return apiGet<WorkspaceSettings>("/settings");
  },

  async update(input: WorkspaceSettingsUpdate): Promise<WorkspaceSettings> {
    return apiPatch<WorkspaceSettings>("/settings", input);
  },
};
