import type { Tracking } from "@/types/api";

export type StepState = "done" | "current" | "upcoming" | "cancelled" | "skipped";

export interface TimelineStepView {
  key: string;
  label: string;
  completedAt: string | null;
  state: StepState;
}

/**
 * Turns the API's steps into display states. The API decides what has happened. The latest completed
 * step is the current one, and steps a cancelled order never reached are marked as skipped.
 */
export function buildTimeline(steps: Tracking["steps"]): TimelineStepView[] {
  const cancelled = steps.some((step) => step.key === "CANCELLED" && step.completedAt);
  const lastDone = steps.reduce((last, step, index) => (step.completedAt ? index : last), -1);

  return steps.map((step, index) => {
    let state: StepState;
    if (step.key === "CANCELLED") state = "cancelled";
    else if (step.completedAt) state = index === lastDone ? "current" : "done";
    else state = cancelled ? "skipped" : "upcoming";
    return { ...step, state };
  });
}

/** The API sends a calendar date, which must not shift with the viewer's time zone. */
export function formatCalendarDate(isoDate: string): string {
  return new Intl.DateTimeFormat("en-IN", { dateStyle: "medium", timeZone: "UTC" }).format(
    new Date(`${isoDate}T00:00:00Z`),
  );
}
