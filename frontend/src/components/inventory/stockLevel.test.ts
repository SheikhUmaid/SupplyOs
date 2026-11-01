import { describe, expect, it } from "vitest";
import { stockPercent, stockStatus, validateStockChange } from "./stockLevel";

const counts = (availableQuantity: number, reorderLevel: number, reservedQuantity = 0) => ({
  availableQuantity,
  reservedQuantity,
  reorderLevel,
});

describe("stockStatus", () => {
  it.each([
    [0, 5, "Out of stock"],
    [4, 5, "Low stock"],
    [5, 5, "Healthy"],
    [0, 0, "Out of stock"],
  ])("%i available with reorder level %i is %s", (available, reorder, label) => {
    expect(stockStatus(counts(available, reorder)).label).toBe(label);
  });
});

describe("stockPercent", () => {
  it("sits at halfway when stock reaches the reorder level", () => {
    expect(stockPercent(counts(10, 10))).toBe(50);
  });

  it("is full when everything physical is available and above twice the level", () => {
    expect(stockPercent(counts(50, 5))).toBe(100);
  });

  it("shows reserved stock as not available", () => {
    expect(stockPercent(counts(25, 5, 75))).toBe(25);
  });

  it("handles an empty record", () => {
    expect(stockPercent(counts(0, 0))).toBe(0);
  });
});

describe("validateStockChange", () => {
  const values = { quantity: "5", reorderLevel: "3", reason: "Delivery" };

  it("accepts valid changes", () => {
    expect(validateStockChange("RESTOCK", values, 3).quantity).toBeNull();
    expect(validateStockChange("ADJUSTMENT", { ...values, quantity: "-3" }, 3).quantity).toBeNull();
  });

  it.each(["0", "-1", "1.5", "", "abc"])("restock rejects a quantity of %s", (quantity) => {
    expect(validateStockChange("RESTOCK", { ...values, quantity }, 3).quantity).toBeTruthy();
  });

  it.each(["0", "1.5", "abc"])("adjustment rejects a quantity of %s", (quantity) => {
    expect(validateStockChange("ADJUSTMENT", { ...values, quantity }, 3).quantity).toBeTruthy();
  });

  it("lets an adjustment leave the quantity blank only when changing the reorder level", () => {
    const blank = { ...values, quantity: "" };
    expect(validateStockChange("ADJUSTMENT", blank, 3).quantity).toBeTruthy();
    expect(validateStockChange("ADJUSTMENT", blank, 9).quantity).toBeNull();
  });

  it("always requires a reason and a valid reorder level", () => {
    const errors = validateStockChange(
      "RESTOCK",
      { ...values, reason: " ", reorderLevel: "-1" },
      3,
    );
    expect(errors.reason).toBeTruthy();
    expect(errors.reorderLevel).toBeTruthy();
  });
});
