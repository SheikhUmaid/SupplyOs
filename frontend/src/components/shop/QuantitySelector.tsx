import { Minus, Plus } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { MAX_LINE_QUANTITY } from "@/stores/cart/cartCalculations";

export function QuantitySelector({
  label,
  value,
  onChange,
}: {
  label: string;
  value: number;
  onChange: (value: number) => void;
}) {
  return (
    <div className="inline-flex items-center gap-1">
      <Button
        variant="secondary"
        size="sm"
        aria-label={`Decrease ${label} quantity`}
        disabled={value <= 1}
        onClick={() => onChange(value - 1)}
      >
        <Minus aria-hidden className="size-3.5" />
      </Button>
      <span aria-live="polite" className="w-8 text-center text-sm font-medium tabular-nums">
        {value}
      </span>
      <Button
        variant="secondary"
        size="sm"
        aria-label={`Increase ${label} quantity`}
        disabled={value >= MAX_LINE_QUANTITY}
        onClick={() => onChange(value + 1)}
      >
        <Plus aria-hidden className="size-3.5" />
      </Button>
    </div>
  );
}
