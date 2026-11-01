"use client";

import dynamic from "next/dynamic";
import { ErrorState, Panel, Skeleton } from "@/components/ui/Feedback";
import { useApiQuery } from "@/hooks/useApiQuery";
import { useAutoRefresh } from "@/hooks/useAutoRefresh";
import { visualizationService } from "@/services/visualization/visualization";
import { MapPanel } from "./MapPanel";

const REFRESH_MS = 15000;

// PixiJS needs a browser, and this keeps it out of the main bundle.
const MapCanvas = dynamic(() => import("./MapCanvas"), {
  ssr: false,
  loading: () => <Skeleton className="h-full w-full" />,
});

export function NetworkMap({ refreshKey }: { refreshKey?: unknown }) {
  const network = useApiQuery(
    (signal) => visualizationService.network(signal),
    [refreshKey ?? null],
  );
  useAutoRefresh(network.reload, REFRESH_MS);

  return (
    <Panel title="Supply chain network">
      {network.error && !network.data ? (
        <ErrorState message={network.error} onRetry={network.reload} />
      ) : (
        <div className="grid grid-cols-[minmax(0,1fr)] lg:grid-cols-[minmax(0,1fr)_17rem]">
          <div className="h-[28rem] border-b border-line lg:h-[38rem] lg:border-r lg:border-b-0">
            <MapCanvas
              network={network.data}
              label="Map of Karnataka showing warehouses, delivery cities and shipment routes. The list beside it contains the same information."
            />
          </div>
          <div className="p-3 lg:max-h-[38rem] lg:overflow-y-auto">
            {network.data ? (
              <MapPanel network={network.data} />
            ) : (
              <Skeleton className="h-64 w-full" />
            )}
          </div>
        </div>
      )}
    </Panel>
  );
}
