import type { Warehouse, WarehouseRequest } from "@/types/api";
import type { FormErrors } from "@/utils/formErrors";
import { postalCode, price, required, validate, wholeNumber } from "@/utils/validators";

export interface WarehouseFormValues {
  name: string;
  address: string;
  city: string;
  state: string;
  postalCode: string;
  latitude: string;
  longitude: string;
  contactNumber: string;
  baseDeliveryCost: string;
  costPerKm: string;
  handlingHours: string;
}

export const EMPTY_WAREHOUSE: WarehouseFormValues = {
  name: "",
  address: "",
  city: "",
  state: "Karnataka",
  postalCode: "",
  latitude: "",
  longitude: "",
  contactNumber: "",
  baseDeliveryCost: "",
  costPerKm: "",
  handlingHours: "",
};

export function toWarehouseFormValues(warehouse: Warehouse): WarehouseFormValues {
  return {
    name: warehouse.name,
    address: warehouse.address,
    city: warehouse.city,
    state: warehouse.state,
    postalCode: warehouse.postalCode,
    latitude: String(warehouse.latitude),
    longitude: String(warehouse.longitude),
    contactNumber: warehouse.contactNumber,
    baseDeliveryCost: String(warehouse.baseDeliveryCost),
    costPerKm: String(warehouse.costPerKm),
    handlingHours: String(warehouse.handlingHours),
  };
}

function coordinate(value: string, limit: number, label: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) return "This field is required.";
  const number = Number(trimmed);
  return Number.isFinite(number) && Math.abs(number) <= limit
    ? null
    : `${label} must be between -${limit} and ${limit}.`;
}

export function validateWarehouseForm(values: WarehouseFormValues): FormErrors {
  return {
    name: validate(values.name, required),
    address: validate(values.address, required),
    city: validate(values.city, required),
    state: validate(values.state, required),
    postalCode: validate(values.postalCode, required, postalCode),
    latitude: coordinate(values.latitude, 90, "Latitude"),
    longitude: coordinate(values.longitude, 180, "Longitude"),
    contactNumber:
      validate(values.contactNumber, required) ??
      (/^[+0-9 -]{7,20}$/.test(values.contactNumber.trim())
        ? null
        : "Enter a valid contact number."),
    baseDeliveryCost: validate(values.baseDeliveryCost, required, price),
    costPerKm: validate(values.costPerKm, required, price),
    handlingHours: validate(values.handlingHours, required, wholeNumber()),
  };
}

export function toWarehouseRequest(
  values: WarehouseFormValues,
  status?: WarehouseRequest["status"],
): WarehouseRequest {
  return {
    name: values.name.trim(),
    address: values.address.trim(),
    city: values.city.trim(),
    state: values.state.trim(),
    postalCode: values.postalCode.trim(),
    latitude: Number(values.latitude),
    longitude: Number(values.longitude),
    contactNumber: values.contactNumber.trim(),
    baseDeliveryCost: Number(values.baseDeliveryCost),
    costPerKm: Number(values.costPerKm),
    handlingHours: Number(values.handlingHours),
    status,
  };
}
