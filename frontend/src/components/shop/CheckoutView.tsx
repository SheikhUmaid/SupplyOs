"use client";

import { ShoppingCart } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { CustomerGuard } from "@/components/layout/AdminGuard";
import { Button, buttonClasses } from "@/components/ui/Button";
import { EmptyState, ErrorState, Panel, Skeleton } from "@/components/ui/Feedback";
import { Input, Select } from "@/components/ui/FormControls";
import { useApiQuery } from "@/hooks/useApiQuery";
import { useCartLines } from "@/hooks/useCartLines";
import { locationService, orderService } from "@/services/orders/orders";
import { useAuthStore } from "@/stores/auth/authStore";
import { lineSubtotal } from "@/stores/cart/cartCalculations";
import { useCartStore } from "@/stores/cart/cartStore";
import { useUiStore } from "@/stores/ui/uiStore";
import type { OrderCreateRequest } from "@/types/api";
import { formatInr } from "@/utils/format";
import { hasErrors, readSaveError, type FormErrors } from "@/utils/formErrors";
import { linesTotal } from "./cartLines";
import {
  attemptFor,
  validateCheckout,
  type CheckoutValues,
  type OrderAttempt,
} from "./checkoutForm";

type Priority = OrderCreateRequest["priority"];

const SPEEDS: { value: Priority; label: string; hint: string }[] = [
  { value: "STANDARD", label: "Standard", hint: "The most economical route." },
  { value: "EXPRESS", label: "Express", hint: "Favours the fastest warehouse. No extra charge." },
];

