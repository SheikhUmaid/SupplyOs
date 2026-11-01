"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { CustomerGuard } from "@/components/layout/AdminGuard";
import { StatusBadge } from "@/components/ui/Badge";
import { DataTable } from "@/components/ui/DataTable";
import { Panel } from "@/components/ui/Feedback";
import { Pagination } from "@/components/ui/Pagination";
import { useApiQuery } from "@/hooks/useApiQuery";
import { orderService } from "@/services/orders/orders";
import { formatCount, formatDate, formatInr } from "@/utils/format";
import { ORDER_STATUS } from "@/utils/status";

function Orders() {
  const router = useRouter();
  const [page, setPage] = useState(0);
  const orders = useApiQuery((signal) => orderService.mine({ page }, signal), [page]);

  return (
    <div className="space-y-3">
      <h1 className="text-lg font-semibold text-ink">My orders</h1>
      <Panel>
        <DataTable
          caption="My orders"
          rows={orders.data?.content ?? []}
          rowKey={(order) => order.id}
          loading={orders.loading && !orders.data}
          error={orders.error}
          onRetry={orders.reload}
          emptyTitle="You haven't placed any orders yet"
          onRowClick={(order) => router.push(`/orders/${order.id}`)}
          columns={[
            {
              key: "orderNumber",
              header: "Order",
              cell: (order) => <span className="font-medium text-ink">{order.orderNumber}</span>,
            },
            { key: "date", header: "Date", cell: (order) => formatDate(order.createdAt) },
            {
              key: "units",
              header: "Items",
              align: "right",
              cell: (order) => formatCount(order.unitCount),
            },
            {
              key: "total",
              header: "Total",
              align: "right",
              cell: (order) => formatInr(order.totalAmount),
            },
            {
              key: "status",
              header: "Status",
              cell: (order) => <StatusBadge status={ORDER_STATUS[order.status]} />,
            },
          ]}
        />
        {orders.data && (
          <Pagination
            page={orders.data.number}
            size={orders.data.size}
            totalElements={orders.data.totalElements}
            totalPages={orders.data.totalPages}
            onPageChange={setPage}
          />
        )}
      </Panel>
    </div>
  );
}

export function OrdersList() {
  return (
    <CustomerGuard>
      <Orders />
    </CustomerGuard>
  );
}
