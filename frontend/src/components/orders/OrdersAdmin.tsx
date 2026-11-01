"use client";

import { useRouter } from "next/navigation";
import { StatusBadge } from "@/components/ui/Badge";
import { DataTable, type Column } from "@/components/ui/DataTable";
import { Panel } from "@/components/ui/Feedback";
import { Select } from "@/components/ui/FormControls";
import { Pagination } from "@/components/ui/Pagination";
import { SearchInput } from "@/components/ui/SearchInput";
import { useApiQuery } from "@/hooks/useApiQuery";
import { useUrlSearch } from "@/hooks/useUrlSearch";
import { orderService } from "@/services/orders/orders";
import { warehouseService } from "@/services/warehouses/warehouses";
import { sortParam, useFilterStore } from "@/stores/filters/filterStore";
import type { OrderStatus, OrderSummary } from "@/types/api";
import { formatCount, formatDateTime, formatInr } from "@/utils/format";
import { ORDER_STATUS } from "@/utils/status";

const COLUMNS: Column<OrderSummary>[] = [
  {
    key: "orderNumber",
    header: "Order ID",
    sortable: true,
    cell: (order) => <span className="font-medium text-ink">{order.orderNumber}</span>,
  },
  { key: "customer", header: "Customer", cell: (order) => order.customerName },
  {
    key: "createdAt",
    header: "Order date",
    sortable: true,
    cell: (order) => <span className="whitespace-nowrap">{formatDateTime(order.createdAt)}</span>,
  },
  {
    key: "units",
    header: "Units",
    align: "right",
    cell: (order) => formatCount(order.unitCount),
  },
  {
    key: "totalAmount",
    header: "Amount",
    align: "right",
    sortable: true,
    cell: (order) => formatInr(order.totalAmount),
  },
  {
    key: "warehouse",
    header: "Selected warehouse",
    cell: (order) =>
      order.selectedWarehouse ?? <span className="text-ink-subtle">Not assigned</span>,
  },
  {
    key: "status",
    header: "Status",
    cell: (order) => <StatusBadge status={ORDER_STATUS[order.status]} />,
  },
];

export function OrdersAdmin() {
  useUrlSearch("orders");
  const router = useRouter();
  const filters = useFilterStore((state) => state.filters.orders);
  const setFilters = useFilterStore((state) => state.setFilters);

  const { q, status, warehouseId, page, sort } = filters;
  const orders = useApiQuery(
    (signal) => orderService.list({ q, status, warehouseId, page, sort: sortParam(sort) }, signal),
    [q, status, warehouseId, page, sort],
  );
  const warehouses = useApiQuery((signal) => warehouseService.list({ size: 100 }, signal), []);

  return (
    <Panel>
      <div className="flex flex-wrap items-end gap-2 p-3">
        <SearchInput
          label="Search order or customer"
          value={q}
          onChange={(value) => setFilters("orders", { q: value })}
        />
        <div className="w-40">
          <Select
            label="Status"
            value={status}
            onChange={(event) =>
              setFilters("orders", { status: event.target.value as OrderStatus | "" })
            }
          >
            <option value="">All statuses</option>
            {Object.entries(ORDER_STATUS).map(([value, display]) => (
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
              setFilters("orders", {
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
        caption="Orders"
        columns={COLUMNS}
        rows={orders.data?.content ?? []}
        rowKey={(order) => order.id}
        loading={orders.loading && !orders.data}
        error={orders.error}
        onRetry={orders.reload}
        emptyTitle="No orders found"
        emptyDescription="Orders appear here as customers place them."
        sort={sort}
        onSortChange={(next) => setFilters("orders", { sort: next })}
        onRowClick={(order) => router.push(`/admin/orders/${order.id}`)}
      />
      {orders.data && (
        <Pagination
          page={orders.data.number}
          size={orders.data.size}
          totalElements={orders.data.totalElements}
          totalPages={orders.data.totalPages}
          onPageChange={(next) => setFilters("orders", { page: next })}
        />
      )}
    </Panel>
  );
}
