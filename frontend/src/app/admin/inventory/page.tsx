import type { Metadata } from "next";
import { Suspense } from "react";
import { InventoryAdmin } from "@/components/inventory/InventoryAdmin";

export const metadata: Metadata = { title: "Inventory" };

export default function AdminInventoryPage() {
  return (
    <Suspense>
      <InventoryAdmin />
    </Suspense>
  );
}
