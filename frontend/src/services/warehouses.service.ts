import type { Warehouse, WarehouseInput } from "@/types";
import { apiDelete, apiGet, apiPatch, apiPost, isApiError } from "./api";

export const warehousesService = {
  async list(): Promise<Warehouse[]> {
    return apiGet<Warehouse[]>("/warehouses");
  },

  async get(id: string): Promise<Warehouse | undefined> {
    try {
      return await apiGet<Warehouse>(`/warehouses/${id}`);
    } catch (err) {
      if (isApiError(err) && err.status === 404) return undefined;
      throw err;
    }
  },

  async create(input: WarehouseInput): Promise<Warehouse> {
    return apiPost<Warehouse>("/warehouses", input);
  },

  async update(id: string, input: WarehouseInput): Promise<Warehouse> {
    return apiPatch<Warehouse>(`/warehouses/${id}`, input);
  },

  async remove(id: string): Promise<void> {
    await apiDelete(`/warehouses/${id}`);
  },
};
