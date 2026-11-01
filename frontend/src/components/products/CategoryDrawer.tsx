"use client";

import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/Feedback";
import { Input } from "@/components/ui/FormControls";
import { Drawer } from "@/components/ui/Overlays";
import { categoryService } from "@/services/catalog/catalog";
import { useUiStore } from "@/stores/ui/uiStore";
import type { Category } from "@/types/api";
import { readSaveError, type FormErrors } from "@/utils/formErrors";
import { required, validate } from "@/utils/validators";

export function CategoryDrawer({
  open,
  categories,
  onChanged,
  onClose,
}: {
  open: boolean;
  categories: Category[];
  onChanged: () => void;
  onClose: () => void;
}) {
  const [editing, setEditing] = useState<Category | null>(null);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [errors, setErrors] = useState<FormErrors>({});
  const [saving, setSaving] = useState(false);
  const pushToast = useUiStore((state) => state.pushToast);

  function edit(category: Category | null) {
    setEditing(category);
    setName(category?.name ?? "");
    setDescription(category?.description ?? "");
    setErrors({});
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const nameError = validate(name, required);
    setErrors({ name: nameError });
    if (nameError) return;

    setSaving(true);
    const body = { name: name.trim(), description: description.trim() || undefined };
    try {
      if (editing) await categoryService.update(editing.id, body);
      else await categoryService.create(body);
      pushToast("success", editing ? "Category updated." : "Category added.");
      edit(null);
      onChanged();
    } catch (error) {
      const { fields, message } = readSaveError(error);
      setErrors(fields);
      pushToast("error", message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Drawer open={open} title="Categories" onClose={onClose}>
      <form onSubmit={submit} noValidate className="space-y-3 border-b border-line pb-4">
        <Input
          label={editing ? `Rename "${editing.name}"` : "New category"}
          required
          value={name}
          error={errors.name}
          onChange={(event) => setName(event.target.value)}
        />
        <Input
          label="Description"
          value={description}
          error={errors.description}
          onChange={(event) => setDescription(event.target.value)}
        />
        <div className="flex justify-end gap-2">
          {editing && (
            <Button variant="secondary" size="sm" onClick={() => edit(null)}>
              Cancel
            </Button>
          )}
          <Button type="submit" size="sm" loading={saving}>
            {editing ? "Save category" : "Add category"}
          </Button>
        </div>
      </form>
      {categories.length === 0 ? (
        <EmptyState title="No categories yet" />
      ) : (
        <ul className="divide-y divide-line">
          {categories.map((category) => (
            <li key={category.id} className="flex items-center justify-between gap-3 py-2.5">
              <div className="min-w-0">
                <p className="truncate text-[13px] font-medium text-ink">{category.name}</p>
                {category.description && (
                  <p className="truncate text-xs text-ink-muted">{category.description}</p>
                )}
              </div>
              <Button
                variant="ghost"
                size="sm"
                aria-label={`Edit ${category.name}`}
                onClick={() => edit(category)}
              >
                Edit
              </Button>
            </li>
          ))}
        </ul>
      )}
    </Drawer>
  );
}
