import { api } from "@/services/api/client";
import type { ListParams } from "@/services/catalog/catalog";
import type { Page, RecordStatus, Warehouse, WarehouseDetail, WarehouseRequest } from "@/types/api";

export interface WarehouseQuery extends ListParams {
  q?: string;
  status?: RecordStatus | "";
}

export const warehouseService = {
  list: (query: WarehouseQuery, signal?: AbortSignal) =>
    api.get<Page<Warehouse>>("/warehouses", { query: { ...query }, signal }),
  get: (id: number, signal?: AbortSignal) =>
    api.get<WarehouseDetail>(`/warehouses/${id}`, { signal }),
  create: (body: WarehouseRequest) => api.post<Warehouse>("/warehouses", { body }),
  update: (id: number, body: WarehouseRequest) => api.put<Warehouse>(`/warehouses/${id}`, { body }),
  deactivate: (id: number) => api.delete<void>(`/warehouses/${id}`),
};
