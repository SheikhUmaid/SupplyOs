import { Suspense } from "react";
import { OrderAdminDetail } from "@/components/orders/OrderAdminDetail";
import { Skeleton } from "@/components/ui/Feedback";

export default function AdminOrderPage() {
  return (
    <Suspense fallback={<Skeleton className="h-96 w-full" />}>
      <OrderAdminDetail />
    </Suspense>
  );
}
