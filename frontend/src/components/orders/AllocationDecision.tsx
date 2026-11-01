import { Info } from "lucide-react";
import { StatusBadge } from "@/components/ui/Badge";
import { Panel } from "@/components/ui/Feedback";
import { cn } from "@/lib/cn";
import type { Allocation } from "@/types/api";
import { formatDateTime, formatInr } from "@/utils/format";
import { EVALUATION_DECISION } from "@/utils/status";
import {
  eligibilityLabel,
  formatFactor,
  formatHours,
  formatKm,
  formatWeight,
  scoreBarPercent,
} from "./allocationDisplay";

const CELL = "px-4 py-2.5";
const HEADER = "px-4 py-2 font-medium whitespace-nowrap text-ink-muted";

export function AllocationDecision({ allocation }: { allocation: Allocation }) {
  const { evaluations, weights } = allocation;
  const scored = evaluations.filter((evaluation) => evaluation.finalScore !== null);

  return (
    <Panel title="Allocation decision">
      <div className="space-y-1 border-b border-line p-4">
        <p className="flex gap-2 text-sm leading-relaxed text-ink">
          <Info aria-hidden className="mt-0.5 size-4 shrink-0 text-primary" />
          <span>{allocation.explanation}</span>
        </p>
        <p className="pl-6 text-xs text-ink-muted">
          Attempt {allocation.attemptNo}, decided {formatDateTime(allocation.allocatedAt)}. Lower
          scores are better.
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-left text-[13px]">
          <caption className="sr-only">Warehouse comparison</caption>
          <thead>
            <tr className="border-b border-line bg-surface-muted">
              <th scope="col" className={HEADER}>
                Warehouse
              </th>
              <th scope="col" className={HEADER}>
                Eligibility
              </th>
              <th scope="col" className={cn(HEADER, "text-right")}>
                Distance
              </th>
              <th scope="col" className={cn(HEADER, "text-right")}>
                Delivery cost
              </th>
              <th scope="col" className={cn(HEADER, "text-right")}>
                Delivery time
              </th>
              <th scope="col" className={HEADER}>
                Score
              </th>
              <th scope="col" className={HEADER}>
                Decision
              </th>
            </tr>
          </thead>
          <tbody>
            {evaluations.map((evaluation) => (
              <tr
                key={evaluation.warehouseId}
                className={cn(
                  "border-b border-line last:border-0",
                  evaluation.decision === "SELECTED" && "bg-success-subtle/50",
                )}
              >
                <td className={cn(CELL, "font-medium whitespace-nowrap text-ink")}>
                  {evaluation.warehouseName}
                </td>
                <td className={cn(CELL, !evaluation.eligible && "text-danger")}>
                  {eligibilityLabel(evaluation)}
                </td>
                <td className={cn(CELL, "text-right whitespace-nowrap tabular-nums")}>
                  {formatKm(evaluation.distanceKm)}
                </td>
                <td className={cn(CELL, "text-right whitespace-nowrap tabular-nums")}>
                  {formatInr(evaluation.deliveryCost)}
                </td>
                <td className={cn(CELL, "text-right whitespace-nowrap tabular-nums")}>
                  {formatHours(evaluation.etaHours)}
                </td>
                <td className={CELL}>
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-ink tabular-nums">
                      {formatFactor(evaluation.finalScore)}
                    </span>
                    {evaluation.finalScore !== null && (
                      <span aria-hidden className="h-1.5 w-16 overflow-hidden rounded-full bg-line">
                        <span
                          className="block h-full bg-primary"
                          style={{
                            width: `${scoreBarPercent(evaluation.finalScore, evaluations)}%`,
                          }}
                        />
                      </span>
                    )}
                  </div>
                </td>
                <td className={CELL}>
                  <StatusBadge status={EVALUATION_DECISION[evaluation.decision]} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {scored.length > 0 && (
        <div className="overflow-x-auto border-t border-line">
          <table className="w-full border-collapse text-left text-[13px]">
            <caption className="px-4 pt-3 pb-1.5 text-left text-xs font-semibold text-ink">
              Score breakdown
            </caption>
            <thead>
              <tr className="border-y border-line bg-surface-muted">
                <th scope="col" className={HEADER}>
                  Warehouse
                </th>
                <th scope="col" className={cn(HEADER, "text-right")}>
                  Distance × {formatWeight(weights.distance)}
                </th>
                <th scope="col" className={cn(HEADER, "text-right")}>
                  Cost × {formatWeight(weights.cost)}
                </th>
                <th scope="col" className={cn(HEADER, "text-right")}>
                  Inventory × {formatWeight(weights.inventory)}
                </th>
                <th scope="col" className={cn(HEADER, "text-right")}>
                  Priority × {formatWeight(weights.priority)}
                </th>
                <th scope="col" className={cn(HEADER, "text-right")}>
                  Final score
                </th>
              </tr>
            </thead>
            <tbody className="tabular-nums">
              {scored.map((evaluation) => (
                <tr key={evaluation.warehouseId} className="border-b border-line last:border-0">
                  <td className={cn(CELL, "font-medium whitespace-nowrap text-ink")}>
                    {evaluation.warehouseName}
                  </td>
                  <td className={cn(CELL, "text-right")}>
                    {formatFactor(evaluation.normDistance)}
                  </td>
                  <td className={cn(CELL, "text-right")}>{formatFactor(evaluation.normCost)}</td>
                  <td className={cn(CELL, "text-right")}>
                    {formatFactor(evaluation.inventoryFactor)}
                  </td>
                  <td className={cn(CELL, "text-right")}>
                    {formatFactor(evaluation.priorityFactor)}
                  </td>
                  <td className={cn(CELL, "text-right font-semibold text-ink")}>
                    {formatFactor(evaluation.finalScore)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Panel>
  );
}
