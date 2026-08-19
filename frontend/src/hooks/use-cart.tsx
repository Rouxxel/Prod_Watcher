import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useSettingsContext } from "@/hooks/use-settings";
import { toastApiError } from "@/lib/api-error";
import { notify, validation } from "@/lib/notify";
import { isApiError } from "@/services/api";
import { transactionsService } from "@/services/transactions.service";
import { cartLineKey, type CartItem, type Product, type Transaction } from "@/types";

type CheckoutResult = { ok: true; transaction: Transaction } | { ok: false };

export interface CartAddContext {
  warehouseId: string;
  warehouseName: string;
  maxStock: number;
}

interface CartCtx {
  items: CartItem[];
  isCheckingOut: boolean;
  add: (p: Product, context: CartAddContext) => boolean;
  setQty: (productId: string, warehouseId: string, qty: number, maxStock?: number) => void;
  remove: (productId: string, warehouseId: string) => void;
  clear: () => void;
  checkout: () => Promise<CheckoutResult>;
  subtotal: number;
  tax: number;
  total: number;
  taxRate: number;
  taxLineLabel: string;
}

const Ctx = createContext<CartCtx | null>(null);

function roundMoney(amount: number) {
  return Math.round(amount * 100) / 100;
}

export function CartProvider({ children }: { children: ReactNode }) {
  const { taxRate, taxLabel } = useSettingsContext();
  const [items, setItems] = useState<CartItem[]>([]);
  const [isCheckingOut, setIsCheckingOut] = useState(false);
  const qc = useQueryClient();

  const subtotal = roundMoney(items.reduce((s, i) => s + i.qty * i.unitPrice, 0));
  const tax = roundMoney(subtotal * taxRate);
  const total = roundMoney(subtotal + tax);
  const taxLineLabel = `${taxLabel} (${Math.round(taxRate * 100)}%)`;

  const add = useCallback((p: Product, context: CartAddContext): boolean => {
    if (context.maxStock <= 0) return false;

    let allowed = true;
    setItems((prev) => {
      const existing = prev.find(
        (i) => i.productId === p.id && i.warehouseId === context.warehouseId,
      );
      const nextQty = (existing?.qty ?? 0) + 1;
      if (nextQty > context.maxStock) {
        allowed = false;
        return prev;
      }
      if (existing) {
        return prev.map((i) =>
          i.productId === p.id && i.warehouseId === context.warehouseId
            ? { ...i, qty: i.qty + 1, maxStock: context.maxStock }
            : i,
        );
      }
      return [
        ...prev,
        {
          productId: p.id,
          name: p.name,
          sku: p.sku,
          qty: 1,
          unitPrice: p.price,
          warehouseId: context.warehouseId,
          warehouseName: context.warehouseName,
          maxStock: context.maxStock,
        },
      ];
    });
    return allowed;
  }, []);

  const setQty = useCallback(
    (productId: string, warehouseId: string, qty: number, maxStock?: number) => {
      setItems((prev) => {
        let nextQty = Math.max(0, qty);
        if (maxStock !== undefined) {
          nextQty = Math.min(nextQty, maxStock);
        }
        return prev
          .map((i) =>
            i.productId === productId && i.warehouseId === warehouseId
              ? { ...i, qty: nextQty, maxStock: maxStock ?? i.maxStock }
              : i,
          )
          .filter((i) => i.qty > 0);
      });
    },
    [],
  );

  const remove = useCallback((productId: string, warehouseId: string) => {
    setItems((prev) =>
      prev.filter((i) => !(i.productId === productId && i.warehouseId === warehouseId)),
    );
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
      taxRate,
      taxLineLabel,
    }),
    [
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
      taxRate,
      taxLineLabel,
    ],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useCart() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useCart must be used inside CartProvider");
  return ctx;
}

export { cartLineKey };
