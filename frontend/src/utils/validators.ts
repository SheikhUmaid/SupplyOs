/** Each validator returns an error message, or null when the value is valid. */
export type Validator = (value: string) => string | null;

export const required: Validator = (value) => (value.trim() ? null : "This field is required.");

export const email: Validator = (value) =>
  /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim()) ? null : "Enter a valid email address.";

export const password: Validator = (value) =>
  value.length >= 8 ? null : "Use at least 8 characters.";

export const indianPhone: Validator = (value) =>
  /^(\+91[ -]?)?[6-9]\d{9}$/.test(value.trim()) ? null : "Enter a valid 10-digit mobile number.";

export const postalCode: Validator = (value) =>
  /^[1-9]\d{5}$/.test(value.trim()) ? null : "Enter a valid 6-digit PIN code.";

export const price: Validator = (value) => {
  const trimmed = value.trim();
  return /^\d+(\.\d{1,2})?$/.test(trimmed) && Number(trimmed) >= 0
    ? null
    : "Enter an amount in rupees, with up to 2 decimals.";
};

/** Whole number of at least `min`, as used for stock and reorder quantities. */
export function wholeNumber(min = 0): Validator {
  return (value) => {
    const trimmed = value.trim();
    return /^\d+$/.test(trimmed) && Number(trimmed) >= min
      ? null
      : `Enter a whole number of ${min} or more.`;
  };
}

/** Runs validators in order and returns the first error. */
export function validate(value: string, ...validators: Validator[]): string | null {
  for (const validator of validators) {
    const error = validator(value);
    if (error) return error;
  }
  return null;
}
