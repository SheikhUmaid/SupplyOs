import { ChevronLeft, ChevronRight } from "lucide-react";
import { formatCount } from "@/utils/format";
import { Button } from "./Button";

/** `page` is zero-based, matching Spring Data. */
export function Pagination({
  page,
  size,
  totalElements,
  totalPages,
  onPageChange,
}: {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  if (totalElements === 0) return null;
  const first = page * size + 1;
  const last = Math.min((page + 1) * size, totalElements);

  return (
    <nav
      aria-label="Pagination"
      className="flex items-center justify-between gap-3 border-t border-line px-4 py-2.5 text-[13px] text-ink-muted"
    >
      <p className="tabular-nums">
        {formatCount(first)}–{formatCount(last)} of {formatCount(totalElements)}
      </p>
      <div className="flex items-center gap-1.5">
        <Button
          variant="secondary"
          size="sm"
          aria-label="Previous page"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
        >
          <ChevronLeft aria-hidden className="size-4" />
        </Button>
        <span className="px-1 tabular-nums">
          Page {page + 1} of {totalPages}
        </span>
        <Button
          variant="secondary"
          size="sm"
          aria-label="Next page"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
        >
          <ChevronRight aria-hidden className="size-4" />
        </Button>
      </div>
    </nav>
  );
}
