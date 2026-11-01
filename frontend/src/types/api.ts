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

// Replaced by a generated type when the dispatch API arrives.
export type ShipmentStatus =
  "PENDING" | "PACKED" | "DISPATCHED" | "IN_TRANSIT" | "DELIVERED" | "CANCELLED";
