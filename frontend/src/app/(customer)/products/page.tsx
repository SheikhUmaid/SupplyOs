import type { Metadata } from "next";
import { Suspense } from "react";
import { Catalogue } from "@/components/shop/Catalogue";
import { ProductGridSkeleton } from "@/components/shop/ProductCard";

export const metadata: Metadata = { title: "Products" };

export default function ProductsPage() {
  return (
    <Suspense fallback={<ProductGridSkeleton count={12} />}>
      <Catalogue />
    </Suspense>
  );
}
