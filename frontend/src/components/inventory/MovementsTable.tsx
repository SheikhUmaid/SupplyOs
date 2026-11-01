"use client";

import { X } from "lucide-react";
import { StatusBadge } from "@/components/ui/Badge";
import { DataTable, type Column } from "@/components/ui/DataTable";
import { Select } from "@/components/ui/FormControls";
import { Pagination } from "@/components/ui/Pagination";
import { SearchInput } from "@/components/ui/SearchInput";
import { useApiQuery } from "@/hooks/useApiQuery";
import { cn } from "@/lib/cn";
import { inventoryService } from "@/services/inventory/inventory";
import { useFilterStore } from "@/stores/filters/filterStore";
import type { InventoryTransaction, InventoryTransactionType, Warehouse } from "@/types/api";
import { formatCount, formatDateTime } from "@/utils/format";
import { MOVEMENT_TYPE } from "@/utils/status";

function signed(value: number): string {
  return value > 0 ? `+${formatCount(value)}` : formatCount(value);
}

function Delta({ value }: { value: number }) {
  if (value === 0) return <span className="text-ink-subtle">0</span>;
  return (
    <span className={cn("font-medium", value > 0 ? "text-success" : "text-danger")}>
      {signed(value)}
    </span>
  );
}

const COLUMNS: Column<InventoryTransaction>[] = [
  {
    key: "createdAt",
    header: "When",
    cell: (entry) => <span className="whitespace-nowrap">{formatDateTime(entry.createdAt)}</span>,
  },
  {
    key: "product",
    header: "Product",
    cell: (entry) => (
      <>
        <span className="block font-medium text-ink">{entry.productName}</span>
        <span className="font-mono text-xs text-ink-muted">{entry.sku}</span>
      </>
    ),
  },
  { key: "warehouse", header: "Warehouse", cell: (entry) => entry.warehouseName },
  {
    key: "type",
    header: "Type",
    cell: (entry) => <StatusBadge status={MOVEMENT_TYPE[entry.type]} />,
  },
  {
    key: "availableDelta",
    header: "Available",
    align: "right",
    cell: (entry) => <Delta value={entry.availableDelta} />,
  },
  {
    key: "reservedDelta",
    header: "Reserved",
    align: "right",
    cell: (entry) => <Delta value={entry.reservedDelta} />,
  },
  {
    key: "after",
    header: "Balance after",
    align: "right",
    cell: (entry) => `${formatCount(entry.availableAfter)} / ${formatCount(entry.reservedAfter)}`,
  },
  { key: "by", header: "By", cell: (entry) => entry.performedBy ?? "System" },
  { key: "note", header: "Note", cell: (entry) => entry.orderNumber ?? entry.note ?? "" },
];

export function MovementsTable({ warehouses }: { warehouses: Warehouse[] }) {
  const filters = useFilterStore((state) => state.filters.movements);
  const setFilters = useFilterStore((state) => state.setFilters);

  const { q, warehouseId, type, inventoryId, inventoryLabel, page } = filters;
  const movements = useApiQuery(
    (signal) => inventoryService.movements({ q, warehouseId, type, inventoryId, page }, signal),
    [q, warehouseId, type, inventoryId, page],
  );

  return (
    <>
      <div className="flex flex-wrap items-end gap-2 p-3">
        <SearchInput
          label="Search product or SKU"
          value={q}
          onChange={(value) => setFilters("movements", { q: value })}
        />
        <div className="w-48">
          <Select
            label="Warehouse"
            value={warehouseId}
            onChange={(event) =>
              setFilters("movements", {
                warehouseId: event.target.value ? Number(event.target.value) : "",
              })
            }
          >
            <option value="">All warehouses</option>
            {warehouses.map((warehouse) => (
              <option key={warehouse.id} value={warehouse.id}>
                {warehouse.name}
              </option>
            ))}
          </Select>
        </div>
        <div className="w-40">
          <Select
            label="Type"
            value={type}
            onChange={(event) =>
              setFilters("movements", {
                type: event.target.value as InventoryTransactionType | "",
              })
            }
          >
            <option value="">All types</option>
            {Object.entries(MOVEMENT_TYPE).map(([value, display]) => (
              <option key={value} value={value}>
                {display.label}
              </option>
            ))}
          </Select>
        </div>
        {inventoryId !== "" && (
          <button
            type="button"
            onClick={() => setFilters("movements", { inventoryId: "", inventoryLabel: "" })}
            className="flex h-9 items-center gap-1.5 rounded-md bg-primary-subtle px-2.5 text-[13px] font-medium text-primary-hover"
          >
            {inventoryLabel}
            <X aria-label="Clear this filter" className="size-3.5" />
          </button>
        )}
      </div>

      <DataTable
        caption="Stock movement history"
        columns={COLUMNS}
        rows={movements.data?.content ?? []}
        rowKey={(entry) => entry.id}
        loading={movements.loading && !movements.data}
        error={movements.error}
        onRetry={movements.reload}
        emptyTitle="No stock movements found"
      />
      {movements.data && (
        <Pagination
          page={movements.data.number}
          size={movements.data.size}
          totalElements={movements.data.totalElements}
          totalPages={movements.data.totalPages}
          onPageChange={(next) => setFilters("movements", { page: next })}
        />
      )}
    </>
  );
}