function CheckoutForm() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const clearCart = useCartStore((state) => state.clear);
  const pushToast = useUiStore((state) => state.pushToast);
  const clearToasts = useUiStore((state) => state.clearToasts);
  const { lines, loading, error, reload } = useCartLines();
  const cities = useApiQuery((signal) => locationService.cities(signal), []);

  const [values, setValues] = useState<CheckoutValues>({
    deliveryName: user?.fullName ?? "",
    deliveryPhone: user?.phone ?? "",
    deliveryAddress: "",
    deliveryCity: "",
    deliveryPostalCode: "",
  });
  const [priority, setPriority] = useState<Priority>("STANDARD");
  const [paid, setPaid] = useState(false);
  const [errors, setErrors] = useState<FormErrors>({});
  const [placing, setPlacing] = useState(false);
  const [placed, setPlaced] = useState(false);
  const lastAttempt = useRef<OrderAttempt | null>(null);

  // Next keeps this page mounted but hidden after leaving it. Returning must start a fresh checkout.
  useEffect(
    () => () => {
      setPlaced(false);
      setPaid(false);
    },
    [],
  );

  const bind = (field: keyof CheckoutValues) => ({
    value: values[field],
    error: errors[field],
    onChange: (event: { target: { value: string } }) => {
      setValues((current) => ({ ...current, [field]: event.target.value }));
      setErrors((current) => ({ ...current, [field]: null }));
    },
  });

  if (error) {
    return (
      <Panel>
        <ErrorState message={error} onRetry={reload} />
      </Panel>
    );
  }
  if (loading || placed) return <Skeleton className="h-96 w-full" />;
  if (lines.length === 0) {
    return (
      <Panel>
        <EmptyState
          icon={ShoppingCart}
          title="Your cart is empty"
          action={
            <Link href="/products" className={buttonClasses("secondary", "sm")}>
              Browse products
            </Link>
          }
        />
      </Panel>
    );
  }
  if (lines.some((line) => !line.product)) {
    return (
      <Panel>
        <EmptyState
          title="Your cart has an unavailable product"
          description="Remove it from your cart, then check out."
          action={
            <Link href="/cart" className={buttonClasses("secondary", "sm")}>
              Return to cart
            </Link>
          }
        />
      </Panel>
    );
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const nextErrors = validateCheckout(values, paid);
    setErrors(nextErrors);
    if (hasErrors(nextErrors)) return;

    const request: OrderCreateRequest = {
      ...values,
      priority,
      simulatedPaymentConfirmed: true,
      items: lines.map(({ productId, quantity }) => ({ productId, quantity })),
    };
    const attempt = attemptFor(lastAttempt.current, request);
    lastAttempt.current = attempt;
    clearToasts(); // An earlier failure message mustn't linger on the confirmation page.
    setPlacing(true);
    try {
      const order = await orderService.place(request, attempt.key);
      lastAttempt.current = null;
      setPlacing(false);
      setPlaced(true);
      clearCart();
      router.replace(`/orders/${order.id}`);
    } catch (saveError) {
      const { fields, message } = readSaveError(saveError);
      setErrors(fields);
      pushToast("error", message);
      setPlacing(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="grid items-start gap-4 lg:grid-cols-[1fr_22rem]">
      <div className="space-y-3">
        <h1 className="text-lg font-semibold text-ink">Checkout</h1>
        <Panel title="Delivery details">
          <div className="grid gap-3 p-4 sm:grid-cols-2">
            <Input label="Name" required autoComplete="name" {...bind("deliveryName")} />
            <Input label="Contact number" required type="tel" {...bind("deliveryPhone")} />
            <div className="sm:col-span-2">
              <Input
                label="Address"
                required
                autoComplete="street-address"
                {...bind("deliveryAddress")}
              />
            </div>
            <Select
              label="City"
              required
              hint="We currently deliver to these Karnataka cities."
              {...bind("deliveryCity")}
            >
              <option value="">Choose a city</option>
              {cities.data?.map((city) => (
                <option key={city.name} value={city.name}>
                  {city.name}
                </option>
              ))}
            </Select>
            <Input label="State" value="Karnataka" readOnly disabled />
            <Input label="PIN code" required inputMode="numeric" {...bind("deliveryPostalCode")} />
          </div>
        </Panel>
        <Panel title="Delivery speed">
          <fieldset className="grid gap-2 p-4 sm:grid-cols-2">
            <legend className="sr-only">Delivery speed</legend>
            {SPEEDS.map((speed) => (
              <label
                key={speed.value}
                className="flex cursor-pointer gap-2.5 rounded-md border border-line p-3 has-checked:border-primary has-checked:bg-primary-subtle"
              >
                <input
                  type="radio"
                  name="priority"
                  value={speed.value}
                  checked={priority === speed.value}
                  onChange={() => setPriority(speed.value)}
                  className="mt-0.5 accent-primary"
                />
                <span>
                  <span className="block text-[13px] font-medium text-ink">{speed.label}</span>
                  <span className="block text-xs text-ink-muted">{speed.hint}</span>
                </span>
              </label>
            ))}
          </fieldset>
        </Panel>
      </div>

      <Panel title="Order summary" className="lg:mt-10">
        <ul className="divide-y divide-line px-4">
          {lines.map(({ productId, quantity, product }) => (
            <li key={productId} className="flex justify-between gap-3 py-2.5 text-[13px]">
              <span className="text-ink">
                {product!.name} × {quantity}
              </span>
              <span className="text-ink tabular-nums">
                {formatInr(lineSubtotal({ unitPrice: product!.price, quantity }))}
              </span>
            </li>
          ))}
        </ul>
        <div className="space-y-3 border-t border-line p-4">
          <p className="flex justify-between text-sm">
            <span className="text-ink-muted">Total</span>
            <span className="text-lg font-semibold text-ink tabular-nums">
              {formatInr(linesTotal(lines))}
            </span>
          </p>
          <div>
            <label className="flex gap-2 text-[13px] text-ink">
              <input
                type="checkbox"
                checked={paid}
                aria-invalid={Boolean(errors.simulatedPaymentConfirmed) || undefined}
                onChange={(event) => {
                  setPaid(event.target.checked);
                  setErrors((current) => ({ ...current, simulatedPaymentConfirmed: null }));
                }}
                className="mt-0.5 size-4 shrink-0 accent-primary"
              />
              I confirm this simulated payment. This is a demonstration, and no real money moves.
            </label>
            {errors.simulatedPaymentConfirmed && (
              <p className="mt-1 text-xs text-danger">{errors.simulatedPaymentConfirmed}</p>
            )}
          </div>
          <Button type="submit" loading={placing} className="w-full">
            Place order
          </Button>
        </div>
      </Panel>
    </form>
  );
}

export function CheckoutView() {
  return (
    <CustomerGuard>
      <CheckoutForm />
    </CustomerGuard>
  );
}
