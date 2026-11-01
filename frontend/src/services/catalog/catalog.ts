import { api } from "@/services/api/client";
import type {
  Category,
  CategoryRequest,
  Page,
  Product,
  ProductRequest,
  RecordStatus,
} from "@/types/api";

export interface ListParams {
  page?: number;
  size?: number;
  sort?: string;
}

export interface ProductQuery extends ListParams {
  q?: string;
  categoryId?: number | "";
  status?: RecordStatus | "";
}

export const categoryService = {
  list: (signal?: AbortSignal) => api.get<Category[]>("/categories", { signal }),
  create: (body: CategoryRequest) => api.post<Category>("/categories", { body }),
  update: (id: number, body: CategoryRequest) => api.put<Category>(`/categories/${id}`, { body }),
};

export const productService = {
  list: (query: ProductQuery, signal?: AbortSignal) =>
    api.get<Page<Product>>("/products", { query: { ...query }, signal }),
  get: (id: number, signal?: AbortSignal) => api.get<Product>(`/products/${id}`, { signal }),
  create: (body: ProductRequest) => api.post<Product>("/products", { body }),
  update: (id: number, body: ProductRequest) => api.put<Product>(`/products/${id}`, { body }),
  deactivate: (id: number) => api.delete<void>(`/products/${id}`),
};
