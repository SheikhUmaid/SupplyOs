import type { AllocationEvaluation } from "@/types/api";

/** Scores and factors are shown to four decimals, like the explanation text. */
export function formatFactor(value: number | null): string {
  return value === null ? "—" : value.toFixed(4);
}

export function formatWeight(value: number): string {
  return value.toFixed(2);
}

export function eligibilityLabel(evaluation: AllocationEvaluation): string {
  return evaluation.eligible ? "Eligible" : (evaluation.rejectionReason ?? "Not eligible");
}

/** Width of a score bar, relative to the highest score in the comparison. */
export function scoreBarPercent(score: number | null, evaluations: AllocationEvaluation[]): number {
  if (score === null) return 0;
  const highest = Math.max(...evaluations.map((evaluation) => evaluation.finalScore ?? 0));
  return highest === 0 ? 0 : Math.round((score / highest) * 100);
}

export function formatKm(value: number): string {
  return `${value.toLocaleString("en-IN", { minimumFractionDigits: 1, maximumFractionDigits: 1 })} km`;
}

export function formatHours(value: number): string {
  return `${value.toFixed(1)} h`;
}
