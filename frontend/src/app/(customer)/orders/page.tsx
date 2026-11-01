import type { Metadata } from "next";
import { OrdersList } from "@/components/shop/OrdersList";

export const metadata: Metadata = { title: "My orders" };

export default function OrdersPage() {
  return <OrdersList />;
}
