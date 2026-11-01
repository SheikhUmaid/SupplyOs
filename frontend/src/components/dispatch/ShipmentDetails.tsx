import type { ReactNode } from "react";
import { StatusBadge } from "@/components/ui/Badge";
import type { Tracking } from "@/types/api";
import { formatDateTime } from "@/utils/format";
import { SHIPMENT_STATUS } from "@/utils/status";
import { formatCalendarDate } from "./trackingDisplay";
import { TrackingTimeline } from "./TrackingTimeline";

function Fact({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <dt className="text-xs text-ink-muted">{label}</dt>
      <dd className="text-[13px] text-ink">{children}</dd>
    </div>
  );
}

/** The shipment facts and timeline, shared by the customer and administrator pages. */
export function ShipmentDetails({ tracking }: { tracking: Tracking }) {
  return (
    <div className="space-y-4">
      {tracking.shipmentStatus && (
        <dl className="grid grid-cols-2 gap-3">
          <Fact label="Shipment status">
            <StatusBadge status={SHIPMENT_STATUS[tracking.shipmentStatus]} />
          </Fact>
          <Fact label="Tracking reference">
            <span className="font-mono text-xs">{tracking.trackingReference}</span>
          </Fact>
          {tracking.warehouseName && <Fact label="Ships from">{tracking.warehouseName}</Fact>}
          {tracking.deliveryDate ? (
            <Fact label="Delivered">{formatDateTime(tracking.deliveryDate)}</Fact>
          ) : (
            tracking.estimatedDeliveryDate &&
            tracking.shipmentStatus !== "CANCELLED" && (
              <Fact label="Estimated delivery">
                {formatCalendarDate(tracking.estimatedDeliveryDate)}
              </Fact>
            )
          )}
        </dl>
      )}
      <TrackingTimeline steps={tracking.steps} />
    </div>
  );
}
