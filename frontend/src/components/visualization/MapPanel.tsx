"use client";

import { X } from "lucide-react";
import Link from "next/link";
import { formatCalendarDate } from "@/components/dispatch/trackingDisplay";
import { StatusBadge } from "@/components/ui/Badge";
import { Skeleton } from "@/components/ui/Feedback";
import { useApiQuery } from "@/hooks/useApiQuery";
import { cn } from "@/lib/cn";
import { inventoryService } from "@/services/inventory/inventory";
import { useVisualizationStore } from "@/stores/warehouse/visualizationStore";
import type { Network, NetworkShipment, NetworkWarehouse } from "@/types/api";
import { formatCount } from "@/utils/format";
import { RECORD_STATUS, SHIPMENT_STATUS } from "@/utils/status";

const LEGEND = [
  { colour: "bg-primary", square: true, label: "Warehouse" },
  { colour: "bg-[#6a3fa0]", square: false, label: "Delivery city" },
  { colour: "bg-warning", square: false, label: "Awaiting dispatch" },
  { colour: "bg-primary", square: false, label: "On its way" },
  { colour: "bg-success", square: false, label: "Delivered" },
];

function Figure({
  label,
  value,
  alert = false,
}: {
  label: string;
  value: number;
  alert?: boolean;
}) {
  return (
    <div>
      <dt className="text-xs text-ink-muted">{label}</dt>
      <dd className={cn("text-sm font-semibold tabular-nums", alert ? "text-warning" : "text-ink")}>
        {formatCount(value)}
      </dd>
    </div>
  );
}

function DetailHeader({ title, onClose }: { title: string; onClose: () => void }) {
  return (
    <div className="flex items-start justify-between gap-2">
      <h3 className="text-sm font-semibold text-ink">{title}</h3>
      <button
        type="button"
        aria-label="Close details"
        onClick={onClose}
        className="rounded-sm p-0.5 text-ink-muted hover:bg-canvas hover:text-ink"
      >
        <X aria-hidden className="size-4" />
      </button>
    </div>
  );
}

