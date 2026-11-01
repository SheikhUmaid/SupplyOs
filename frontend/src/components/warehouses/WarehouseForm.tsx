"use client";

import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/FormControls";
import { useUiStore } from "@/stores/ui/uiStore";
import type { WarehouseRequest } from "@/types/api";
import { hasErrors, readSaveError, type FormErrors } from "@/utils/formErrors";
import {
  toWarehouseRequest,
  validateWarehouseForm,
  type WarehouseFormValues,
} from "./warehouseForm";

export function WarehouseForm({
  initial,
  submitLabel,
  onSubmit,
}: {
  initial: WarehouseFormValues;
  submitLabel: string;
  onSubmit: (request: WarehouseRequest) => Promise<void>;
}) {
  const [values, setValues] = useState(initial);
  const [errors, setErrors] = useState<FormErrors>({});
  const [saving, setSaving] = useState(false);
  const pushToast = useUiStore((state) => state.pushToast);

  const bind = (field: keyof WarehouseFormValues) => ({
    value: values[field],
    error: errors[field],
    onChange: (event: { target: { value: string } }) => {
      setValues((current) => ({ ...current, [field]: event.target.value }));
      setErrors((current) => ({ ...current, [field]: null }));
    },
  });

  async function submit(event: FormEvent) {
    event.preventDefault();
    const nextErrors = validateWarehouseForm(values);
    setErrors(nextErrors);
    if (hasErrors(nextErrors)) return;

    setSaving(true);
    try {
      await onSubmit(toWarehouseRequest(values));
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
      <Input label="Warehouse name" required {...bind("name")} />
      <Input label="Address" required {...bind("address")} />
      <div className="grid gap-3 sm:grid-cols-3">
        <Input label="City" required {...bind("city")} />
        <Input label="State" required {...bind("state")} />
        <Input label="PIN code" required inputMode="numeric" {...bind("postalCode")} />
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <Input label="Latitude" required inputMode="decimal" {...bind("latitude")} />
        <Input label="Longitude" required inputMode="decimal" {...bind("longitude")} />
      </div>
      <Input label="Contact number" required inputMode="tel" {...bind("contactNumber")} />
      <fieldset className="space-y-2 border-t border-line pt-3">
        <legend className="text-[13px] font-semibold text-ink">Delivery cost parameters</legend>
        <p className="text-xs text-ink-muted">
          The allocation engine estimates cost as base cost + cost per km × distance, and delivery
          time as handling time + travel time.
        </p>
        <div className="grid gap-3 sm:grid-cols-3">
          <Input label="Base cost (₹)" required inputMode="decimal" {...bind("baseDeliveryCost")} />
          <Input label="Cost per km (₹)" required inputMode="decimal" {...bind("costPerKm")} />
          <Input label="Handling (hours)" required inputMode="numeric" {...bind("handlingHours")} />
        </div>
      </fieldset>
      <div className="flex justify-end pt-1">
        <Button type="submit" loading={saving}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
