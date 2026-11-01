import type { Metadata } from "next";
import { NewProduct } from "@/components/products/NewProduct";

// The admin guard shows nothing until it knows who is logged in, so this can't be instant.
export const instant = false;

export const metadata: Metadata = { title: "New product" };

export default function NewProductPage() {
  return <NewProduct />;
}
