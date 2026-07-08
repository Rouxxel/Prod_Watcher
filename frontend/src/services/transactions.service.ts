import { transactions as seed } from "@/mock/seed";
import type { Transaction } from "@/types";
import { fakeDelay } from "./mock-utils";

export const transactionsService = {
  async list(): Promise<Transaction[]> {
    await fakeDelay();
    return [...seed].sort((a, b) => b.timestamp.localeCompare(a.timestamp));
  },
};
