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

// Districts that have a pricing zone (a district without a zone gives a 404 from the API).
export const DISTRICTS = ["Colombo", "Gampaha", "Kandy", "Galle", "Jaffna"];

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

export const cancelParcel = async (id: number): Promise<ParcelDetail> => {
  const res = await api.post<ParcelDetail>(`/parcels/${id}/cancel`);
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