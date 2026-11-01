"use client";

import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { CustomerGuard } from "@/components/layout/AdminGuard";
import { StatusBadge } from "@/components/ui/Badge";
import { Button, buttonClasses } from "@/components/ui/Button";
import { ConfirmModal } from "@/components/ui/ConfirmModal";
import { EmptyState, ErrorState, Panel, Skeleton } from "@/components/ui/Feedback";
import { useApiQuery } from "@/hooks/useApiQuery";
import { ApiError } from "@/services/api/client";
import { orderService } from "@/services/orders/orders";
import { useUiStore } from "@/stores/ui/uiStore";
import { formatDateTime, formatInr } from "@/utils/format";
import { ORDER_STATUS, PAYMENT_STATUS } from "@/utils/status";
import { ProductImage } from "./ProductImage";

function Details() {
  const id = Number(useParams<{ id: string }>().id);
  const pushToast = useUiStore((state) => state.pushToast);
  const [cancelling, setCancelling] = useState(false);
  const order = useApiQuery(
    async (signal) => {
      try {
        return await orderService.get(id, signal);
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) return null;
        throw error;
      }
    },
    [id],
  );

  if (order.error) {
    return (
      <Panel>
        <ErrorState message={order.error} onRetry={order.reload} />
      </Panel>
    );
  }
  if (order.data === undefined) return <Skeleton className="h-96 w-full" />;
  if (order.data === null) {
    return (
      <Panel>
        <EmptyState
          title="We couldn't find this order"
          action={
            <Link href="/orders" className={buttonClasses("secondary", "sm")}>
              View my orders
            </Link>
          }
        />
      </Panel>
    );
  }

  const data = order.data;

  async function cancel() {
    try {
      await orderService.cancel(id);
      pushToast("success", "Your order has been cancelled.");
      order.reload();
    } catch (error) {
      pushToast("error", error instanceof ApiError ? error.message : "Something went wrong.");
      throw error;
    }
  }

  return (
    <div className="space-y-3">
      <Link
        href="/orders"
        className="inline-flex items-center gap-1 text-[13px] text-ink-muted hover:text-primary"
      >
        <ArrowLeft aria-hidden className="size-3.5" />
        My orders
      </Link>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="text-lg font-semibold text-ink">{data.orderNumber}</h1>
          <p className="text-[13px] text-ink-muted">Placed {formatDateTime(data.createdAt)}</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge status={ORDER_STATUS[data.status]} />
          <StatusBadge status={PAYMENT_STATUS[data.paymentStatus]} />
          {data.cancellable && (
            <Button variant="secondary" size="sm" onClick={() => setCancelling(true)}>
              Cancel order
            </Button>
          )}
        </div>
      </div>

      {data.status === "PENDING" && (
        <p className="rounded-md bg-warning-subtle px-3 py-2 text-[13px] text-warning">
          No single warehouse can supply your whole order right now, so nothing has been reserved
          and you haven&apos;t been charged. We&apos;ll confirm it when stock arrives, or you can
          cancel it.
        </p>
      )}

      {data.status === "CANCELLED" && (
        <p className="rounded-md bg-danger-subtle px-3 py-2 text-[13px] text-danger">
          Your order has been cancelled
          {data.paymentStatus === "REFUNDED_SIMULATED" && ", and your simulated payment refunded"}.
        </p>
      )}

      <div className="grid items-start gap-4 lg:grid-cols-[1fr_20rem]">
        <Panel title="Items">
          <ul className="divide-y divide-line">
            {data.items.map((item) => (
              <li key={item.productId} className="flex items-center gap-3 p-3">
                <div className="w-14 shrink-0">
                  <ProductImage src={item.imageUrl} alt={item.productName} sizes="56px" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-ink">{item.productName}</p>
                  <p className="text-[13px] text-ink-muted tabular-nums">
                    {item.quantity} × {formatInr(item.unitPrice)}
                  </p>
                </div>
                <p className="text-sm font-semibold text-ink tabular-nums">
                  {formatInr(item.subtotal)}
                </p>
              </li>
            ))}
          </ul>
          <p className="flex justify-between border-t border-line p-3 text-sm">
            <span className="text-ink-muted">Total</span>
            <span className="font-semibold text-ink tabular-nums">
              {formatInr(data.totalAmount)}
            </span>
          </p>
        </Panel>
        <div className="space-y-3">
          <Panel title="Delivery address">
            <address className="p-4 text-[13px] leading-relaxed text-ink not-italic">
              {data.deliveryName}
              <br />
              {data.deliveryAddress}
              <br />
              {data.deliveryCity}, {data.deliveryState} {data.deliveryPostalCode}
              <br />
              {data.deliveryPhone}
            </address>
          </Panel>
          <Panel title="Fulfilment">
            <dl className="space-y-2 p-4 text-[13px]">
              <div className="flex justify-between gap-2">
                <dt className="text-ink-muted">Delivery speed</dt>
                <dd className="text-ink">{data.priority === "EXPRESS" ? "Express" : "Standard"}</dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-ink-muted">Warehouse</dt>
                <dd className="text-ink">{data.selectedWarehouse ?? "Not assigned yet"}</dd>
              </div>
            </dl>
          </Panel>
        </div>
      </div>

      <ConfirmModal
        open={cancelling}
        title="Cancel this order?"
        confirmLabel="Cancel order"
        danger
        onConfirm={cancel}
        onClose={() => setCancelling(false)}
      >
        {data.orderNumber} will be cancelled and your simulated payment refunded. This can&apos;t be
        undone.
      </ConfirmModal>
    </div>
  );
}

export function OrderDetails() {
  return (
    <CustomerGuard>
      <Details />
    </CustomerGuard>
  );
}
