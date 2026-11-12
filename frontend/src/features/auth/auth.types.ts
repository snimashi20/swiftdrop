export const ROLES = ["CUSTOMER", "MERCHANT", "COURIER", "DISPATCHER", "ADMIN"] as const;
export type Role = (typeof ROLES)[number];

export const SELF_REGISTER_ROLES = ["CUSTOMER", "MERCHANT", "COURIER"] as const;
export type SelfRegisterRole = (typeof SELF_REGISTER_ROLES)[number];

export type UserStatus = "ACTIVE" | "PENDING_APPROVAL" | "PENDING_ROLE" | "REJECTED" | "INACTIVE";

export type User = {
  id?: number | string;
  name: string;
  email: string;
  role: Role;
  status?: UserStatus | string;
};

export type LoginInput = { email: string; password: string; remember: boolean };
export type SignupInput = { name: string; email: string; password: string; role: SelfRegisterRole };
export type SelectRoleInput = { role: SelfRegisterRole };
export type AuthResponse = {
  accessToken?: string;
  token?: string;
  refreshToken?: string;
  user?: Partial<User>;
  status?: string;
};
