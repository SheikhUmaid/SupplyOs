import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "@/services/api/client";
import type { Product } from "@/types/api";
import { hasErrors } from "@/utils/formErrors";
import { ProductForm } from "./ProductForm";
import {
  EMPTY_PRODUCT,
  toProductFormValues,
  toProductRequest,
  validateProductForm,
} from "./productForm";

const valid = {
  name: "Phone",
  sku: "PH-1",
  description: "",
  categoryId: "2",
  price: "1499.50",
  imageUrl: "",
};

describe("validateProductForm", () => {
  it("accepts a valid product", () => {
    expect(hasErrors(validateProductForm(valid))).toBe(false);
  });

  it("reports every required field", () => {
    const errors = validateProductForm(EMPTY_PRODUCT);
    expect(errors.name).toBeTruthy();
    expect(errors.sku).toBeTruthy();
    expect(errors.categoryId).toBeTruthy();
    expect(errors.price).toBeTruthy();
    expect(errors.imageUrl).toBeNull();
  });

  it("matches the backend's rules for SKU, price and image", () => {
    expect(validateProductForm({ ...valid, sku: "has space" }).sku).toBeTruthy();
    expect(validateProductForm({ ...valid, price: "-1" }).price).toBeTruthy();
    expect(
      validateProductForm({ ...valid, imageUrl: "javascript:alert(1)" }).imageUrl,
    ).toBeTruthy();
    expect(validateProductForm({ ...valid, imageUrl: "/products/a.svg" }).imageUrl).toBeNull();
  });
});

describe("product form mapping", () => {
  it("converts form text to the API's types and omits blanks", () => {
    expect(toProductRequest({ ...valid, name: " Phone " })).toEqual({
      name: "Phone",
      sku: "PH-1",
      description: undefined,
      categoryId: 2,
      price: 1499.5,
      imageUrl: undefined,
      status: undefined,
    });
  });

  it("fills the form from a product, turning nulls into blanks", () => {
    const product = {
      name: "Phone",
      sku: "PH-1",
      description: null,
      categoryId: 2,
      price: 10,
      imageUrl: null,
    } as Product;
    expect(toProductFormValues(product)).toEqual({ ...valid, price: "10" });
  });
});

describe("ProductForm", () => {
  const categories = [{ id: 2, name: "Phones", description: null }];

  it("does not submit an invalid form", () => {
    const onSubmit = vi.fn();
    render(
      <ProductForm
        initial={EMPTY_PRODUCT}
        categories={categories}
        submitLabel="Add"
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Add" }));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByLabelText(/Product name/)).toBeInvalid();
  });

  it("shows server field errors beside the right field", async () => {
    const onSubmit = vi
      .fn()
      .mockRejectedValue(
        new ApiError(400, "Please correct the fields.", [
          { field: "sku", message: "Rejected by the server." },
        ]),
      );
    render(
      <ProductForm
        initial={valid}
        categories={categories}
        submitLabel="Save"
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() =>
      expect(screen.getByLabelText(/SKU/)).toHaveAccessibleDescription("Rejected by the server."),
    );
    expect(screen.getByRole("button", { name: "Save" })).toBeEnabled();
  });

  it("clears a field's error as soon as it is edited, leaving the others", () => {
    render(
      <ProductForm
        initial={EMPTY_PRODUCT}
        categories={categories}
        submitLabel="Add"
        onSubmit={vi.fn()}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Add" }));
    expect(screen.getByLabelText(/Product name/)).toBeInvalid();

    fireEvent.change(screen.getByLabelText(/Product name/), { target: { value: "Phone" } });

    expect(screen.getByLabelText(/Product name/)).toBeValid();
    expect(screen.getByLabelText(/SKU/)).toBeInvalid();
  });
});
