"use client";

import { AlertTriangle, Bell, Clock } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Dropdown } from "@/components/ui/Dropdown";
import { EmptyState, ErrorState } from "@/components/ui/Feedback";
import { useApiQuery } from "@/hooks/useApiQuery";
import { useAutoRefresh } from "@/hooks/useAutoRefresh";
import { analyticsService } from "@/services/analytics/analytics";
import { useFilterStore } from "@/stores/filters/filterStore";
import { formatCount } from "@/utils/format";

const REFRESH_MS = 60000;

function plural(count: number, one: string, many: string): string {
  return `${formatCount(count)} ${count === 1 ? one : many}`;
}

/** Real counts of what needs attention: orders waiting for stock and items below reorder level. */
export function NotificationsMenu({ triggerClassName }: { triggerClassName: string }) {
  const pathname = usePathname();
  const setFilters = useFilterStore((state) => state.setFilters);
  const alerts = useApiQuery((signal) => analyticsService.alerts(signal), [pathname]);
  useAutoRefresh(alerts.reload, REFRESH_MS);

  const pending = alerts.data?.pendingOrders ?? 0;
  const lowStock = alerts.data?.lowStockItems ?? 0;
  const total = pending + lowStock;

  return (
    <Dropdown
      label={total > 0 ? `Notifications: ${formatCount(total)}` : "Notifications"}
      triggerClassName={`${triggerClassName} relative`}
      trigger={
        <>
          <Bell aria-hidden className="size-4.5" strokeWidth={1.75} />
          {total > 0 && (
            <span
              aria-hidden
              className="absolute top-0.5 right-0.5 min-w-4 rounded-full bg-danger px-1 text-center text-[10px] leading-4 font-semibold text-white tabular-nums"
            >
              {total > 99 ? "99+" : total}
            </span>
          )}
        </>
      }
    >
      {(close) =>
        alerts.error && !alerts.data ? (
          <ErrorState message={alerts.error} onRetry={alerts.reload} />
        ) : total === 0 ? (
          <EmptyState title="No notifications" />
        ) : (
          <div className="py-1 text-[13px]">
            {pending > 0 && (
              <Link
                href="/admin/orders"
                className="flex items-center gap-2 px-3 py-2 text-ink hover:bg-canvas"
                onClick={() => {
                  setFilters("orders", { status: "PENDING" });
                  close();
                }}
              >
                <Clock aria-hidden className="size-4 shrink-0 text-warning" strokeWidth={1.75} />
                {plural(pending, "order waiting for stock", "orders waiting for stock")}
              </Link>
            )}
            {lowStock > 0 && (
              <Link
                href="/admin/inventory"
                className="flex items-center gap-2 px-3 py-2 text-ink hover:bg-canvas"
                onClick={() => {
                  setFilters("inventory", { lowStockOnly: true });
                  close();
                }}
              >
                <AlertTriangle
                  aria-hidden
                  className="size-4 shrink-0 text-warning"
                  strokeWidth={1.75}
                />
                {plural(lowStock, "item below reorder level", "items below reorder level")}
              </Link>
            )}
          </div>
        )
      }
    </Dropdown>
  );
}
