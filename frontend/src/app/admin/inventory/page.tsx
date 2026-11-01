import type { Metadata } from "next";
import { Suspense } from "react";
import { InventoryAdmin } from "@/components/inventory/InventoryAdmin";

// The admin guard shows nothing until it knows who is logged in, so this can't be instant.
export const instant = false;

export const metadata: Metadata = { title: "Inventory" };

export default function AdminInventoryPage() {
  return (
    <Suspense>
      <InventoryAdmin />
    </Suspense>
  );
}
