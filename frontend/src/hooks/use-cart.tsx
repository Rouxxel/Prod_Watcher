import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import type { CartItem, Product } from "@/types";

const TAX_RATE = 0.16;

interface CartCtx {
  items: CartItem[];
  add: (p: Product) => void;
  setQty: (productId: string, qty: number) => void;
  remove: (productId: string) => void;
  clear: () => void;
  subtotal: number;
  tax: number;
  total: number;
  taxRate: number;
}

const Ctx = createContext<CartCtx | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);

  const value = useMemo<CartCtx>(() => {
    const subtotal = items.reduce((s, i) => s + i.qty * i.unitPrice, 0);
    const tax = subtotal * TAX_RATE;
    return {
      items,
      add: (p) =>
        setItems((prev) => {
          const existing = prev.find((i) => i.productId === p.id);
          if (existing) {
            return prev.map((i) =>
              i.productId === p.id ? { ...i, qty: i.qty + 1 } : i,
            );
          }
          return [
            ...prev,
            { productId: p.id, name: p.name, sku: p.sku, qty: 1, unitPrice: p.price },
          ];
        }),
      setQty: (productId, qty) =>
        setItems((prev) =>
          prev
            .map((i) => (i.productId === productId ? { ...i, qty: Math.max(0, qty) } : i))
            .filter((i) => i.qty > 0),
        ),
      remove: (productId) =>
        setItems((prev) => prev.filter((i) => i.productId !== productId)),
      clear: () => setItems([]),
      subtotal,
      tax,
      total: subtotal + tax,
      taxRate: TAX_RATE,
    };
  }, [items]);

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useCart() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useCart must be used inside CartProvider");
  return ctx;
}
