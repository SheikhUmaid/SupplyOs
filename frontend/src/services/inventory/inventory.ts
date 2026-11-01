import { api } from "@/services/api/client";
import type { ListParams } from "@/services/catalog/catalog";
import type {
  InventoryCreateRequest,
  InventoryRecord,
  InventoryTransaction,
  InventoryTransactionType,
  InventoryUpdateRequest,
  Page,
} from "@/types/api";

export interface InventoryQuery extends ListParams {
  q?: string;
  warehouseId?: number | "";
  lowStockOnly?: boolean;
}

export interface MovementQuery extends ListParams {
  q?: string;
  inventoryId?: number | "";
  warehouseId?: number | "";
  type?: InventoryTransactionType | "";
}

export const inventoryService = {
  list: (query: InventoryQuery, signal?: AbortSignal) =>
    api.get<Page<InventoryRecord>>("/inventory", { query: { ...query }, signal }),
  movements: (query: MovementQuery, signal?: AbortSignal) =>
    api.get<Page<InventoryTransaction>>("/inventory/transactions", {
      query: { ...query },
      signal,
    }),
  create: (body: InventoryCreateRequest) => api.post<InventoryRecord>("/inventory", { body }),
  update: (id: number, body: InventoryUpdateRequest) =>
    api.put<InventoryRecord>(`/inventory/${id}`, { body }),
};
