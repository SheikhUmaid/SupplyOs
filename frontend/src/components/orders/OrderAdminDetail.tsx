"use client";

import { ArrowLeft, RefreshCw, TriangleAlert } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { StatusBadge } from "@/components/ui/Badge";
import { Button, buttonClasses } from "@/components/ui/Button";
import { ConfirmModal } from "@/components/ui/ConfirmModal";
import { EmptyState, ErrorState, Panel, Skeleton } from "@/components/ui/Feedback";
import { useApiQuery } from "@/hooks/useApiQuery";
import { ApiError } from "@/services/api/client";
import { allocationService, orderService } from "@/services/orders/orders";
import { useUiStore } from "@/stores/ui/uiStore";
import { formatDateTime, formatInr } from "@/utils/format";
import { ORDER_STATUS, PAYMENT_STATUS } from "@/utils/status";
import { AllocationDecision } from "./AllocationDecision";

function orNull<T>(request: Promise<T>): Promise<T | null> {
  return request.catch((error: unknown) => {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  });
}

export function OrderAdminDetail() {
  const id = Number(useParams<{ id: string }>().id);
  const pushToast = useUiStore((state) => state.pushToast);
  const order = useApiQuery((signal) => orNull(orderService.get(id, signal)), [id]);
  const allocation = useApiQuery((signal) => orNull(allocationService.forOrder(id, signal)), [id]);
  const [rerunning, setRerunning] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  function refresh() {
    order.reload();
    allocation.reload();
  }

  async function rerun() {
    setRerunning(true);
    try {
      const updated = await orderService.reallocate(id);
      if (updated.status === "PENDING") {
        pushToast("info", "Still no warehouse can supply the whole order.");
      } else {
        pushToast("success", `Allocated to ${updated.selectedWarehouse}.`);
      }
      refresh();
    } catch (error) {
      pushToast("error", error instanceof ApiError ? error.message : "Something went wrong.");
    } finally {
      setRerunning(false);
    }
  }

  async function cancel() {
    try {
      await orderService.cancel(id);
      pushToast("success", "Order cancelled. Any reserved stock has been released.");
      refresh();
    } catch (error) {
      pushToast("error", error instanceof ApiError ? error.message : "Something went wrong.");
      throw error;
    }
  }

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
          title="This order doesn't exist"
          action={
            <Link href="/admin/orders" className={buttonClasses("secondary", "sm")}>
              View all orders
            </Link>
          }
        />
      </Panel>
    );
  }

  const data = order.data;
  return (
    <div className="space-y-3">
      <Link
        href="/admin/orders"
        className="inline-flex items-center gap-1 text-[13px] text-ink-muted hover:text-primary"
      >
        <ArrowLeft aria-hidden className="size-3.5" />
        All orders
      </Link>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-lg font-semibold text-ink">{data.orderNumber}</h2>
            <StatusBadge status={ORDER_STATUS[data.status]} />
            <StatusBadge status={PAYMENT_STATUS[data.paymentStatus]} />
          </div>
          <p className="text-[13px] text-ink-muted">
            Placed {formatDateTime(data.createdAt)} by {data.customerName}
          </p>
        </div>
        <div className="flex gap-2">
          {data.status === "PENDING" && (
            <Button loading={rerunning} onClick={rerun}>
              {!rerunning && <RefreshCw aria-hidden className="size-4" />}
              Re-run allocation
            </Button>
          )}
          {data.cancellable && (
            <Button variant="secondary" onClick={() => setCancelling(true)}>
              Cancel order
            </Button>
          )}
        </div>
      </div>

      {data.status === "PENDING" && (
        <p className="flex gap-2 rounded-md bg-warning-subtle px-3 py-2 text-[13px] text-warning">
          <TriangleAlert aria-hidden className="mt-0.5 size-4 shrink-0" />
          This order needs attention: no single warehouse can supply all of it, so nothing is
          reserved. Restock a warehouse and re-run allocation, or cancel the order.
        </p>
      )}

      {allocation.error ? (
        <Panel>
          <ErrorState message={allocation.error} onRetry={allocation.reload} />
        </Panel>
      ) : allocation.data === undefined ? (
        <Skeleton className="h-64 w-full" />
      ) : allocation.data === null ? (
        <Panel>
          <EmptyState title="This order has no allocation decision" />
        </Panel>
      ) : (
        <AllocationDecision allocation={allocation.data} />
      )}

      <div className="grid grid-cols-[minmax(0,1fr)] items-start gap-3 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <Panel title="Items">
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-[13px]">
              <caption className="sr-only">Order items</caption>
              <thead>
                <tr className="border-b border-line bg-surface-muted text-ink-muted">
                  <th scope="col" className="px-4 py-2 font-medium">
                    Product
                  </th>
                  <th scope="col" className="px-4 py-2 text-right font-medium">
                    Quantity
                  </th>
                  <th scope="col" className="px-4 py-2 text-right font-medium">
                    Unit price
                  </th>
                  <th scope="col" className="px-4 py-2 text-right font-medium">
                    Subtotal
                  </th>
                </tr>
              </thead>
              <tbody className="tabular-nums">
                {data.items.map((item) => (
                  <tr key={item.productId} className="border-b border-line">
                    <td className="px-4 py-2.5">
                      <span className="block font-medium text-ink">{item.productName}</span>
                      <span className="font-mono text-xs text-ink-muted">{item.sku}</span>
                    </td>
                    <td className="px-4 py-2.5 text-right">{item.quantity}</td>
                    <td className="px-4 py-2.5 text-right">{formatInr(item.unitPrice)}</td>
                    <td className="px-4 py-2.5 text-right">{formatInr(item.subtotal)}</td>
                  </tr>
                ))}
                <tr>
                  <td colSpan={3} className="px-4 py-2.5 text-right text-ink-muted">
                    Total
                  </td>
                  <td className="px-4 py-2.5 text-right font-semibold text-ink">
                    {formatInr(data.totalAmount)}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </Panel>
        <Panel title="Delivery">
          <dl className="space-y-2.5 p-4 text-[13px]">
            <div>
              <dt className="text-xs text-ink-muted">Recipient</dt>
              <dd className="text-ink">
                {data.deliveryName}, {data.deliveryPhone}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-ink-muted">Address</dt>
              <dd className="text-ink">
                {data.deliveryAddress}, {data.deliveryCity}, {data.deliveryState}{" "}
                {data.deliveryPostalCode}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-ink-muted">Delivery speed</dt>
              <dd className="text-ink">{data.priority === "EXPRESS" ? "Express" : "Standard"}</dd>
            </div>
            <div>
              <dt className="text-xs text-ink-muted">Warehouse</dt>
              <dd className="text-ink">{data.selectedWarehouse ?? "Not assigned"}</dd>
            </div>
          </dl>
        </Panel>
      </div>

      <ConfirmModal
        open={cancelling}
        title="Cancel this order?"
        confirmLabel="Cancel order"
        danger
        onConfirm={cancel}
        onClose={() => setCancelling(false)}
      >
        {data.orderNumber} will be cancelled and any reserved stock returned to its warehouse. This
        can&apos;t be undone.
      </ConfirmModal>
    </div>
  );
}
