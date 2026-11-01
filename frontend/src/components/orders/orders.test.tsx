import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { Allocation, AllocationEvaluation } from "@/types/api";
import { AllocationDecision } from "./AllocationDecision";
import {
  eligibilityLabel,
  formatFactor,
  formatHours,
  formatKm,
  scoreBarPercent,
} from "./allocationDisplay";

const scored = (overrides: Partial<AllocationEvaluation> = {}): AllocationEvaluation => ({
  warehouseId: 1,
  warehouseName: "Hubli Warehouse",
  eligible: true,
  rejectionReason: null,
  distanceKm: 164.7,
  deliveryCost: 2426.41,
  etaHours: 15.66,
  normDistance: 0.5371,
  normCost: 0.477,
  inventoryFactor: 0.5833,
  priorityFactor: 0,
  finalScore: 0.4746,
  rank: 1,
  decision: "SELECTED",
  ...overrides,
});

const rejected = scored({
  warehouseId: 3,
  warehouseName: "Mysuru Warehouse",
  eligible: false,
  rejectionReason: "Insufficient stock of Smartphone (needs 10, has 5)",
  normDistance: null,
  normCost: null,
  inventoryFactor: null,
  priorityFactor: null,
  finalScore: null,
  rank: null,
  decision: "REJECTED",
});

const allocation = (evaluations: AllocationEvaluation[]): Allocation => ({
  id: 1,
  orderId: 1,
  orderNumber: "ORD-2026-000001",
  attemptNo: 2,
  outcome: "ALLOCATED",
  selectedWarehouse: "Hubli Warehouse",
  weights: { distance: 0.4, cost: 0.3, inventory: 0.2, priority: 0.1 },
  explanation: "Hubli Warehouse was selected.",
  allocatedAt: "2026-03-01T10:00:00Z",
  evaluations,
});

describe("allocation display helpers", () => {
  it("formats scores to four decimals and marks unscored warehouses", () => {
    expect(formatFactor(0.4746)).toBe("0.4746");
    expect(formatFactor(0)).toBe("0.0000");
    expect(formatFactor(null)).toBe("—");
  });

  it("formats distance and time", () => {
    expect(formatKm(1164.72)).toBe("1,164.7 km");
    expect(formatHours(15.66)).toBe("15.7 h");
  });

  it("shows why a warehouse was ruled out", () => {
    expect(eligibilityLabel(scored())).toBe("Eligible");
    expect(eligibilityLabel(rejected)).toContain("needs 10, has 5");
  });

  it("sizes score bars against the highest score", () => {
    const all = [scored({ finalScore: 0.25 }), scored({ finalScore: 0.5 }), rejected];
    expect(scoreBarPercent(0.25, all)).toBe(50);
    expect(scoreBarPercent(0.5, all)).toBe(100);
    expect(scoreBarPercent(null, all)).toBe(0);
    expect(scoreBarPercent(0, [scored({ finalScore: 0 })])).toBe(0);
  });
});

describe("AllocationDecision", () => {
  const second = scored({
    warehouseId: 2,
    warehouseName: "Bengaluru Warehouse",
    finalScore: 0.7367,
    rank: 2,
    decision: "NOT_SELECTED",
  });

  it("shows the explanation and every warehouse's outcome", () => {
    render(<AllocationDecision allocation={allocation([scored(), second, rejected])} />);
    expect(screen.getByText("Hubli Warehouse was selected.")).toBeInTheDocument();

    const table = screen.getByRole("table", { name: "Warehouse comparison" });
    const rows = within(table).getAllByRole("row");
    expect(rows).toHaveLength(4);
    expect(rows[1]).toHaveTextContent("Hubli Warehouse");
    expect(rows[1]).toHaveTextContent("0.4746");
    expect(rows[1]).toHaveTextContent("Selected");
    expect(rows[2]).toHaveTextContent("Not selected");
    expect(rows[3]).toHaveTextContent("needs 10, has 5");
    expect(rows[3]).toHaveTextContent("Rejected");
  });

  it("shows the weights that were in force for this decision", () => {
    render(<AllocationDecision allocation={allocation([scored()])} />);
    const breakdown = screen.getByRole("table", { name: "Score breakdown" });
    expect(breakdown).toHaveTextContent("Distance × 0.40");
    expect(breakdown).toHaveTextContent("Priority × 0.10");
  });

  it("leaves rejected warehouses out of the score breakdown", () => {
    render(<AllocationDecision allocation={allocation([scored(), rejected])} />);
    const breakdown = screen.getByRole("table", { name: "Score breakdown" });
    expect(within(breakdown).queryByText("Mysuru Warehouse")).not.toBeInTheDocument();
  });

  it("omits the score breakdown when nothing was scored", () => {
    render(
      <AllocationDecision
        allocation={{ ...allocation([rejected]), outcome: "NO_ELIGIBLE_WAREHOUSE" }}
      />,
    );
    expect(screen.queryByRole("table", { name: "Score breakdown" })).not.toBeInTheDocument();
  });

  it("says which attempt this is", () => {
    render(<AllocationDecision allocation={allocation([scored()])} />);
    expect(screen.getByText(/Attempt 2/)).toBeInTheDocument();
  });
});
