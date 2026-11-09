import type { Role } from "../auth/auth.types";

export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  phoneNumber?: string | null;
  role: Role;
  provider: "LOCAL" | "GOOGLE" | string;
  enabled: boolean;
}

export interface UpdateRoleDto {
  role: Role;
}

export interface StatusToggleDto {
  enabled: boolean;
}

export type RoleFilterOption = "ALL" | Role;

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
