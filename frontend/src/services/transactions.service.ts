import type { Transaction, TransactionCreateInput } from "@/types";
import { apiGet, apiPost } from "./api";

export const transactionsService = {
  async list(): Promise<Transaction[]> {
    const rows = await apiGet<Transaction[]>("/transactions");
    return rows.sort((a, b) => b.timestamp.localeCompare(a.timestamp));
  },

  async create(input: TransactionCreateInput): Promise<Transaction> {
    return apiPost<Transaction>("/transactions", input);
  },
};
