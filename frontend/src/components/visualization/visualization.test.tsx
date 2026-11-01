import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { inventoryService } from "@/services/inventory/inventory";
import { useVisualizationStore } from "@/stores/warehouse/visualizationStore";
import type {
  Allocation,
  AllocationEvaluation,
  Network,
  NetworkShipment,
  NetworkWarehouse,
  Page,
  InventoryRecord,
} from "@/types/api";
import { MapPanel } from "./MapPanel";
import { outlinePoints, project } from "./projection";
import { buildReplay, replayFrameAt } from "./replay";
import { clearOfNode, isMoving, placeMarker, pointOnRoute } from "./shipmentMotion";

const SIZE = { width: 800, height: 600 };

describe("project", () => {
  const hubli = project(15.3647, 75.124, SIZE);
  const bengaluru = project(12.9716, 77.5946, SIZE);
  const mysuru = project(12.2958, 76.6394, SIZE);

  it("keeps cities in their real relative positions", () => {
    expect(bengaluru.x).toBeGreaterThan(hubli.x); // Bengaluru is east of Hubli.
    expect(bengaluru.y).toBeGreaterThan(hubli.y); // And south of it.
    expect(mysuru.y).toBeGreaterThan(bengaluru.y); // Mysuru is south of Bengaluru.
    expect(mysuru.x).toBeLessThan(bengaluru.x); // And west of it.
  });

  it("keeps the whole outline inside the canvas at any shape", () => {
    for (const size of [SIZE, { width: 320, height: 416 }, { width: 1400, height: 300 }]) {
      const points = outlinePoints(size);
      for (let index = 0; index < points.length; index += 2) {
        expect(points[index]).toBeGreaterThanOrEqual(0);
        expect(points[index]).toBeLessThanOrEqual(size.width);
        expect(points[index + 1]).toBeGreaterThanOrEqual(0);
        expect(points[index + 1]).toBeLessThanOrEqual(size.height);
      }
    }
  });

  it("keeps proportions when the canvas changes shape", () => {
    const wide = { width: 1600, height: 600 };
    const a = project(15, 75, wide);
    const b = project(14, 75, wide);
    const c = project(15, 76, wide);
    const northSouth = Math.abs(b.y - a.y);
    const eastWest = Math.abs(c.x - a.x);
    // At this latitude a degree of longitude is about 96% of a degree of latitude.
    expect(eastWest / northSouth).toBeCloseTo(Math.cos((15 * Math.PI) / 180), 1);
  });

  it("does not break on an empty canvas", () => {
    const point = project(15, 75, { width: 0, height: 0 });
    expect(Number.isFinite(point.x)).toBe(true);
  });
});

describe("shipment motion", () => {
  it("holds a shipment still until it leaves the warehouse", () => {
    expect(placeMarker("PENDING", 5000, false)).toEqual({ progress: 0, alpha: 1 });
    expect(placeMarker("PACKED", 5000, false).progress).toBeLessThan(0.08);
    expect(isMoving("PENDING")).toBe(false);
    expect(isMoving("PACKED")).toBe(false);
  });

  it("places a delivered shipment at the customer, holding still", () => {
    expect(placeMarker("DELIVERED", 5000, false)).toEqual({ progress: 1, alpha: 1 });
    expect(isMoving("DELIVERED")).toBe(false);
  });

  it("moves dispatched and in-transit shipments along their stage of the route", () => {
    expect(isMoving("DISPATCHED")).toBe(true);
    expect(isMoving("IN_TRANSIT")).toBe(true);
    expect(placeMarker("IN_TRANSIT", 1000, false).progress).toBeLessThan(
      placeMarker("IN_TRANSIT", 4000, false).progress,
    );
    for (let ms = 0; ms < 5000; ms += 250) {
      const dispatched = placeMarker("DISPATCHED", ms, false).progress;
      const transit = placeMarker("IN_TRANSIT", ms, false).progress;
      expect(dispatched).toBeLessThanOrEqual(0.4);
      expect(transit).toBeGreaterThanOrEqual(0.35);
      expect(transit).toBeLessThan(1);
    }
  });

  it("fades out where a pass restarts, so the marker never visibly jumps back", () => {
    expect(placeMarker("IN_TRANSIT", 0, false).alpha).toBe(0);
    expect(placeMarker("IN_TRANSIT", 2500, false).alpha).toBe(1);
    expect(placeMarker("IN_TRANSIT", 4999, false).alpha).toBeLessThan(0.05);
  });

  it("holds still under reduced motion, in the right part of the route", () => {
    const first = placeMarker("IN_TRANSIT", 0, true);
    expect(placeMarker("IN_TRANSIT", 5000, true)).toEqual(first);
    expect(first.alpha).toBe(1);
    expect(first.progress).toBeGreaterThan(0.3);
  });

  it("keeps a waiting marker beside its warehouse, not hidden underneath it", () => {
    const from = { x: 0, y: 0 };
    const to = { x: 200, y: 0 };
    expect(clearOfNode(from, to, 0)).toBeCloseTo(0.1);
    expect(clearOfNode(from, to, 1)).toBe(1);
    expect(clearOfNode(from, { x: 10, y: 0 }, 0)).toBe(1);
    expect(clearOfNode(from, from, 0)).toBe(0);
  });

  it("still moves a shipment on a route barely longer than the clearance", () => {
    const from = { x: 0, y: 0 };
    const short = { x: 40, y: 0 };
    const start = clearOfNode(from, short, placeMarker("DISPATCHED", 500, false).progress);
    const later = clearOfNode(from, short, placeMarker("DISPATCHED", 4000, false).progress);
    expect(later).toBeGreaterThan(start);
  });

  it("interpolates along a route", () => {
    expect(pointOnRoute({ x: 0, y: 0 }, { x: 100, y: 50 }, 0.5)).toEqual({ x: 50, y: 25 });
  });
});

