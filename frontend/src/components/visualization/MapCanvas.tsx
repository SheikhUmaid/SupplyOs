"use client";

import { useEffect, useRef, useState } from "react";
import { useVisualizationStore } from "@/stores/warehouse/visualizationStore";
import type { Network } from "@/types/api";
import type { Replay, ReplayFrame } from "./replay";
import { SupplyChainScene } from "./SupplyChainScene";

/**
 * The thin bridge between React and the scene. All setup and cleanup is in effects because Next keeps
 * hidden pages mounted: cleanup runs when the page hides, so a hidden map holds no GPU context.
 */
export default function MapCanvas({
  network,
  replay = null,
  onReplayFrame,
  label,
}: {
  network: Network | undefined;
  replay?: Replay | null;
  onReplayFrame?: (frame: ReplayFrame) => void;
  label: string;
}) {
  const hostRef = useRef<HTMLDivElement>(null);
  const sceneRef = useRef<SupplyChainScene | null>(null);
  const [failed, setFailed] = useState(false);

  const latest = useRef({ network, replay, onReplayFrame });
  useEffect(() => {
    latest.current = { network, replay, onReplayFrame };
  });

  useEffect(() => {
    const host = hostRef.current;
    if (!host) return;
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const { selectWarehouse, selectShipment } = useVisualizationStore.getState();

    const scene = new SupplyChainScene(
      host,
      {
        onSelectWarehouse: selectWarehouse,
        onSelectShipment: selectShipment,
        onReplayFrame: (frame) => latest.current.onReplayFrame?.(frame),
      },
      reducedMotion,
    );
    sceneRef.current = scene;

    const unsubscribe = useVisualizationStore.subscribe((state) =>
      scene.setSelection({
        warehouseId: state.selectedWarehouseId,
        shipmentId: state.selectedShipmentId,
      }),
    );
    const observer = new ResizeObserver(([entry]) => {
      if (entry) {
        scene.resize({ width: entry.contentRect.width, height: entry.contentRect.height });
      }
    });
    observer.observe(host);

    scene
      .start()
      .then(() => {
        if (latest.current.network) scene.setNetwork(latest.current.network);
        const { selectedWarehouseId, selectedShipmentId } = useVisualizationStore.getState();
        scene.setSelection({ warehouseId: selectedWarehouseId, shipmentId: selectedShipmentId });
        scene.playReplay(latest.current.replay);
      })
      .catch((error: unknown) => {
        console.error(error);
        setFailed(true);
      });

    return () => {
      unsubscribe();
      observer.disconnect();
      scene.destroy();
      sceneRef.current = null;
      delete host.dataset.layout;
    };
  }, []);

  useEffect(() => {
    if (network) sceneRef.current?.setNetwork(network);
  }, [network]);

  useEffect(() => {
    sceneRef.current?.playReplay(replay);
  }, [replay]);

  if (failed) {
    return (
      <div className="flex h-full items-center justify-center p-6 text-center text-[13px] text-ink-muted">
        This browser can&apos;t display the map. Everything it shows is available in the list beside
        it.
      </div>
    );
  }

  return <div ref={hostRef} role="img" aria-label={label} className="h-full w-full" />;
}
