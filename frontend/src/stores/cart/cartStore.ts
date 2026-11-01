import { create } from "zustand";
import { persist } from "zustand/middleware";
import { clampQuantity } from "./cartCalculations";

export interface CartItem {
  productId: number;
  quantity: number;
}

interface CartState {
  items: CartItem[];
  add: (productId: number, quantity?: number) => void;
  setQuantity: (productId: number, quantity: number) => void;
  remove: (productId: number) => void;
  clear: () => void;
}

/** Holds only IDs and quantities. Prices and stock are server data and are always fetched. */
export const useCartStore = create<CartState>()(
  persist(
    (set) => ({
      items: [],
      add: (productId, quantity = 1) =>
        set(({ items }) => {
          const existing = items.find((item) => item.productId === productId);
          if (!existing) {
            return { items: [...items, { productId, quantity: clampQuantity(quantity) }] };
          }
          return {
            items: items.map((item) =>
              item === existing
                ? { productId, quantity: clampQuantity(item.quantity + quantity) }
                : item,
            ),
          };
        }),
      setQuantity: (productId, quantity) =>
        set(({ items }) => ({
          items: items.map((item) =>
            item.productId === productId ? { productId, quantity: clampQuantity(quantity) } : item,
          ),
        })),
      remove: (productId) =>
        set(({ items }) => ({ items: items.filter((item) => item.productId !== productId) })),
      clear: () => set({ items: [] }),
    }),
    { name: "scos-cart", skipHydration: true },
  ),
);
