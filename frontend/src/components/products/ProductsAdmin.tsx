"use client";

import { Plus, Tags } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { StatusBadge } from "@/components/ui/Badge";
import { Button, buttonClasses } from "@/components/ui/Button";
import { ConfirmModal } from "@/components/ui/ConfirmModal";
import { DataTable, type Column } from "@/components/ui/DataTable";
import { Panel } from "@/components/ui/Feedback";
import { Select } from "@/components/ui/FormControls";
import { Drawer } from "@/components/ui/Overlays";
import { Pagination } from "@/components/ui/Pagination";
import { SearchInput } from "@/components/ui/SearchInput";
import { useApiQuery } from "@/hooks/useApiQuery";
import { useUrlSearch } from "@/hooks/useUrlSearch";
import { ApiError } from "@/services/api/client";
import { categoryService, productService } from "@/services/catalog/catalog";
import { sortParam, useFilterStore } from "@/stores/filters/filterStore";
import { useUiStore } from "@/stores/ui/uiStore";
import type { Product, ProductRequest, RecordStatus } from "@/types/api";
import { formatDate, formatInr } from "@/utils/format";
import { AVAILABILITY, RECORD_STATUS } from "@/utils/status";
import { CategoryDrawer } from "./CategoryDrawer";
import { ProductForm } from "./ProductForm";
import { toProductFormValues, toProductRequest } from "./productForm";

const COLUMNS: Column<Product>[] = [
  {
    key: "name",
    header: "Product",
    sortable: true,
    cell: (product) => <span className="font-medium text-ink">{product.name}</span>,
  },
  {
    key: "sku",
    header: "SKU",
    sortable: true,
    cell: (product) => <span className="font-mono text-xs text-ink-muted">{product.sku}</span>,
  },
  { key: "category", header: "Category", cell: (product) => product.categoryName },
  {
    key: "price",
    header: "Price",
    align: "right",
    sortable: true,
    cell: (product) => formatInr(product.price),
  },
  {
    key: "availability",
    header: "Availability",
    cell: (product) => <StatusBadge status={AVAILABILITY[product.availability]} />,
  },
  {
    key: "status",
    header: "Status",
    cell: (product) => <StatusBadge status={RECORD_STATUS[product.status]} />,
  },
  {
    key: "createdAt",
    header: "Created",
    sortable: true,
    cell: (product) => formatDate(product.createdAt),
  },
];

export function ProductsAdmin() {
  useUrlSearch("products");
  const filters = useFilterStore((state) => state.filters.products);
  const setFilters = useFilterStore((state) => state.setFilters);
  const pushToast = useUiStore((state) => state.pushToast);

  const [selected, setSelected] = useState<Product | null>(null);
  const [confirming, setConfirming] = useState<Product | null>(null);
  const [categoriesOpen, setCategoriesOpen] = useState(false);

  const { q, categoryId, status, page, sort } = filters;
  const products = useApiQuery(
    (signal) => productService.list({ q, categoryId, status, page, sort: sortParam(sort) }, signal),
    [q, categoryId, status, page, sort],
  );
  const categories = useApiQuery((signal) => categoryService.list(signal), []);
  const categoryList = categories.data ?? [];

  async function saveEdit(product: Product, request: ProductRequest) {
    await productService.update(product.id, request);
    pushToast("success", `${request.name} saved.`);
    setSelected(null);
    products.reload();
  }

  async function reactivate(product: Product) {
    try {
      await productService.update(
        product.id,
        toProductRequest(toProductFormValues(product), "ACTIVE"),
      );
      pushToast("success", `${product.name} is active again.`);
      setSelected(null);
      products.reload();
    } catch (error) {
      pushToast("error", error instanceof ApiError ? error.message : "Something went wrong.");
    }
  }

  async function deactivate(product: Product) {
    try {
      await productService.deactivate(product.id);
      pushToast("success", `${product.name} deactivated.`);
      products.reload();
    } catch (error) {
      pushToast("error", error instanceof ApiError ? error.message : "Something went wrong.");
      throw error;
    }
  }

  return (
    <Panel>
      <div className="flex flex-wrap items-end gap-2 p-3">
        <SearchInput
          label="Search name or SKU"
          value={q}
          onChange={(value) => setFilters("products", { q: value })}
        />
        <div className="w-44">
          <Select
            label="Category"
            value={categoryId}
            onChange={(event) =>
              setFilters("products", {
                categoryId: event.target.value ? Number(event.target.value) : "",
              })
            }
          >
            <option value="">All categories</option>
            {categoryList.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </Select>
        </div>
        <div className="w-36">
          <Select
            label="Status"
            value={status}
            onChange={(event) =>
              setFilters("products", { status: event.target.value as RecordStatus | "" })
            }
          >
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </Select>
        </div>
        <div className="ml-auto flex gap-2">
          <Button variant="secondary" onClick={() => setCategoriesOpen(true)}>
            <Tags aria-hidden className="size-4" />
            Categories
          </Button>
          <Link href="/admin/products/new" className={buttonClasses()}>
            <Plus aria-hidden className="size-4" />
            Add product
          </Link>
        </div>
      </div>

      <DataTable
        caption="Products"
        columns={COLUMNS}
        rows={products.data?.content ?? []}
        rowKey={(product) => product.id}
        loading={products.loading && !products.data}
        error={products.error}
        onRetry={products.reload}
        emptyTitle="No products found"
        emptyDescription="Try different filters, or add a product."
        sort={sort}
        onSortChange={(next) => setFilters("products", { sort: next })}
        onRowClick={setSelected}
      />
      {products.data && (
        <Pagination
          page={products.data.number}
          size={products.data.size}
          totalElements={products.data.totalElements}
          totalPages={products.data.totalPages}
          onPageChange={(next) => setFilters("products", { page: next })}
        />
      )}

      <Drawer
        open={selected !== null}
        title={selected?.name ?? ""}
        onClose={() => setSelected(null)}
      >
        {selected && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-2">
              <StatusBadge status={RECORD_STATUS[selected.status]} />
              <StatusBadge status={AVAILABILITY[selected.availability]} />
            </div>
            <ProductForm
              key={selected.id}
              initial={toProductFormValues(selected)}
              categories={categoryList}
              submitLabel="Save changes"
              onSubmit={(request) => saveEdit(selected, request)}
            />
            <div className="border-t border-line pt-3">
              {selected.status === "ACTIVE" ? (
                <Button
                  variant="secondary"
                  onClick={() => {
                    setConfirming(selected);
                    setSelected(null);
                  }}
                >
                  Deactivate product
                </Button>
              ) : (
                <Button variant="secondary" onClick={() => reactivate(selected)}>
                  Reactivate product
                </Button>
              )}
            </div>
          </div>
        )}
      </Drawer>

      <ConfirmModal
        open={confirming !== null}
        title="Deactivate this product?"
        confirmLabel="Deactivate"
        danger
        onConfirm={() => deactivate(confirming!)}
        onClose={() => setConfirming(null)}
      >
        {confirming?.name} will disappear from the shop and can&apos;t be ordered. Past orders keep
        their record of it, and you can reactivate it at any time.
      </ConfirmModal>

      <CategoryDrawer
        open={categoriesOpen}
        categories={categoryList}
        onChanged={() => {
          categories.reload();
          products.reload();
        }}
        onClose={() => setCategoriesOpen(false)}
      />
    </Panel>
  );
}
