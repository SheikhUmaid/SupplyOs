"use client";

import { useCallback, useState } from "react";
import { Panel } from "@/components/ui/Feedback";
import { Tabs } from "@/components/ui/Tabs";
import { useApiQuery } from "@/hooks/useApiQuery";
import { useUrlSearch } from "@/hooks/useUrlSearch";
import { warehouseService } from "@/services/warehouses/warehouses";
import { useFilterStore } from "@/stores/filters/filterStore";
import type { InventoryRecord } from "@/types/api";
import { MovementsTable } from "./MovementsTable";
import { StockTable } from "./StockTable";

type Tab = "stock" | "movements";

const TABS = [
  { id: "stock", label: "Stock levels" },
  { id: "movements", label: "Movement history" },
] as const;

export function InventoryAdmin() {
  useUrlSearch("inventory");
  const [tab, setTab] = useState<Tab>("stock");
  const setFilters = useFilterStore((state) => state.setFilters);
  const warehouses = useApiQuery((signal) => warehouseService.list({ size: 100 }, signal), []);
  const warehouseList = warehouses.data?.content ?? [];

  const showHistory = useCallback(
    (record: InventoryRecord) => {
      setFilters("movements", {
        inventoryId: record.id,
        inventoryLabel: `${record.productName} at ${record.warehouseName}`,
        q: "",
        warehouseId: "",
        type: "",
      });
      setTab("movements");
    },
    [setFilters],
  );

  return (
    <Panel>
      <div className="px-3 pt-2">
        <Tabs label="Inventory views" tabs={TABS} active={tab} onChange={setTab} />
      </div>
      {tab === "stock" ? (
        <StockTable warehouses={warehouseList} onShowHistory={showHistory} />
      ) : (
        <MovementsTable warehouses={warehouseList} />
      )}
    </Panel>
  );
}
