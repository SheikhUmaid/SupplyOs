import { beforeEach, describe, expect, it } from "vitest";
import {
  cartTotal,
  cartUnitCount,
  clampQuantity,
  lineSubtotal,
  MAX_LINE_QUANTITY,
} from "./cartCalculations";
import { useCartStore } from "./cartStore";

describe("cart calculations", () => {
  it("calculates a line subtotal", () => {
    expect(lineSubtotal({ unitPrice: 1499.5, quantity: 3 })).toBe(4498.5);
  });

  it("totals lines without floating-point drift", () => {
    expect(
      cartTotal([
        { unitPrice: 0.1, quantity: 1 },
        { unitPrice: 0.2, quantity: 1 },
      ]),
    ).toBe(0.3);
    expect(cartTotal([{ unitPrice: 19.99, quantity: 3 }])).toBe(59.97);
  });

  it("totals an empty cart as zero", () => {
    expect(cartTotal([])).toBe(0);
  });

  it("counts units rather than lines", () => {
    expect(cartUnitCount([{ quantity: 2 }, { quantity: 3 }])).toBe(5);
  });

  it.each([
    [0, 1],
    [-4, 1],
    [2.9, 2],
    [Number.NaN, 1],
    [1000, MAX_LINE_QUANTITY],
  ])("clamps %s to %s", (input, expected) => {
    expect(clampQuantity(input)).toBe(expected);
  });
});

describe("cart store", () => {
  const items = () => useCartStore.getState().items;
  const cart = () => useCartStore.getState();

  beforeEach(() => cart().clear());

  it("merges repeat additions of a product", () => {
    cart().add(1, 2);
    cart().add(2);
    cart().add(1, 3);
    expect(items()).toEqual([
      { productId: 1, quantity: 5 },
      { productId: 2, quantity: 1 },
    ]);
  });

  it("caps merged quantities at the line maximum", () => {
    cart().add(1, 98);
    cart().add(1, 5);
    expect(items()[0]?.quantity).toBe(MAX_LINE_QUANTITY);
  });

  it("does not let setQuantity drop below one", () => {
    cart().add(1, 2);
    cart().setQuantity(1, 0);
    expect(items()[0]?.quantity).toBe(1);
  });

  it("removes only the given product", () => {
    cart().add(1);
    cart().add(2);
    cart().remove(1);
    expect(items().map((item) => item.productId)).toEqual([2]);
  });

  it("clears the cart", () => {
    cart().add(1);
    cart().clear();
    expect(items()).toEqual([]);
  });
});
