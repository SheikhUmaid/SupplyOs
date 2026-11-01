import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/services/api/client";
import { productService } from "@/services/catalog/catalog";
import { useCartStore } from "@/stores/cart/cartStore";
import type { Product } from "@/types/api";
import { hasErrors } from "@/utils/formErrors";
import { buildCartLines, fetchCartProducts, linesTotal } from "./cartLines";
import { attemptFor, validateCheckout } from "./checkoutForm";
import { ProductCard } from "./ProductCard";
import { QuantitySelector } from "./QuantitySelector";

const product = (overrides: Partial<Product> = {}): Product => ({
  id: 1,
  name: "Phone",
  sku: "PH-1",
  description: null,
  categoryId: 1,
  categoryName: "Phones",
  price: 999.5,
  imageUrl: null,
  status: "ACTIVE",
  availability: "IN_STOCK",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
  ...overrides,
});

describe("cart lines", () => {
  it("attaches products and marks withdrawn ones", () => {
    const lines = buildCartLines(
      [
        { productId: 1, quantity: 2 },
        { productId: 2, quantity: 1 },
      ],
      new Map([
        [1, product()],
        [2, null],
      ]),
    );
    expect(lines[0]?.product?.name).toBe("Phone");
    expect(lines[1]?.product).toBeNull();
  });

  it("totals only products that can still be bought", () => {
    const lines = [
      { productId: 1, quantity: 2, product: product({ price: 0.1 }) },
      { productId: 2, quantity: 1, product: product({ price: 0.2 }) },
      { productId: 3, quantity: 9, product: null },
    ];
    expect(linesTotal(lines)).toBe(0.4);
  });

  it("treats a 404 as withdrawn", async () => {
    vi.spyOn(productService, "get").mockRejectedValue(new ApiError(404, "Gone"));
    const products = await fetchCartProducts([5], new AbortController().signal);
    expect(products.get(5)).toBeNull();
  });

  it("fails the load on any other error rather than hiding the product", async () => {
    vi.spyOn(productService, "get").mockRejectedValue(new ApiError(500, "Server down"));
    await expect(fetchCartProducts([5], new AbortController().signal)).rejects.toMatchObject({
      status: 500,
    });
  });
});

describe("validateCheckout", () => {
  const valid = {
    deliveryName: "Asha Rao",
    deliveryPhone: "9876543210",
    deliveryAddress: "12 MG Road",
    deliveryCity: "Davanagere",
    deliveryPostalCode: "577001",
  };

  it("accepts a complete form", () => {
    expect(hasErrors(validateCheckout(valid, true))).toBe(false);
  });

  it("requires the simulated payment to be confirmed", () => {
    expect(validateCheckout(valid, false).simulatedPaymentConfirmed).toBeTruthy();
  });

  it.each([
    ["deliveryName", ""],
    ["deliveryPhone", "123"],
    ["deliveryAddress", " "],
    ["deliveryCity", ""],
    ["deliveryPostalCode", "12"],
  ] as const)("rejects %s = '%s'", (field, value) => {
    expect(validateCheckout({ ...valid, [field]: value }, true)[field]).toBeTruthy();
  });
});

describe("attemptFor", () => {
  const order = { city: "Hubli", items: [{ productId: 1, quantity: 2 }] };

  it("creates a key the server accepts", () => {
    expect(attemptFor(null, order).key).toMatch(/^[0-9a-f]{32}$/);
  });

  it("reuses the key when retrying the same submission", () => {
    const first = attemptFor(null, order);
    expect(attemptFor(first, { ...order }).key).toBe(first.key);
  });

  it("uses a new key when anything in the submission changes", () => {
    const first = attemptFor(null, order);
    expect(attemptFor(first, { ...order, city: "Mysuru" }).key).not.toBe(first.key);
    expect(attemptFor(first, { ...order, items: [{ productId: 1, quantity: 3 }] }).key).not.toBe(
      first.key,
    );
  });

  it("uses a new key for an identical order once the last one is finished", () => {
    const first = attemptFor(null, order);
    expect(attemptFor(null, order).key).not.toBe(first.key);
  });
});

describe("QuantitySelector", () => {
  it("steps and clamps to the allowed range", () => {
    const onChange = vi.fn();
    const { rerender } = render(<QuantitySelector label="Phone" value={1} onChange={onChange} />);
    expect(screen.getByRole("button", { name: "Decrease Phone quantity" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Increase Phone quantity" }));
    expect(onChange).toHaveBeenCalledWith(2);

    rerender(<QuantitySelector label="Phone" value={99} onChange={onChange} />);
    expect(screen.getByRole("button", { name: "Increase Phone quantity" })).toBeDisabled();
  });
});

describe("ProductCard", () => {
  beforeEach(() => useCartStore.getState().clear());

  it("adds one unit to the cart", () => {
    render(<ProductCard product={product()} />);
    fireEvent.click(screen.getByRole("button", { name: "Add Phone to cart" }));
    expect(useCartStore.getState().items).toEqual([{ productId: 1, quantity: 1 }]);
  });

  it("does not offer an out-of-stock product", () => {
    render(<ProductCard product={product({ availability: "OUT" })} />);
    expect(screen.getByRole("button", { name: "Add Phone to cart" })).toBeDisabled();
    expect(screen.getByText("Out of stock")).toBeInTheDocument();
  });

  it("shows the price in rupees and links to details", () => {
    render(<ProductCard product={product()} />);
    expect(screen.getByText("₹999.50")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "View details of Phone" })).toHaveAttribute(
      "href",
      "/products/1",
    );
  });
});