const evaluation = (overrides: Partial<AllocationEvaluation>): AllocationEvaluation => ({
  warehouseId: 1,
  warehouseName: "Hubli",
  eligible: true,
  rejectionReason: null,
  distanceKm: 1,
  deliveryCost: 1,
  etaHours: 1,
  normDistance: 0.5,
  normCost: 0.5,
  inventoryFactor: 0.5,
  priorityFactor: 0,
  finalScore: 0.4746,
  rank: 1,
  decision: "SELECTED",
  ...overrides,
});

const allocation = (evaluations: AllocationEvaluation[]): Allocation => ({
  id: 1,
  orderId: 1,
  orderNumber: "ORD-2026-000001",
  attemptNo: 1,
  outcome: "ALLOCATED",
  selectedWarehouse: "Hubli",
  weights: { distance: 0.4, cost: 0.3, inventory: 0.2, priority: 0.1 },
  explanation: "",
  allocatedAt: "2026-03-01T10:00:00Z",
  deliveryCity: "Davanagere",
  deliveryLatitude: 14.46,
  deliveryLongitude: 75.92,
  evaluations,
});

describe("replay", () => {
  const scenario = buildReplay(
    allocation([
      evaluation({}),
      evaluation({ warehouseId: 2, warehouseName: "Bengaluru", rank: 2, decision: "NOT_SELECTED" }),
      evaluation({
        warehouseId: 3,
        warehouseName: "Mysuru",
        eligible: false,
        rejectionReason: "Insufficient stock of Smartphone (needs 10, has 5)",
        finalScore: null,
        decision: "REJECTED",
      }),
    ]),
  );

  const stepStart = (index: number) =>
    scenario.steps.slice(0, index).reduce((sum, step) => sum + step.durationMs, 0);
  const frameOfStep = (index: number) => replayFrameAt(scenario, stepStart(index) + 1);

  it("follows the specified sequence", () => {
    expect(frameOfStep(0).nodes).toEqual({ 1: "evaluating", 2: "evaluating", 3: "evaluating" });
    expect(frameOfStep(1).nodes).toEqual({ 1: "evaluating", 2: "evaluating", 3: "rejected" });
    expect(frameOfStep(2).nodes).toEqual({ 1: "eligible", 2: "eligible", 3: "rejected" });
    expect(frameOfStep(3).nodes).toEqual({ 1: "selected", 2: "eligible", 3: "rejected" });
    expect(frameOfStep(4).routeProgress).toBeLessThan(0.1);
  });

  it("captions each step with the stored facts", () => {
    expect(frameOfStep(0).caption).toContain("Evaluating 3 warehouses for ORD-2026-000001");
    expect(frameOfStep(1).caption).toContain("Mysuru: Insufficient stock of Smartphone");
    expect(frameOfStep(2).caption).toBe("Eligible: Hubli, Bengaluru");
    expect(frameOfStep(3).caption).toContain("Hubli selected, with the lowest score (0.4746)");
    expect(frameOfStep(4).caption).toBe("Hubli to Davanagere, ORD-2026-000001");
  });

  it("draws no route until a warehouse is selected", () => {
    for (const index of [0, 1, 2, 3]) expect(frameOfStep(index).routeProgress).toBeNull();
  });

  it("holds on its final frame", () => {
    const end = replayFrameAt(scenario, scenario.totalMs + 100000);
    expect(end.done).toBe(true);
    expect(end.routeProgress).toBe(1);
    expect(end.nodes[1]).toBe("selected");
  });

  it("is not done until the end", () => {
    expect(replayFrameAt(scenario, scenario.totalMs - 1).done).toBe(false);
  });

  it("skips the rejection step when nothing was rejected", () => {
    const allEligible = buildReplay(allocation([evaluation({})]));
    expect(allEligible.steps.map((step) => step.caption)[1]).toBe("Eligible: Hubli");
  });

  it("never draws a route or selects anything when no warehouse qualified", () => {
    const none = buildReplay(
      allocation([
        evaluation({
          eligible: false,
          rejectionReason: "Insufficient stock",
          finalScore: null,
          decision: "REJECTED",
        }),
      ]),
    );
    const end = replayFrameAt(none, none.totalMs);
    expect(none.selectedWarehouseId).toBeNull();
    expect(end.routeProgress).toBeNull();
    expect(end.nodes[1]).toBe("rejected");
    expect(end.caption).toContain("nothing was reserved");
  });
});

