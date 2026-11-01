import { describe, expect, it } from "vitest";
import { hasErrors } from "@/utils/formErrors";
import {
  EMPTY_WAREHOUSE,
  toWarehouseRequest,
  validateWarehouseForm,
  type WarehouseFormValues,
} from "./warehouseForm";

const valid: WarehouseFormValues = {
  name: "Hubli Warehouse",
  address: "Gokul Road",
  city: "Hubli",
  state: "Karnataka",
  postalCode: "580030",
  latitude: "15.3647",
  longitude: "75.1240",
  contactNumber: "+91 9876543210",
  baseDeliveryCost: "120",
  costPerKm: "14.50",
  handlingHours: "12",
};

describe("validateWarehouseForm", () => {
  it("accepts a valid warehouse", () => {
    expect(hasErrors(validateWarehouseForm(valid))).toBe(false);
  });

  it("requires everything except the pre-filled state", () => {
    const errors = validateWarehouseForm(EMPTY_WAREHOUSE);
    expect(errors.state).toBeNull();
    expect(errors.name).toBeTruthy();
    expect(errors.latitude).toBeTruthy();
    expect(errors.handlingHours).toBeTruthy();
  });

  it.each([
    ["latitude", "91"],
    ["latitude", "abc"],
    ["longitude", "-181"],
    ["postalCode", "12345"],
    ["contactNumber", "call me"],
    ["baseDeliveryCost", "-5"],
    ["handlingHours", "1.5"],
  ] as const)("rejects %s = %s", (field, value) => {
    expect(validateWarehouseForm({ ...valid, [field]: value })[field]).toBeTruthy();
  });

  it("accepts negative coordinates within range", () => {
    expect(validateWarehouseForm({ ...valid, latitude: "-33.8" }).latitude).toBeNull();
  });
});

describe("toWarehouseRequest", () => {
  it("converts numeric fields", () => {
    expect(toWarehouseRequest(valid, "ACTIVE")).toMatchObject({
      latitude: 15.3647,
      costPerKm: 14.5,
      handlingHours: 12,
      status: "ACTIVE",
    });
  });
});
