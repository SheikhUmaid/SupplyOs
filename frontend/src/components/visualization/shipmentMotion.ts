import type { ShipmentStatus } from "@/types/api";
import type { Point } from "./projection";

const TRANSIT_CYCLE_MS = 5000;
const FADE_FRACTION = 0.12;

export interface MarkerPlacement {
  /** How far along the route, from 0 at the warehouse to 1 at the customer. */
  progress: number;
  alpha: number;
}

/** Only these statuses animate. The rest hold still, so an idle map uses no GPU time. */
export function isMoving(status: ShipmentStatus): boolean {
  return status === "DISPATCHED" || status === "IN_TRANSIT";
}

/**
 * Where a marker sits, decided only by the shipment's status. This is a simulation: nothing here
 * knows where a parcel really is, and the map says so. Moving markers travel their stage of the route
 * and fade out at the end of each pass, so that restarting doesn't look like the parcel jumped back.
 */
export function placeMarker(
  status: ShipmentStatus,
  elapsedMs: number,
  reducedMotion: boolean,
): MarkerPlacement {
  const stage = TRANSIT_STAGES[status];
  if (stage) {
    if (reducedMotion) return { progress: (stage.from + stage.to) / 2, alpha: 1 };
    const phase = (elapsedMs % TRANSIT_CYCLE_MS) / TRANSIT_CYCLE_MS;
    const fade = Math.min(1, phase / FADE_FRACTION, (1 - phase) / FADE_FRACTION);
    return { progress: stage.from + (stage.to - stage.from) * phase, alpha: fade };
  }
  return { progress: REST_POSITIONS[status] ?? 0, alpha: 1 };
}

const TRANSIT_STAGES: Partial<Record<ShipmentStatus, { from: number; to: number }>> = {
  DISPATCHED: { from: 0.05, to: 0.4 },
  IN_TRANSIT: { from: 0.35, to: 0.95 },
};

const REST_POSITIONS: Partial<Record<ShipmentStatus, number>> = {
  PENDING: 0,
  PACKED: 0.04,
  DELIVERED: 1,
};

const NODE_CLEARANCE_PX = 20;

/**
 * Fits progress into the part of the route beyond the warehouse node, so that a waiting marker stays
 * visible beside its warehouse. Scaling rather than clamping keeps motion smooth on short routes.
 */
export function clearOfNode(from: Point, to: Point, progress: number): number {
  const length = Math.hypot(to.x - from.x, to.y - from.y);
  if (length === 0) return progress;
  const clearance = Math.min(1, NODE_CLEARANCE_PX / length);
  return clearance + (1 - clearance) * progress;
}

export function pointOnRoute(from: Point, to: Point, progress: number): Point {
  return {
    x: from.x + (to.x - from.x) * progress,
    y: from.y + (to.y - from.y) * progress,
  };
}
