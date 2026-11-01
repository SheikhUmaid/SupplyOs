import type { Metadata } from "next";
import { WarehousesAdmin } from "@/components/warehouses/WarehousesAdmin";

export const metadata: Metadata = { title: "Warehouses" };

export default function AdminWarehousesPage() {
  return <WarehousesAdmin />;
}
