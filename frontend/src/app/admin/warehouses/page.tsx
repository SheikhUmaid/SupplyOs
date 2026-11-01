import type { Metadata } from "next";
import { WarehousesAdmin } from "@/components/warehouses/WarehousesAdmin";

// The admin guard shows nothing until it knows who is logged in, so this can't be instant.
export const instant = false;

export const metadata: Metadata = { title: "Warehouses" };

export default function AdminWarehousesPage() {
  return <WarehousesAdmin />;
}
