import { api } from "../../lib/api";
import type { Role } from "../auth/auth.types";
import type {
  AdminUser,
  CreateStaffRequestDto,
  StatusToggleDto,
  UpdateRoleDto,
} from "./admin.types";

/**
 * Fetch all users with optional role filtering (e.g. CUSTOMER, COURIER, DISPATCHER, MERCHANT, ADMIN).
 */
export const fetchAdminUsers = async (role?: string): Promise<AdminUser[]> => {
  const params = role && role !== "ALL" ? { role } : {};
  const response = await api.get<AdminUser[]>("/admin/users", { params });
  return response.data;
};

/**
 * Update a user's role (PUT /api/v1/admin/users/{id}/role).
 */
export const updateUserRole = async (userId: number, role: Role): Promise<AdminUser> => {
  const payload: UpdateRoleDto = { role };
  const response = await api.put<AdminUser>(`/admin/users/${userId}/role`, payload);
  return response.data;
};

/**
 * Enable or disable a user account (PATCH /api/v1/admin/users/{id}/status).
 */
export const toggleUserStatus = async (userId: number, enabled: boolean): Promise<AdminUser> => {
  const payload: StatusToggleDto = { enabled };
  const response = await api.patch<AdminUser>(`/admin/users/${userId}/status`, payload);
  return response.data;
};

/**
 * Create a Courier or Dispatcher staff account (POST /api/v1/admin/users/staff).
 */
export const createStaffAccount = async (
  data: CreateStaffRequestDto
): Promise<AdminUser> => {
  const response = await api.post<AdminUser>("/admin/users/staff", data);
  return response.data;
};
