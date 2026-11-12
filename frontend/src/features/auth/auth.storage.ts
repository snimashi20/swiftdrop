import type { User, Role } from "./auth.types";

export const TOKEN_KEY = "swiftdrop_access_token";
export const USER_KEY = "swiftdrop_user";

export const readStoredAuth = (): { token: string | null; user: User | null } => {
  const token = localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
  const rawUser = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY);
  return { token, user: rawUser ? (JSON.parse(rawUser) as User) : null };
};

export const persistAuth = (token: string, user: User, remember: boolean) => {
  const storage = remember ? localStorage : sessionStorage;
  const other = remember ? sessionStorage : localStorage;
  other.removeItem(TOKEN_KEY);
  other.removeItem(USER_KEY);
  storage.setItem(TOKEN_KEY, token);
  storage.setItem(USER_KEY, JSON.stringify(user));
};

export const clearAuth = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
};

export const decodeJwt = (token: string): Partial<User> => {
  try {
    const payload = JSON.parse(
      atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"))
    ) as Record<string, unknown>;

    const roles = payload.roles as string[] | undefined;
    const authorities = payload.authorities as string[] | undefined;
    let rawRole = (payload.role || roles?.[0] || authorities?.[0]) as string | undefined;
    if (rawRole && rawRole.startsWith("ROLE_")) {
      rawRole = rawRole.substring(5);
    }

    const isKnownRole = rawRole && ["CUSTOMER", "MERCHANT", "COURIER", "DISPATCHER", "ADMIN"].includes(rawRole);
    const resolvedRole: Role = isKnownRole ? (rawRole as Role) : "CUSTOMER";
    const status = (payload.status as string) || (rawRole === "PENDING_ROLE" ? "PENDING_ROLE" : "ACTIVE");

    return {
      id: (payload.sub || payload.userId) as string | undefined,
      name: (payload.name || payload.fullName) as string | undefined,
      email: payload.email as string | undefined,
      role: resolvedRole,
      status,
    };
  } catch {
    return {};
  }
};
