"use client";

import { useRouter } from "next/navigation";
import { ErrorState, Panel, Skeleton } from "@/components/ui/Feedback";
import { useApiQuery } from "@/hooks/useApiQuery";
import { categoryService, productService } from "@/services/catalog/catalog";
import { useUiStore } from "@/stores/ui/uiStore";
import { ProductForm } from "./ProductForm";
import { EMPTY_PRODUCT } from "./productForm";

export function NewProduct() {
  const router = useRouter();
  const pushToast = useUiStore((state) => state.pushToast);
  const categories = useApiQuery((signal) => categoryService.list(signal), []);

  return (
    <Panel className="mx-auto max-w-2xl p-4">
      {categories.error ? (
        <ErrorState message={categories.error} onRetry={categories.reload} />
      ) : !categories.data ? (
        <Skeleton className="h-64 w-full" />
      ) : (
        <ProductForm
          initial={EMPTY_PRODUCT}
          categories={categories.data}
          submitLabel="Add product"
          onCancel={() => router.push("/admin/products")}
          onSubmit={async (request) => {
            await productService.create(request);
            pushToast("success", `${request.name} added.`);
            router.push("/admin/products");
          }}
        />
      )}
    </Panel>
  );
}
