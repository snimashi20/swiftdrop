// Mirrors the DTOs of /api/v1/dispatch/** in core-api (package com.swiftdrop.dispatch.dto).

export type VehicleType = "BICYCLE" | "MOTORBIKE" | "THREE_WHEELER" | "CAR" | "VAN";

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export type UnassignedParcel = {
  id: number;
  trackingNumber: string;
  status: string;
  pickupCity: string | null;
  pickupDistrict: string | null;
  dropoffCity: string | null;
  dropoffDistrict: string | null;
  size: string;
  weightKg: number;
  createdAt: string;
};

export type ParcelAssignment = {
  parcelId: number;
  trackingNumber: string;
  status: string;
  assignedCourierId: number;
  assignedByUserId: number | null;
  assignedAt: string;
};

export type CourierProfile = {
  id: number;
  userId: number;
  vehicleType: VehicleType;
  zoneId: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CourierWorkload = {
  courierProfileId: number;
  userId: number;
  vehicleType: VehicleType;
  zoneId: number;
  active: boolean;
  activeDeliveries: number;
};

export type DailyDeliveries = { date: string; delivered: number; failed: number; cancelled: number };
export type Outcomes = {
  delivered: number;
  failed: number;
  cancelled: number;
  successRatePercent: number | null;
  failureRatePercent: number | null;
};
export type ZoneDeliveries = { district: string; delivered: number; failed: number; cancelled: number };

export type ReportSummary = {
  days: number;
  from: string;
  to: string;
  daily: DailyDeliveries[];
  outcomes: Outcomes;
  perZone: ZoneDeliveries[];
};