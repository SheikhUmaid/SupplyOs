"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { Logo } from "@/components/layout/Logo";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/FormControls";
import { homeFor, safeNextPath } from "@/lib/authRoutes";
import { authService } from "@/services/auth/auth";
import { useAuthStore } from "@/stores/auth/authStore";
import { hasErrors, readSaveError, type FormErrors } from "@/utils/formErrors";
import {
  email as validEmail,
  indianPhone,
  password as validPassword,
  required,
  validate,
} from "@/utils/validators";

const EMPTY = { fullName: "", email: "", phone: "", password: "" };

export function RegisterForm() {
  const router = useRouter();
  const next = safeNextPath(useSearchParams().get("next"));
  const user = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);

  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState<FormErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (user) router.replace(next ?? homeFor(user.role));
  }, [user, next, router]);

  const bind = (field: keyof typeof EMPTY) => ({
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
      fullName: validate(values.fullName, required),
      email: validate(values.email, required, validEmail),
      phone: values.phone.trim() ? validate(values.phone, indianPhone) : null,
      password: validate(values.password, validPassword),
    };
    setErrors(nextErrors);
    setFormError(null);
    if (hasErrors(nextErrors)) return;

    setSubmitting(true);
    try {
      setUser(
        await authService.register({
          fullName: values.fullName.trim(),
          email: values.email.trim(),
          password: values.password,
          phone: values.phone.trim() || undefined,
        }),
      );
    } catch (error) {
      const { fields, message } = readSaveError(error);
      setErrors(fields);
      setFormError(message);
      setSubmitting(false);
    }
  }

  const loginHref = next ? `/login?next=${encodeURIComponent(next)}` : "/login";

  return (
    <div className="w-full max-w-sm">
      <div className="mb-5 flex justify-center">
        <Logo />
      </div>
      <div className="rounded-lg border border-line bg-surface p-5">
        <h1 className="mb-4 text-base font-semibold text-ink">Create your account</h1>
        <form onSubmit={submit} noValidate className="space-y-3">
          {formError && (
            <p
              role="alert"
              className="rounded-md bg-danger-subtle px-3 py-2 text-[13px] text-danger"
            >
              {formError}
            </p>
          )}
          <Input label="Full name" required autoComplete="name" {...bind("fullName")} />
          <Input label="Email" required type="email" autoComplete="email" {...bind("email")} />
          <Input label="Mobile number" type="tel" autoComplete="tel" {...bind("phone")} />
          <Input
            label="Password"
            required
            type="password"
            autoComplete="new-password"
            hint="Use at least 8 characters."
            {...bind("password")}
          />
          <Button type="submit" loading={submitting} className="w-full">
            Create account
          </Button>
        </form>
      </div>
      <p className="mt-4 text-center text-[13px] text-ink-muted">
        Already have an account?{" "}
        <Link href={loginHref} className="font-medium text-primary hover:underline">
          Log in
        </Link>
      </p>
    </div>
  );
}
