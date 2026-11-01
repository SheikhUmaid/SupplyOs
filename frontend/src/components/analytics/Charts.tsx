"use client";

import { useState, type ReactNode } from "react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  LabelList,
  Line,
  LineChart,
  Rectangle,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { RectangleProps } from "recharts";
import { Button } from "@/components/ui/Button";
import { DataTable } from "@/components/ui/DataTable";
import { EmptyState, Panel } from "@/components/ui/Feedback";
import { cn } from "@/lib/cn";
import { formatCount } from "@/utils/format";
import {
  CHART_COLORS,
  formatBarValue,
  formatDay,
  type BarDatum,
  type ChartSeries,
  type StackedStock,
} from "./analyticsDisplay";

const AXIS_TICK = { fill: "var(--color-ink-muted)", fontSize: 12 };
const GRID = "var(--color-line)";
const SURFACE = "var(--color-surface)";
const NAME_LENGTH = 22;

// Recharts drops zero-length bars, and their labels with them, unless the bar has its own shape.
// A zero still needs its label: "0%" and "No orders" are different facts.
const KEEP_ZERO_BARS = (props: RectangleProps) => <Rectangle {...props} />;

function truncate(name: string): string {
  return name.length > NAME_LENGTH ? `${name.slice(0, NAME_LENGTH - 1)}…` : name;
}

interface TooltipRow {
  label: string;
  value: string;
  color: string;
}

function ChartTooltip({ title, rows }: { title: string; rows: TooltipRow[] }) {
  return (
    <div className="rounded-md border border-line bg-surface px-3 py-2 text-xs shadow-overlay">
      <p className="mb-1 text-ink-muted">{title}</p>
      {rows.map((row) => (
        <p key={row.label} className="flex items-center gap-2">
          <span aria-hidden className="h-0.5 w-3 rounded-full" style={{ background: row.color }} />
          <span className="font-semibold text-ink tabular-nums">{row.value}</span>
          <span className="text-ink-muted">{row.label}</span>
        </p>
      ))}
    </div>
  );
}

export function ChartLegend({ series }: { series: ChartSeries[] }) {
  return (
    <ul className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-ink-muted">
      {series.map((item) => (
        <li key={item.key} className="flex items-center gap-1.5">
          <span aria-hidden className="size-2.5 rounded-sm" style={{ background: item.color }} />
          {item.label}
        </li>
      ))}
    </ul>
  );
}

export function ChartTable({
  caption,
  headers,
  rows,
}: {
  caption: string;
  headers: string[];
  rows: (string | number)[][];
}) {
  return (
    <DataTable<(string | number)[]>
      caption={caption}
      rows={rows}
      rowKey={(row) => String(row[0])}
      columns={headers.map((header, index) => ({
        key: header,
        header,
        align: index === 0 ? "left" : "right",
        cell: (row) => row[index],
      }))}
    />
  );
}

/** A chart with its table beside it, so that every value can be read without hovering. */
export function ChartPanel({
  title,
  note,
  table,
  isEmpty = false,
  emptyTitle = "No data for this period",
  children,
}: {
  title: string;
  note?: string;
  table: ReactNode;
  isEmpty?: boolean;
  emptyTitle?: string;
  children: ReactNode;
}) {
  const [showTable, setShowTable] = useState(false);

  return (
    <Panel
      title={title}
      actions={
        !isEmpty && (
          <Button
            variant="secondary"
            size="sm"
            aria-pressed={showTable}
            aria-label={`${showTable ? "Show chart" : "Show table"}: ${title}`}
            onClick={() => setShowTable((value) => !value)}
          >
            {showTable ? "Chart" : "Table"}
          </Button>
        )
      }
    >
      {isEmpty ? (
        <EmptyState title={emptyTitle} />
      ) : showTable ? (
        table
      ) : (
        <div className="space-y-2 p-4">
          {note && <p className="text-xs text-ink-muted">{note}</p>}
          {children}
        </div>
      )}
    </Panel>
  );
}

