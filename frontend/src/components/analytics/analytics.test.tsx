import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/services/api/client";
import { analyticsService } from "@/services/analytics/analytics";
import { FILTER_DEFAULTS, useFilterStore } from "@/stores/filters/filterStore";
import type { Dashboard, InventoryAnalytics, WarehouseComparison } from "@/types/api";
import { ORDER_STATUS } from "@/utils/status";
import { NotificationsMenu } from "@/components/layout/NotificationsMenu";
import {
  CHART_COLORS,
  formatBarValue,
  formatDay,
  formatRate,
  hasValues,
  stackedStock,
  statusBars,
  warehouseLabel,
} from "./analyticsDisplay";
import { ChartPanel, ChartTable } from "./Charts";
import { DashboardView } from "./DashboardView";

vi.mock("@/components/visualization/NetworkMap", () => ({ NetworkMap: () => null }));
vi.mock("next/navigation", () => ({ usePathname: () => "/admin" }));

class NoResize {
  observe() {}
  unobserve() {}
  disconnect() {}
}

beforeEach(() => {
  vi.stubGlobal("ResizeObserver", NoResize);
});

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  useFilterStore.setState({ filters: FILTER_DEFAULTS });
});

const warehouse = (id: number, name: string): InventoryAnalytics["warehouses"][number] => ({
  id,
  name,
});

describe("analytics display helpers", () => {
  it("shows a reporting day as the day it says, whatever the machine's zone", () => {
    expect(formatDay("2026-03-01")).toBe("1 Mar");
  });

  it("tells a zero rate apart from no data", () => {
    expect(formatRate(0)).toBe("0%");
    expect(formatRate(0.5)).toBe("50%");
    expect(formatRate(null)).toBe("No data");
    expect(formatBarValue(null, String)).toBe("No orders");
    expect(formatBarValue(0, String)).toBe("0");
  });

  it("marks an inactive warehouse", () => {
    const row = { name: "Hubli" } as WarehouseComparison;
    expect(warehouseLabel({ ...row, active: true })).toBe("Hubli");
    expect(warehouseLabel({ ...row, active: false })).toBe("Hubli (inactive)");
  });

  it("lists every status in lifecycle order, including empty ones", () => {
    const bars = statusBars([{ key: "DELIVERED", count: 4 }], ORDER_STATUS);
    expect(bars.map((bar) => bar.label)).toEqual([
      "Pending",
      "Confirmed",
      "Processing",
      "Dispatched",
      "Delivered",
      "Cancelled",
    ]);
    expect(bars[4]?.value).toBe(4);
    expect(bars[0]?.value).toBe(0);
    expect(hasValues(bars)).toBe(true);
    expect(hasValues(statusBars([], ORDER_STATUS))).toBe(false);
  });

  it("stacks stock per warehouse and fills gaps with zero", () => {
    const { series, rows } = stackedStock(
      [warehouse(1, "A"), warehouse(2, "B")],
      [
        {
          productId: 9,
          name: "Phone",
          totalAvailable: 5,
          byWarehouse: [{ warehouseId: 2, available: 5 }],
        },
      ],
    );
    expect(series.map((item) => item.color)).toEqual([CHART_COLORS[0], CHART_COLORS[1]]);
    expect(rows[0]).toEqual({ label: "Phone", total: 5, w1: 0, w2: 5 });
  });

  it("folds warehouses beyond the palette into Other instead of inventing colours", () => {
    const many = [1, 2, 3, 4, 5, 6, 7].map((id) => warehouse(id, `W${id}`));
    const { series, rows } = stackedStock(many, [
      {
        productId: 1,
        name: "Phone",
        totalAvailable: 7,
        byWarehouse: [
          { warehouseId: 1, available: 2 },
          { warehouseId: 6, available: 2 },
          { warehouseId: 7, available: 3 },
        ],
      },
    ]);
    expect(series).toHaveLength(6);
    expect(series.at(-1)?.label).toBe("Other");
    expect(rows[0]?.other).toBe(5);
    expect(rows[0]?.w1).toBe(2);
  });

  it("keeps a warehouse's colour when a later one is added", () => {
    const one = stackedStock([warehouse(1, "A")], []).series[0];
    const two = stackedStock([warehouse(1, "A"), warehouse(2, "B")], []).series[0];
    expect(one?.color).toBe(two?.color);
  });
});

