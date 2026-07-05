import { auditEntries as seed } from "@/mock/seed";
import type { AuditEntry } from "@/types";
import { fakeDelay } from "./api";

export const auditService = {
  async list(): Promise<AuditEntry[]> {
    await fakeDelay();
    return [...seed].sort((a, b) => b.timestamp.localeCompare(a.timestamp));
  },
};
