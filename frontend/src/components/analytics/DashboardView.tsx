"use client";

import {
  AlertTriangle,
  Boxes,
  CheckCircle2,
  Clock,
  Package,
  ShoppingCart,
  Truck,
  Warehouse,
} from "lucide-react";
import Link from "next/link";
import type { ComponentProps } from "react";
import { StatusBadge } from "@/components/ui/Badge";
import { DataTable, type Column } from "@/components/ui/DataTable";
import { ErrorState, Panel } from "@/components/ui/Feedback";
import { StatCard } from "@/components/ui/StatCard";
import { NetworkMap } from "@/components/visualization/NetworkMap";
import { useApiQuery } from "@/hooks/useApiQuery";
import { useAutoRefresh } from "@/hooks/useAutoRefresh";
import { analyticsService } from "@/services/analytics/analytics";
import { useFilterStore } from "@/stores/filters/filterStore";
import type { Dashboard, Dispatch, InventoryRecord, OrderSummary } from "@/types/api";
import { formatCount, formatDateTime, formatInr } from "@/utils/format";
import { ORDER_STATUS, SHIPMENT_STATUS } from "@/utils/status";
import { formatDay, formatRate, hasValues, warehouseLabel } from "./analyticsDisplay";
import { ChartPanel, ChartTable, HorizontalBars, TrendChart } from "./Charts";

const REFRESH_MS = 30000;

const ORDER_COLUMNS: Column<OrderSummary>[] = [
  {
    key: "orderNumber",
    header: "Order ID",
    cell: (order) => (
      <Link
        href={`/admin/orders/${order.id}`}
        className="font-mono text-xs font-medium whitespace-nowrap text-primary hover:underline"
      >
        {order.orderNumber}
      </Link>
    ),
  },
  { key: "customer", header: "Customer", cell: (order) => order.customerName },
  {
    key: "createdAt",
    header: "Order date",
    cell: (order) => <span className="whitespace-nowrap">{formatDateTime(order.createdAt)}</span>,
  },
  {
    key: "totalAmount",
    header: "Amount",
    align: "right",
    cell: (order) => formatInr(order.totalAmount),
  },
  {
    key: "warehouse",
    header: "Selected warehouse",
    cell: (order) =>
      order.selectedWarehouse ?? <span className="text-ink-subtle">Not allocated</span>,
  },
  {
    key: "status",
    header: "Status",
    cell: (order) => <StatusBadge status={ORDER_STATUS[order.status]} />,
  },
];

const DISPATCH_COLUMNS: Column<Dispatch>[] = [
  {
    key: "trackingReference",
    header: "Dispatch ID",
    cell: (dispatch) => (
      <span className="font-mono text-xs font-medium whitespace-nowrap text-ink">
        {dispatch.trackingReference}
      </span>
    ),
  },
  { key: "warehouse", header: "Warehouse", cell: (dispatch) => dispatch.warehouseName },
  {
    key: "order",
    header: "Order ID",
    cell: (dispatch) => (
      <Link
        href={`/admin/orders/${dispatch.orderId}`}
        className="font-mono text-xs whitespace-nowrap text-primary hover:underline"
      >
        {dispatch.orderNumber}
      </Link>
    ),
  },
  {
    key: "status",
    header: "Shipment status",
    cell: (dispatch) => <StatusBadge status={SHIPMENT_STATUS[dispatch.status]} />,
  },
];

const ALERT_COLUMNS: Column<InventoryRecord>[] = [
  { key: "product", header: "Product", cell: (item) => item.productName },
  { key: "warehouse", header: "Warehouse", cell: (item) => item.warehouseName },
  {
    key: "available",
    header: "Available",
    align: "right",
    cell: (item) => formatCount(item.availableQuantity),
  },
  {
    key: "reorder",
    header: "Reorder level",
    align: "right",
    cell: (item) => formatCount(item.reorderLevel),
  },
];

function KpiCard({ href, onClick, ...card }: ComponentProps<typeof StatCard> & LinkTarget) {
  return (
    <Link
      href={href}
      onClick={onClick}
      className="block rounded-lg transition-shadow hover:shadow-overlay"
    >
      <StatCard {...card} />
    </Link>
  );
}

interface LinkTarget {
  href: string;
  onClick?: () => void;
}

function KpiGrid({ kpis }: { kpis: Dashboard["kpis"] | undefined }) {
  const setFilters = useFilterStore((state) => state.setFilters);
  const loading = !kpis;
  const value = (figure: number | undefined) => formatCount(figure ?? 0);
  return (
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
      <KpiCard
        label="Total orders"
        icon={ShoppingCart}
        loading={loading}
        value={value(kpis?.totalOrders)}
        href="/admin/orders"
        onClick={() => setFilters("orders", { status: "" })}
      />
      <KpiCard
        label="Pending orders"
        icon={Clock}
        loading={loading}
        alert={(kpis?.pendingOrders ?? 0) > 0}
        value={value(kpis?.pendingOrders)}
        href="/admin/orders"
        onClick={() => setFilters("orders", { status: "PENDING" })}
      />
      <KpiCard
        label="Total products"
        icon={Package}
        loading={loading}
        value={value(kpis?.totalProducts)}
        href="/admin/products"
      />
      <KpiCard
        label="Total warehouses"
        icon={Warehouse}
        loading={loading}
        value={value(kpis?.totalWarehouses)}
        href="/admin/warehouses"
      />
      <KpiCard
        label="Total inventory"
        hint="Units, including reserved"
        icon={Boxes}
        loading={loading}
        value={value(kpis?.totalInventory)}
        href="/admin/inventory"
        onClick={() => setFilters("inventory", { lowStockOnly: false })}
      />
      <KpiCard
        label="Active dispatches"
        icon={Truck}
        loading={loading}
        value={value(kpis?.activeDispatches)}
        href="/admin/dispatch"
      />
      <KpiCard
        label="Delivered orders"
        icon={CheckCircle2}
        loading={loading}
        value={value(kpis?.deliveredOrders)}
        href="/admin/orders"
        onClick={() => setFilters("orders", { status: "DELIVERED" })}
      />
      <KpiCard
        label="Low stock items"
        hint="Below reorder level"
        icon={AlertTriangle}
        loading={loading}
        alert={(kpis?.lowStockItems ?? 0) > 0}
        value={value(kpis?.lowStockItems)}
        href="/admin/inventory"
        onClick={() => setFilters("inventory", { lowStockOnly: true })}
      />
    </div>
  );
}