describe("ChartPanel", () => {
  const table = <ChartTable caption="Values" headers={["Day", "Orders"]} rows={[["1 Mar", 3]]} />;

  it("offers the same figures as a table", () => {
    render(
      <ChartPanel title="Orders" table={table}>
        <p>the chart</p>
      </ChartPanel>,
    );
    expect(screen.getByText("the chart")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Show table: Orders" }));
    expect(screen.queryByText("the chart")).not.toBeInTheDocument();
    expect(within(screen.getByRole("table")).getByText("3")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Show chart: Orders" }));
    expect(screen.getByText("the chart")).toBeInTheDocument();
  });

  it("says when there is nothing to chart, with no table button", () => {
    render(
      <ChartPanel title="Orders" table={table} isEmpty emptyTitle="No orders">
        <p>the chart</p>
      </ChartPanel>,
    );
    expect(screen.getByText("No orders")).toBeInTheDocument();
    expect(screen.queryByText("the chart")).not.toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
});

const emptyPage = { content: [], number: 0, size: 5, totalElements: 0, totalPages: 0 };
const dashboard: Dashboard = {
  kpis: {
    totalOrders: 1234,
    pendingOrders: 2,
    totalProducts: 12,
    totalWarehouses: 3,
    totalInventory: 503,
    activeDispatches: 4,
    deliveredOrders: 5,
    lowStockItems: 0,
  },
  orderActivity: [{ date: "2026-03-01", count: 0 }],
  warehousePerformance: [],
  recentOrders: emptyPage,
  inventoryAlerts: emptyPage,
  recentDispatches: emptyPage,
};

describe("DashboardView", () => {
  it("shows the backend's figures and honest empty states", async () => {
    vi.spyOn(analyticsService, "dashboard").mockResolvedValue(dashboard);
    render(<DashboardView />);

    const card = (await screen.findByText("Total orders")).closest("a");
    await waitFor(() => expect(card).toHaveTextContent("1,234"));
    expect(screen.getByText("Low stock items").closest("a")).toHaveTextContent("0");
    expect(screen.getByText("No orders in the last 14 days")).toBeInTheDocument();
    expect(screen.getByText("Nothing below its reorder level")).toBeInTheDocument();
    expect(screen.getByText("No dispatches yet")).toBeInTheDocument();
  });

  it("opens the matching filter from a KPI card", async () => {
    vi.spyOn(analyticsService, "dashboard").mockResolvedValue(dashboard);
    render(<DashboardView />);

    const card = (await screen.findByText("Pending orders")).closest("a");
    fireEvent.click(card as HTMLElement);
    expect(useFilterStore.getState().filters.orders.status).toBe("PENDING");
  });

  it("offers a retry when the figures can't load", async () => {
    const load = vi
      .spyOn(analyticsService, "dashboard")
      .mockRejectedValueOnce(new ApiError(500, "The server failed."))
      .mockResolvedValue(dashboard);
    render(<DashboardView />);

    fireEvent.click(await screen.findByRole("button", { name: "Try again" }));
    expect(await screen.findByText("Total orders")).toBeInTheDocument();
    expect(load).toHaveBeenCalledTimes(2);
  });
});

describe("NotificationsMenu", () => {
  it("counts what needs attention and links to it", async () => {
    vi.spyOn(analyticsService, "alerts").mockResolvedValue({ pendingOrders: 1, lowStockItems: 3 });
    render(<NotificationsMenu triggerClassName="" />);

    fireEvent.click(await screen.findByRole("button", { name: "Notifications: 4" }));
    expect(screen.getByRole("link", { name: "1 order waiting for stock" })).toHaveAttribute(
      "href",
      "/admin/orders",
    );
    fireEvent.click(screen.getByRole("link", { name: "3 items below reorder level" }));
    expect(useFilterStore.getState().filters.inventory.lowStockOnly).toBe(true);
  });

  it("says so when nothing needs attention", async () => {
    vi.spyOn(analyticsService, "alerts").mockResolvedValue({ pendingOrders: 0, lowStockItems: 0 });
    render(<NotificationsMenu triggerClassName="" />);

    await waitFor(() => expect(analyticsService.alerts).toHaveBeenCalled());
    fireEvent.click(screen.getByRole("button", { name: "Notifications" }));
    expect(await screen.findByText("No notifications")).toBeInTheDocument();
  });
});
