import type { Metadata } from "next";
import { NewProduct } from "@/components/products/NewProduct";

export const metadata: Metadata = { title: "New product" };

export default function NewProductPage() {
  return <NewProduct />;
}
