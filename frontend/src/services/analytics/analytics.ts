import { api } from "@/services/api/client";
import type {
  Alerts,
  Dashboard,
  DispatchAnalytics,
  InventoryAnalytics,
  OrderAnalytics,
  WarehouseAnalytics,
} from "@/types/api";

export const analyticsService = {
  dashboard: (signal?: AbortSignal) => api.get<Dashboard>("/analytics/dashboard", { signal }),
  alerts: (signal?: AbortSignal) => api.get<Alerts>("/analytics/alerts", { signal }),
  orders: (days: number, signal?: AbortSignal) =>
    api.get<OrderAnalytics>("/analytics/orders", { query: { days }, signal }),
  warehouses: (days: number, signal?: AbortSignal) =>
    api.get<WarehouseAnalytics>("/analytics/warehouses", { query: { days }, signal }),
  inventory: (days: number, signal?: AbortSignal) =>
    api.get<InventoryAnalytics>("/analytics/inventory", { query: { days }, signal }),
  dispatches: (days: number, signal?: AbortSignal) =>
    api.get<DispatchAnalytics>("/analytics/dispatches", { query: { days }, signal }),
};
