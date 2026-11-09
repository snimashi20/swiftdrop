export type MerchantStatus = "PENDING" | "APPROVED" | "REJECTED";
export type ParcelStatus = "CREATED" | "PICKED_UP" | "IN_TRANSIT" | "OUT_FOR_DELIVERY" | "DELIVERED" | "FAILED";

export interface Merchant {
  id: number;
  businessName: string;
  registrationNumber: string;
  contactName: string;
  contactEmail: string;
  contactPhone: string;
  status: MerchantStatus;
  rejectionReason?: string | null;
  createdAt: string;
}

export interface RegisterMerchantRequest {
  email: string;
  password: string;
  businessName: string;
  registrationNumber: string;
  contactName: string;
  contactPhone: string;
}

export interface ApiKey {
  id: number;
  label: string;
  keyPrefix: string;
  active: boolean;
  createdAt: string;
  lastUsedAt?: string | null;
  revokedAt?: string | null;
}

export interface GeneratedApiKey extends ApiKey {
  apiKey: string;
}

export interface MerchantOrder {
  trackingNumber: string;
  status: ParcelStatus;
  fee: number;
  createdAt: string;
  externalOrderId?: string | null;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
