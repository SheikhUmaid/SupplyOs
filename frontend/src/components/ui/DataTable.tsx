import { ArrowDown, ArrowUp } from "lucide-react";
import type { Key, ReactNode } from "react";
import { cn } from "@/lib/cn";
import { EmptyState, ErrorState, Skeleton } from "./Feedback";

export interface Column<T> {
  key: string;
  header: string;
  cell: (row: T) => ReactNode;
  align?: "left" | "right";
  sortable?: boolean;
}

export interface SortState {
  key: string;
  direction: "asc" | "desc";
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => Key;
  caption: string;
  loading?: boolean;
  error?: string | null;
  onRetry?: () => void;
  emptyTitle?: string;
  emptyDescription?: string;
  sort?: SortState | null;
  onSortChange?: (sort: SortState) => void;
  onRowClick?: (row: T) => void;
}

const SKELETON_ROWS = 5;

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  caption,
  loading = false,
  error = null,
  onRetry,
  emptyTitle = "Nothing to show",
  emptyDescription,
  sort = null,
  onSortChange,
  onRowClick,
}: DataTableProps<T>) {
  if (error) return <ErrorState message={error} onRetry={onRetry} />;
  if (!loading && rows.length === 0) {
    return <EmptyState title={emptyTitle} description={emptyDescription} />;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-left text-[13px]" aria-busy={loading}>
        <caption className="sr-only">{caption}</caption>
        <thead>
          <tr className="border-b border-line bg-surface-muted">
            {columns.map((column) => {
              const active = sort?.key === column.key;
              return (
                <th
                  key={column.key}
                  scope="col"
                  aria-sort={
                    active ? (sort.direction === "asc" ? "ascending" : "descending") : undefined
                  }
                  className={cn(
                    "px-4 py-2 font-medium whitespace-nowrap text-ink-muted",
                    column.align === "right" && "text-right",
                  )}
                >
                  {column.sortable && onSortChange ? (
                    <button
                      type="button"
                      className="inline-flex items-center gap-1 hover:text-ink"
                      onClick={() =>
                        onSortChange({
                          key: column.key,
                          direction: active && sort.direction === "asc" ? "desc" : "asc",
                        })
                      }
                    >
                      {column.header}
                      {active &&
                        (sort.direction === "asc" ? (
                          <ArrowUp aria-hidden className="size-3" />
                        ) : (
                          <ArrowDown aria-hidden className="size-3" />
                        ))}
                    </button>
                  ) : (
                    column.header
                  )}
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody>
          {loading
            ? Array.from({ length: SKELETON_ROWS }, (_, index) => (
                <tr key={index} className="border-b border-line last:border-0">
                  {columns.map((column) => (
                    <td key={column.key} className="px-4 py-3">
                      <Skeleton className="h-4 w-full max-w-32" />
                    </td>
                  ))}
                </tr>
              ))
            : rows.map((row) => (
                <tr
                  key={rowKey(row)}
                  tabIndex={onRowClick ? 0 : undefined}
                  onClick={onRowClick && (() => onRowClick(row))}
                  onKeyDown={
                    onRowClick &&
                    ((event) => {
                      if (event.key === "Enter") onRowClick(row);
                    })
                  }
                  className={cn(
                    "border-b border-line last:border-0",
                    onRowClick && "cursor-pointer hover:bg-surface-muted",
                  )}
                >
                  {columns.map((column) => (
                    <td
                      key={column.key}
                      className={cn(
                        "px-4 py-2.5",
                        column.align === "right" && "text-right tabular-nums",
                      )}
                    >
                      {column.cell(row)}
                    </td>
                  ))}
                </tr>
              ))}
        </tbody>
      </table>
    </div>
  );
}
