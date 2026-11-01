import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import type { StatusDisplay, StatusTone } from "@/utils/status";

const TONES: Record<StatusTone, string> = {
  neutral: "bg-canvas text-ink-muted",
  info: "bg-primary-subtle text-primary-hover",
  success: "bg-success-subtle text-success",
  warning: "bg-warning-subtle text-warning",
  danger: "bg-danger-subtle text-danger",
};

const DOTS: Record<StatusTone, string> = {
  neutral: "bg-ink-subtle",
  info: "bg-primary",
  success: "bg-success",
  warning: "bg-warning",
  danger: "bg-danger",
};

export function Badge({ tone = "neutral", children }: { tone?: StatusTone; children: ReactNode }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-sm px-2 py-0.5 text-xs font-medium whitespace-nowrap",
        TONES[tone],
      )}
    >
      <span aria-hidden className={cn("size-1.5 rounded-full", DOTS[tone])} />
      {children}
    </span>
  );
}

export function StatusBadge({ status }: { status: StatusDisplay }) {
  return <Badge tone={status.tone}>{status.label}</Badge>;
}
