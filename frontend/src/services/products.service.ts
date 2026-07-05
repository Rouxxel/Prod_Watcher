import { products as seed } from "@/mock/seed";
import type { Product, ProductInput } from "@/types";
import { fakeDelay, newId } from "./api";

let store: Product[] = [...seed];

export const productsService = {
  async list(): Promise<Product[]> {
    await fakeDelay();
    return [...store];
  },
  async get(id: string): Promise<Product | undefined> {
    await fakeDelay(150);
    return store.find((p) => p.id === id);
  },
  async create(input: ProductInput): Promise<Product> {
    await fakeDelay();
    const created: Product = { ...input, id: newId("p") };
    store = [created, ...store];
    return created;
  },
  async update(id: string, input: ProductInput): Promise<Product> {
    await fakeDelay();
    store = store.map((p) => (p.id === id ? { ...input, id } : p));
    return { ...input, id };
  },
  async remove(id: string): Promise<void> {
    await fakeDelay();
    store = store.filter((p) => p.id !== id);
  },
};
