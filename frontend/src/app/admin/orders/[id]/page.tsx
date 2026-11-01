import { Suspense } from "react";
import { OrderAdminDetail } from "@/components/orders/OrderAdminDetail";
import { Skeleton } from "@/components/ui/Feedback";

// The admin guard shows nothing until it knows who is logged in, so this can't be instant.
export const instant = false;

export default function AdminOrderPage() {
  return (
    <Suspense fallback={<Skeleton className="h-96 w-full" />}>
      <OrderAdminDetail />
    </Suspense>
  );
}
