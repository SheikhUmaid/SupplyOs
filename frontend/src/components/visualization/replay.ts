import type { Allocation } from "@/types/api";

export type NodeState = "idle" | "evaluating" | "rejected" | "eligible" | "selected";

interface ReplayStep {
  caption: string;
  durationMs: number;
  nodes: Record<number, NodeState>;
  /** True while the route is being drawn, and from then on. */
  route: boolean;
}

export interface Replay {
  steps: ReplayStep[];
  totalMs: number;
  orderNumber: string;
  deliveryCity: string;
  deliveryLatitude: number;
  deliveryLongitude: number;
  selectedWarehouseId: number | null;
}

export interface ReplayFrame {
  stepIndex: number;
  caption: string;
  nodes: Record<number, NodeState>;
  /** From 0 to 1, or null when no route is shown. */
  routeProgress: number | null;
  done: boolean;
}

const STEP_MS = 1100;

/**
 * Builds the animation from the STORED decision. Nothing is recalculated in the browser, so the replay
 * always shows what the system actually decided, even if stock has changed since.
 */
export function buildReplay(allocation: Allocation): Replay {
  const { evaluations } = allocation;
  const rejected = evaluations.filter((evaluation) => !evaluation.eligible);
  const eligible = evaluations.filter((evaluation) => evaluation.eligible);
  const selected = evaluations.find((evaluation) => evaluation.decision === "SELECTED") ?? null;

  const stateOf = (assign: (id: number, isEligible: boolean) => NodeState) =>
    Object.fromEntries(
      evaluations.map((evaluation) => [
        evaluation.warehouseId,
        assign(evaluation.warehouseId, evaluation.eligible),
      ]),
    );
  const judged = stateOf((_, isEligible) => (isEligible ? "eligible" : "rejected"));

  const steps: ReplayStep[] = [
    {
      caption: `Evaluating ${evaluations.length} ${evaluations.length === 1 ? "warehouse" : "warehouses"} for ${allocation.orderNumber}`,
      durationMs: STEP_MS,
      nodes: stateOf(() => "evaluating"),
      route: false,
    },
  ];

  if (rejected.length > 0) {
    steps.push({
      caption: rejected
        .map((evaluation) => `${evaluation.warehouseName}: ${evaluation.rejectionReason}`)
        .join(". "),
      durationMs: STEP_MS * 1.5,
      nodes: stateOf((_, isEligible) => (isEligible ? "evaluating" : "rejected")),
      route: false,
    });
  }

  if (selected) {
    steps.push({
      caption: `Eligible: ${eligible.map((evaluation) => evaluation.warehouseName).join(", ")}`,
      durationMs: STEP_MS,
      nodes: judged,
      route: false,
    });
    const final = stateOf((id, isEligible) => {
      if (id === selected.warehouseId) return "selected";
      return isEligible ? "eligible" : "rejected";
    });
    steps.push({
      caption: `${selected.warehouseName} selected, with the lowest score (${selected.finalScore?.toFixed(4)})`,
      durationMs: STEP_MS,
      nodes: final,
      route: false,
    });
    steps.push({
      caption: `${selected.warehouseName} to ${allocation.deliveryCity}, ${allocation.orderNumber}`,
      durationMs: STEP_MS,
      nodes: final,
      route: true,
    });
  } else {
    steps.push({
      caption: `No warehouse can supply the whole of ${allocation.orderNumber}, so nothing was reserved`,
      durationMs: STEP_MS,
      nodes: judged,
      route: false,
    });
  }

  return {
    steps,
    totalMs: steps.reduce((sum, step) => sum + step.durationMs, 0),
    orderNumber: allocation.orderNumber,
    deliveryCity: allocation.deliveryCity,
    deliveryLatitude: allocation.deliveryLatitude,
    deliveryLongitude: allocation.deliveryLongitude,
    selectedWarehouseId: selected?.warehouseId ?? null,
  };
}

/** The picture at a moment in the replay. It holds on the final frame once finished. */
export function replayFrameAt(replay: Replay, elapsedMs: number): ReplayFrame {
  let stepStart = 0;
  for (let index = 0; index < replay.steps.length; index++) {
    const step = replay.steps[index]!;
    const isLast = index === replay.steps.length - 1;
    if (elapsedMs < stepStart + step.durationMs || isLast) {
      const stepProgress = Math.min(1, Math.max(0, (elapsedMs - stepStart) / step.durationMs));
      return {
        stepIndex: index,
        caption: step.caption,
        nodes: step.nodes,
        routeProgress: step.route ? stepProgress : null,
        done: elapsedMs >= replay.totalMs,
      };
    }
    stepStart += step.durationMs;
  }
  throw new Error("A replay always has at least one step");
}
