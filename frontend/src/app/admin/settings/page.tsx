import type { Metadata } from "next";
import { AllocationSettingsView } from "@/components/orders/AllocationSettingsView";

export const metadata: Metadata = { title: "Settings" };

export default function AdminSettingsPage() {
  return <AllocationSettingsView />;
}
