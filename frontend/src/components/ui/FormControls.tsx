import {
  useId,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from "react";
import { cn } from "@/lib/cn";

interface FieldProps {
  label: string;
  error?: string | null;
  hint?: string;
}

function controlClasses(hasError: boolean, className?: string): string {
  return cn(
    "w-full rounded-md border bg-surface px-3 text-sm text-ink placeholder:text-ink-subtle",
    "disabled:cursor-not-allowed disabled:bg-canvas disabled:text-ink-muted",
    hasError ? "border-danger" : "border-line-strong",
    className,
  );
}

function Field({
  id,
  label,
  error,
  hint,
  required,
  children,
}: FieldProps & { id: string; required?: boolean; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-[13px] font-medium text-ink">
        {label}
        {required && (
          <span aria-hidden className="ml-0.5 text-danger">
            *
          </span>
        )}
      </label>
      {children}
      {error ? (
        <p id={`${id}-message`} className="text-xs text-danger">
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${id}-message`} className="text-xs text-ink-muted">
            {hint}
          </p>
        )
      )}
    </div>
  );
}

function describedBy(id: string, { error, hint }: FieldProps): string | undefined {
  return error || hint ? `${id}-message` : undefined;
}

type InputProps = FieldProps & InputHTMLAttributes<HTMLInputElement>;

export function Input({ label, error, hint, className, ...props }: InputProps) {
  const id = useId();
  return (
    <Field id={id} label={label} error={error} hint={hint} required={props.required}>
      <input
        id={id}
        aria-invalid={Boolean(error) || undefined}
        aria-describedby={describedBy(id, { label, error, hint })}
        className={controlClasses(Boolean(error), cn("h-9", className))}
        {...props}
      />
    </Field>
  );
}

type SelectProps = FieldProps & SelectHTMLAttributes<HTMLSelectElement>;

export function Select({ label, error, hint, className, children, ...props }: SelectProps) {
  const id = useId();
  return (
    <Field id={id} label={label} error={error} hint={hint} required={props.required}>
      <select
        id={id}
        aria-invalid={Boolean(error) || undefined}
        aria-describedby={describedBy(id, { label, error, hint })}
        className={controlClasses(Boolean(error), cn("h-9", className))}
        {...props}
      >
        {children}
      </select>
    </Field>
  );
}

type TextareaProps = FieldProps & TextareaHTMLAttributes<HTMLTextAreaElement>;

export function Textarea({ label, error, hint, className, ...props }: TextareaProps) {
  const id = useId();
  return (
    <Field id={id} label={label} error={error} hint={hint} required={props.required}>
      <textarea
        id={id}
        rows={4}
        aria-invalid={Boolean(error) || undefined}
        aria-describedby={describedBy(id, { label, error, hint })}
        className={controlClasses(Boolean(error), cn("py-2", className))}
        {...props}
      />
    </Field>
  );
}
