import type { Product, ProductRequest } from "@/types/api";
import type { FormErrors } from "@/utils/formErrors";
import { price as validPrice, required, validate } from "@/utils/validators";

export interface ProductFormValues {
  name: string;
  sku: string;
  description: string;
  categoryId: string;
  price: string;
  imageUrl: string;
}

export const EMPTY_PRODUCT: ProductFormValues = {
  name: "",
  sku: "",
  description: "",
  categoryId: "",
  price: "",
  imageUrl: "",
};

export function toProductFormValues(product: Product): ProductFormValues {
  return {
    name: product.name,
    sku: product.sku,
    description: product.description ?? "",
    categoryId: String(product.categoryId),
    price: String(product.price),
    imageUrl: product.imageUrl ?? "",
  };
}

export function validateProductForm(values: ProductFormValues): FormErrors {
  return {
    name: validate(values.name, required),
    sku:
      validate(values.sku, required) ??
      (/^[A-Za-z0-9._-]+$/.test(values.sku.trim())
        ? null
        : "Use letters, numbers, dots, dashes or underscores."),
    categoryId: values.categoryId ? null : "Choose a category.",
    price: validate(values.price, required, validPrice),
    imageUrl:
      !values.imageUrl.trim() || /^(https?:\/\/|\/)/.test(values.imageUrl.trim())
        ? null
        : "Use a web address or a path starting with /.",
  };
}

export function toProductRequest(
  values: ProductFormValues,
  status?: ProductRequest["status"],
): ProductRequest {
  return {
    name: values.name.trim(),
    sku: values.sku.trim(),
    description: values.description.trim() || undefined,
    categoryId: Number(values.categoryId),
    price: Number(values.price),
    imageUrl: values.imageUrl.trim() || undefined,
    status,
  };
}
