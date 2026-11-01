import type { Metadata } from "next";
import { CheckoutView } from "@/components/shop/CheckoutView";

export const metadata: Metadata = { title: "Checkout" };

export default function CheckoutPage() {
  return <CheckoutView />;
}
