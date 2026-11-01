"use client";

import { useState } from "react";
import { StatusBadge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { DataTable } from "@/components/ui/DataTable";
import { ErrorState, Skeleton } from "@/components/ui/Feedback";
import { Drawer } from "@/components/ui/Overlays";
import { StatCard } from "@/components/ui/StatCard";
import { Tabs } from "@/components/ui/Tabs";
import { useApiQuery } from "@/hooks/useApiQuery";
import { ApiError } from "@/services/api/client";
import { inventoryService } from "@/services/inventory/inventory";
import { warehouseService } from "@/services/warehouses/warehouses";
import { useUiStore } from "@/stores/ui/uiStore";
import type { Warehouse, WarehouseRequest } from "@/types/api";
import { formatCount, formatInr } from "@/utils/format";
import { RECORD_STATUS } from "@/utils/status";
import { WarehouseForm } from "./WarehouseForm";
import { toWarehouseFormValues, toWarehouseRequest } from "./warehouseForm";

type Tab = "overview" | "inventory" | "edit";

const TABS = [
  { id: "overview", label: "Overview" },
  { id: "inventory", label: "Inventory" },
  { id: "edit", label: "Edit" },
] as const;

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-ink-muted">{label}</dt>
      <dd className="text-[13px] text-ink">{value}</dd>
    </div>
  );
}

function Overview({ warehouseId }: { warehouseId: number }) {
  const detail = useApiQuery((signal) => warehouseService.get(warehouseId, signal), [warehouseId]);

  if (detail.error) return <ErrorState message={detail.error} onRetry={detail.reload} />;
  if (!detail.data) return <Skeleton className="h-64 w-full" />;

  const { warehouse, performance } = detail.data;
  const { stock } = warehouse;
  return (
    <div className="space-y-4">
      <dl className="grid grid-cols-2 gap-3">
        <div className="col-span-2">
          <Fact
            label="Address"
            value={`${warehouse.address}, ${warehouse.city}, ${warehouse.state} ${warehouse.postalCode}`}
          />
        </div>
        <Fact label="Contact" value={warehouse.contactNumber} />
        <Fact label="Coordinates" value={`${warehouse.latitude}, ${warehouse.longitude}`} />
        <Fact
          label="Delivery cost"
          value={`${formatInr(warehouse.baseDeliveryCost)} + ${formatInr(warehouse.costPerKm)}/km`}
        />
        <Fact label="Handling time" value={`${warehouse.handlingHours} hours`} />
      </dl>

      <section>
        <h3 className="mb-2 text-[13px] font-semibold text-ink">Stock</h3>
        <div className="grid grid-cols-2 gap-2">
          <StatCard label="Products" value={formatCount(stock.productCount)} />
          <StatCard label="Available units" value={formatCount(stock.totalAvailable)} />
          <StatCard label="Reserved units" value={formatCount(stock.totalReserved)} />
          <StatCard
            label="Low stock items"
            value={formatCount(stock.lowStockItems)}
            alert={stock.lowStockItems > 0}
          />
        </div>
      </section>

      <section>
        <h3 className="mb-2 text-[13px] font-semibold text-ink">Performance</h3>
        <div className="grid grid-cols-2 gap-2">
          <StatCard label="Allocated orders" value={formatCount(performance.allocatedOrders)} />
          <StatCard label="Active dispatches" value={formatCount(performance.activeDispatches)} />
          <StatCard
            label="Completed dispatches"
            value={formatCount(performance.completedDispatches)}
          />
          <StatCard
            label="Fulfilment rate"
            value={
              performance.fulfilmentRate === null
                ? "n/a"
                : `${Math.round(performance.fulfilmentRate * 100)}%`
            }
            hint={performance.fulfilmentRate === null ? "No orders yet" : undefined}
          />
        </div>
      </section>
    </div>
  );
}

function WarehouseStock({ warehouseId }: { warehouseId: number }) {
  const stock = useApiQuery(
    (signal) => inventoryService.list({ warehouseId, size: 100 }, signal),
    [warehouseId],
  );
  return (
    <DataTable
      caption="Warehouse inventory"
      rows={stock.data?.content ?? []}
      rowKey={(record) => record.id}
      loading={stock.loading && !stock.data}
      error={stock.error}
      onRetry={stock.reload}
      emptyTitle="No stock in this warehouse"
      columns={[
        { key: "product", header: "Product", cell: (record) => record.productName },
        {
          key: "available",
          header: "Available",
          align: "right",
          cell: (record) => formatCount(record.availableQuantity),
        },
        {
          key: "reserved",
          header: "Reserved",
          align: "right",
          cell: (record) => formatCount(record.reservedQuantity),
        },
      ]}
    />
  );
}

export function WarehouseDrawer({
  warehouse,
  onChanged,
  onClose,
}: {
  warehouse: Warehouse | null;
  onChanged: () => void;
  onClose: () => void;
}) {
  const [tab, setTab] = useState<Tab>("overview");
  const pushToast = useUiStore((state) => state.pushToast);

  function close() {
    setTab("overview");
    onClose();
  }

  async function save(id: number, request: WarehouseRequest, message: string) {
    await warehouseService.update(id, request);
    pushToast("success", message);
    onChanged();
    close();
  }

  async function toggleStatus(current: Warehouse) {
    try {
      if (current.status === "ACTIVE") {
        await warehouseService.deactivate(current.id);
        pushToast("success", `${current.name} deactivated. New orders won't consider it.`);
        onChanged();
        close();
      } else {
        await save(
          current.id,
          toWarehouseRequest(toWarehouseFormValues(current), "ACTIVE"),
          `${current.name} is active again.`,
        );
      }
    } catch (error) {
      pushToast("error", error instanceof ApiError ? error.message : "Something went wrong.");
    }
  }

  return (
    <Drawer open={warehouse !== null} title={warehouse?.name ?? ""} onClose={close}>
      {warehouse && (
        <div className="space-y-4">
          <div className="flex items-center justify-between gap-2">
            <StatusBadge status={RECORD_STATUS[warehouse.status]} />
            <Button variant="secondary" size="sm" onClick={() => toggleStatus(warehouse)}>
              {warehouse.status === "ACTIVE" ? "Deactivate" : "Reactivate"}
            </Button>
          </div>
          <Tabs label="Warehouse details" tabs={TABS} active={tab} onChange={setTab} />
          {tab === "overview" && <Overview warehouseId={warehouse.id} />}
          {tab === "inventory" && <WarehouseStock warehouseId={warehouse.id} />}
          {tab === "edit" && (
            <WarehouseForm
              key={warehouse.id}
              initial={toWarehouseFormValues(warehouse)}
              submitLabel="Save changes"
              onSubmit={(request) => save(warehouse.id, request, `${request.name} saved.`)}
            />
          )}
        </div>
      )}
    </Drawer>
  );
}
