import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/services/api/client";
import { dispatchService } from "@/services/dispatch/dispatch";
import type { Tracking } from "@/types/api";
import { AdvanceShipmentButton } from "./AdvanceShipmentButton";
import { buildTimeline, formatCalendarDate } from "./trackingDisplay";
import { TrackingTimeline } from "./TrackingTimeline";

const AT = "2026-03-01T10:00:00Z";
const step = (key: string, completedAt: string | null): Tracking["steps"][number] => ({
  key,
  label: key,
  completedAt,
});

const packed = [
  step("PLACED", AT),
  step("CONFIRMED", AT),
  step("ALLOCATED", AT),
  step("PACKED", AT),
  step("DISPATCHED", null),
  step("IN_TRANSIT", null),
  step("DELIVERED", null),
];

describe("buildTimeline", () => {
  it("marks the latest completed step as current and the rest by what happened", () => {
    expect(buildTimeline(packed).map((s) => s.state)).toEqual([
      "done",
      "done",
      "done",
      "current",
      "upcoming",
      "upcoming",
      "upcoming",
    ]);
  });

  it("shows a delivered order with its last step current", () => {
    const delivered = packed.map((s) => ({ ...s, completedAt: AT }));
    expect(buildTimeline(delivered).at(-1)?.state).toBe("current");
  });

  it("marks unreached steps of a cancelled order as skipped, not upcoming", () => {
    const cancelled = [...packed, step("CANCELLED", AT)];
    const states = buildTimeline(cancelled).map((s) => s.state);
    expect(states.slice(4, 7)).toEqual(["skipped", "skipped", "skipped"]);
    expect(states.at(-1)).toBe("cancelled");
  });

  it("handles a pending order that has only been placed", () => {
    const states = buildTimeline([step("PLACED", AT), step("CONFIRMED", null)]).map((s) => s.state);
    expect(states).toEqual(["current", "upcoming"]);
  });
});

describe("formatCalendarDate", () => {
  it("shows the date itself, whatever the viewer's time zone", () => {
    expect(formatCalendarDate("2026-03-05")).toBe("5 Mar 2026");
  });
});

describe("TrackingTimeline", () => {
  it("marks the current step for assistive technology and labels unreached steps", () => {
    render(<TrackingTimeline steps={packed} />);
    expect(screen.getByText("PACKED")).toHaveAttribute("aria-current", "step");
    expect(screen.getAllByText("Not yet reached")).toHaveLength(3);
  });
});

describe("AdvanceShipmentButton", () => {
  afterEach(() => vi.restoreAllMocks());

  it("offers only the status the server allows", () => {
    render(
      <AdvanceShipmentButton
        dispatchId={1}
        allowedNextStatuses={["IN_TRANSIT"]}
        onAdvanced={vi.fn()}
      />,
    );
    expect(screen.getByRole("button", { name: "Mark as In Transit" })).toBeInTheDocument();
  });

  it("renders nothing for a finished shipment", () => {
    const { container } = render(
      <AdvanceShipmentButton dispatchId={1} allowedNextStatuses={[]} onAdvanced={vi.fn()} />,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("sends the next status and reports back", async () => {
    const update = vi.spyOn(dispatchService, "updateStatus").mockResolvedValue({} as never);
    const onAdvanced = vi.fn();
    render(
      <AdvanceShipmentButton
        dispatchId={7}
        allowedNextStatuses={["PACKED"]}
        onAdvanced={onAdvanced}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Mark as Packed" }));

    await waitFor(() => expect(onAdvanced).toHaveBeenCalledOnce());
    expect(update).toHaveBeenCalledWith(7, "PACKED");
  });

  it("refreshes after a refusal, since someone else may have moved it first", async () => {
    vi.spyOn(dispatchService, "updateStatus").mockRejectedValue(new ApiError(409, "Refused"));
    const onAdvanced = vi.fn();
    render(
      <AdvanceShipmentButton
        dispatchId={7}
        allowedNextStatuses={["PACKED"]}
        onAdvanced={onAdvanced}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Mark as Packed" }));

    await waitFor(() => expect(onAdvanced).toHaveBeenCalledOnce());
  });
});
