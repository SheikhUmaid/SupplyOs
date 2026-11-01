import { describe, expect, it } from "vitest";
import { isNavItemActive, resolveAdminPage, resolveAdminSearchHref } from "./navigation";

describe("resolveAdminPage", () => {
  it("resolves the overview without a link back to itself", () => {
    expect(resolveAdminPage("/admin")).toEqual({
      title: "Overview",
      crumbs: [{ label: "Control Tower" }],
    });
  });

  it("resolves a section", () => {
    const page = resolveAdminPage("/admin/inventory");
    expect(page.title).toBe("Inventory");
    expect(page.crumbs.map((c) => c.label)).toEqual(["Control Tower", "Inventory"]);
  });

  it("resolves dynamic order details with a link to the parent section", () => {
    const page = resolveAdminPage("/admin/orders/42");
    expect(page.title).toBe("Order details");
    expect(page.crumbs[1]).toEqual({ label: "Orders", href: "/admin/orders" });
  });

  it("resolves the new-product page and settings", () => {
    expect(resolveAdminPage("/admin/products/new").title).toBe("New product");
    expect(resolveAdminPage("/admin/settings").title).toBe("Settings");
  });

  it("ignores a trailing slash", () => {
    expect(resolveAdminPage("/admin/orders/").title).toBe("Orders");
  });
});

describe("isNavItemActive", () => {
  it("matches Overview only on the exact path", () => {
    expect(isNavItemActive("/admin", "/admin")).toBe(true);
    expect(isNavItemActive("/admin", "/admin/orders")).toBe(false);
  });

  it("matches child routes but not routes sharing a prefix", () => {
    expect(isNavItemActive("/admin/orders", "/admin/orders/42")).toBe(true);
    expect(isNavItemActive("/admin/orders", "/admin/orders-archive")).toBe(false);
  });
});

describe("resolveAdminSearchHref", () => {
  it("searches the section in view and encodes the query", () => {
    expect(resolveAdminSearchHref("/admin/products", " wireless & co ")).toBe(
      "/admin/products?q=wireless%20%26%20co",
    );
  });

  it("falls back to orders for sections without search", () => {
    expect(resolveAdminSearchHref("/admin/analytics", "ORD-1")).toBe("/admin/orders?q=ORD-1");
  });
});
