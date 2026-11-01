"use client";

import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { StatusBadge } from "@/components/ui/Badge";
import { Button, buttonClasses } from "@/components/ui/Button";
import { EmptyState, ErrorState, Panel, Skeleton } from "@/components/ui/Feedback";
import { useApiQuery } from "@/hooks/useApiQuery";
import { ApiError } from "@/services/api/client";
import { productService } from "@/services/catalog/catalog";
import { useCartStore } from "@/stores/cart/cartStore";
import { useUiStore } from "@/stores/ui/uiStore";
import { formatInr } from "@/utils/format";
import { AVAILABILITY } from "@/utils/status";
import { ProductImage } from "./ProductImage";
import { QuantitySelector } from "./QuantitySelector";

export function ProductDetails() {
  const id = Number(useParams<{ id: string }>().id);
  const [quantity, setQuantity] = useState(1);
  const add = useCartStore((state) => state.add);
  const pushToast = useUiStore((state) => state.pushToast);
  const product = useApiQuery(
    async (signal) => {
      try {
        return await productService.get(id, signal);
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) return null;
        throw error;
      }
    },
    [id],
  );

  if (product.error) {
    return (
      <Panel>
        <ErrorState message={product.error} onRetry={product.reload} />
      </Panel>
    );
  }
  if (product.data === undefined) return <Skeleton className="h-96 w-full" />;
  if (product.data === null) {
    return (
      <Panel>
        <EmptyState
          title="This product isn't available"
          description="It may have been withdrawn from sale."
          action={
            <Link href="/products" className={buttonClasses("secondary", "sm")}>
              Browse all products
            </Link>
          }
        />
      </Panel>
    );
  }

  const item = product.data;
  return (
    <div className="space-y-3">
      <Link
        href="/products"
        className="inline-flex items-center gap-1 text-[13px] text-ink-muted hover:text-primary"
      >
        <ArrowLeft aria-hidden className="size-3.5" />
        All products
      </Link>
      <Panel className="grid gap-6 p-4 md:grid-cols-2">
        <ProductImage src={item.imageUrl} alt={item.name} sizes="(min-width: 768px) 50vw, 100vw" />
        <div>
          <p className="text-[13px] text-ink-muted">{item.categoryName}</p>
          <h1 className="mt-0.5 text-xl font-semibold text-ink">{item.name}</h1>
          <p className="mt-0.5 font-mono text-xs text-ink-muted">SKU: {item.sku}</p>
          <p className="mt-3 text-2xl font-semibold text-ink tabular-nums">
            {formatInr(item.price)}
          </p>
          <div className="mt-2">
            <StatusBadge status={AVAILABILITY[item.availability]} />
          </div>
          {item.description && (
            <p className="mt-4 text-sm leading-relaxed text-ink-muted">{item.description}</p>
          )}
          <div className="mt-5 flex flex-wrap items-center gap-3">
            <QuantitySelector label={item.name} value={quantity} onChange={setQuantity} />
            <Button
              disabled={item.availability === "OUT"}
              onClick={() => {
                add(item.id, quantity);
                pushToast("success", `${item.name} added to your cart.`);
              }}
            >
              Add to cart
            </Button>
          </div>
        </div>
      </Panel>
    </div>
  );
}
