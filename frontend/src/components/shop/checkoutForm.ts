import type { FormErrors } from "@/utils/formErrors";
import { indianPhone, postalCode, required, validate } from "@/utils/validators";

export interface CheckoutValues {
  deliveryName: string;
  deliveryPhone: string;
  deliveryAddress: string;
  deliveryCity: string;
  deliveryPostalCode: string;
}

export function validateCheckout(
  values: CheckoutValues,
  simulatedPaymentConfirmed: boolean,
): FormErrors {
  return {
    deliveryName: validate(values.deliveryName, required),
    deliveryPhone: validate(values.deliveryPhone, required, indianPhone),
    deliveryAddress: validate(values.deliveryAddress, required),
    deliveryCity: values.deliveryCity ? null : "Choose a city.",
    deliveryPostalCode: validate(values.deliveryPostalCode, required, postalCode),
    simulatedPaymentConfirmed: simulatedPaymentConfirmed
      ? null
      : "Confirm the simulated payment to place your order.",
  };
}

function newIdempotencyKey(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
}

export interface OrderAttempt {
  signature: string;
  key: string;
}

/**
 * Sent with an order so that it can't be created twice. Retrying the same submission reuses its key,
 * in case the first attempt succeeded but its response was lost. Anything different is a new order.
 */
export function attemptFor(previous: OrderAttempt | null, submission: unknown): OrderAttempt {
  const signature = JSON.stringify(submission);
  return previous?.signature === signature ? previous : { signature, key: newIdempotencyKey() };
}
