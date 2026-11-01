"use client";

import { useState, type ReactNode } from "react";
import { StatusBadge } from "@/components/ui/Badge";
import { DataTable, type Column } from "@/components/ui/DataTable";
import { ErrorState, Panel, Skeleton } from "@/components/ui/Feedback";
import { Select } from "@/components/ui/FormControls";
import { StatCard } from "@/components/ui/StatCard";
import { Tabs } from "@/components/ui/Tabs";
import { useApiQuery, type ApiQuery } from "@/hooks/useApiQuery";
import { formatHours } from "@/components/orders/allocationDisplay";
import { analyticsService } from "@/services/analytics/analytics";
import type { InventoryRecord } from "@/types/api";
import { formatCount, formatInr } from "@/utils/format";
import { AVAILABILITY, ORDER_STATUS, SHIPMENT_STATUS } from "@/utils/status";
import {
  CHART_COLORS,
  formatDay,
  formatRate,
  hasValues,
  stackedStock,
  statusBars,
  warehouseLabel,
  type ChartSeries,
} from "./analyticsDisplay";
import {
  ChartPanel,
  ChartTable,
  HorizontalBars,
  RefetchFrame,
  StackedBars,
  TrendChart,
  TrendLines,
} from "./Charts";

const PERIODS = [
  { days: 7, label: "Last 7 days" },
  { days: 30, label: "Last 30 days" },
  { days: 90, label: "Last 90 days" },
  { days: 365, label: "Last 12 months" },
];

const TABS = [
  { id: "orders", label: "Orders" },
  { id: "warehouses", label: "Warehouses" },
  { id: "inventory", label: "Inventory" },
  { id: "dispatch", label: "Dispatch" },
] as const;
type TabId = (typeof TABS)[number]["id"];

const CHARTED_PRODUCTS = 12;

const MOVEMENT_SERIES: ChartSeries[] = [
  { key: "received", label: "Received", color: CHART_COLORS[0] },
  { key: "shipped", label: "Shipped", color: CHART_COLORS[1] },
];

const LOW_STOCK_COLUMNS: Column<InventoryRecord>[] = [
  { key: "product", header: "Product", cell: (item) => item.productName },
  { key: "sku", header: "SKU", cell: (item) => item.sku },
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
  {
    key: "state",
    header: "State",
    cell: (item) => (
      <StatusBadge status={AVAILABILITY[item.availableQuantity === 0 ? "OUT" : "LOW"]} />
    ),
  },
];

function Section<T>({ query, children }: { query: ApiQuery<T>; children: (data: T) => ReactNode }) {
  if (query.error && !query.data) {
    return (
      <Panel>
        <ErrorState message={query.error} onRetry={query.reload} />
      </Panel>
    );
  }
  if (!query.data) return <Skeleton className="h-96 w-full" />;
  return <RefetchFrame busy={query.loading}>{children(query.data)}</RefetchFrame>;
}

function Stats({ children }: { children: ReactNode }) {
  return <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">{children}</div>;
}

function OrdersTab({ days }: { days: number }) {
  const query = useApiQuery((signal) => analyticsService.orders(days, signal), [days]);
  return (
    <Section query={query}>
      {(data) => {
        const byStatus = statusBars(data.ordersByStatus, ORDER_STATUS);
        return (
          <>
            <Stats>
              <StatCard
                label="Total orders"
                value={formatCount(data.totalOrders)}
                hint={`${formatInr(data.totalValue)} excluding cancelled`}
              />
              <StatCard
                label="Average order value"
                value={formatInr(data.averageOrderValue)}
                hint="Excluding cancelled"
              />
              <StatCard
                label="Allocation rate"
                value={formatRate(data.allocationRate)}
                hint={`${formatRate(data.cancellationRate, "No")} cancelled`}
              />
              <StatCard
                label="Average delivery time"
                value={
                  data.averageDeliveryHours === null
                    ? "No data"
                    : formatHours(data.averageDeliveryHours)
                }
                hint="From order placed to delivered"
              />
            </Stats>
            <ChartPanel
              title="Orders over time"
              isEmpty={data.totalOrders === 0}
              table={
                <ChartTable
                  caption="Orders per day"
                  headers={["Date", "Orders"]}
                  rows={data.ordersOverTime.map((point) => [
                    formatDay(point.date),
                    formatCount(point.count),
                  ])}
                />
              }
            >
              <TrendChart data={data.ordersOverTime} label="Orders" />
            </ChartPanel>
            <div className="grid grid-cols-[minmax(0,1fr)] gap-3 xl:grid-cols-2">
              <ChartPanel
                title="Orders by status"
                isEmpty={!hasValues(byStatus)}
                table={
                  <ChartTable
                    caption="Orders by status"
                    headers={["Status", "Orders"]}
                    rows={byStatus.map((bar) => [bar.label, formatCount(bar.value ?? 0)])}
                  />
                }
              >
                <HorizontalBars data={byStatus} label="Orders" />
              </ChartPanel>
              <ChartPanel
                title="Top-selling products"
                note="Units sold, excluding cancelled orders"
                isEmpty={data.topProducts.length === 0}
                table={
                  <ChartTable
                    caption="Top-selling products"
                    headers={["Product", "SKU", "Units", "Revenue"]}
                    rows={data.topProducts.map((product) => [
                      product.name,
                      product.sku,
                      formatCount(product.units),
                      formatInr(product.revenue),
                    ])}
                  />
                }
              >
                <HorizontalBars
                  data={data.topProducts.map((product) => ({
                    label: product.name,
                    value: product.units,
                  }))}
                  label="Units sold"
                  labelWidth={160}
                />
              </ChartPanel>
            </div>
          </>
        );
      }}
    </Section>
  );
}

