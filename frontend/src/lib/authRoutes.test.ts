import { describe, expect, it } from "vitest";
import { homeFor, safeNextPath } from "./authRoutes";

describe("homeFor", () => {
  it("sends each role to its own interface", () => {
    expect(homeFor("ADMIN")).toBe("/admin");
    expect(homeFor("CUSTOMER")).toBe("/");
  });
});

describe("safeNextPath", () => {
  it("keeps same-site paths", () => {
    expect(safeNextPath("/admin/orders?status=PENDING")).toBe("/admin/orders?status=PENDING");
  });

  it.each([
    "https://evil.example",
    "//evil.example",
    "/\\evil.example",
    "javascript:alert(1)",
    "",
    null,
  ])("rejects %s", (next) => {
    expect(safeNextPath(next)).toBeNull();
  });
});
