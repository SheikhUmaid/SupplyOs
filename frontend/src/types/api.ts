import type { components } from "./api.generated";

/** API types come from the backend's OpenAPI document. Refresh them with `npm run gen:api`. */
type Schemas = components["schemas"];

/** The envelope returned by every paginated list endpoint. Page numbers are zero-based. */
export interface Page<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type CurrentUser = Schemas["UserResponse"];
export type RegisterRequest = Schemas["RegisterRequest"];
export type UserRole = CurrentUser["role"];

export type Category = Schemas["CategoryResponse"];
export type CategoryRequest = Schemas["CategoryRequest"];

export type Product = Schemas["ProductResponse"];
export type ProductRequest = Schemas["ProductRequest"];
export type Availability = Product["availability"];
export type RecordStatus = Product["status"];

export type Warehouse = Schemas["WarehouseResponse"];
export type WarehouseRequest = Schemas["WarehouseRequest"];
export type WarehouseDetail = Schemas["WarehouseDetailResponse"];

export type InventoryRecord = Schemas["InventoryResponse"];
export type InventoryCreateRequest = Schemas["InventoryCreateRequest"];
export type InventoryUpdateRequest = Schemas["InventoryUpdateRequest"];
export type InventoryTransaction = Schemas["InventoryTransactionResponse"];
export type InventoryTransactionType = InventoryTransaction["type"];

export type City = Schemas["CityResponse"];
export type OrderCreateRequest = Schemas["OrderCreateRequest"];
export type OrderDetail = Schemas["OrderDetailResponse"];
export type OrderSummary = Schemas["OrderSummaryResponse"];
export type Allocation = Schemas["AllocationResponse"];
export type AllocationEvaluation = Schemas["EvaluationResponse"];
export type AllocationSettings = Schemas["AllocationSettingsResponse"];
export type OrderStatus = OrderDetail["status"];
export type PaymentStatus = OrderDetail["paymentStatus"];

export type Dispatch = Schemas["DispatchResponse"];
export type ShipmentStatus = Dispatch["status"];
export type Tracking = Schemas["TrackingResponse"];

export type Network = Schemas["NetworkResponse"];
export type NetworkWarehouse = Schemas["NetworkWarehouseResponse"];
export type NetworkShipment = Schemas["NetworkShipmentResponse"];

export type Dashboard = Schemas["DashboardResponse"];
export type Alerts = Schemas["AlertsResponse"];
export type OrderAnalytics = Schemas["OrderAnalyticsResponse"];
export type WarehouseAnalytics = Schemas["WarehouseAnalyticsResponse"];
export type WarehouseComparison = Schemas["WarehouseComparisonResponse"];
export type InventoryAnalytics = Schemas["InventoryAnalyticsResponse"];
export type DispatchAnalytics = Schemas["DispatchAnalyticsResponse"];
export type LabelledCount = Schemas["LabelledCountResponse"];
