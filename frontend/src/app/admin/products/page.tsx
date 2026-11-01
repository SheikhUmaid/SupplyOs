import type { Metadata } from "next";
import { Suspense } from "react";
import { ProductsAdmin } from "@/components/products/ProductsAdmin";

export const metadata: Metadata = { title: "Products" };

export default function AdminProductsPage() {
  return (
    <Suspense>
      <ProductsAdmin />
    </Suspense>
  );
}
