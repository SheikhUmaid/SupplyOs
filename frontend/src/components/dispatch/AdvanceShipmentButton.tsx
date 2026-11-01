"use client";

import { ArrowRight } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/services/api/client";
import { dispatchService } from "@/services/dispatch/dispatch";
import { useUiStore } from "@/stores/ui/uiStore";
import type { ShipmentStatus } from "@/types/api";
import { SHIPMENT_STATUS } from "@/utils/status";

/** The server says which status may come next, so the frontend holds no transition rules. */
export function AdvanceShipmentButton({
  dispatchId,
  allowedNextStatuses,
  onAdvanced,
}: {
  dispatchId: number;
  allowedNextStatuses: ShipmentStatus[];
  onAdvanced: () => void;
}) {
  const [saving, setSaving] = useState(false);
  const pushToast = useUiStore((state) => state.pushToast);
  const next = allowedNextStatuses[0];
  if (!next) return null;
  const label = SHIPMENT_STATUS[next].label;

  async function advance(status: ShipmentStatus) {
    setSaving(true);
    try {
      await dispatchService.updateStatus(dispatchId, status);
      pushToast("success", `Shipment marked as ${label}.`);
      onAdvanced();
    } catch (error) {
      pushToast("error", error instanceof ApiError ? error.message : "Something went wrong.");
      // Someone else may have moved it first, so show the current state.
      onAdvanced();
    } finally {
      setSaving(false);
    }
  }

  return (
    <Button loading={saving} onClick={() => advance(next)}>
      Mark as {label}
      {!saving && <ArrowRight aria-hidden className="size-4" />}
    </Button>
  );
}
