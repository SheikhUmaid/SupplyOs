"use client";

import { ErrorState, Panel, Skeleton } from "@/components/ui/Feedback";
import { useApiQuery } from "@/hooks/useApiQuery";
import { allocationService } from "@/services/orders/orders";
import { formatWeight } from "./allocationDisplay";

const FACTORS = [
  {
    key: "distance",
    label: "Distance",
    description:
      "Estimated road distance to the customer, relative to the farthest eligible warehouse.",
  },
  {
    key: "cost",
    label: "Delivery cost",
    description:
      "Base cost + cost per km × distance, relative to the most expensive eligible warehouse.",
  },
  {
    key: "inventory",
    label: "Inventory suitability",
    description: "How much of the warehouse's available stock the order would use.",
  },
  {
    key: "priority",
    label: "Order priority",
    description:
      "Zero for Standard. For Express: delivery time relative to the slowest eligible warehouse.",
  },
] as const;

export function AllocationSettingsView() {
  const settings = useApiQuery((signal) => allocationService.settings(signal), []);

  if (settings.error) {
    return (
      <Panel>
        <ErrorState message={settings.error} onRetry={settings.reload} />
      </Panel>
    );
  }
  if (!settings.data) return <Skeleton className="h-96 w-full" />;

  const { weights, roadFactor, speedKmh } = settings.data;
  return (
    <div className="max-w-3xl space-y-3">
      <Panel title="How warehouses are chosen">
        <ol className="list-decimal space-y-1.5 py-4 pr-4 pl-9 text-sm text-ink">
          <li>
            <span className="font-medium">Eligibility.</span> A warehouse is ruled out if it is
            inactive or can&apos;t supply every item in full. Orders are never split, and no score
            can override this rule.
          </li>
          <li>
            <span className="font-medium">Scoring.</span> Each remaining warehouse gets a weighted
            score from the factors below, each from 0 (best) to 1 (worst). The lowest score wins.
          </li>
          <li>
            <span className="font-medium">Ties.</span> The shorter distance wins, then the lower
            cost, then the lowest warehouse ID.
          </li>
        </ol>
      </Panel>

      <Panel title="Scoring weights">
        <ul className="divide-y divide-line">
          {FACTORS.map(({ key, label, description }) => (
            <li key={key} className="flex items-center gap-4 px-4 py-3">
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-ink">{label}</p>
                <p className="text-xs text-ink-muted">{description}</p>
              </div>
              <span aria-hidden className="h-1.5 w-24 overflow-hidden rounded-full bg-line">
                <span
                  className="block h-full bg-primary"
                  style={{ width: `${weights[key] * 100}%` }}
                />
              </span>
              <span className="w-10 text-right text-sm font-semibold text-ink tabular-nums">
                {formatWeight(weights[key])}
              </span>
            </li>
          ))}
        </ul>
      </Panel>

      <Panel title="Estimates">
        <dl className="divide-y divide-line text-[13px]">
          <div className="flex justify-between gap-3 px-4 py-3">
            <dt className="text-ink-muted">Road distance</dt>
            <dd className="text-ink tabular-nums">Straight-line distance × {roadFactor}</dd>
          </div>
          <div className="flex justify-between gap-3 px-4 py-3">
            <dt className="text-ink-muted">Delivery time</dt>
            <dd className="text-ink tabular-nums">Handling time + distance ÷ {speedKmh} km/h</dd>
          </div>
        </dl>
      </Panel>

      <p className="text-xs text-ink-muted">
        These values are read-only here and are set in the backend&apos;s application.yml. Each
        decision records the weights in force at the time, so past decisions stay explainable after
        a change.
      </p>
    </div>
  );
}
