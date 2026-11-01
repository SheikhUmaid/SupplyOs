"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { EmptyState, ErrorState, Panel } from "@/components/ui/Feedback";
import { Select } from "@/components/ui/FormControls";
import { Pagination } from "@/components/ui/Pagination";
import { useApiQuery } from "@/hooks/useApiQuery";
import { categoryService, productService } from "@/services/catalog/catalog";
import { ProductCard, ProductGridSkeleton } from "./ProductCard";

const PAGE_SIZE = 12;

const SORT_OPTIONS = [
  { value: "name,asc", label: "Name" },
  { value: "price,asc", label: "Price: low to high" },
  { value: "price,desc", label: "Price: high to low" },
];

/** Filters live in the URL, so a search or category can be bookmarked or shared. */
export function Catalogue() {
  const router = useRouter();
  const params = useSearchParams();
  const q = params.get("q") ?? "";
  const categoryId = params.get("categoryId") ?? "";
  const sort = params.get("sort") ?? "name,asc";
  const page = Number(params.get("page") ?? 0);

  const categories = useApiQuery((signal) => categoryService.list(signal), []);
  const products = useApiQuery(
    (signal) =>
      productService.list(
        {
          q,
          categoryId: categoryId ? Number(categoryId) : "",
          sort,
          page,
          size: PAGE_SIZE,
        },
        signal,
      ),
    [q, categoryId, sort, page],
  );

  function setParams(changes: Record<string, string | null>) {
    const next = new URLSearchParams(params);
    if (!("page" in changes)) next.delete("page");
    for (const [key, value] of Object.entries(changes)) {
      if (value) next.set(key, value);
      else next.delete(key);
    }
    const query = next.toString();
    router.push(query ? `/products?${query}` : "/products");
  }

  const heading = q
    ? `Results for "${q}"`
    : (categories.data?.find((category) => String(category.id) === categoryId)?.name ??
      "All products");

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="text-lg font-semibold text-ink">{heading}</h1>
        <div className="flex gap-2">
          <div className="w-44">
            <Select
              label="Category"
              value={categoryId}
              onChange={(event) => setParams({ categoryId: event.target.value || null })}
            >
              <option value="">All categories</option>
              {categories.data?.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </Select>
          </div>
          <div className="w-44">
            <Select
              label="Sort by"
              value={sort}
              onChange={(event) => setParams({ sort: event.target.value })}
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Select>
          </div>
        </div>
      </div>

      {products.error ? (
        <Panel>
          <ErrorState message={products.error} onRetry={products.reload} />
        </Panel>
      ) : !products.data ? (
        <ProductGridSkeleton count={PAGE_SIZE} />
      ) : products.data.content.length === 0 ? (
        <Panel>
          <EmptyState title="No products found" description="Try a different search or category." />
        </Panel>
      ) : (
        <>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
            {products.data.content.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
          <Panel>
            <Pagination
              page={products.data.number}
              size={products.data.size}
              totalElements={products.data.totalElements}
              totalPages={products.data.totalPages}
              onPageChange={(next) => setParams({ page: String(next) })}
            />
          </Panel>
        </>
      )}
    </div>
  );
}
