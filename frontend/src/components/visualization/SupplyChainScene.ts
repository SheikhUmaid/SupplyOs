import { Application, Container, Graphics, Text } from "pixi.js";
import type { Network, NetworkShipment, NetworkWarehouse, ShipmentStatus } from "@/types/api";
import { outlinePoints, project, type Point, type Size } from "./projection";
import { replayFrameAt, type NodeState, type Replay, type ReplayFrame } from "./replay";
import { clearOfNode, isMoving, placeMarker, pointOnRoute } from "./shipmentMotion";

const COLOURS = {
  land: 0xeef2f7,
  landEdge: 0xc4ccd6,
  link: 0xc4ccd6,
  ink: 0x152233,
  muted: 0x566375,
  primary: 0x1a5fc9,
  success: 0x1a7f4e,
  warning: 0x8f5a00,
  danger: 0xbf2e2e,
  inactive: 0x9aa4b1,
  customer: 0x6a3fa0,
  white: 0xffffff,
};

const NODE_FILL: Record<NodeState, number> = {
  idle: COLOURS.primary,
  evaluating: COLOURS.warning,
  rejected: COLOURS.danger,
  eligible: COLOURS.primary,
  selected: COLOURS.success,
};

const SHIPMENT_COLOUR: Partial<Record<ShipmentStatus, number>> = {
  PENDING: COLOURS.warning,
  PACKED: COLOURS.warning,
  DISPATCHED: COLOURS.primary,
  IN_TRANSIT: COLOURS.primary,
  DELIVERED: COLOURS.success,
};

const NODE_SIZE = 18;
const LAYOUT_PUBLISH_MS = 400;
const FONT = "IBM Plex Sans, sans-serif";

export interface SceneCallbacks {
  onSelectWarehouse: (id: number) => void;
  onSelectShipment: (id: number) => void;
  onReplayFrame: (frame: ReplayFrame) => void;
}

export interface Selection {
  warehouseId: number | null;
  shipmentId: number | null;
}

interface Marker {
  shipment: NetworkShipment;
  graphic: Graphics;
  from: Point;
  to: Point;
}

/**
 * Draws the network. This is a plain class rather than a React component, so that a moving shipment
 * never causes a React render. It renders only when something changes or moves, leaving an idle map
 * using no GPU time.
 */
export class SupplyChainScene {
  private readonly app = new Application();
  private readonly staticLayer = new Container();
  private readonly routeLayer = new Container();
  private readonly nodeLayer = new Container();
  private readonly markerLayer = new Container();
  private readonly replayLayer = new Graphics();
  private markers: Marker[] = [];
  private ready = false;
  private destroyed = false;
  private size: Size;
  private network: Network | null = null;
  private selection: Selection = { warehouseId: null, shipmentId: null };
  private replay: Replay | null = null;
  private replayElapsedMs = 0;
  private lastStepIndex = -1;
  private lastFrame: ReplayFrame | null = null;
  private clockMs = 0;
  private lastLayoutPublishMs = 0;

  constructor(
    private readonly host: HTMLElement,
    private readonly callbacks: SceneCallbacks,
    private readonly reducedMotion: boolean,
  ) {
    this.size = { width: host.clientWidth, height: host.clientHeight };
  }

  /** Startup is asynchronous, so the scene may be destroyed before it finishes. */
  async start(): Promise<void> {
    await this.app.init({
      width: Math.max(1, this.size.width),
      height: Math.max(1, this.size.height),
      backgroundAlpha: 0,
      antialias: true,
      autoDensity: true,
      resolution: Math.min(window.devicePixelRatio || 1, 2),
      autoStart: false,
      preference: "webgl",
    });
    if (this.destroyed) {
      this.tearDown();
      return;
    }
    this.ready = true;
    this.app.canvas.style.display = "block";
    this.host.appendChild(this.app.canvas);
    this.app.stage.addChild(
      this.staticLayer,
      this.routeLayer,
      this.replayLayer,
      this.markerLayer,
      // On top, so that a warehouse can always be clicked whatever is passing through it.
      this.nodeLayer,
    );
    this.app.ticker.add(this.tick);
    this.redraw();
  }

  setNetwork(network: Network): void {
    this.network = network;
    this.redraw();
  }

  setSelection(selection: Selection): void {
    this.selection = selection;
    this.redraw();
  }

  /** Starts a replay, or with null returns to showing the live network. */
  playReplay(replay: Replay | null): void {
    this.replay = replay;
    this.replayElapsedMs = replay && this.reducedMotion ? replay.totalMs : 0;
    this.lastStepIndex = -1;
    this.lastFrame = null;
    this.redraw();
  }

  resize(size: Size): void {
    this.size = size;
    if (!this.ready || size.width === 0 || size.height === 0) return;
    this.app.renderer.resize(size.width, size.height);
    this.redraw();
  }

  destroy(): void {
    this.destroyed = true;
    if (this.ready) this.tearDown();
  }

  private tearDown(): void {
    this.ready = false;
    this.app.ticker.remove(this.tick);
    this.app.stop();
    this.app.destroy({ removeView: true }, { children: true });
  }

