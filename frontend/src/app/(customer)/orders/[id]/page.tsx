import { Suspense } from "react";
import { Skeleton } from "@/components/ui/Feedback";
import { OrderDetails } from "@/components/shop/OrderDetails";

export default function OrderPage() {
  return (
    <Suspense fallback={<Skeleton className="h-96 w-full" />}>
      <OrderDetails />
    </Suspense>
  );
}