function WarehousePerformance({ warehouses }: { warehouses: Dashboard["warehousePerformance"] }) {
  const measure = (pick: (warehouse: (typeof warehouses)[number]) => number | null) =>
    warehouses.map((warehouse) => ({ label: warehouseLabel(warehouse), value: pick(warehouse) }));
  const multiples = [
    {
      title: "Inventory quantity",
      data: measure((warehouse) => warehouse.availableUnits + warehouse.reservedUnits),
    },
    { title: "Allocated orders", data: measure((warehouse) => warehouse.allocatedOrders) },
    { title: "Completed dispatches", data: measure((warehouse) => warehouse.completedDispatches) },
  ];
  const fulfilment = measure((warehouse) => warehouse.fulfilmentRate);

  return (
    <ChartPanel
      title="Warehouse performance"
      isEmpty={warehouses.length === 0}
      emptyTitle="No warehouses yet"
      table={
        <ChartTable
          caption="Warehouse performance"
          headers={["Warehouse", "Inventory", "Allocated", "Completed", "Fulfilment"]}
          rows={warehouses.map((warehouse) => [
            warehouseLabel(warehouse),
            formatCount(warehouse.availableUnits + warehouse.reservedUnits),
            formatCount(warehouse.allocatedOrders),
            formatCount(warehouse.completedDispatches),
            formatRate(warehouse.fulfilmentRate),
          ])}
        />
      }
    >
      <div className="grid gap-x-6 gap-y-4 md:grid-cols-2">
        {multiples.map(({ title, data }) => (
          <div key={title}>
            <h3 className="mb-1 text-xs font-medium text-ink-muted">{title}</h3>
            <HorizontalBars data={data} label={title} />
          </div>
        ))}
        <div>
          <h3 className="mb-1 text-xs font-medium text-ink-muted">
            Fulfilment (delivered ÷ allocated)
          </h3>
          <HorizontalBars
            data={fulfilment}
            label="Fulfilment rate"
            format={(rate) => formatRate(rate)}
            max={1}
          />
        </div>
      </div>
    </ChartPanel>
  );
}

function ViewAll({ href, onClick }: LinkTarget) {
  return (
    <Link
      href={href}
      onClick={onClick}
      className="text-xs leading-8 font-medium text-primary hover:underline"
    >
      View all
    </Link>
  );
}

export function DashboardView() {
  const dashboard = useApiQuery((signal) => analyticsService.dashboard(signal), []);
  useAutoRefresh(dashboard.reload, REFRESH_MS);
  const setFilters = useFilterStore((state) => state.setFilters);
  const data = dashboard.data;

  if (dashboard.error && !data) {
    return (
      <Panel>
        <ErrorState message={dashboard.error} onRetry={dashboard.reload} />
      </Panel>
    );
  }

  const activity = data?.orderActivity ?? [];

  return (
    <div className="space-y-3">
      <KpiGrid kpis={data?.kpis} />

      <div className="grid grid-cols-[minmax(0,1fr)] gap-3 xl:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <ChartPanel
          title="Order activity"
          note="Orders placed per day, last 14 days"
          isEmpty={!!data && !hasValues(activity.map(({ count }) => ({ label: "", value: count })))}
          emptyTitle="No orders in the last 14 days"
          table={
            <ChartTable
              caption="Orders placed per day"
              headers={["Date", "Orders"]}
              rows={activity.map((point) => [formatDay(point.date), formatCount(point.count)])}
            />
          }
        >
          <TrendChart data={activity} label="Orders" />
        </ChartPanel>

        <Panel
          title="Inventory alerts"
          actions={
            <ViewAll
              href="/admin/inventory"
              onClick={() => setFilters("inventory", { lowStockOnly: true })}
            />
          }
        >
          <DataTable
            caption="Products below their reorder level"
            columns={ALERT_COLUMNS}
            rows={data?.inventoryAlerts.content ?? []}
            rowKey={(item) => item.id}
            loading={!data}
            emptyTitle="Nothing below its reorder level"
          />
        </Panel>
      </div>

      {data && <WarehousePerformance warehouses={data.warehousePerformance} />}

      <Panel
        title="Recent orders"
        actions={
          <ViewAll href="/admin/orders" onClick={() => setFilters("orders", { status: "" })} />
        }
      >
        <DataTable
          caption="Most recent orders"
          columns={ORDER_COLUMNS}
          rows={data?.recentOrders.content ?? []}
          rowKey={(order) => order.id}
          loading={!data}
          emptyTitle="No orders yet"
        />
      </Panel>
      <Panel title="Recent dispatches" actions={<ViewAll href="/admin/dispatch" />}>
        <DataTable
          caption="Most recent dispatches"
          columns={DISPATCH_COLUMNS}
          rows={data?.recentDispatches.content ?? []}
          rowKey={(dispatch) => dispatch.id}
          loading={!data}
          emptyTitle="No dispatches yet"
        />
      </Panel>

      <NetworkMap />
    </div>
  );
}
