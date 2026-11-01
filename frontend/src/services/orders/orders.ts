import { api } from "@/services/api/client";
import type { ListParams } from "@/services/catalog/catalog";
import type {
  Allocation,
  AllocationSettings,
  City,
  OrderCreateRequest,
  OrderDetail,
  OrderStatus,
  OrderSummary,
  Page,
} from "@/types/api";

export interface OrderQuery extends ListParams {
  q?: string;
  status?: OrderStatus | "";
  warehouseId?: number | "";
}

export const locationService = {
  cities: (signal?: AbortSignal) => api.get<City[]>("/locations/cities", { signal }),
};

export const orderService = {
  place: (body: OrderCreateRequest, idempotencyKey: string) =>
    api.post<OrderDetail>("/orders", { body, headers: { "Idempotency-Key": idempotencyKey } }),
  mine: (query: ListParams, signal?: AbortSignal) =>
    api.get<Page<OrderSummary>>("/orders/my-orders", { query: { ...query }, signal }),
  list: (query: OrderQuery, signal?: AbortSignal) =>
    api.get<Page<OrderSummary>>("/orders", { query: { ...query }, signal }),
  get: (id: number, signal?: AbortSignal) => api.get<OrderDetail>(`/orders/${id}`, { signal }),
  cancel: (id: number) => api.patch<OrderDetail>(`/orders/${id}/cancel`),
  reallocate: (id: number) => api.post<OrderDetail>(`/orders/${id}/reallocate`),
};

export const allocationService = {
  forOrder: (orderId: number, signal?: AbortSignal) =>
    api.get<Allocation>(`/allocations/${orderId}`, { signal }),
  settings: (signal?: AbortSignal) =>
    api.get<AllocationSettings>("/settings/allocation", { signal }),
};
