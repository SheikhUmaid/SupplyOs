"use client";

import { useEffect } from "react";
import { ErrorState, Panel } from "@/components/ui/Feedback";

/** Shared body for the route `error.tsx` boundaries. */
export function RouteError({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <Panel className="m-4">
      <ErrorState message="This page could not be displayed." onRetry={retry} />
    </Panel>
  );
}
