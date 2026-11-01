import { api } from "@/services/api/client";
import type { ListParams } from "@/services/catalog/catalog";
import type { Dispatch, Page, ShipmentStatus, Tracking } from "@/types/api";

export interface DispatchQuery extends ListParams {
  q?: string;
  status?: ShipmentStatus | "";
  warehouseId?: number | "";
}

export const dispatchService = {
  list: (query: DispatchQuery, signal?: AbortSignal) =>
    api.get<Page<Dispatch>>("/dispatches", { query: { ...query }, signal }),
  updateStatus: (id: number, status: ShipmentStatus) =>
    api.patch<Dispatch>(`/dispatches/${id}/status`, { body: { status } }),
};

export const trackingService = {
  forOrder: (orderId: number, signal?: AbortSignal) =>
    api.get<Tracking>(`/tracking/order/${orderId}`, { signal }),
  forDispatch: (dispatchId: number, signal?: AbortSignal) =>
    api.get<Tracking>(`/tracking/dispatch/${dispatchId}`, { signal }),
};