  private readonly tick = (): void => {
    if (!this.ready) return;
    const deltaMs = this.app.ticker.deltaMS;
    this.clockMs += deltaMs;

    if (this.replay && this.replayElapsedMs < this.replay.totalMs) {
      this.replayElapsedMs = Math.min(this.replay.totalMs, this.replayElapsedMs + deltaMs);
      this.applyReplayFrame();
    }
    this.positionMarkers();
    this.updateRunning();
    if (this.clockMs - this.lastLayoutPublishMs > LAYOUT_PUBLISH_MS) this.publishLayout();
  };

  private replayIsRunning(): boolean {
    return this.replay !== null && this.replayElapsedMs < this.replay.totalMs;
  }

  /** Runs the render loop only while something is moving. */
  private updateRunning(): void {
    const moving = this.markers.some((marker) => isMoving(marker.shipment.status));
    const animate = this.replayIsRunning() || (moving && !this.reducedMotion);
    if (animate === this.app.ticker.started) return;
    if (animate) {
      this.app.start();
    } else {
      this.app.stop();
      this.app.render();
    }
    this.publishLayout();
  }

  private redraw(): void {
    if (!this.ready || this.size.width === 0 || this.size.height === 0) return;
    this.drawBase();
    this.drawRoutes();
    this.applyReplayFrame();
    this.drawNodes();
    this.drawMarkers();
    this.updateRunning();
    this.publishLayout();
    if (!this.app.ticker.started) this.app.render();
  }

  private clear(layer: Container): void {
    for (const child of layer.removeChildren()) child.destroy({ children: true });
  }

  private point(latitude: number, longitude: number): Point {
    return project(latitude, longitude, this.size);
  }

  private warehousePoint(warehouse: NetworkWarehouse): Point {
    return this.point(warehouse.latitude, warehouse.longitude);
  }

  private label(text: string, size: number, fill: number, weight: "400" | "600" = "400"): Text {
    return new Text({
      text,
      style: { fontFamily: FONT, fontSize: size, fill, fontWeight: weight },
      resolution: Math.min(window.devicePixelRatio || 1, 2) * 1.5,
    });
  }

  private drawBase(): void {
    this.clear(this.staticLayer);
    const land = new Graphics()
      .poly(outlinePoints(this.size))
      .fill(COLOURS.land)
      .stroke({ width: 1.5, color: COLOURS.landEdge });
    this.staticLayer.addChild(land);

    const caption = this.label("Karnataka (schematic, not to survey accuracy)", 10, COLOURS.muted);
    caption.position.set(10, this.size.height - 18);
    this.staticLayer.addChild(caption);

    const warehouses = this.network?.warehouses ?? [];
    const links = new Graphics();
    warehouses.forEach((from, index) => {
      for (const to of warehouses.slice(index + 1)) {
        const a = this.warehousePoint(from);
        const b = this.warehousePoint(to);
        links.moveTo(a.x, a.y).lineTo(b.x, b.y);
      }
    });
    links.stroke({ width: 1, color: COLOURS.link, alpha: 0.9 });
    this.staticLayer.addChild(links);
  }

  private visibleShipments(): NetworkShipment[] {
    return this.replay ? [] : (this.network?.shipments ?? []);
  }

  private warehouseOf(shipment: NetworkShipment): NetworkWarehouse | undefined {
    return this.network?.warehouses.find((warehouse) => warehouse.id === shipment.warehouseId);
  }

  private drawRoutes(): void {
    this.clear(this.routeLayer);
    const cities = new Map<string, Point>();

    for (const shipment of this.visibleShipments()) {
      const warehouse = this.warehouseOf(shipment);
      if (!warehouse) continue;
      const from = this.warehousePoint(warehouse);
      const to = this.point(shipment.latitude, shipment.longitude);
      cities.set(shipment.destinationCity, to);

      const selected = this.selection.shipmentId === shipment.dispatchId;
      const delivered = shipment.status === "DELIVERED";
      const route = new Graphics()
        .moveTo(from.x, from.y)
        .lineTo(to.x, to.y)
        .stroke({
          width: selected ? 3.5 : 2,
          color: SHIPMENT_COLOUR[shipment.status] ?? COLOURS.muted,
          alpha: selected ? 1 : delivered ? 0.35 : 0.75,
        });
      this.routeLayer.addChild(route);
    }

    if (this.replay) {
      cities.set(
        this.replay.deliveryCity,
        this.point(this.replay.deliveryLatitude, this.replay.deliveryLongitude),
      );
    }

    for (const [city, point] of cities) {
      this.routeLayer.addChild(
        new Graphics()
          .circle(point.x, point.y, 4)
          .fill(COLOURS.customer)
          .stroke({ width: 1.5, color: COLOURS.white }),
      );
      // A warehouse in the same city already labels this spot.
      if (this.network?.warehouses.some((warehouse) => warehouse.city === city)) continue;
      const name = this.label(city, 11, COLOURS.customer);
      name.position.set(point.x + 7, point.y - 7);
      this.routeLayer.addChild(name);
    }

    if (this.replay) {
      const point = this.point(this.replay.deliveryLatitude, this.replay.deliveryLongitude);
      const order = this.label(this.replay.orderNumber, 11, COLOURS.ink, "600");
      order.position.set(point.x + 7, point.y + 6);
      this.routeLayer.addChild(order);
    }
  }