function WarehousesTab({ days }: { days: number }) {
  const query = useApiQuery((signal) => analyticsService.warehouses(days, signal), [days]);
  return (
    <Section query={query}>
      {({ warehouses }) => {
        const table = (
          <ChartTable
            caption="Warehouse comparison"
            headers={["Warehouse", "Available", "Reserved", "Allocated", "Delivered", "Fulfilment"]}
            rows={warehouses.map((warehouse) => [
              warehouseLabel(warehouse),
              formatCount(warehouse.availableUnits),
              formatCount(warehouse.reservedUnits),
              formatCount(warehouse.allocatedOrders),
              formatCount(warehouse.completedDispatches),
              formatRate(warehouse.fulfilmentRate),
            ])}
          />
        );
        const allocated = warehouses.map((warehouse) => ({
          label: warehouseLabel(warehouse),
          value: warehouse.allocatedOrders,
        }));
        const stock = warehouses.map((warehouse) => ({
          label: warehouseLabel(warehouse),
          value: warehouse.availableUnits + warehouse.reservedUnits,
        }));
        return (
          <div className="grid grid-cols-[minmax(0,1fr)] gap-3 xl:grid-cols-3">
            <ChartPanel
              title="Orders allocated per warehouse"
              note="Orders placed in this period, excluding cancelled"
              isEmpty={!hasValues(allocated)}
              table={table}
            >
              <HorizontalBars data={allocated} label="Allocated orders" />
            </ChartPanel>
            <ChartPanel
              title="Fulfilment comparison"
              note="Delivered ÷ allocated, for orders placed in this period"
              isEmpty={!hasValues(allocated)}
              table={table}
            >
              <HorizontalBars
                data={warehouses.map((warehouse) => ({
                  label: warehouseLabel(warehouse),
                  value: warehouse.fulfilmentRate,
                }))}
                label="Fulfilment rate"
                format={(rate) => formatRate(rate)}
                max={1}
              />
            </ChartPanel>
            <ChartPanel
              title="Inventory distribution"
              note="Current units held, including reserved. Not affected by the period."
              isEmpty={!hasValues(stock)}
              emptyTitle="No stock recorded"
              table={table}
            >
              <HorizontalBars data={stock} label="Units held" />
            </ChartPanel>
          </div>
        );
      }}
    </Section>
  );
}

