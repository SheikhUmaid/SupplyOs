import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError } from "@/services/api/client";

interface QueryResult<T> {
  requestKey: string;
  data?: T;
  error?: string;
}

export interface ApiQuery<T> {
  data: T | undefined;
  error: string | null;
  /** True while a request is running. `data` keeps the previous result so tables don't flash. */
  loading: boolean;
  reload: () => void;
}

/** Fetches when `deps` change, cancelling any request it has replaced. */
export function useApiQuery<T>(
  fetcher: (signal: AbortSignal) => Promise<T>,
  deps: readonly unknown[],
): ApiQuery<T> {
  const [reloads, setReloads] = useState(0);
  const [result, setResult] = useState<QueryResult<T>>();
  const requestKey = `${JSON.stringify(deps)}#${reloads}`;

  const latestFetcher = useRef(fetcher);
  useEffect(() => {
    latestFetcher.current = fetcher;
  });

  useEffect(() => {
    const controller = new AbortController();
    latestFetcher
      .current(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setResult({ requestKey, data });
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        const message = error instanceof ApiError ? error.message : "Something went wrong.";
        setResult((previous) => ({ requestKey, data: previous?.data, error: message }));
      });
    return () => controller.abort();
  }, [requestKey]);

  const reload = useCallback(() => setReloads((count) => count + 1), []);
  const loading = result?.requestKey !== requestKey;

  return { data: result?.data, error: loading ? null : (result?.error ?? null), loading, reload };
}
