import type { Role } from "../auth/auth.types";

export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  phoneNumber?: string | null;
  role: Role;
  provider: "LOCAL" | "GOOGLE" | string;
  enabled: boolean;
  status?: string;
  createdAt?: string;
}

export interface AdminUserDetails extends AdminUser {
  merchantProfile?: {
    id: number;
    businessName: string;
    registrationNumber: string;
    contactName: string;
    contactEmail: string;
    contactPhone: string;
    status: string;
    rejectionReason?: string | null;
    createdAt?: string | null;
  } | null;
  courierProfile?: {
    id: number;
    vehicleType: string;
    zoneId: number;
    active: boolean;
    createdAt?: string | null;
  } | null;
}

export interface RejectUserRequestDto {
  reason?: string;
}

export interface UpdateRoleDto {
  role: Role;
}

export interface StatusToggleDto {
  enabled: boolean;
}

export type RoleFilterOption = "ALL" | Role;
export type StatusFilterOption = "ALL" | "PENDING_APPROVAL" | "ACTIVE" | "INACTIVE";

export interface CreateStaffRequestDto {
  fullName: string;
  email: string;
  phoneNumber?: string;
  role: "COURIER" | "DISPATCHER";
  password: string;
}

export interface ToastMessage {
  id: string;
  type: "success" | "error" | "info";
  title: string;
  message?: string;
}
