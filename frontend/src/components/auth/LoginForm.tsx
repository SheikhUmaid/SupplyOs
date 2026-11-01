"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { Logo } from "@/components/layout/Logo";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/FormControls";
import { homeFor, safeNextPath } from "@/lib/authRoutes";
import { ApiError } from "@/services/api/client";
import { authService } from "@/services/auth/auth";
import { useAuthStore } from "@/stores/auth/authStore";
import { email as validEmail, required, validate } from "@/utils/validators";

export function LoginForm() {
  const router = useRouter();
  const next = safeNextPath(useSearchParams().get("next"));
  const user = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [emailError, setEmailError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Covers both a successful login and a visitor who was already logged in.
  useEffect(() => {
    if (user) router.replace(next ?? homeFor(user.role));
  }, [user, next, router]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    const nextEmailError = validate(email, required, validEmail);
    const nextPasswordError = validate(password, required);
    setEmailError(nextEmailError);
    setPasswordError(nextPasswordError);
    setFormError(null);
    if (nextEmailError || nextPasswordError) return;

    setSubmitting(true);
    try {
      setUser(await authService.login({ email, password }));
    } catch (error) {
      setFormError(error instanceof ApiError ? error.message : "Something went wrong.");
      setSubmitting(false);
    }
  }

  return (
    <div className="w-full max-w-sm">
      <div className="mb-5 flex justify-center">
        <Logo />
      </div>
      <div className="rounded-lg border border-line bg-surface p-5">
        <h1 className="text-base font-semibold text-ink">Log in</h1>
        <p className="mt-0.5 mb-4 text-[13px] text-ink-muted">
          Customers and administrators both log in here.
        </p>
        <form onSubmit={onSubmit} noValidate className="space-y-3">
          {formError && (
            <p
              role="alert"
              className="rounded-md bg-danger-subtle px-3 py-2 text-[13px] text-danger"
            >
              {formError}
            </p>
          )}
          <Input
            label="Email"
            type="email"
            autoComplete="email"
            value={email}
            error={emailError}
            onChange={(event) => setEmail(event.target.value)}
          />
          <Input
            label="Password"
            type="password"
            autoComplete="current-password"
            value={password}
            error={passwordError}
            onChange={(event) => setPassword(event.target.value)}
          />
          <Button type="submit" loading={submitting} className="w-full">
            Log in
          </Button>
        </form>
      </div>
      <p className="mt-4 text-center text-[13px] text-ink-muted">
        New here?{" "}
        <Link
          href={next ? `/register?next=${encodeURIComponent(next)}` : "/register"}
          className="font-medium text-primary hover:underline"
        >
          Create an account
        </Link>
      </p>
    </div>
  );
}
