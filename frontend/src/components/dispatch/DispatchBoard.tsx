"use client";

import Link from "next/link";
import { useState } from "react";
import { StatusBadge } from "@/components/ui/Badge";
import { DataTable, type Column } from "@/components/ui/DataTable";
import { ErrorState, Panel, Skeleton } from "@/components/ui/Feedback";
import { Select } from "@/components/ui/FormControls";
import { Drawer } from "@/components/ui/Overlays";
import { Pagination } from "@/components/ui/Pagination";
import { SearchInput } from "@/components/ui/SearchInput";
import { useApiQuery } from "@/hooks/useApiQuery";
import { useAutoRefresh } from "@/hooks/useAutoRefresh";
import { useUrlSearch } from "@/hooks/useUrlSearch";
import { dispatchService, trackingService } from "@/services/dispatch/dispatch";
import { warehouseService } from "@/services/warehouses/warehouses";
import { sortParam, useFilterStore } from "@/stores/filters/filterStore";
import type { Dispatch, ShipmentStatus } from "@/types/api";
import { formatDateTime } from "@/utils/format";
import { SHIPMENT_STATUS } from "@/utils/status";
import { AdvanceShipmentButton } from "./AdvanceShipmentButton";
import { ShipmentDetails } from "./ShipmentDetails";
import { ShipmentHistory } from "./ShipmentHistory";
import { formatCalendarDate } from "./trackingDisplay";

const REFRESH_MS = 15000;

const COLUMNS: Column<Dispatch>[] = [
  {
    key: "trackingReference",
    header: "Dispatch ID",
    cell: (dispatch) => (
      <span className="font-mono text-xs font-medium text-ink">{dispatch.trackingReference}</span>
    ),
  },
  { key: "order", header: "Order", cell: (dispatch) => dispatch.orderNumber },
  { key: "customer", header: "Customer", cell: (dispatch) => dispatch.customerName },
  { key: "warehouse", header: "Warehouse", cell: (dispatch) => dispatch.warehouseName },
  { key: "city", header: "Destination", cell: (dispatch) => dispatch.deliveryCity },
  {
    key: "dispatchDate",
    header: "Dispatch date",
    sortable: true,
    cell: (dispatch) =>
      dispatch.dispatchDate ? (
        <span className="whitespace-nowrap">{formatDateTime(dispatch.dispatchDate)}</span>
      ) : (
        <span className="text-ink-subtle">Not yet</span>
      ),
  },
  {
    key: "estimatedDeliveryDate",
    header: "Estimated delivery",
    sortable: true,
    cell: (dispatch) => (
      <span className="whitespace-nowrap">
        {formatCalendarDate(dispatch.estimatedDeliveryDate)}
      </span>
    ),
  },
  {
    key: "status",
    header: "Status",
    cell: (dispatch) => <StatusBadge status={SHIPMENT_STATUS[dispatch.status]} />,
  },
];

function DispatchDetail({ dispatch, onChanged }: { dispatch: Dispatch; onChanged: () => void }) {
  const tracking = useApiQuery(
    (signal) => trackingService.forDispatch(dispatch.id, signal),
    [dispatch.id, dispatch.status],
  );

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <Link
          href={`/admin/orders/${dispatch.orderId}`}
          className="text-[13px] font-medium text-primary hover:underline"
        >
          View order {dispatch.orderNumber}
        </Link>
        <AdvanceShipmentButton
          dispatchId={dispatch.id}
          allowedNextStatuses={dispatch.allowedNextStatuses}
          onAdvanced={onChanged}
        />
      </div>
      {tracking.error ? (
        <ErrorState message={tracking.error} onRetry={tracking.reload} />
      ) : !tracking.data ? (
        <Skeleton className="h-64 w-full" />
      ) : (
        <>
          <ShipmentDetails tracking={tracking.data} />
          <ShipmentHistory history={tracking.data.history} />
        </>
      )}
    </div>
  );
}

export function DispatchBoard() {
  useUrlSearch("dispatches");
  const filters = useFilterStore((state) => state.filters.dispatches);
  const setFilters = useFilterStore((state) => state.setFilters);
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const { q, status, warehouseId, page, sort } = filters;
  const dispatches = useApiQuery(
    (signal) =>
      dispatchService.list({ q, status, warehouseId, page, sort: sortParam(sort) }, signal),
    [q, status, warehouseId, page, sort],
  );
  const warehouses = useApiQuery((signal) => warehouseService.list({ size: 100 }, signal), []);
  useAutoRefresh(dispatches.reload, REFRESH_MS);

  const selected = dispatches.data?.content.find((dispatch) => dispatch.id === selectedId) ?? null;

  return (
    <Panel>
      <div className="flex flex-wrap items-end gap-2 p-3">
        <SearchInput
          label="Search dispatch or customer"
          value={q}
          onChange={(value) => setFilters("dispatches", { q: value })}
        />
        <div className="w-40">
          <Select
            label="Shipment status"
            value={status}
            onChange={(event) =>
              setFilters("dispatches", { status: event.target.value as ShipmentStatus | "" })
            }
          >
            <option value="">All statuses</option>
            {Object.entries(SHIPMENT_STATUS).map(([value, display]) => (
              <option key={value} value={value}>
                {display.label}
              </option>
            ))}
          </Select>
        </div>
        <div className="w-48">
          <Select
            label="Warehouse"
            value={warehouseId}
            onChange={(event) =>
              setFilters("dispatches", {
                warehouseId: event.target.value ? Number(event.target.value) : "",
              })
            }
          >
            <option value="">All warehouses</option>
            {warehouses.data?.content.map((warehouse) => (
              <option key={warehouse.id} value={warehouse.id}>
                {warehouse.name}
              </option>
            ))}
          </Select>
        </div>
      </div>

      <DataTable
        caption="Dispatches"
        columns={COLUMNS}
        rows={dispatches.data?.content ?? []}
        rowKey={(dispatch) => dispatch.id}
        loading={dispatches.loading && !dispatches.data}
        error={dispatches.error}
        onRetry={dispatches.reload}
        emptyTitle="No dispatches found"
        emptyDescription="A dispatch is created automatically when an order is confirmed."
        sort={sort}
        onSortChange={(next) => setFilters("dispatches", { sort: next })}
        onRowClick={(dispatch) => setSelectedId(dispatch.id)}
      />
      {dispatches.data && (
        <Pagination
          page={dispatches.data.number}
          size={dispatches.data.size}
          totalElements={dispatches.data.totalElements}
          totalPages={dispatches.data.totalPages}
          onPageChange={(next) => setFilters("dispatches", { page: next })}
        />
      )}

      <Drawer
        open={selected !== null}
        title={selected?.trackingReference ?? ""}
        onClose={() => setSelectedId(null)}
      >
        {selected && <DispatchDetail dispatch={selected} onChanged={dispatches.reload} />}
      </Drawer>
    </Panel>
  );
}
