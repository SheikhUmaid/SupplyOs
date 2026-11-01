import { describe, expect, it } from "vitest";
import {
  email,
  indianPhone,
  password,
  postalCode,
  price,
  required,
  validate,
  wholeNumber,
} from "./validators";

describe("validators", () => {
  it("required rejects blank values", () => {
    expect(required("   ")).not.toBeNull();
    expect(required("x")).toBeNull();
  });

  it.each([
    ["a@b.co", true],
    ["a@b", false],
    ["a b@c.co", false],
  ])("email %s", (value, valid) => {
    expect(email(value) === null).toBe(valid);
  });

  it("password requires 8 characters", () => {
    expect(password("1234567")).not.toBeNull();
    expect(password("12345678")).toBeNull();
  });

  it.each([
    ["9876543210", true],
    ["+91 9876543210", true],
    ["5876543210", false],
    ["98765", false],
  ])("indianPhone %s", (value, valid) => {
    expect(indianPhone(value) === null).toBe(valid);
  });

  it.each([
    ["580020", true],
    ["080020", false],
    ["58002", false],
  ])("postalCode %s", (value, valid) => {
    expect(postalCode(value) === null).toBe(valid);
  });

  it.each([
    ["1499", true],
    ["1499.50", true],
    ["1499.505", false],
    ["-1", false],
    ["abc", false],
  ])("price %s", (value, valid) => {
    expect(price(value) === null).toBe(valid);
  });

  it("wholeNumber rejects negatives, decimals and values under the minimum", () => {
    expect(wholeNumber()("0")).toBeNull();
    expect(wholeNumber()("-1")).not.toBeNull();
    expect(wholeNumber()("1.5")).not.toBeNull();
    expect(wholeNumber(1)("0")).not.toBeNull();
  });

  it("validate returns the first failure", () => {
    expect(validate("", required, email)).toBe("This field is required.");
    expect(validate("nope", required, email)).toBe("Enter a valid email address.");
    expect(validate("a@b.co", required, email)).toBeNull();
  });
});
