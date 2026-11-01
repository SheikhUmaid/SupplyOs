import type { StatusDisplay } from "@/utils/status";

export interface StockCounts {
  availableQuantity: number;
  reservedQuantity: number;
  reorderLevel: number;
}

export function stockStatus({ availableQuantity, reorderLevel }: StockCounts): StatusDisplay {
  if (availableQuantity === 0) return { label: "Out of stock", tone: "danger" };
  if (availableQuantity < reorderLevel) return { label: "Low stock", tone: "warning" };
  return { label: "Healthy", tone: "success" };
}

/**
 * How full the stock bar is. The scale is twice the reorder level, or the physical stock if larger,
 * so a bar sitting at the halfway mark means stock has reached its reorder level.
 */
export function stockPercent({
  availableQuantity,
  reservedQuantity,
  reorderLevel,
}: StockCounts): number {
  const scale = Math.max(reorderLevel * 2, availableQuantity + reservedQuantity);
  return scale === 0 ? 0 : Math.round((availableQuantity / scale) * 100);
}

export interface StockChangeValues {
  quantity: string;
  reorderLevel: string;
  reason: string;
}

/** Availability is checked by the server under a row lock, not here. */
export function validateStockChange(
  type: "RESTOCK" | "ADJUSTMENT",
  values: StockChangeValues,
  currentReorderLevel: number,
): Record<string, string | null> {
  const quantity = values.quantity.trim();
  const reorderLevel = values.reorderLevel.trim();
  const reorderChanged = reorderLevel !== String(currentReorderLevel);

  let quantityError: string | null = null;
  if (type === "RESTOCK") {
    if (!/^\d+$/.test(quantity) || Number(quantity) === 0) {
      quantityError = "Enter how many units to add.";
    }
  } else if (quantity === "") {
    if (!reorderChanged) quantityError = "Enter a change, such as 5 or -3.";
  } else if (!/^-?\d+$/.test(quantity) || Number(quantity) === 0) {
    quantityError = "Enter a whole number other than zero, such as 5 or -3.";
  }

  return {
    quantity: quantityError,
    reorderLevel: /^\d+$/.test(reorderLevel) ? null : "Enter a whole number of 0 or more.",
    reason: values.reason.trim() ? null : "Enter a reason. It is kept in the stock history.",
  };
}
