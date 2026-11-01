"use client";

import { Plus } from "lucide-react";
import { useState } from "react";
import { StatusBadge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { DataTable, type Column } from "@/components/ui/DataTable";
import { Panel } from "@/components/ui/Feedback";
import { Select } from "@/components/ui/FormControls";
import { Drawer } from "@/components/ui/Overlays";
import { Pagination } from "@/components/ui/Pagination";
import { SearchInput } from "@/components/ui/SearchInput";
import { useApiQuery } from "@/hooks/useApiQuery";
import { warehouseService } from "@/services/warehouses/warehouses";
import { sortParam, useFilterStore } from "@/stores/filters/filterStore";
import { useUiStore } from "@/stores/ui/uiStore";
import type { RecordStatus, Warehouse } from "@/types/api";
import { formatCount } from "@/utils/format";
import { RECORD_STATUS } from "@/utils/status";
import { WarehouseDrawer } from "./WarehouseDrawer";
import { WarehouseForm } from "./WarehouseForm";
import { EMPTY_WAREHOUSE } from "./warehouseForm";

const COLUMNS: Column<Warehouse>[] = [
  {
    key: "name",
    header: "Warehouse",
    sortable: true,
    cell: (warehouse) => <span className="font-medium text-ink">{warehouse.name}</span>,
  },
  { key: "city", header: "City", sortable: true, cell: (warehouse) => warehouse.city },
  {
    key: "products",
    header: "Products",
    align: "right",
    cell: (warehouse) => formatCount(warehouse.stock.productCount),
  },
  {
    key: "available",
    header: "Available",
    align: "right",
    cell: (warehouse) => formatCount(warehouse.stock.totalAvailable),
  },
  {
    key: "reserved",
    header: "Reserved",
    align: "right",
    cell: (warehouse) => formatCount(warehouse.stock.totalReserved),
  },
  {
    key: "lowStock",
    header: "Low stock",
    align: "right",
    cell: (warehouse) =>
      warehouse.stock.lowStockItems > 0 ? (
        <span className="font-medium text-warning">
          {formatCount(warehouse.stock.lowStockItems)}
        </span>
      ) : (
        "0"
      ),
  },
  {
    key: "status",
    header: "Status",
    cell: (warehouse) => <StatusBadge status={RECORD_STATUS[warehouse.status]} />,
  },
];

export function WarehousesAdmin() {
  const filters = useFilterStore((state) => state.filters.warehouses);
  const setFilters = useFilterStore((state) => state.setFilters);
  const pushToast = useUiStore((state) => state.pushToast);
  const [selected, setSelected] = useState<Warehouse | null>(null);
  const [adding, setAdding] = useState(false);

  const { q, status, page, sort } = filters;
  const warehouses = useApiQuery(
    (signal) => warehouseService.list({ q, status, page, sort: sortParam(sort) }, signal),
    [q, status, page, sort],
  );

  return (
    <Panel>
      <div className="flex flex-wrap items-end gap-2 p-3">
        <SearchInput
          label="Search name or city"
          value={q}
          onChange={(value) => setFilters("warehouses", { q: value })}
        />
        <div className="w-36">
          <Select
            label="Status"
            value={status}
            onChange={(event) =>
              setFilters("warehouses", { status: event.target.value as RecordStatus | "" })
            }
          >
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </Select>
        </div>
        <Button className="ml-auto" onClick={() => setAdding(true)}>
          <Plus aria-hidden className="size-4" />
          Add warehouse
        </Button>
      </div>

      <DataTable
        caption="Warehouses"
        columns={COLUMNS}
        rows={warehouses.data?.content ?? []}
        rowKey={(warehouse) => warehouse.id}
        loading={warehouses.loading && !warehouses.data}
        error={warehouses.error}
        onRetry={warehouses.reload}
        emptyTitle="No warehouses found"
        emptyDescription="Try different filters, or add a warehouse."
        sort={sort}
        onSortChange={(next) => setFilters("warehouses", { sort: next })}
        onRowClick={setSelected}
      />
      {warehouses.data && (
        <Pagination
          page={warehouses.data.number}
          size={warehouses.data.size}
          totalElements={warehouses.data.totalElements}
          totalPages={warehouses.data.totalPages}
          onPageChange={(next) => setFilters("warehouses", { page: next })}
        />
      )}

      <WarehouseDrawer
        warehouse={selected}
        onChanged={warehouses.reload}
        onClose={() => setSelected(null)}
      />

      <Drawer open={adding} title="Add warehouse" onClose={() => setAdding(false)}>
        <WarehouseForm
          initial={EMPTY_WAREHOUSE}
          submitLabel="Add warehouse"
          onSubmit={async (request) => {
            await warehouseService.create(request);
            pushToast("success", `${request.name} added.`);
            setAdding(false);
            warehouses.reload();
          }}
        />
      </Drawer>
    </Panel>
  );
}
