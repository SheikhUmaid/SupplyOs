import type { Metadata } from "next";
import { AllocationSettingsView } from "@/components/orders/AllocationSettingsView";

// The admin guard shows nothing until it knows who is logged in, so this can't be instant.
export const instant = false;

export const metadata: Metadata = { title: "Settings" };

export default function AdminSettingsPage() {
  return <AllocationSettingsView />;
}
