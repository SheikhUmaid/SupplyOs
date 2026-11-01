import type { Metadata } from "next";
import { Suspense } from "react";
import { ProductsAdmin } from "@/components/products/ProductsAdmin";

// The admin guard shows nothing until it knows who is logged in, so this can't be instant.
export const instant = false;

export const metadata: Metadata = { title: "Products" };

export default function AdminProductsPage() {
  return (
    <Suspense>
      <ProductsAdmin />
    </Suspense>
  );
}
