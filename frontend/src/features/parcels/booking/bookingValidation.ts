// Checks for the parcel details form. Each function returns an error message, or "" when the value is fine.
// The rules match the backend (CreateParcelRequest), so the server never rejects what this page accepted.

const NAME_PATTERN = /^\p{L}[\p{L}\p{M} .'-]*$/u;
const PHONE_PATTERN = /^(\+94|0)[1-9]\d{8}$/;
const WEIGHT_PATTERN = /^\d+(\.\d{1,2})?$/;

export const MAX_WEIGHT = 9999.99;
export const MAX_INSTRUCTIONS = 500;

// "077 123 4567" and "077-123-4567" are read as 0771234567.
export const normalizePhone = (value: string) => value.replace(/[\s-]/g, "");

export function validateReceiverName(value: string): string {
  const name = value.trim();
  if (!name) return "Enter the receiver's name.";
  if (name.length < 2) return "The name must have at least 2 letters.";
  if (name.length > 100) return "The name can have at most 100 characters.";
  if (!NAME_PATTERN.test(name)) return "The name can only have letters, spaces, . ' and -.";
  return "";
}

export function validatePhone(value: string): string {
  const phone = normalizePhone(value);
  if (!phone) return "Enter the receiver's phone number.";
  if (!PHONE_PATTERN.test(phone)) return "Enter a valid Sri Lankan number, like 0771234567 or +94771234567.";
  return "";
}

export function validateWeight(value: string): string {
  const text = value.trim();
  if (!text) return "Enter the weight.";
  if (!WEIGHT_PATTERN.test(text)) return "Use a number with up to 2 decimals, like 1.5.";
  const kg = Number(text);
  if (!(kg > 0)) return "The weight must be more than 0 kg.";
  if (kg > MAX_WEIGHT) return `The weight can be at most ${MAX_WEIGHT} kg.`;
  return "";
}

export function validateItemType(value: string): string {
  return value ? "" : "Select the item type.";
}

export function validateInstructions(value: string): string {
  return value.length > MAX_INSTRUCTIONS ? `Instructions can have at most ${MAX_INSTRUCTIONS} characters.` : "";
}