  private nodeState(id: number): NodeState {
    return this.lastFrame?.nodes[id] ?? "idle";
  }

  private drawNodes(): void {
    this.clear(this.nodeLayer);
    for (const warehouse of this.network?.warehouses ?? []) {
      const { x, y } = this.warehousePoint(warehouse);
      const state = this.nodeState(warehouse.id);
      const active = warehouse.status === "ACTIVE";
      const selected = this.selection.warehouseId === warehouse.id;
      const half = NODE_SIZE / 2;

      const node = new Graphics();
      if (selected || state === "selected") {
        node
          .roundRect(x - half - 4, y - half - 4, NODE_SIZE + 8, NODE_SIZE + 8, 6)
          .stroke({ width: 2, color: state === "selected" ? COLOURS.success : COLOURS.primary });
      }
      node
        .roundRect(x - half, y - half, NODE_SIZE, NODE_SIZE, 4)
        .fill(active || this.replay ? NODE_FILL[state] : COLOURS.inactive)
        .stroke({ width: 2, color: COLOURS.white });
      node.eventMode = "static";
      node.cursor = "pointer";
      node.on("pointertap", () => this.callbacks.onSelectWarehouse(warehouse.id));
      this.nodeLayer.addChild(node);

      const name = this.label(warehouse.name.replace(" Warehouse", ""), 13, COLOURS.ink, "600");
      name.position.set(x + half + 10, y - 9);
      name.eventMode = "none";
      this.nodeLayer.addChild(name);
    }
  }

  private drawMarkers(): void {
    this.clear(this.markerLayer);
    this.markers = [];
    for (const shipment of this.visibleShipments()) {
      const warehouse = this.warehouseOf(shipment);
      if (!warehouse) continue;
      const selected = this.selection.shipmentId === shipment.dispatchId;
      const graphic = new Graphics()
        .circle(0, 0, selected ? 8 : 6)
        .fill(SHIPMENT_COLOUR[shipment.status] ?? COLOURS.muted)
        .stroke({ width: 2, color: COLOURS.white });
      graphic.eventMode = "static";
      graphic.cursor = "pointer";
      graphic.on("pointertap", () => this.callbacks.onSelectShipment(shipment.dispatchId));
      this.markerLayer.addChild(graphic);
      this.markers.push({
        shipment,
        graphic,
        from: this.warehousePoint(warehouse),
        to: this.point(shipment.latitude, shipment.longitude),
      });
    }
    this.positionMarkers();
  }

  private positionMarkers(): void {
    for (const { shipment, graphic, from, to } of this.markers) {
      const { progress, alpha } = placeMarker(shipment.status, this.clockMs, this.reducedMotion);
      const { x, y } = pointOnRoute(from, to, clearOfNode(from, to, progress));
      graphic.position.set(x, y);
      graphic.alpha = alpha;
    }
  }

  private applyReplayFrame(): void {
    this.replayLayer.clear();
    if (!this.replay) {
      this.lastFrame = null;
      return;
    }
    const frame = replayFrameAt(this.replay, this.replayElapsedMs);
    const stepChanged = frame.stepIndex !== this.lastStepIndex;
    this.lastFrame = frame;

    const selected = this.network?.warehouses.find(
      (warehouse) => warehouse.id === this.replay?.selectedWarehouseId,
    );
    if (selected && frame.routeProgress !== null) {
      const from = this.warehousePoint(selected);
      const to = this.point(this.replay.deliveryLatitude, this.replay.deliveryLongitude);
      const end = pointOnRoute(from, to, frame.routeProgress);
      this.replayLayer
        .moveTo(from.x, from.y)
        .lineTo(end.x, end.y)
        .stroke({ width: 3, color: COLOURS.success });
    }

    if (stepChanged) {
      this.lastStepIndex = frame.stepIndex;
      this.drawNodes();
      this.callbacks.onReplayFrame(frame);
    }
  }

  /** Where things are, for automated browser checks: a canvas has no DOM to inspect. */
  private publishLayout(): void {
    this.lastLayoutPublishMs = this.clockMs;
    const warehouses = Object.fromEntries(
      (this.network?.warehouses ?? []).map((warehouse) => [
        warehouse.id,
        { ...this.warehousePoint(warehouse), state: this.nodeState(warehouse.id) },
      ]),
    );
    const shipments = Object.fromEntries(
      this.markers.map(({ shipment, graphic }) => [
        shipment.dispatchId,
        { x: graphic.x, y: graphic.y, status: shipment.status },
      ]),
    );
    this.host.dataset.layout = JSON.stringify({
      renderer: this.app.renderer.name,
      running: this.app.ticker.started,
      warehouses,
      shipments,
      routeProgress: this.lastFrame?.routeProgress ?? null,
      replayDone: this.lastFrame?.done ?? null,
    });
  }
}
