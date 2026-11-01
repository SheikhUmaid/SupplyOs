import type { Metadata } from "next";
import { Suspense } from "react";
import { OrdersAdmin } from "@/components/orders/OrdersAdmin";

export const metadata: Metadata = { title: "Orders" };

export default function AdminOrdersPage() {
  return (
    <Suspense>
      <OrdersAdmin />
    </Suspense>
  );
}
