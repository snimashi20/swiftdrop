export type MerchantStatus = "PENDING" | "APPROVED" | "REJECTED";
export type ParcelStatus = "CREATED" | "PICKED_UP" | "IN_TRANSIT" | "OUT_FOR_DELIVERY" | "DELIVERED" | "FAILED" | "CANCELLED";

export interface Merchant {
  id: number;
  businessName: string;
  businessType?: "SOLE_PROPRIETORSHIP" | "PARTNERSHIP" | "PRIVATE_LIMITED_COMPANY" | "OTHER" | null;
  registrationNumber: string;
  businessAddress?: string | null;
  businessDescription?: string | null;
  verificationDeclarationAccepted?: boolean | null;
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
  businessType: "SOLE_PROPRIETORSHIP" | "PARTNERSHIP" | "PRIVATE_LIMITED_COMPANY" | "OTHER";
  registrationNumber: string;
  businessAddress: string;
  businessDescription: string;
  contactName: string;
  contactPhone: string;
  verificationDeclarationAccepted: true;
}

export type UpdateMerchantProfileRequest = Omit<RegisterMerchantRequest, "email" | "password" | "verificationDeclarationAccepted"> & {
  verificationDeclarationAccepted: boolean;
};

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

export interface MerchantOrderTrendPoint {
  date: string;
  count: number;
}

export interface Page<T> {
  content: T[];
  number?: number;
  page?: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
