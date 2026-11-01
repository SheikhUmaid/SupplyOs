"use client";

import { History, PackagePlus, Plus, SlidersHorizontal } from "lucide-react";
import { useMemo, useState } from "react";
import { StatusBadge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { DataTable, type Column } from "@/components/ui/DataTable";
import { Select } from "@/components/ui/FormControls";
import { Pagination } from "@/components/ui/Pagination";
import { SearchInput } from "@/components/ui/SearchInput";
import { useApiQuery } from "@/hooks/useApiQuery";
import { cn } from "@/lib/cn";
import { inventoryService } from "@/services/inventory/inventory";
import { sortParam, useFilterStore } from "@/stores/filters/filterStore";
import type { InventoryRecord, Warehouse } from "@/types/api";
import { formatCount, formatDateTime } from "@/utils/format";
import { NewStockRecordModal } from "./NewStockRecordModal";
import { StockChangeModal, type StockChangeType } from "./StockChangeModal";
import { stockPercent, stockStatus } from "./stockLevel";

const BAR_COLOURS = {
  success: "bg-success",
  warning: "bg-warning",
  danger: "bg-danger",
  info: "bg-primary",
  neutral: "bg-ink-subtle",
};

function StockBar({ record }: { record: InventoryRecord }) {
  const percent = stockPercent(record);
  const { tone } = stockStatus(record);
  return (
    <div className="flex items-center gap-2">
      <div
        role="progressbar"
        aria-label={`Stock level for ${record.productName}`}
        aria-valuenow={percent}
        aria-valuemin={0}
        aria-valuemax={100}
        className="h-1.5 w-20 overflow-hidden rounded-full bg-line"
      >
        <div className={cn("h-full", BAR_COLOURS[tone])} style={{ width: `${percent}%` }} />
      </div>
      <span className="font-medium text-ink tabular-nums">
        {formatCount(record.availableQuantity)}
      </span>
    </div>
  );
}

export function StockTable({
  warehouses,
  onShowHistory,
}: {
  warehouses: Warehouse[];
  onShowHistory: (record: InventoryRecord) => void;
}) {
  const filters = useFilterStore((state) => state.filters.inventory);
  const setFilters = useFilterStore((state) => state.setFilters);
  const [change, setChange] = useState<{
    record: InventoryRecord;
    type: StockChangeType;
  } | null>(null);
  const [adding, setAdding] = useState(false);

  const { q, warehouseId, lowStockOnly, page, sort } = filters;
  const stock = useApiQuery(
    (signal) =>
      inventoryService.list({ q, warehouseId, lowStockOnly, page, sort: sortParam(sort) }, signal),
    [q, warehouseId, lowStockOnly, page, sort],
  );

  const columns = useMemo<Column<InventoryRecord>[]>(
    () => [
      {
        key: "product",
        header: "Product",
        sortable: true,
        cell: (record) => (
          <>
            <span className="block font-medium text-ink">{record.productName}</span>
            <span className="font-mono text-xs text-ink-muted">{record.sku}</span>
          </>
        ),
      },
      { key: "warehouse", header: "Warehouse", sortable: true, cell: (r) => r.warehouseName },
      {
        key: "available",
        header: "Available",
        sortable: true,
        cell: (record) => <StockBar record={record} />,
      },
      {
        key: "reserved",
        header: "Reserved",
        align: "right",
        sortable: true,
        cell: (record) => formatCount(record.reservedQuantity),
      },
      {
        key: "reorder",
        header: "Reorder level",
        align: "right",
        cell: (record) => formatCount(record.reorderLevel),
      },
      {
        key: "state",
        header: "Status",
        cell: (record) => <StatusBadge status={stockStatus(record)} />,
      },
      {
        key: "lastUpdated",
        header: "Last updated",
        sortable: true,
        cell: (record) => (
          <span className="whitespace-nowrap">{formatDateTime(record.lastUpdated)}</span>
        ),
      },
      {
        key: "actions",
        header: "Actions",
        cell: (record) => (
          <div className="flex gap-1">
            <Button
              variant="secondary"
              size="sm"
              aria-label={`Add stock: ${record.productName} at ${record.warehouseName}`}
              title="Add stock"
              onClick={() => setChange({ record, type: "RESTOCK" })}
            >
              <PackagePlus aria-hidden className="size-4" />
            </Button>
            <Button
              variant="secondary"
              size="sm"
              aria-label={`Adjust stock: ${record.productName} at ${record.warehouseName}`}
              title="Adjust stock"
              onClick={() => setChange({ record, type: "ADJUSTMENT" })}
            >
              <SlidersHorizontal aria-hidden className="size-4" />
            </Button>
            <Button
              variant="secondary"
              size="sm"
              aria-label={`Stock history: ${record.productName} at ${record.warehouseName}`}
              title="Stock history"
              onClick={() => onShowHistory(record)}
            >
              <History aria-hidden className="size-4" />
            </Button>
          </div>
        ),
      },
    ],
    [onShowHistory],
  );

  return (
    <>
      <div className="flex flex-wrap items-end gap-2 p-3">
        <SearchInput
          label="Search product or SKU"
          value={q}
          onChange={(value) => setFilters("inventory", { q: value })}
        />
        <div className="w-48">
          <Select
            label="Warehouse"
            value={warehouseId}
            onChange={(event) =>
              setFilters("inventory", {
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
        <label className="flex h-9 items-center gap-2 text-[13px] text-ink">
          <input
            type="checkbox"
            checked={lowStockOnly}
            onChange={(event) => setFilters("inventory", { lowStockOnly: event.target.checked })}
            className="size-4 accent-primary"
          />
          Low stock only
        </label>
        <Button className="ml-auto" onClick={() => setAdding(true)}>
          <Plus aria-hidden className="size-4" />
          Add stock record
        </Button>
      </div>

      <DataTable
        caption="Inventory by product and warehouse"
        columns={columns}
        rows={stock.data?.content ?? []}
        rowKey={(record) => record.id}
        loading={stock.loading && !stock.data}
        error={stock.error}
        onRetry={stock.reload}
        emptyTitle={lowStockOnly ? "Nothing is below its reorder level" : "No stock records found"}
        emptyDescription={lowStockOnly ? undefined : "Try different filters, or add a record."}
        sort={sort}
        onSortChange={(next) => setFilters("inventory", { sort: next })}
      />
      {stock.data && (
        <Pagination
          page={stock.data.number}
          size={stock.data.size}
          totalElements={stock.data.totalElements}
          totalPages={stock.data.totalPages}
          onPageChange={(next) => setFilters("inventory", { page: next })}
        />
      )}

      <StockChangeModal change={change} onSaved={stock.reload} onClose={() => setChange(null)} />
      <NewStockRecordModal
        open={adding}
        warehouses={warehouses}
        onSaved={stock.reload}
        onClose={() => setAdding(false)}
      />
    </>
  );
}
