"use client";

import Link from "next/link";
import { StatusBadge } from "@/components/ui/Badge";
import { Button, buttonClasses } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Feedback";
import { useCartStore } from "@/stores/cart/cartStore";
import { useUiStore } from "@/stores/ui/uiStore";
import type { Product } from "@/types/api";
import { formatInr } from "@/utils/format";
import { AVAILABILITY } from "@/utils/status";
import { ProductImage } from "./ProductImage";

export function ProductCard({ product }: { product: Product }) {
  const add = useCartStore((state) => state.add);
  const pushToast = useUiStore((state) => state.pushToast);

  return (
    <article className="flex flex-col rounded-lg border border-line bg-surface p-3">
      <ProductImage
        src={product.imageUrl}
        alt={product.name}
        sizes="(min-width: 1024px) 25vw, (min-width: 640px) 33vw, 50vw"
      />
      <p className="mt-2.5 text-xs text-ink-muted">{product.categoryName}</p>
      <h3 className="text-sm font-medium text-ink">{product.name}</h3>
      <p className="mt-1 text-[15px] font-semibold text-ink tabular-nums">
        {formatInr(product.price)}
      </p>
      <div className="mt-1.5 mb-3">
        <StatusBadge status={AVAILABILITY[product.availability]} />
      </div>
      <div className="mt-auto grid gap-1.5 xl:grid-cols-2">
        <Link
          href={`/products/${product.id}`}
          aria-label={`View details of ${product.name}`}
          className={buttonClasses("secondary", "sm")}
        >
          View details
        </Link>
        <Button
          size="sm"
          aria-label={`Add ${product.name} to cart`}
          disabled={product.availability === "OUT"}
          onClick={() => {
            add(product.id);
            pushToast("success", `${product.name} added to your cart.`);
          }}
        >
          Add to cart
        </Button>
      </div>
    </article>
  );
}

export function ProductGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <div
      role="status"
      aria-label="Loading products"
      className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4"
    >
      {Array.from({ length: count }, (_, index) => (
        <Skeleton key={index} className="aspect-[3/4] w-full" />
      ))}
    </div>
  );
}