function WarehouseDetail({ warehouse }: { warehouse: NetworkWarehouse }) {
  const clear = useVisualizationStore((state) => state.clearSelection);
  const stock = useApiQuery(
    (signal) =>
      inventoryService.list({ warehouseId: warehouse.id, sort: "available,desc", size: 5 }, signal),
    [warehouse.id, warehouse.totalAvailable],
  );

  return (
    <div className="space-y-3">
      <DetailHeader title={warehouse.name} onClose={clear} />
      <div className="flex flex-wrap items-center gap-2">
        <StatusBadge status={RECORD_STATUS[warehouse.status]} />
        <span className="text-xs text-ink-muted">
          {warehouse.city}, {warehouse.state}
        </span>
      </div>
      <dl className="grid grid-cols-2 gap-2.5">
        <Figure label="Active orders" value={warehouse.activeOrders} />
        <Figure label="Active dispatches" value={warehouse.activeDispatches} />
        <Figure label="Available units" value={warehouse.totalAvailable} />
        <Figure label="Reserved units" value={warehouse.totalReserved} />
        <Figure label="Products in stock" value={warehouse.inStockProducts} />
        <Figure
          label="Low stock items"
          value={warehouse.lowStockItems}
          alert={warehouse.lowStockItems > 0}
        />
      </dl>
      <div>
        <h4 className="mb-1 text-xs font-semibold text-ink">Most available products</h4>
        {!stock.data ? (
          <Skeleton className="h-16 w-full" />
        ) : (
          <ul className="space-y-1 text-[13px]">
            {stock.data.content.map((row) => (
              <li key={row.id} className="flex justify-between gap-2">
                <span className="truncate text-ink">{row.productName}</span>
                <span className="text-ink-muted tabular-nums">
                  {formatCount(row.availableQuantity)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

function ShipmentDetail({ shipment }: { shipment: NetworkShipment }) {
  const clear = useVisualizationStore((state) => state.clearSelection);
  return (
    <div className="space-y-3">
      <DetailHeader title={shipment.trackingReference} onClose={clear} />
      <StatusBadge status={SHIPMENT_STATUS[shipment.status]} />
      <dl className="space-y-2 text-[13px]">
        <div>
          <dt className="text-xs text-ink-muted">Route</dt>
          <dd className="text-ink">
            {shipment.warehouseName} to {shipment.destinationCity}
          </dd>
        </div>
        <div>
          <dt className="text-xs text-ink-muted">Estimated delivery</dt>
          <dd className="text-ink">{formatCalendarDate(shipment.estimatedDeliveryDate)}</dd>
        </div>
      </dl>
      <Link
        href={`/admin/orders/${shipment.orderId}`}
        className="inline-block text-[13px] font-medium text-primary hover:underline"
      >
        View order {shipment.orderNumber}
      </Link>
      <p className="text-xs text-ink-muted">
        The marker&apos;s position is simulated from the shipment status. It is not GPS.
      </p>
    </div>
  );
}

/** Mirrors the canvas in the DOM, so everything on the map also works by keyboard and screen reader. */
export function MapPanel({ network }: { network: Network | undefined }) {
  const selectedWarehouseId = useVisualizationStore((state) => state.selectedWarehouseId);
  const selectedShipmentId = useVisualizationStore((state) => state.selectedShipmentId);
  const selectWarehouse = useVisualizationStore((state) => state.selectWarehouse);
  const selectShipment = useVisualizationStore((state) => state.selectShipment);

  const warehouse = network?.warehouses.find((w) => w.id === selectedWarehouseId);
  const shipment = network?.shipments.find((s) => s.dispatchId === selectedShipmentId);
  if (warehouse) return <WarehouseDetail warehouse={warehouse} />;
  if (shipment) return <ShipmentDetail shipment={shipment} />;

  const active = network?.shipments.filter((s) => s.status !== "DELIVERED") ?? [];
  const BUTTON = "w-full rounded-sm px-1.5 py-1 text-left text-[13px] text-ink hover:bg-canvas";

  return (
    <div className="space-y-3.5">
      <section>
        <h3 className="mb-1 text-xs font-semibold text-ink">Warehouses</h3>
        <ul>
          {network?.warehouses.map((w) => (
            <li key={w.id}>
              <button type="button" className={BUTTON} onClick={() => selectWarehouse(w.id)}>
                <span className="flex justify-between gap-2">
                  <span className="truncate">{w.name}</span>
                  <span className="text-xs whitespace-nowrap text-ink-muted tabular-nums">
                    {formatCount(w.activeDispatches)} active
                  </span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      </section>
      <section>
        <h3 className="mb-1 text-xs font-semibold text-ink">Active shipments</h3>
        {active.length === 0 ? (
          <p className="px-1.5 text-[13px] text-ink-muted">None right now.</p>
        ) : (
          <ul className="max-h-40 overflow-y-auto">
            {active.map((s) => (
              <li key={s.dispatchId}>
                <button
                  type="button"
                  className={BUTTON}
                  onClick={() => selectShipment(s.dispatchId)}
                >
                  <span className="flex justify-between gap-2">
                    <span className="truncate">To {s.destinationCity}</span>
                    <span className="text-xs whitespace-nowrap text-ink-muted">
                      {SHIPMENT_STATUS[s.status].label}
                    </span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
      <section>
        <h3 className="mb-1 text-xs font-semibold text-ink">Legend</h3>
        <ul className="grid grid-cols-2 gap-x-2 gap-y-1">
          {LEGEND.map((item) => (
            <li key={item.label} className="flex items-center gap-1.5 text-xs text-ink-muted">
              <span
                aria-hidden
                className={cn("size-2.5", item.colour, item.square ? "rounded-sm" : "rounded-full")}
              />
              {item.label}
            </li>
          ))}
        </ul>
      </section>
      <p className="text-xs text-ink-muted">
        Select a warehouse or shipment on the map. Marker positions are simulated from shipment
        status, not GPS.
      </p>
    </div>
  );
}
