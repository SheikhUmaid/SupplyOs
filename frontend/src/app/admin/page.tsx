import { LayoutDashboard } from "lucide-react";
import { EmptyState, Panel } from "@/components/ui/Feedback";

export default function AdminOverviewPage() {
  return (
    <Panel>
      <EmptyState
        icon={LayoutDashboard}
        title="No operational data yet"
        description="KPIs, the network map and alerts appear here once the backend is connected."
      />
    </Panel>
  );
}
