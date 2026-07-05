import { stockMovements as seed } from "@/mock/seed";
import type { StockMovement } from "@/types";
import { fakeDelay } from "./api";

export const movementsService = {
  async list(): Promise<StockMovement[]> {
    await fakeDelay();
    return [...seed].sort((a, b) => b.timestamp.localeCompare(a.timestamp));
  },
};
