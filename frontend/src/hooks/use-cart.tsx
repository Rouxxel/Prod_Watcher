import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toastApiError } from "@/lib/api-error";
import { notify, validation } from "@/lib/notify";
import { isApiError } from "@/services/api";
import { transactionsService } from "@/services/transactions.service";
import type { CartItem, Product, Transaction } from "@/types";

const TAX_RATE = 0.16;

type CheckoutResult =
  | { ok: true; transaction: Transaction }
  | { ok: false };

interface CartCtx {
  items: CartItem[];
  isCheckingOut: boolean;
  add: (p: Product) => boolean;
  setQty: (productId: string, qty: number, maxStock?: number) => void;
  remove: (productId: string) => void;
  clear: () => void;
  checkout: () => Promise<CheckoutResult>;
  subtotal: number;
  tax: number;
  total: number;
  taxRate: number;
}

const Ctx = createContext<CartCtx | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);
  const [isCheckingOut, setIsCheckingOut] = useState(false);
  const qc = useQueryClient();

  const subtotal = items.reduce((s, i) => s + i.qty * i.unitPrice, 0);
  const tax = subtotal * TAX_RATE;
  const total = subtotal + tax;

  const add = useCallback((p: Product): boolean => {
    if (p.stock <= 0) return false;

    let allowed = true;
    setItems((prev) => {
      const existing = prev.find((i) => i.productId === p.id);
      const nextQty = (existing?.qty ?? 0) + 1;
      if (nextQty > p.stock) {
        allowed = false;
        return prev;
      }
      if (existing) {
        return prev.map((i) =>
          i.productId === p.id ? { ...i, qty: i.qty + 1 } : i,
        );
      }
      return [
        ...prev,
        { productId: p.id, name: p.name, sku: p.sku, qty: 1, unitPrice: p.price },
      ];
    });
    return allowed;
  }, []);

  const setQty = useCallback((productId: string, qty: number, maxStock?: number) => {
    setItems((prev) => {
      let nextQty = Math.max(0, qty);
      if (maxStock !== undefined) {
        nextQty = Math.min(nextQty, maxStock);
      }
      return prev
        .map((i) => (i.productId === productId ? { ...i, qty: nextQty } : i))
        .filter((i) => i.qty > 0);
    });
  }, []);

  const remove = useCallback((productId: string) => {
    setItems((prev) => prev.filter((i) => i.productId !== productId));
  }, []);

  const clear = useCallback(() => setItems([]), []);

  const checkout = useCallback(async (): Promise<CheckoutResult> => {
    if (items.length === 0) return { ok: false };

    setIsCheckingOut(true);
    try {
      const transaction = await transactionsService.create({
        items,
        subtotal,
        tax,
        total,
      });
      await Promise.all([
        qc.invalidateQueries({ queryKey: ["products"] }),
        qc.invalidateQueries({ queryKey: ["transactions"] }),
        qc.invalidateQueries({ queryKey: ["movements"] }),
      ]);
      setItems([]);
      return { ok: true, transaction };
    } catch (err) {
      if (isApiError(err) && err.status === 409) {
        notify.error("Not enough stock", err.message);
      } else {
        toastApiError(err, "Checkout failed");
      }
      return { ok: false };
    } finally {
      setIsCheckingOut(false);
    }
  }, [items, subtotal, tax, total, qc]);

  const value = useMemo<CartCtx>(
    () => ({
      items,
      isCheckingOut,
      add,
      setQty,
      remove,
      clear,
      checkout,
      subtotal,
      tax,
      total,
      taxRate: TAX_RATE,
    }),
    [items, isCheckingOut, add, setQty, remove, clear, checkout, subtotal, tax, total],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useCart() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useCart must be used inside CartProvider");
  return ctx;
}
