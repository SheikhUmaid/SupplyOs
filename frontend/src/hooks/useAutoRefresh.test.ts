import { renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useAutoRefresh } from "./useAutoRefresh";

function setVisibility(state: "visible" | "hidden") {
  Object.defineProperty(document, "visibilityState", { configurable: true, value: state });
  document.dispatchEvent(new Event("visibilitychange"));
}

describe("useAutoRefresh", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    Object.defineProperty(document, "visibilityState", { configurable: true, value: "visible" });
  });

  afterEach(() => vi.useRealTimers());

  it("refreshes on its interval", () => {
    const refresh = vi.fn();
    renderHook(() => useAutoRefresh(refresh, 1000));
    expect(refresh).not.toHaveBeenCalled();
    vi.advanceTimersByTime(3000);
    expect(refresh).toHaveBeenCalledTimes(3);
  });

  it("pauses while the tab is hidden and refreshes on return", () => {
    const refresh = vi.fn();
    renderHook(() => useAutoRefresh(refresh, 1000));

    setVisibility("hidden");
    vi.advanceTimersByTime(5000);
    expect(refresh).not.toHaveBeenCalled();

    setVisibility("visible");
    expect(refresh).toHaveBeenCalledOnce();
  });

  it("stops when unmounted", () => {
    const refresh = vi.fn();
    const { unmount } = renderHook(() => useAutoRefresh(refresh, 1000));
    unmount();
    vi.advanceTimersByTime(5000);
    setVisibility("visible");
    expect(refresh).not.toHaveBeenCalled();
  });

  it("uses the latest callback", () => {
    const first = vi.fn();
    const second = vi.fn();
    const { rerender } = renderHook(({ refresh }) => useAutoRefresh(refresh, 1000), {
      initialProps: { refresh: first },
    });
    rerender({ refresh: second });
    vi.advanceTimersByTime(1000);
    expect(first).not.toHaveBeenCalled();
    expect(second).toHaveBeenCalledOnce();
  });
});
