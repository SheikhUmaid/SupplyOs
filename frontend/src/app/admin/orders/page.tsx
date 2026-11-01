import type { Metadata } from "next";
import { Suspense } from "react";
import { OrdersAdmin } from "@/components/orders/OrdersAdmin";

// The admin guard shows nothing until it knows who is logged in, so this can't be instant.
export const instant = false;

export const metadata: Metadata = { title: "Orders" };

export default function AdminOrdersPage() {
  return (
    <Suspense>
      <OrdersAdmin />
    </Suspense>
  );
}
