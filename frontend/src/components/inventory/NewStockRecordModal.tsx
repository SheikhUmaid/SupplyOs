"use client";

import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { Input, Select } from "@/components/ui/FormControls";
import { Modal } from "@/components/ui/Overlays";
import { useApiQuery } from "@/hooks/useApiQuery";
import { productService } from "@/services/catalog/catalog";
import { inventoryService } from "@/services/inventory/inventory";
import { useUiStore } from "@/stores/ui/uiStore";
import type { Warehouse } from "@/types/api";
import { hasErrors, readSaveError, type FormErrors } from "@/utils/formErrors";
import { validate, wholeNumber } from "@/utils/validators";

function NewStockRecordForm({
  warehouses,
  onSaved,
  onCancel,
}: {
  warehouses: Warehouse[];
  onSaved: () => void;
  onCancel: () => void;
}) {
  const products = useApiQuery(
    (signal) => productService.list({ status: "ACTIVE", size: 100 }, signal),
    [],
  );
  const [values, setValues] = useState({
    productId: "",
    warehouseId: "",
    availableQuantity: "",
    reorderLevel: "",
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [saving, setSaving] = useState(false);
  const pushToast = useUiStore((state) => state.pushToast);

  const bind = (field: keyof typeof values) => ({
    value: values[field],
    error: errors[field],
    onChange: (event: { target: { value: string } }) => {
      setValues((current) => ({ ...current, [field]: event.target.value }));
      setErrors((current) => ({ ...current, [field]: null }));
    },
  });

  async function submit(event: FormEvent) {
    event.preventDefault();
    const nextErrors = {
      productId: values.productId ? null : "Choose a product.",
      warehouseId: values.warehouseId ? null : "Choose a warehouse.",
      availableQuantity: validate(values.availableQuantity, wholeNumber()),
      reorderLevel: validate(values.reorderLevel, wholeNumber()),
    };
    setErrors(nextErrors);
    if (hasErrors(nextErrors)) return;

    setSaving(true);
    try {
      await inventoryService.create({
        productId: Number(values.productId),
        warehouseId: Number(values.warehouseId),
        availableQuantity: Number(values.availableQuantity),
        reorderLevel: Number(values.reorderLevel),
      });
      pushToast("success", "Stock record added.");
      onSaved();
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
      <Select label="Product" required {...bind("productId")}>
        <option value="">Choose a product</option>
        {products.data?.content.map((product) => (
          <option key={product.id} value={product.id}>
            {product.name} ({product.sku})
          </option>
        ))}
      </Select>
      <Select label="Warehouse" required {...bind("warehouseId")}>
        <option value="">Choose a warehouse</option>
        {warehouses.map((warehouse) => (
          <option key={warehouse.id} value={warehouse.id}>
            {warehouse.name}
          </option>
        ))}
      </Select>
      <div className="grid gap-3 sm:grid-cols-2">
        <Input
          label="Opening quantity"
          required
          inputMode="numeric"
          {...bind("availableQuantity")}
        />
        <Input label="Reorder level" required inputMode="numeric" {...bind("reorderLevel")} />
      </div>
      <div className="flex justify-end gap-2 pt-1">
        <Button variant="secondary" onClick={onCancel} disabled={saving}>
          Cancel
        </Button>
        <Button type="submit" loading={saving}>
          Add stock record
        </Button>
      </div>
    </form>
  );
}

export function NewStockRecordModal({
  open,
  warehouses,
  onSaved,
  onClose,
}: {
  open: boolean;
  warehouses: Warehouse[];
  onSaved: () => void;
  onClose: () => void;
}) {
  return (
    <Modal open={open} title="Add stock record" onClose={onClose}>
      <NewStockRecordForm
        warehouses={warehouses}
        onCancel={onClose}
        onSaved={() => {
          onSaved();
          onClose();
        }}
      />
    </Modal>
  );
}
