import { api } from "../../lib/api";
import type { Page, UnassignedParcel } from "./types";

// What the Assign / Reassign dialog needs to show about a parcel.
export type ParcelSummary = Pick<
  UnassignedParcel,
  "id" | "trackingNumber" | "pickupCity" | "pickupDistrict" | "dropoffCity" | "dropoffDistrict" | "size" | "weightKg"
>;

export type AssignedParcel = ParcelSummary & {
  status: string;
  assignedCourierId: number;
  courierName: string | null;
  assignedAt: string;
};

export const assignedParcelsApi = {
  list: async (params: { courierId?: number; activeOnly?: boolean; page?: number; size?: number } = {}) =>
    (await api.get<Page<AssignedParcel>>("/dispatch/parcels/assigned", { params })).data,
};