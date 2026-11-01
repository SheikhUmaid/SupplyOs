"use client";

import { RotateCcw } from "lucide-react";
import dynamic from "next/dynamic";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Panel, Skeleton } from "@/components/ui/Feedback";
import { useApiQuery } from "@/hooks/useApiQuery";
import { visualizationService } from "@/services/visualization/visualization";
import type { Allocation } from "@/types/api";
import { buildReplay, type ReplayFrame } from "./replay";

const MapCanvas = dynamic(() => import("./MapCanvas"), {
  ssr: false,
  loading: () => <Skeleton className="h-full w-full" />,
});

/** Replays the stored decision on the map, step by step. */
export function AllocationReplay({ allocation }: { allocation: Allocation }) {
  const network = useApiQuery((signal) => visualizationService.network(signal), []);
  const [runs, setRuns] = useState(0);
  const [caption, setCaption] = useState("");
  const [seen, setSeen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  // Waits until it scrolls into view, so the animation isn't over before anyone sees it.
  useEffect(() => {
    const panel = panelRef.current;
    if (!panel) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry?.isIntersecting) {
          setSeen(true);
          observer.disconnect();
        }
      },
      { threshold: 0.4 },
    );
    observer.observe(panel);
    return () => observer.disconnect();
  }, []);

  // A new object restarts the animation, so it changes only for a new decision or a button press.
  const replay = useMemo(
    () => (runs >= 0 ? buildReplay(allocation) : null),
    // eslint-disable-next-line react-hooks/exhaustive-deps -- polling returns equal data as a new object.
    [allocation.id, runs],
  );
  const onFrame = useCallback((frame: ReplayFrame) => setCaption(frame.caption), []);

  return (
    <Panel
      title="Decision replay"
      actions={
        <Button variant="secondary" size="sm" onClick={() => setRuns((count) => count + 1)}>
          <RotateCcw aria-hidden className="size-3.5" />
          Replay
        </Button>
      }
    >
      <p
        aria-live="polite"
        data-testid="replay-caption"
        className="min-h-10 border-b border-line px-4 py-2.5 text-[13px] text-ink"
      >
        {caption}
      </p>
      <div ref={panelRef} className="h-[30rem]">
        {seen ? (
          <MapCanvas
            network={network.data}
            replay={replay}
            onReplayFrame={onFrame}
            label={`Replay of the warehouse decision for ${allocation.orderNumber}. The table above contains the same information.`}
          />
        ) : (
          <Skeleton className="h-full w-full" />
        )}
      </div>
    </Panel>
  );
}
