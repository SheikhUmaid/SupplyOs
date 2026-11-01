import { DashboardView } from "@/components/analytics/DashboardView";

// The admin guard shows nothing until it knows who is logged in, so this can't be instant.
export const instant = false;

export default function AdminOverviewPage() {
  return <DashboardView />;
}
