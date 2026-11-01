import { create } from "zustand";
import type { SortState } from "@/components/ui/DataTable";
import type {
  InventoryTransactionType,
  OrderStatus,
  RecordStatus,
  ShipmentStatus,
} from "@/types/api";

interface ListFilters {
  q: string;
  page: number;
  sort: SortState | null;
}

export interface FilterSections {
  products: ListFilters & { categoryId: number | ""; status: RecordStatus | "" };
  warehouses: ListFilters & { status: RecordStatus | "" };
  inventory: ListFilters & { warehouseId: number | ""; lowStockOnly: boolean };
  orders: ListFilters & { status: OrderStatus | ""; warehouseId: number | "" };
  dispatches: ListFilters & { status: ShipmentStatus | ""; warehouseId: number | "" };
  movements: ListFilters & {
    warehouseId: number | "";
    type: InventoryTransactionType | "";
    inventoryId: number | "";
    inventoryLabel: string;
  };
}

const LIST_DEFAULTS: ListFilters = { q: "", page: 0, sort: null };

export const FILTER_DEFAULTS: FilterSections = {
  products: { ...LIST_DEFAULTS, categoryId: "", status: "" },
  warehouses: { ...LIST_DEFAULTS, status: "" },
  inventory: { ...LIST_DEFAULTS, warehouseId: "", lowStockOnly: false },
  orders: { ...LIST_DEFAULTS, status: "", warehouseId: "" },
  dispatches: { ...LIST_DEFAULTS, status: "", warehouseId: "" },
  movements: { ...LIST_DEFAULTS, warehouseId: "", type: "", inventoryId: "", inventoryLabel: "" },
};

interface FilterState {
  filters: FilterSections;
  setFilters: <S extends keyof FilterSections>(
    section: S,
    patch: Partial<FilterSections[S]>,
  ) => void;
}

/** Keeps each list's filters while moving between pages. Changing a filter returns to page one. */
export const useFilterStore = create<FilterState>()((set) => ({
  filters: FILTER_DEFAULTS,
  setFilters: (section, patch) =>
    set((state) => ({
      filters: {
        ...state.filters,
        [section]: { ...state.filters[section], page: 0, ...patch },
      },
    })),
}));

export function sortParam(sort: SortState | null): string | undefined {
  return sort ? `${sort.key},${sort.direction}` : undefined;
}
