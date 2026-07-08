import type { AuditEntry } from "@/types";
import { apiGet } from "./api";

export const auditService = {
  async list(): Promise<AuditEntry[]> {
    const rows = await apiGet<AuditEntry[]>("/audit");
    return rows.sort((a, b) => b.timestamp.localeCompare(a.timestamp));
  },
};