const warehouse: NetworkWarehouse = {
  id: 1,
  name: "Hubli Warehouse",
  city: "Hubli",
  state: "Karnataka",
  latitude: 15.36,
  longitude: 75.12,
  status: "ACTIVE",
  productCount: 12,
  inStockProducts: 11,
  totalAvailable: 149,
  totalReserved: 10,
  lowStockItems: 2,
  activeOrders: 3,
  activeDispatches: 4,
};

const shipment: NetworkShipment = {
  dispatchId: 9,
  trackingReference: "TRK-2026-000009",
  orderId: 5,
  orderNumber: "ORD-2026-000005",
  warehouseId: 1,
  warehouseName: "Hubli Warehouse",
  destinationCity: "Davanagere",
  latitude: 14.46,
  longitude: 75.92,
  status: "IN_TRANSIT",
  estimatedDeliveryDate: "2026-03-05",
};

const network: Network = { warehouses: [warehouse], shipments: [shipment] };

describe("MapPanel", () => {
  beforeEach(() => {
    useVisualizationStore.getState().clearSelection();
    vi.spyOn(inventoryService, "list").mockResolvedValue({
      content: [{ id: 1, productName: "Smartphone", availableQuantity: 15 }],
    } as Page<InventoryRecord>);
  });

  it("offers every warehouse and active shipment without needing the canvas", () => {
    render(<MapPanel network={network} />);
    expect(screen.getByRole("button", { name: /Hubli Warehouse/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /To Davanagere/ })).toBeInTheDocument();
  });

  it("states that positions are simulated", () => {
    render(<MapPanel network={network} />);
    expect(screen.getByText(/simulated from shipment status, not GPS/)).toBeInTheDocument();
  });

  it("leaves delivered shipments out of the active list", () => {
    render(
      <MapPanel network={{ ...network, shipments: [{ ...shipment, status: "DELIVERED" }] }} />,
    );
    expect(screen.getByText("None right now.")).toBeInTheDocument();
  });

  it("shows the specified warehouse details", async () => {
    render(<MapPanel network={network} />);
    fireEvent.click(screen.getByRole("button", { name: /Hubli Warehouse/ }));

    expect(screen.getByRole("heading", { name: "Hubli Warehouse" })).toBeInTheDocument();
    expect(screen.getByText("Hubli, Karnataka")).toBeInTheDocument();
    expect(screen.getByText("Active orders").nextSibling).toHaveTextContent("3");
    expect(screen.getByText("Active dispatches").nextSibling).toHaveTextContent("4");
    expect(screen.getByText("Available units").nextSibling).toHaveTextContent("149");
    expect(await screen.findByText("Smartphone")).toBeInTheDocument();
  });

  it("shows shipment details, says it is not GPS, and links to the order", () => {
    render(<MapPanel network={network} />);
    fireEvent.click(screen.getByRole("button", { name: /To Davanagere/ }));

    expect(screen.getByRole("heading", { name: "TRK-2026-000009" })).toBeInTheDocument();
    expect(screen.getByText("Hubli Warehouse to Davanagere")).toBeInTheDocument();
    expect(screen.getByText(/It is not GPS/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /ORD-2026-000005/ })).toHaveAttribute(
      "href",
      "/admin/orders/5",
    );
  });

  it("returns to the list when details close", () => {
    render(<MapPanel network={network} />);
    act(() => useVisualizationStore.getState().selectWarehouse(1));
    fireEvent.click(screen.getByRole("button", { name: "Close details" }));
    expect(screen.getByRole("heading", { name: "Warehouses" })).toBeInTheDocument();
  });

  it("keeps one selection at a time", () => {
    const store = useVisualizationStore.getState();
    store.selectWarehouse(1);
    store.selectShipment(9);
    expect(useVisualizationStore.getState().selectedWarehouseId).toBeNull();
    expect(useVisualizationStore.getState().selectedShipmentId).toBe(9);
  });
});
