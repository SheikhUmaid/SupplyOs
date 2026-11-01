import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "@/services/api/client";
import { useApiQuery } from "./useApiQuery";

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((r) => {
    resolve = r;
  });
  return { promise, resolve };
}

describe("useApiQuery", () => {
  it("reports loading and then the data", async () => {
    const { result } = renderHook(() => useApiQuery(() => Promise.resolve("rows"), []));
    expect(result.current.loading).toBe(true);
    await waitFor(() => expect(result.current.data).toBe("rows"));
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it("reports an API error's message", async () => {
    const { result } = renderHook(() =>
      useApiQuery(() => Promise.reject(new ApiError(500, "Server down")), []),
    );
    await waitFor(() => expect(result.current.error).toBe("Server down"));
  });

  it("keeps showing the previous data while new data loads", async () => {
    const second = deferred<string>();
    const fetcher = vi.fn((q: string) => (q === "a" ? Promise.resolve("first") : second.promise));
    const { result, rerender } = renderHook(({ q }) => useApiQuery(() => fetcher(q), [q]), {
      initialProps: { q: "a" },
    });
    await waitFor(() => expect(result.current.data).toBe("first"));

    rerender({ q: "b" });
    expect(result.current.loading).toBe(true);
    expect(result.current.data).toBe("first");

    await act(async () => second.resolve("second"));
    expect(result.current.data).toBe("second");
  });

  it("ignores a slow response that a newer request has replaced", async () => {
    const slow = deferred<string>();
    const fetcher = vi.fn((q: string) => (q === "old" ? slow.promise : Promise.resolve("new")));
    const { result, rerender } = renderHook(({ q }) => useApiQuery(() => fetcher(q), [q]), {
      initialProps: { q: "old" },
    });

    rerender({ q: "latest" });
    await waitFor(() => expect(result.current.data).toBe("new"));

    await act(async () => slow.resolve("old"));
    expect(result.current.data).toBe("new");
  });

  it("does not refetch when deps are unchanged, and refetches on reload", async () => {
    const fetcher = vi.fn(() => Promise.resolve("rows"));
    const { result, rerender } = renderHook(() => useApiQuery(fetcher, ["same"]));
    await waitFor(() => expect(result.current.data).toBe("rows"));

    rerender();
    expect(fetcher).toHaveBeenCalledTimes(1);

    act(() => result.current.reload());
    await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
  });
});