function InventoryTab({ days }: { days: number }) {
  const query = useApiQuery((signal) => analyticsService.inventory(days, signal), [days]);
  return (
    <Section query={query}>
      {(data) => {
        const stock = stackedStock(data.warehouses, data.productAvailability);
        const charted = { ...stock, rows: stock.rows.slice(0, CHARTED_PRODUCTS) };
        const moved = data.stockMovement.some((day) => day.received > 0 || day.shipped > 0);
        return (
          <>
            <Stats>
              <StatCard
                label="Total stock"
                value={formatCount(data.totalAvailable + data.totalReserved)}
                hint="Units in active warehouses"
              />
              <StatCard label="Available" value={formatCount(data.totalAvailable)} />
              <StatCard label="Reserved" value={formatCount(data.totalReserved)} />
              <StatCard
                label="Low stock items"
                value={formatCount(data.lowStockItems)}
                alert={data.lowStockItems > 0}
                hint="Below reorder level"
              />
            </Stats>
            <ChartPanel
              title="Product availability"
              note={
                stock.rows.length > CHARTED_PRODUCTS
                  ? `Current units available. The ${CHARTED_PRODUCTS} best-stocked of ${stock.rows.length} products are shown; the table has them all.`
                  : "Current units available in each warehouse. Not affected by the period."
              }
              isEmpty={stock.rows.length === 0}
              emptyTitle="No stock recorded"
              table={
                <ChartTable
                  caption="Units available by product and warehouse"
                  headers={["Product", ...stock.series.map((item) => item.label), "Total"]}
                  rows={stock.rows.map((row) => [
                    row.label,
                    ...stock.series.map((item) => formatCount(Number(row[item.key] ?? 0))),
                    formatCount(row.total),
                  ])}
                />
              }
            >
              <StackedBars stock={charted} label="Units available by product and warehouse" />
            </ChartPanel>
            <ChartPanel
              title="Stock movement"
              note="Units restocked and units shipped per day, from the stock ledger. Opening stock is not a delivery, so it isn't counted."
              isEmpty={!moved}
              emptyTitle="No stock moved in this period"
              table={
                <ChartTable
                  caption="Stock movement per day"
                  headers={["Date", "Received", "Shipped"]}
                  rows={data.stockMovement.map((day) => [
                    formatDay(day.date),
                    formatCount(day.received),
                    formatCount(day.shipped),
                  ])}
                />
              }
            >
              <TrendLines
                data={data.stockMovement}
                series={MOVEMENT_SERIES}
                label="Units received and shipped per day"
              />
            </ChartPanel>
            <Panel title="Low-stock products">
              <DataTable
                caption="Products below their reorder level, lowest stock first"
                columns={LOW_STOCK_COLUMNS}
                rows={data.lowStockProducts}
                rowKey={(item) => item.id}
                emptyTitle="Nothing below its reorder level"
              />
            </Panel>
          </>
        );
      }}
    </Section>
  );
}

function DispatchTab({ days }: { days: number }) {
  const query = useApiQuery((signal) => analyticsService.dispatches(days, signal), [days]);
  return (
    <Section query={query}>
      {(data) => {
        const bars = statusBars(data.statusDistribution, SHIPMENT_STATUS);
        return (
          <>
            <Stats>
              <StatCard label="Active dispatches" value={formatCount(data.activeDispatches)} />
              <StatCard
                label="Pending shipments"
                value={formatCount(data.pendingShipments)}
                hint="Pending or packed"
              />
              <StatCard
                label="Completed deliveries"
                value={formatCount(data.completedDeliveries)}
              />
              <StatCard
                label="On-time rate"
                value={formatRate(data.onTimeRate)}
                hint="Delivered by the estimated date"
              />
            </Stats>
            <ChartPanel
              title="Shipment status distribution"
              note="Shipments created in this period, in delivery order"
              isEmpty={!hasValues(bars)}
              table={
                <ChartTable
                  caption="Shipments by status"
                  headers={["Status", "Shipments"]}
                  rows={bars.map((bar) => [bar.label, formatCount(bar.value ?? 0)])}
                />
              }
            >
              <HorizontalBars data={bars} label="Shipments" />
            </ChartPanel>
          </>
        );
      }}
    </Section>
  );
}

export function AnalyticsView() {
  const [tab, setTab] = useState<TabId>("orders");
  const [days, setDays] = useState(30);

  return (
    <div className="space-y-3">
      <div className="max-w-48">
        <Select
          label="Period"
          value={days}
          onChange={(event) => setDays(Number(event.target.value))}
        >
          {PERIODS.map((period) => (
            <option key={period.days} value={period.days}>
              {period.label}
            </option>
          ))}
        </Select>
      </div>
      <Tabs tabs={TABS} active={tab} onChange={setTab} label="Analytics section" />
      {tab === "orders" && <OrdersTab days={days} />}
      {tab === "warehouses" && <WarehousesTab days={days} />}
      {tab === "inventory" && <InventoryTab days={days} />}
      {tab === "dispatch" && <DispatchTab days={days} />}
    </div>
  );
}
