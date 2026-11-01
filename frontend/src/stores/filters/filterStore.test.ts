import { beforeEach, describe, expect, it } from "vitest";
import { FILTER_DEFAULTS, sortParam, useFilterStore } from "./filterStore";

const state = () => useFilterStore.getState();

describe("filter store", () => {
  beforeEach(() => useFilterStore.setState({ filters: FILTER_DEFAULTS }));

  it("returns to page one when a filter changes", () => {
    state().setFilters("products", { page: 3 });
    state().setFilters("products", { q: "phone" });
    expect(state().filters.products).toMatchObject({ q: "phone", page: 0 });
  });

  it("keeps the page when only the page changes", () => {
    state().setFilters("products", { page: 3 });
    expect(state().filters.products.page).toBe(3);
  });

  it("keeps sections independent", () => {
    state().setFilters("inventory", { q: "laptop" });
    expect(state().filters.products.q).toBe("");
  });
});

describe("sortParam", () => {
  it("formats the API's sort parameter", () => {
    expect(sortParam({ key: "name", direction: "desc" })).toBe("name,desc");
    expect(sortParam(null)).toBeUndefined();
  });
});
