import type { InventoryAnalytics, LabelledCount, WarehouseComparison } from "@/types/api";
import type { StatusDisplay } from "@/utils/status";

/** Checked with the dataviz palette validator. Aqua and yellow need labels, so a table is offered. */
export const CHART_COLORS = ["#1a5fc9", "#eb6834", "#1baf7a", "#eda100", "#6250d6"] as const;
const OTHER_COLOR = "#8a94a3";
const PRIMARY = CHART_COLORS[0];

export interface ChartSeries {
  key: string;
  label: string;
  color: string;
}

export interface BarDatum {
  label: string;
  value: number | null;
}

const day = new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", timeZone: "UTC" });

/** Reporting days arrive as plain dates, already in Indian time. */
export function formatDay(isoDate: string): string {
  return day.format(new Date(`${isoDate}T00:00:00Z`));
}

export function formatRate(rate: number | null, whenNull = "No data"): string {
  return rate === null ? whenNull : `${Math.round(rate * 100)}%`;
}

export function warehouseLabel(warehouse: WarehouseComparison): string {
  return warehouse.active ? warehouse.name : `${warehouse.name} (inactive)`;
}

/** Every stage in lifecycle order, including empty ones, so the shape reads the same each time. */
export function statusBars(
  counts: LabelledCount[],
  statuses: Record<string, StatusDisplay>,
): BarDatum[] {
  const byKey = new Map(counts.map((entry) => [entry.key, entry.count]));
  return Object.entries(statuses).map(([key, status]) => ({
    label: status.label,
    value: byKey.get(key) ?? 0,
  }));
}

export function hasValues(bars: BarDatum[]): boolean {
  return bars.some((bar) => (bar.value ?? 0) > 0);
}

export const singleSeries = (label: string): ChartSeries[] => [
  { key: "value", label, color: PRIMARY },
];

export interface StackedStock {
  series: ChartSeries[];
  rows: ({ label: string; total: number } & Record<string, string | number>)[];
}

/**
 * One row per product, one series per warehouse. A warehouse keeps its colour however many others
 * exist, and any beyond the palette fold into "Other" rather than getting an invented hue.
 */
export function stackedStock(
  warehouses: InventoryAnalytics["warehouses"],
  products: InventoryAnalytics["productAvailability"],
): StackedStock {
  const named = warehouses.slice(0, CHART_COLORS.length);
  const namedIds = new Set(named.map((warehouse) => warehouse.id));
  const series: ChartSeries[] = named.map((warehouse, index) => ({
    key: `w${warehouse.id}`,
    label: warehouse.name,
    color: CHART_COLORS[index] ?? OTHER_COLOR,
  }));
  if (warehouses.length > named.length) {
    series.push({ key: "other", label: "Other", color: OTHER_COLOR });
  }

  const rows = products.map((product) => {
    const row: StackedStock["rows"][number] = {
      label: product.name,
      total: product.totalAvailable,
    };
    for (const item of series) row[item.key] = 0;
    for (const stock of product.byWarehouse) {
      const key = namedIds.has(stock.warehouseId) ? `w${stock.warehouseId}` : "other";
      row[key] = Number(row[key] ?? 0) + stock.available;
    }
    return row;
  });
  return { series, rows };
}

export function formatBarValue(value: number | null, format: (value: number) => string): string {
  return value === null ? "No orders" : format(value);
}
