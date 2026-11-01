import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/cn";
import { Skeleton } from "./Feedback";

export function StatCard({
  label,
  value,
  hint,
  icon: Icon,
  alert = false,
  loading = false,
}: {
  label: string;
  value: string;
  hint?: string;
  icon?: LucideIcon;
  alert?: boolean;
  loading?: boolean;
}) {
  return (
    <div className="rounded-lg border border-line bg-surface p-4">
      <div className="flex items-center justify-between gap-2">
        <p className="text-[13px] text-ink-muted">{label}</p>
        {Icon && (
          <Icon
            aria-hidden
            className={cn("size-4", alert ? "text-warning" : "text-ink-subtle")}
            strokeWidth={1.75}
          />
        )}
      </div>
      {loading ? (
        <Skeleton className="mt-2 h-7 w-20" />
      ) : (
        <p
          className={cn(
            "mt-1 text-2xl font-semibold tabular-nums",
            alert ? "text-warning" : "text-ink",
          )}
        >
          {value}
        </p>
      )}
      {hint && <p className="mt-1 text-xs text-ink-muted">{hint}</p>}
    </div>
  );
}
