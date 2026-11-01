"use client";

import { ShoppingCart, Trash2 } from "lucide-react";
import Link from "next/link";
import { StatusBadge } from "@/components/ui/Badge";
import { Button, buttonClasses } from "@/components/ui/Button";
import { EmptyState, ErrorState, Panel, Skeleton } from "@/components/ui/Feedback";
import { useCartLines } from "@/hooks/useCartLines";
import { lineSubtotal } from "@/stores/cart/cartCalculations";
import { useCartStore } from "@/stores/cart/cartStore";
import { formatInr } from "@/utils/format";
import { AVAILABILITY } from "@/utils/status";
import { linesTotal } from "./cartLines";
import { ProductImage } from "./ProductImage";
import { QuantitySelector } from "./QuantitySelector";

export function CartView() {
  const { lines, loading, error, reload } = useCartLines();
  const setQuantity = useCartStore((state) => state.setQuantity);
  const remove = useCartStore((state) => state.remove);

  if (error) {
    return (
      <Panel>
        <ErrorState message={error} onRetry={reload} />
      </Panel>
    );
  }
  if (loading) return <Skeleton className="h-64 w-full" />;
  if (lines.length === 0) {
    return (
      <Panel>
        <EmptyState
          icon={ShoppingCart}
          title="Your cart is empty"
          action={
            <Link href="/products" className={buttonClasses("secondary", "sm")}>
              Browse products
            </Link>
          }
        />
      </Panel>
    );
  }

  const hasWithdrawn = lines.some((line) => !line.product);

  return (
    <div className="grid items-start gap-4 lg:grid-cols-[1fr_20rem]">
      <div className="space-y-3">
        <h1 className="text-lg font-semibold text-ink">Your cart</h1>
        <Panel>
          <ul className="divide-y divide-line">
            {lines.map(({ productId, quantity, product }) => (
              <li key={productId} className="flex gap-3 p-3">
                <div className="w-20 shrink-0">
                  <ProductImage
                    src={product?.imageUrl ?? null}
                    alt={product?.name ?? "Unavailable product"}
                    sizes="80px"
                  />
                </div>
                {product ? (
                  <div className="grid min-w-0 flex-1 items-start gap-3 sm:grid-cols-[1fr_auto_11rem]">
                    <div className="min-w-0">
                      <Link
                        href={`/products/${product.id}`}
                        className="text-sm font-medium text-ink hover:text-primary"
                      >
                        {product.name}
                      </Link>
                      <p className="text-[13px] text-ink-muted tabular-nums">
                        {formatInr(product.price)} each
                      </p>
                      {product.availability !== "IN_STOCK" && (
                        <div className="mt-1">
                          <StatusBadge status={AVAILABILITY[product.availability]} />
                        </div>
                      )}
                    </div>
                    <QuantitySelector
                      label={product.name}
                      value={quantity}
                      onChange={(value) => setQuantity(productId, value)}
                    />
                    <div className="flex items-center justify-end gap-2">
                      <p className="text-right text-sm font-semibold text-ink tabular-nums">
                        {formatInr(lineSubtotal({ unitPrice: product.price, quantity }))}
                      </p>
                      <Button
                        variant="ghost"
                        size="sm"
                        aria-label={`Remove ${product.name} from cart`}
                        onClick={() => remove(productId)}
                      >
                        <Trash2 aria-hidden className="size-4" />
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div className="flex flex-1 items-start justify-between gap-3">
                    <p className="text-[13px] text-danger">This product is no longer available.</p>
                    <Button variant="secondary" size="sm" onClick={() => remove(productId)}>
                      Remove
                    </Button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        </Panel>
        <Link href="/products" className="inline-block text-[13px] text-primary hover:underline">
          Continue shopping
        </Link>
      </div>

      <Panel title="Order total" className="lg:mt-10">
        <div className="space-y-3 p-4">
          <p className="flex justify-between text-sm">
            <span className="text-ink-muted">Total</span>
            <span className="text-lg font-semibold text-ink tabular-nums">
              {formatInr(linesTotal(lines))}
            </span>
          </p>
          {hasWithdrawn ? (
            <p className="text-[13px] text-danger">
              Remove unavailable products before you check out.
            </p>
          ) : (
            <Link href="/checkout" className={`${buttonClasses()} w-full`}>
              Proceed to checkout
            </Link>
          )}
        </div>
      </Panel>
    </div>
  );
}
