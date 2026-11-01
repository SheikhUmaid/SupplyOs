import { useEffect, useRef } from "react";

/** Refreshes on a timer while the tab is visible, and at once when the user returns to it. */
export function useAutoRefresh(refresh: () => void, intervalMs: number): void {
  const latestRefresh = useRef(refresh);
  useEffect(() => {
    latestRefresh.current = refresh;
  });

  useEffect(() => {
    function refreshIfVisible() {
      if (document.visibilityState === "visible") latestRefresh.current();
    }
    const timer = setInterval(refreshIfVisible, intervalMs);
    document.addEventListener("visibilitychange", refreshIfVisible);
    return () => {
      clearInterval(timer);
      document.removeEventListener("visibilitychange", refreshIfVisible);
    };
  }, [intervalMs]);
}
