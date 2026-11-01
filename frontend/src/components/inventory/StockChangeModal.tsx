"use client";

import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/FormControls";
import { Modal } from "@/components/ui/Overlays";
import { inventoryService } from "@/services/inventory/inventory";
import { useUiStore } from "@/stores/ui/uiStore";
import type { InventoryRecord } from "@/types/api";
import { hasErrors, readSaveError, type FormErrors } from "@/utils/formErrors";
import { validateStockChange } from "./stockLevel";

export type StockChangeType = "RESTOCK" | "ADJUSTMENT";

function StockChangeForm({
  record,
  type,
  onSaved,
  onCancel,
}: {
  record: InventoryRecord;
  type: StockChangeType;
  onSaved: () => void;
  onCancel: () => void;
}) {
  const [values, setValues] = useState({
    quantity: "",
    reorderLevel: String(record.reorderLevel),
    reason: "",
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
    const nextErrors = validateStockChange(type, values, record.reorderLevel);
    setErrors(nextErrors);
    if (hasErrors(nextErrors)) return;

    setSaving(true);
    try {
      await inventoryService.update(record.id, {
        type,
        quantity: Number(values.quantity.trim() || 0),
        reorderLevel: Number(values.reorderLevel),
        reason: values.reason.trim(),
      });
      pushToast("success", `Stock updated for ${record.productName}.`);
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
      <dl className="grid grid-cols-3 gap-2 rounded-md bg-surface-muted p-3 text-[13px]">
        <div>
          <dt className="text-xs text-ink-muted">Warehouse</dt>
          <dd className="font-medium text-ink">{record.warehouseName}</dd>
        </div>
        <div>
          <dt className="text-xs text-ink-muted">Available</dt>
          <dd className="font-medium text-ink tabular-nums">{record.availableQuantity}</dd>
        </div>
        <div>
          <dt className="text-xs text-ink-muted">Reserved</dt>
          <dd className="font-medium text-ink tabular-nums">{record.reservedQuantity}</dd>
        </div>
      </dl>
      <div className="grid gap-3 sm:grid-cols-2">
        <Input
          label={type === "RESTOCK" ? "Units to add" : "Change in units"}
          hint={type === "ADJUSTMENT" ? "Use a negative number to remove stock." : undefined}
          required={type === "RESTOCK"}
          inputMode="numeric"
          {...bind("quantity")}
        />
        <Input label="Reorder level" required inputMode="numeric" {...bind("reorderLevel")} />
      </div>
      <Input
        label="Reason"
        required
        placeholder={type === "RESTOCK" ? "Supplier delivery" : "Stock count correction"}
        {...bind("reason")}
      />
      <div className="flex justify-end gap-2 pt-1">
        <Button variant="secondary" onClick={onCancel} disabled={saving}>
          Cancel
        </Button>
        <Button type="submit" loading={saving}>
          {type === "RESTOCK" ? "Add stock" : "Save adjustment"}
        </Button>
      </div>
    </form>
  );
}

export function StockChangeModal({
  change,
  onSaved,
  onClose,
}: {
  change: { record: InventoryRecord; type: StockChangeType } | null;
  onSaved: () => void;
  onClose: () => void;
}) {
  const title = change
    ? `${change.type === "RESTOCK" ? "Add stock" : "Adjust stock"}: ${change.record.productName}`
    : "";
  return (
    <Modal open={change !== null} title={title} onClose={onClose}>
      {change && (
        <StockChangeForm
          key={`${change.record.id}-${change.type}`}
          record={change.record}
          type={change.type}
          onCancel={onClose}
          onSaved={() => {
            onSaved();
            onClose();
          }}
        />
      )}
    </Modal>
  );
}
