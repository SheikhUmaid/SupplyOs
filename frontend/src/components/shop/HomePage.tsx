"use client";

import Link from "next/link";
import { buttonClasses } from "@/components/ui/Button";
import { EmptyState, ErrorState, Panel } from "@/components/ui/Feedback";
import { useApiQuery } from "@/hooks/useApiQuery";
import { categoryService, productService } from "@/services/catalog/catalog";
import { ProductCard, ProductGridSkeleton } from "./ProductCard";

const FEATURED_COUNT = 8;

export function HomePage() {
  const categories = useApiQuery((signal) => categoryService.list(signal), []);
  const featured = useApiQuery(
    (signal) => productService.list({ size: FEATURED_COUNT, sort: "price,desc" }, signal),
    [],
  );

  return (
    <div className="space-y-6">
      <section className="rounded-lg bg-navy-900 px-6 py-8 text-white">
        <h1 className="text-2xl font-semibold tracking-tight">
          Electronics, delivered across Karnataka
        </h1>
        <p className="mt-1.5 max-w-xl text-sm text-navy-200">
          Every order ships from whichever of our warehouses can reach you best.
        </p>
        <Link href="/products" className={`${buttonClasses()} mt-4`}>
          Browse all products
        </Link>
      </section>

      {categories.data && categories.data.length > 0 && (
        <section aria-labelledby="categories-heading">
          <h2 id="categories-heading" className="mb-2.5 text-[15px] font-semibold text-ink">
            Shop by category
          </h2>
          <div className="flex flex-wrap gap-2">
            {categories.data.map((category) => (
              <Link
                key={category.id}
                href={`/products?categoryId=${category.id}`}
                className={buttonClasses("secondary", "sm")}
              >
                {category.name}
              </Link>
            ))}
          </div>
        </section>
      )}

      <section aria-labelledby="featured-heading">
        <h2 id="featured-heading" className="mb-2.5 text-[15px] font-semibold text-ink">
          Featured products
        </h2>
        {featured.error ? (
          <Panel>
            <ErrorState message={featured.error} onRetry={featured.reload} />
          </Panel>
        ) : !featured.data ? (
          <ProductGridSkeleton />
        ) : featured.data.content.length === 0 ? (
          <Panel>
            <EmptyState title="No products to show yet" />
          </Panel>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
            {featured.data.content.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
