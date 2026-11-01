import { ApiError } from "@/services/api/client";
import { productService } from "@/services/catalog/catalog";
import type { CartItem } from "@/stores/cart/cartStore";
import { cartTotal } from "@/stores/cart/cartCalculations";
import type { Product } from "@/types/api";

export interface CartLineView {
  productId: number;
  quantity: number;
  /** Null when the product has been withdrawn since it was added. */
  product: Product | null;
}

/** Fetches current prices. A 404 means withdrawn; any other failure fails the whole load. */
export async function fetchCartProducts(
  productIds: number[],
  signal: AbortSignal,
): Promise<Map<number, Product | null>> {
  const results = await Promise.allSettled(productIds.map((id) => productService.get(id, signal)));
  const products = new Map<number, Product | null>();
  results.forEach((result, index) => {
    const id = productIds[index]!;
    if (result.status === "fulfilled") products.set(id, result.value);
    else if (result.reason instanceof ApiError && result.reason.status === 404) {
      products.set(id, null);
    } else throw result.reason;
  });
  return products;
}

export function buildCartLines(
  items: CartItem[],
  products: Map<number, Product | null>,
): CartLineView[] {
  return items.map((item) => ({ ...item, product: products.get(item.productId) ?? null }));
}

export function linesTotal(lines: CartLineView[]): number {
  return cartTotal(
    lines.flatMap((line) =>
      line.product ? [{ unitPrice: line.product.price, quantity: line.quantity }] : [],
    ),
  );
}
