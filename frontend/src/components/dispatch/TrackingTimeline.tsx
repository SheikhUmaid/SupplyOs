"use client";

import { motion } from "framer-motion";
import { Check, X } from "lucide-react";
import { cn } from "@/lib/cn";
import { formatDateTime } from "@/utils/format";
import type { Tracking } from "@/types/api";
import { buildTimeline, type StepState } from "./trackingDisplay";

const DOT: Record<StepState, string> = {
  done: "border-success bg-success text-white",
  current: "border-primary bg-primary text-white",
  upcoming: "border-line-strong bg-surface",
  skipped: "border-line bg-canvas",
  cancelled: "border-danger bg-danger text-white",
};

const STATE_LABEL: Partial<Record<StepState, string>> = {
  upcoming: "Not yet reached",
  skipped: "Did not happen",
};

export function TrackingTimeline({ steps }: { steps: Tracking["steps"] }) {
  const timeline = buildTimeline(steps);

  return (
    <ol aria-label="Shipment progress">
      {timeline.map((step, index) => {
        const reached = step.state !== "upcoming" && step.state !== "skipped";
        return (
          <li key={step.key} className="flex gap-3">
            <div className="flex flex-col items-center">
              <motion.span
                aria-hidden
                key={step.state}
                initial={{ scale: reached ? 0.7 : 1 }}
                animate={{ scale: 1 }}
                transition={{ duration: 0.2 }}
                className={cn(
                  "flex size-5 shrink-0 items-center justify-center rounded-full border-2",
                  DOT[step.state],
                )}
              >
                {step.state === "cancelled" ? (
                  <X className="size-3" strokeWidth={3} />
                ) : (
                  reached && <Check className="size-3" strokeWidth={3} />
                )}
              </motion.span>
              {index < timeline.length - 1 && (
                <span
                  aria-hidden
                  className={cn("w-0.5 flex-1", step.state === "done" ? "bg-success" : "bg-line")}
                />
              )}
            </div>
            <div className="min-h-10 pb-3">
              <p
                aria-current={step.state === "current" ? "step" : undefined}
                className={cn(
                  "text-[13px] leading-5",
                  reached ? "font-medium text-ink" : "text-ink-subtle",
                )}
              >
                {step.label}
              </p>
              <p className="text-xs text-ink-muted">
                {step.completedAt ? formatDateTime(step.completedAt) : STATE_LABEL[step.state]}
              </p>
            </div>
          </li>
        );
      })}
    </ol>
  );
}
