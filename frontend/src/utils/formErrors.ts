import { ApiError } from "@/services/api/client";

export type FormErrors = Record<string, string | null>;

/** Splits a failed save into errors shown beside fields and a message for the toast. */
export function readSaveError(error: unknown): { fields: FormErrors; message: string } {
  if (!(error instanceof ApiError)) return { fields: {}, message: "Something went wrong." };
  const fields: FormErrors = {};
  for (const { field, message } of error.fieldErrors) fields[field] ??= message;
  return { fields, message: error.message };
}

export function hasErrors(errors: FormErrors): boolean {
  return Object.values(errors).some(Boolean);
}
