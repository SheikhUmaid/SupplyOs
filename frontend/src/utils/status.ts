import type {
  AllocationEvaluation,
  Availability,
  InventoryTransactionType,
  OrderStatus,
  PaymentStatus,
  RecordStatus,
  ShipmentStatus,
} from "@/types/api";

export type StatusTone = "neutral" | "info" | "success" | "warning" | "danger";

export interface StatusDisplay {
  label: string;
  tone: StatusTone;
}

export const ORDER_STATUS: Record<OrderStatus, StatusDisplay> = {
  PENDING: { label: "Pending", tone: "warning" },
  CONFIRMED: { label: "Confirmed", tone: "info" },
  PROCESSING: { label: "Processing", tone: "info" },
  DISPATCHED: { label: "Dispatched", tone: "info" },
  DELIVERED: { label: "Delivered", tone: "success" },
  CANCELLED: { label: "Cancelled", tone: "danger" },
};

export const SHIPMENT_STATUS: Record<ShipmentStatus, StatusDisplay> = {
  PENDING: { label: "Pending", tone: "warning" },
  PACKED: { label: "Packed", tone: "info" },
  DISPATCHED: { label: "Dispatched", tone: "info" },
  IN_TRANSIT: { label: "In Transit", tone: "info" },
  DELIVERED: { label: "Delivered", tone: "success" },
  CANCELLED: { label: "Cancelled", tone: "danger" },
};

export const PAYMENT_STATUS: Record<PaymentStatus, StatusDisplay> = {
  UNPAID: { label: "Unpaid", tone: "warning" },
  PAID_SIMULATED: { label: "Paid (simulated)", tone: "success" },
  REFUNDED_SIMULATED: { label: "Refunded (simulated)", tone: "neutral" },
};

export const EVALUATION_DECISION: Record<AllocationEvaluation["decision"], StatusDisplay> = {
  SELECTED: { label: "Selected", tone: "success" },
  NOT_SELECTED: { label: "Not selected", tone: "neutral" },
  REJECTED: { label: "Rejected", tone: "danger" },
};

export const RECORD_STATUS: Record<RecordStatus, StatusDisplay> = {
  ACTIVE: { label: "Active", tone: "success" },
  INACTIVE: { label: "Inactive", tone: "neutral" },
};

export const AVAILABILITY: Record<Availability, StatusDisplay> = {
  IN_STOCK: { label: "In stock", tone: "success" },
  LOW: { label: "Low stock", tone: "warning" },
  OUT: { label: "Out of stock", tone: "danger" },
};

export const MOVEMENT_TYPE: Record<InventoryTransactionType, StatusDisplay> = {
  RESTOCK: { label: "Restock", tone: "success" },
  ADJUSTMENT: { label: "Adjustment", tone: "warning" },
  RESERVE: { label: "Reserve", tone: "info" },
  RELEASE: { label: "Release", tone: "neutral" },
  SHIP: { label: "Ship", tone: "info" },
};
