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
  courierName: string | null;
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

export type CourierCandidate = { userId: number; fullName: string | null };

export type ZoneOption = { id: number; name: string; district: string };

// Contract of GET /api/v1/notifications/admin (notification-service, ADMIN and DISPATCHER).
export type NotificationChannel = "EMAIL" | "SMS";
export type NotificationDeliveryStatus = "PENDING" | "SENT" | "FAILED";

export type NotificationEntry = {
  id: number;
  eventId: string;
  trackingNumber: string;
  parcelId: number;
  customerId: number | null;
  type: NotificationChannel; // the API calls the channel "type"
  recipient: string; // already masked by the notification service
  oldStatus: string | null;
  newStatus: string;
  status: NotificationDeliveryStatus;
  failureReason: string | null;
  createdAt: string;
  sentAt: string | null;
};

export type NotificationPage = {
  content: NotificationEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
};

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