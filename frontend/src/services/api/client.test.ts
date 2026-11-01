import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api, ApiError, buildUrl, setUnauthorizedHandler } from "./client";

const fetchMock = vi.fn<typeof fetch>();

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status });
}

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
  setUnauthorizedHandler(null);
});

describe("buildUrl", () => {
  it("prefixes /api and drops empty query values but keeps 0 and false", () => {
    expect(buildUrl("/orders", { q: "", status: null, x: undefined, page: 0, active: false })).toBe(
      "/api/orders?page=0&active=false",
    );
  });

  it("encodes query values", () => {
    expect(buildUrl("/products", { q: "a&b c" })).toBe("/api/products?q=a%26b+c");
  });
});

describe("api", () => {
  it("sends a JSON body and returns the parsed response", async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, { id: 7 }));

    const result = await api.post<{ id: number }>("/orders", {
      body: { a: 1 },
      headers: { "Idempotency-Key": "k1" },
    });

    expect(result).toEqual({ id: 7 });
    const [url, init] = fetchMock.mock.calls[0]!;
    expect(url).toBe("/api/orders");
    expect(init?.body).toBe('{"a":1}');
    expect(init?.headers).toMatchObject({
      "Content-Type": "application/json",
      "Idempotency-Key": "k1",
    });
  });

  it("returns undefined for 204", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    await expect(api.delete("/products/1")).resolves.toBeUndefined();
  });

  it("maps a ProblemDetail response, including field errors", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(400, {
        title: "Bad Request",
        detail: "Validation failed",
        fieldErrors: [{ field: "sku", message: "already in use" }],
      }),
    );

    const error = await api.post("/products", { body: {} }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 400,
      message: "Validation failed",
      fieldErrors: [{ field: "sku", message: "already in use" }],
    });
  });

  it("survives an error response that is not JSON", async () => {
    fetchMock.mockResolvedValue(new Response("<html>Bad gateway</html>", { status: 502 }));
    await expect(api.get("/products")).rejects.toMatchObject({
      status: 502,
      message: "The request failed (502).",
    });
  });

  it("reports a network failure as status 0", async () => {
    fetchMock.mockRejectedValue(new TypeError("Failed to fetch"));
    await expect(api.get("/products")).rejects.toMatchObject({ status: 0 });
  });

  it("rethrows aborts so callers can ignore them", async () => {
    fetchMock.mockRejectedValue(new DOMException("Aborted", "AbortError"));
    await expect(api.get("/products")).rejects.toMatchObject({ name: "AbortError" });
  });

  it("notifies the unauthorized handler only on 401", async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    fetchMock.mockResolvedValueOnce(jsonResponse(403, { title: "Forbidden" }));
    await expect(api.get("/orders")).rejects.toMatchObject({ status: 403 });
    expect(handler).not.toHaveBeenCalled();

    fetchMock.mockResolvedValueOnce(jsonResponse(401, { title: "Unauthorized" }));
    await expect(api.get("/orders")).rejects.toMatchObject({ status: 401 });
    expect(handler).toHaveBeenCalledOnce();
  });
});