export function TrendChart({
  data,
  label,
  height = 240,
}: {
  data: { date: string; count: number }[];
  label: string;
  height?: number;
}) {
  const color = CHART_COLORS[0];
  return (
    <div role="img" aria-label={`${label} per day`} style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          <CartesianGrid vertical={false} stroke={GRID} />
          <XAxis
            dataKey="date"
            tickFormatter={formatDay}
            tick={AXIS_TICK}
            tickLine={false}
            axisLine={{ stroke: GRID }}
            minTickGap={40}
          />
          <YAxis
            allowDecimals={false}
            tick={AXIS_TICK}
            tickLine={false}
            axisLine={false}
            width={36}
          />
          <Tooltip
            cursor={{ stroke: "var(--color-line-strong)" }}
            content={({ active, payload }) => {
              const point = payload?.[0]?.payload as { date: string; count: number } | undefined;
              return active && point ? (
                <ChartTooltip
                  title={formatDay(point.date)}
                  rows={[{ label, value: formatCount(point.count), color }]}
                />
              ) : null;
            }}
          />
          <Area
            type="linear"
            dataKey="count"
            stroke={color}
            strokeWidth={2}
            fill={color}
            fillOpacity={0.1}
            isAnimationActive={false}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

const BAR_THICKNESS = 18;
const BAR_ROW = 32;

/** Single-measure horizontal bars. One hue throughout, because the bar's label gives its identity. */
export function HorizontalBars({
  data,
  label,
  format = formatCount,
  max,
  labelWidth = 128,
}: {
  data: BarDatum[];
  label: string;
  format?: (value: number) => string;
  max?: number;
  labelWidth?: number;
}) {
  const color = CHART_COLORS[0];
  const rows = data.map((bar) => ({ ...bar, length: bar.value ?? 0 }));
  return (
    <div role="img" aria-label={label} style={{ height: data.length * BAR_ROW + 8 }}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart
          data={rows}
          layout="vertical"
          margin={{ top: 4, right: 72, bottom: 4, left: 0 }}
          barSize={BAR_THICKNESS}
        >
          <XAxis type="number" hide domain={[0, max ?? "auto"]} />
          <YAxis
            type="category"
            dataKey="label"
            width={labelWidth}
            tick={AXIS_TICK}
            tickLine={false}
            axisLine={{ stroke: GRID }}
            tickFormatter={truncate}
            interval={0}
          />
          <Tooltip
            cursor={{ fill: "var(--color-surface-muted)" }}
            content={({ active, payload }) => {
              const bar = payload?.[0]?.payload as BarDatum | undefined;
              return active && bar ? (
                <ChartTooltip
                  title={bar.label}
                  rows={[{ label, value: formatBarValue(bar.value, format), color }]}
                />
              ) : null;
            }}
          />
          <Bar
            dataKey="length"
            fill={color}
            radius={[0, 4, 4, 0]}
            shape={KEEP_ZERO_BARS}
            isAnimationActive={false}
          >
            <LabelList
              position="right"
              fill="var(--color-ink)"
              fontSize={12}
              valueAccessor={(entry: { payload?: BarDatum }) =>
                entry.payload ? formatBarValue(entry.payload.value, format) : ""
              }
            />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

/** Lines over time on one shared scale, labelled directly at their right-hand end. */
export function TrendLines({
  data,
  series,
  label,
  height = 260,
}: {
  data: ({ date: string } & Record<string, string | number>)[];
  series: ChartSeries[];
  label: string;
  height?: number;
}) {
  return (
    <div className="space-y-2">
      <ChartLegend series={series} />
      <div role="img" aria-label={label} style={{ height }}>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 12, right: 68, bottom: 0, left: 0 }}>
            <CartesianGrid vertical={false} stroke={GRID} />
            <XAxis
              dataKey="date"
              tickFormatter={formatDay}
              tick={AXIS_TICK}
              tickLine={false}
              axisLine={{ stroke: GRID }}
              minTickGap={40}
            />
            <YAxis
              allowDecimals={false}
              tick={AXIS_TICK}
              tickLine={false}
              axisLine={false}
              width={36}
            />
            <Tooltip
              cursor={{ stroke: "var(--color-line-strong)" }}
              content={({ active, payload }) => {
                const point = payload?.[0]?.payload as (typeof data)[number] | undefined;
                return active && point ? (
                  <ChartTooltip
                    title={formatDay(point.date)}
                    rows={series.map((item) => ({
                      label: item.label,
                      color: item.color,
                      value: formatCount(Number(point[item.key] ?? 0)),
                    }))}
                  />
                ) : null;
              }}
            />
            {series.map((item, seriesIndex) => (
              <Line
                key={item.key}
                type="linear"
                dataKey={item.key}
                stroke={item.color}
                strokeWidth={2}
                dot={false}
                isAnimationActive={false}
                label={(props: { index?: number; x?: number | string; y?: number | string }) =>
                  props.index === data.length - 1 ? (
                    <text
                      key={item.key}
                      x={Number(props.x) + 8}
                      y={Number(props.y)}
                      // Lines often end at the same value, so the labels sit either side of it.
                      dy={seriesIndex === 0 ? -4 : 12}
                      fontSize={12}
                      fill="var(--color-ink-muted)"
                    >
                      {item.label}
                    </text>
                  ) : (
                    <g key={props.index} />
                  )
                }
              />
            ))}
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

/** Part-to-whole per row. Segments are separated by a surface-coloured stroke. */
export function StackedBars({ stock, label }: { stock: StackedStock; label: string }) {
  const lastKey = stock.series.at(-1)?.key;
  return (
    <div className="space-y-2">
      <ChartLegend series={stock.series} />
      <div role="img" aria-label={label} style={{ height: stock.rows.length * BAR_ROW + 8 }}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={stock.rows}
            layout="vertical"
            margin={{ top: 4, right: 48, bottom: 4, left: 0 }}
            barSize={BAR_THICKNESS}
          >
            <XAxis type="number" hide />
            <YAxis
              type="category"
              dataKey="label"
              width={160}
              tick={AXIS_TICK}
              tickLine={false}
              axisLine={{ stroke: GRID }}
              tickFormatter={truncate}
              interval={0}
            />
            <Tooltip
              cursor={{ fill: "var(--color-surface-muted)" }}
              content={({ active, payload }) => {
                const row = payload?.[0]?.payload as StackedStock["rows"][number] | undefined;
                return active && row ? (
                  <ChartTooltip
                    title={`${row.label}: ${formatCount(row.total)} units`}
                    rows={stock.series.map((item) => ({
                      label: item.label,
                      color: item.color,
                      value: formatCount(Number(row[item.key] ?? 0)),
                    }))}
                  />
                ) : null;
              }}
            />
            {stock.series.map((item) => (
              <Bar
                key={item.key}
                dataKey={item.key}
                stackId="stock"
                fill={item.color}
                stroke={SURFACE}
                strokeWidth={2}
                radius={item.key === lastKey ? [0, 4, 4, 0] : 0}
                shape={KEEP_ZERO_BARS}
                isAnimationActive={false}
              >
                {item.key === lastKey && (
                  <LabelList
                    position="right"
                    fill="var(--color-ink)"
                    fontSize={12}
                    valueAccessor={(entry: { payload?: StackedStock["rows"][number] }) =>
                      entry.payload ? formatCount(entry.payload.total) : ""
                    }
                  />
                )}
              </Bar>
            ))}
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

/** Holds the previous render at reduced opacity while a new period loads. */
export function RefetchFrame({ busy, children }: { busy: boolean; children: ReactNode }) {
  return (
    <div aria-busy={busy} className={cn("space-y-3 transition-opacity", busy && "opacity-60")}>
      {children}
    </div>
  );
}
