import { buildCartLines, fetchCartProducts, type CartLineView } from "@/components/shop/cartLines";
import { useCartHydrated } from "@/hooks/useCartHydrated";
import { useApiQuery } from "@/hooks/useApiQuery";
import { useCartStore } from "@/stores/cart/cartStore";

export interface CartLinesState {
  lines: CartLineView[];
  /** True until the saved cart and every product's current details have loaded. */
  loading: boolean;
  error: string | null;
  reload: () => void;
}

export function useCartLines(): CartLinesState {
  const hydrated = useCartHydrated();
  const items = useCartStore((state) => state.items);
  // Depends on the IDs alone, so changing a quantity doesn't refetch prices.
  const ids = items.map((item) => item.productId);
  const products = useApiQuery(
    (signal) => fetchCartProducts(ids, signal),
    [ids.slice().sort((a, b) => a - b)],
  );

  const known = products.data;
  const allKnown = known !== undefined && ids.every((id) => known.has(id));
  return {
    lines: known
      ? buildCartLines(
          items.filter((item) => known.has(item.productId)),
          known,
        )
      : [],
    loading: !hydrated || (!allKnown && !products.error),
    error: products.error,
    reload: products.reload,
  };
}
