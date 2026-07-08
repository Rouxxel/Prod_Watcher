import { warehouses as seed } from "@/mock/seed";
import type { Warehouse } from "@/types";
import { fakeDelay } from "./mock-utils";

export const warehousesService = {
  async list(): Promise<Warehouse[]> {
    await fakeDelay();
    return [...seed];
  },
};
