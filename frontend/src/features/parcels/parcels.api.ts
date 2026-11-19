import { api } from "../../lib/api";

export type ParcelSize = "S" | "M" | "L";

export type ParcelStatus =
  | "CREATED"
  | "PICKED_UP"
  | "IN_TRANSIT"
  | "OUT_FOR_DELIVERY"
  | "DELIVERED"
  | "FAILED"
  | "CANCELLED";

export const STATUSES: ParcelStatus[] = [
  "CREATED",
  "PICKED_UP",
  "IN_TRANSIT",
  "OUT_FOR_DELIVERY",
  "DELIVERED",
  "FAILED",
  "CANCELLED",
];

// The names a customer sees. CREATED reads "Booked" because that is what the customer did.
export const STATUS_LABELS: Record<ParcelStatus, string> = {
  CREATED: "Booked",
  PICKED_UP: "Picked up",
  IN_TRANSIT: "In transit",
  OUT_FOR_DELIVERY: "Out for delivery",
  DELIVERED: "Delivered",
  FAILED: "Failed",
  CANCELLED: "Cancelled",
};

// Districts that have a pricing zone (a district without a zone gives a 404 from the API).
export const DISTRICTS = [
  "Ampara", "Anuradhapura", "Badulla", "Batticaloa", "Colombo", 
  "Galle", "Gampaha", "Hambantota", "Jaffna", "Kalutara", 
  "Kandy", "Kegalle", "Kilinochchi", "Kurunegala", "Mannar", 
  "Matale", "Matara", "Monaragala", "Mullaitivu", "Nuwara Eliya", 
  "Polonnaruwa", "Puttalam", "Ratnapura", "Trincomalee", "Vavuniya"
];

// What is inside the parcel (the drop-down on the booking page) and who pays the delivery fee.
export const ITEM_TYPES = [
  { value: "DOCUMENTS", label: "Documents" },
  { value: "FOOD", label: "Food" },
  { value: "GROCERIES", label: "Groceries" },
  { value: "BUSINESS_SUPPLIES", label: "Business Supplies / Samples" },
  { value: "CLOTHES", label: "Clothes" },
  { value: "HOUSEHOLD_GOODS", label: "Household Goods" },
  { value: "ELECTRONICS", label: "Electronics" },
  { value: "SPORTS_GOODS", label: "Sports Goods" },
  { value: "OTHER", label: "Other" },
] as const;

export type ParcelItemType = (typeof ITEM_TYPES)[number]["value"];
export type DeliveryPayer = "SENDER" | "RECEIVER";

export const itemTypeLabel = (value: string | null | undefined) =>
  ITEM_TYPES.find((t) => t.value === value)?.label ?? "";

export interface ParcelAddress {
  line1: string;
  city: string;
  district: string;
}

export interface CreateParcelRequest {
  pickup: ParcelAddress;
  dropoff: ParcelAddress;
  receiverName: string;
  receiverPhone: string;
  size: ParcelSize;
  weightKg: number;
  deliveryInstructions?: string;
  itemType?: ParcelItemType;
  deliveryPaidBy?: DeliveryPayer;
}

export interface ParcelCreated {
  id: number;
  trackingNumber: string;
  status: ParcelStatus;
  fee: number;
  createdAt: string;
}

export interface ParcelDetail {
  id: number;
  trackingNumber: string;
  status: ParcelStatus;
  fee: number;
  pickup: ParcelAddress;
  dropoff: ParcelAddress;
  receiverName: string;
  receiverPhone: string;
  size: ParcelSize;
  weightKg: number;
  deliveryInstructions: string | null;
  itemType?: ParcelItemType | null;
  deliveryPaidBy?: DeliveryPayer | null;
  cancelReason?: CancelReason | null;
  cancelComment?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ParcelPage {
  content: ParcelDetail[];
  number: number;
  totalPages: number;
  totalElements: number;
}

export interface Quote {
  fee: number;
  pickupZone: string;
  dropoffZone: string;
  interZone: boolean;
}

export const getQuote = async (
  pickupDistrict: string,
  dropoffDistrict: string,
  size: ParcelSize,
  weightKg: number
): Promise<Quote> => {
  const res = await api.post<Quote>("/pricing/quote", { pickupDistrict, dropoffDistrict, size, weightKg });
  return res.data;
};

export const createParcel = async (request: CreateParcelRequest): Promise<ParcelCreated> => {
  const res = await api.post<ParcelCreated>("/parcels", request);
  return res.data;
};

export const listParcels = async (page: number, status?: ParcelStatus): Promise<ParcelPage> => {
  const res = await api.get<ParcelPage>("/parcels", { params: { page, size: 10, status } });
  return res.data;
};

export type CancelReason = "BOOKED_BY_MISTAKE" | "WRONG_DETAILS" | "FOUND_ANOTHER_OPTION" | "NO_LONGER_NEEDED" | "OTHER";

export const CANCEL_REASONS: { value: CancelReason; label: string }[] = [
  { value: "BOOKED_BY_MISTAKE", label: "Booked by mistake" },
  { value: "WRONG_DETAILS", label: "Wrong address or receiver details" },
  { value: "FOUND_ANOTHER_OPTION", label: "Found another option" },
  { value: "NO_LONGER_NEEDED", label: "Delivery no longer needed" },
  { value: "OTHER", label: "Other" },
];

export const cancelReasonLabel = (value?: CancelReason | null): string =>
  CANCEL_REASONS.find((r) => r.value === value)?.label ?? "";

export const cancelParcel = async (id: number, reason: CancelReason, comment?: string): Promise<ParcelDetail> => {
  const res = await api.post<ParcelDetail>(`/parcels/${id}/cancel`, {
    reason,
    comment: comment?.trim() || undefined,
  });
  return res.data;
};

export const downloadReceipt = async (id: number, trackingNumber: string): Promise<void> => {
  const res = await api.get<Blob>(`/parcels/${id}/receipt`, { responseType: "blob" });
  const url = URL.createObjectURL(res.data);
  const link = document.createElement("a");
  link.href = url;
  link.download = `receipt-${trackingNumber}.pdf`;
  link.click();
  URL.revokeObjectURL(url);
};
