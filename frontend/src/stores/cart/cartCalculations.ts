export const MAX_LINE_QUANTITY = 99;

export interface CartLine {
  unitPrice: number;
  quantity: number;
}

/** Clamps a requested quantity to what a cart line allows. */
export function clampQuantity(quantity: number): number {
  if (!Number.isFinite(quantity)) return 1;
  return Math.min(MAX_LINE_QUANTITY, Math.max(1, Math.trunc(quantity)));
}

/** Calculated in paise so that amounts like ₹0.10 + ₹0.20 stay exact. */
function toPaise(rupees: number): number {
  return Math.round(rupees * 100);
}

export function lineSubtotal(line: CartLine): number {
  return (toPaise(line.unitPrice) * line.quantity) / 100;
}

export function cartTotal(lines: CartLine[]): number {
  return lines.reduce((paise, line) => paise + toPaise(line.unitPrice) * line.quantity, 0) / 100;
}

export function cartUnitCount(items: { quantity: number }[]): number {
  return items.reduce((units, item) => units + item.quantity, 0);
}
