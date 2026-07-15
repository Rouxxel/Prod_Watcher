import type { Product, ProductInput } from "@/types";
import { apiDelete, apiGet, apiPatch, apiPost, isApiError } from "./api";

function toProductPayload(input: ProductInput) {
  const { stock: _stock, ...body } = input;
  return body;
}

export const productsService = {
  async list(warehouseId?: string): Promise<Product[]> {
    const query = warehouseId ? `?warehouseId=${encodeURIComponent(warehouseId)}` : "";
    return apiGet<Product[]>(`/products${query}`);
  },

  async get(id: string): Promise<Product | undefined> {
    try {
      return await apiGet<Product>(`/products/${id}`);
    } catch (err) {
      if (isApiError(err) && err.status === 404) return undefined;
      throw err;
    }
  },

  async create(input: ProductInput): Promise<Product> {
    return apiPost<Product>("/products", toProductPayload(input));
  },

  async update(id: string, input: ProductInput): Promise<Product> {
    return apiPatch<Product>(`/products/${id}`, toProductPayload(input));
  },

  async remove(id: string): Promise<void> {
    await apiDelete(`/products/${id}`);
  },
};
