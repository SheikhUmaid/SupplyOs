import { describe, expect, it } from "vitest";
import { formatCount, formatDate, formatDateTime, formatInr } from "./format";

describe("formatInr", () => {
  it("uses Indian digit grouping and two decimals", () => {
    expect(formatInr(1234567.5)).toBe("₹12,34,567.50");
    expect(formatInr(0)).toBe("₹0.00");
  });
});

describe("formatCount", () => {
  it("uses Indian digit grouping", () => {
    expect(formatCount(100000)).toBe("1,00,000");
  });
});

describe("date formatting", () => {
  it("converts UTC to IST, including across midnight", () => {
    expect(formatDate("2026-03-01T20:00:00Z")).toBe("2 Mar 2026");
    expect(formatDateTime("2026-03-01T20:00:00Z").toLowerCase()).toContain("1:30");
  });
});
