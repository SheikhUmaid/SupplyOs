import { Suspense } from "react";
import { Skeleton } from "@/components/ui/Feedback";
import { ProductDetails } from "@/components/shop/ProductDetails";

export default function ProductPage() {
  return (
    <Suspense fallback={<Skeleton className="h-96 w-full" />}>
      <ProductDetails />
    </Suspense>
  );
}
