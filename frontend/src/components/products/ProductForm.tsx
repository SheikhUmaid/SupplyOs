"use client";

import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { Input, Select, Textarea } from "@/components/ui/FormControls";
import { useUiStore } from "@/stores/ui/uiStore";
import type { Category, ProductRequest } from "@/types/api";
import { hasErrors, readSaveError, type FormErrors } from "@/utils/formErrors";
import { toProductRequest, validateProductForm, type ProductFormValues } from "./productForm";

export function ProductForm({
  initial,
  categories,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  initial: ProductFormValues;
  categories: Category[];
  submitLabel: string;
  onSubmit: (request: ProductRequest) => Promise<void>;
  onCancel?: () => void;
}) {
  const [values, setValues] = useState(initial);
  const [errors, setErrors] = useState<FormErrors>({});
  const [saving, setSaving] = useState(false);
  const pushToast = useUiStore((state) => state.pushToast);

  const bind = (field: keyof ProductFormValues) => ({
    value: values[field],
    error: errors[field],
    onChange: (event: { target: { value: string } }) => {
      setValues((current) => ({ ...current, [field]: event.target.value }));
      setErrors((current) => ({ ...current, [field]: null }));
    },
  });

  async function submit(event: FormEvent) {
    event.preventDefault();
    const nextErrors = validateProductForm(values);
    setErrors(nextErrors);
    if (hasErrors(nextErrors)) return;

    setSaving(true);
    try {
      await onSubmit(toProductRequest(values));
    } catch (error) {
      const { fields, message } = readSaveError(error);
      setErrors(fields);
      pushToast("error", message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-3">
      <Input label="Product name" required {...bind("name")} />
      <div className="grid gap-3 sm:grid-cols-2">
        <Input label="SKU" required {...bind("sku")} />
        <Input label="Price (₹)" required inputMode="decimal" {...bind("price")} />
      </div>
      <Select label="Category" required {...bind("categoryId")}>
        <option value="">Choose a category</option>
        {categories.map((category) => (
          <option key={category.id} value={category.id}>
            {category.name}
          </option>
        ))}
      </Select>
      <Textarea label="Description" {...bind("description")} />
      <Input label="Image URL" placeholder="/products/phone.svg" {...bind("imageUrl")} />
      <div className="flex justify-end gap-2 pt-1">
        {onCancel && (
          <Button variant="secondary" onClick={onCancel} disabled={saving}>
            Cancel
          </Button>
        )}
        <Button type="submit" loading={saving}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
