export const ROLES = ["CUSTOMER", "MERCHANT", "COURIER", "DISPATCHER", "ADMIN"] as const;
export type Role = (typeof ROLES)[number];
export type User = { id?: number | string; name: string; email: string; role: Role };
export type LoginInput = { email: string; password: string; remember: boolean };
export type SignupInput = { name: string; email: string; password: string; role: Role };
export type AuthResponse = { accessToken?: string; token?: string; refreshToken?: string; user?: Partial<User> };