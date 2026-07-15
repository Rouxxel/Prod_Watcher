import type { StockMovement, StockMovementInput } from "@/types";
import { apiGet, apiPost } from "./api";

export const movementsService = {
  async list(): Promise<StockMovement[]> {
    const rows = await apiGet<StockMovement[]>("/stock-movements");
    return rows.sort((a, b) => b.timestamp.localeCompare(a.timestamp));
  },

  async create(input: StockMovementInput): Promise<StockMovement> {
    return apiPost<StockMovement>("/stock-movements", input);
  },
};
