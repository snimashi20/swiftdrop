import { describe, expect, it } from "vitest";
import {
  normalizePhone,
  validateInstructions,
  validateItemType,
  validatePhone,
  validateReceiverName,
  validateWeight,
} from "./bookingValidation";

describe("validateReceiverName", () => {
  it("accepts normal names", () => {
    expect(validateReceiverName("Nimal Perera")).toBe("");
    expect(validateReceiverName("K. D. O'Neil-Perera")).toBe("");
    expect(validateReceiverName("සුනිල් පෙරේරා")).toBe("");
    expect(validateReceiverName("கார்த்திக்")).toBe("");
  });

  it("rejects empty, short, numbers and symbols", () => {
    expect(validateReceiverName("  ")).not.toBe("");
    expect(validateReceiverName("A")).not.toBe("");
    expect(validateReceiverName("hhh123")).not.toBe("");
    expect(validateReceiverName("Nimal@Perera")).not.toBe("");
    expect(validateReceiverName("a".repeat(101))).not.toBe("");
  });
});

describe("validatePhone", () => {
  it("accepts Sri Lankan numbers, with or without spaces", () => {
    expect(validatePhone("0771234567")).toBe("");
    expect(validatePhone("+94771234567")).toBe("");
    expect(validatePhone("077 123 4567")).toBe("");
    expect(validatePhone("077-123-4567")).toBe("");
  });

  it("rejects letters, wrong length and bad starts", () => {
    for (const phone of ["", "abc", "12345", "077123456", "07712345678", "0000000000", "+9477123456"]) {
      expect(validatePhone(phone)).not.toBe("");
    }
  });

  it("removes spaces and dashes before sending", () => {
    expect(normalizePhone("077 123-4567")).toBe("0771234567");
  });
});

describe("validateWeight", () => {
  it("accepts a positive number with up to 2 decimals", () => {
    expect(validateWeight("1")).toBe("");
    expect(validateWeight("2.5")).toBe("");
    expect(validateWeight("9999.99")).toBe("");
  });

  it("rejects empty, zero, negative, too many decimals, letters and too heavy", () => {
    for (const weight of ["", "0", "-1", "1.234", "abc", "1e3", "10000"]) {
      expect(validateWeight(weight)).not.toBe("");
    }
  });
});

describe("validateItemType and validateInstructions", () => {
  it("needs an item type", () => {
    expect(validateItemType("")).not.toBe("");
    expect(validateItemType("FOOD")).toBe("");
  });

  it("allows up to 500 characters of instructions", () => {
    expect(validateInstructions("a".repeat(500))).toBe("");
    expect(validateInstructions("a".repeat(501))).not.toBe("");
  });
});
