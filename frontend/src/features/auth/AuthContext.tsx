import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { api } from "../../lib/api";
import { clearAuth, decodeJwt, persistAuth, readStoredAuth } from "./auth.storage";
import type {
  AuthResponse,
  LoginInput,
  Role,
  SelfRegisterRole,
  SignupInput,
  User,
  UserStatus,
} from "./auth.types";

type AuthContextValue = {
  user: User | null;
  isLoading: boolean;
  login: (input: LoginInput) => Promise<User>;
  signup: (input: SignupInput) => Promise<User>;
  selectRole: (role: SelfRegisterRole) => Promise<User>;
  refreshProfile: () => Promise<User | null>;
  logout: () => void;
  hasRole: (roles: Role | Role[]) => boolean;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const toUser = (response: AuthResponse, fallback: Partial<User>): User => {
  const token = response.accessToken || response.token || "";
  const decoded = decodeJwt(token);
  const source = { ...decoded, ...fallback, ...response.user };

  let role = (source.role || "CUSTOMER") as string;
  if (role.startsWith("ROLE_")) {
    role = role.substring(5);
  }

  const isKnownRole = ["CUSTOMER", "MERCHANT", "COURIER", "DISPATCHER", "ADMIN"].includes(role);
  const resolvedRole = (isKnownRole ? role : "CUSTOMER") as Role;
  const status = (response.status || source.status || (role === "PENDING_ROLE" ? "PENDING_ROLE" : "ACTIVE")) as UserStatus;

  return {
    id: source.id,
    name: source.name || source.email?.split("@")[0] || "SwiftDrop user",
    email: source.email || "",
    role: resolvedRole,
    status,
  };
};

export function AuthProvider({ children }: { children: ReactNode }) {
  const stored = readStoredAuth();
  const [user, setUser] = useState<User | null>(stored.user);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const handleUnauthorized = () => {
      clearAuth();
      setUser(null);
    };
    window.addEventListener("swiftdrop:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("swiftdrop:unauthorized", handleUnauthorized);
  }, []);

  const authenticate = async (
    path: "login" | "signup",
    input: LoginInput | SignupInput
  ): Promise<User> => {
    setIsLoading(true);
    try {
      const { remember = true, ...payload } = input as LoginInput;
      const { data } = await api.post<AuthResponse>(`/auth/${path}`, payload);
      const token = data.accessToken || data.token;
      if (!token) throw new Error("The server did not return an access token.");

      const nextUser = toUser(
        data,
        "role" in input
          ? { name: input.name, email: input.email, role: input.role }
          : { email: input.email }
      );
      persistAuth(token, nextUser, remember);
      setUser(nextUser);
      return nextUser;
    } finally {
      setIsLoading(false);
    }
  };

  const selectRole = async (role: SelfRegisterRole): Promise<User> => {
    setIsLoading(true);
    try {
      const { data } = await api.post<AuthResponse>("/auth/select-role", { role });
      const token = data.accessToken || data.token;
      const nextUser = toUser(data, { role, status: data.status });
      if (token) {
        persistAuth(token, nextUser, true);
      }
      setUser(nextUser);
      return nextUser;
    } finally {
      setIsLoading(false);
    }
  };

  const refreshProfile = async (): Promise<User | null> => {
    try {
      const { data } = await api.get<{
        id: number;
        email: string;
        fullName: string;
        role: string;
        enabled: boolean;
        status: string;
      }>("/users/me");

      let cleanRole = data.role;
      if (cleanRole && cleanRole.startsWith("ROLE_")) {
        cleanRole = cleanRole.substring(5);
      }

      const updatedUser: User = {
        id: data.id,
        name: data.fullName,
        email: data.email,
        role: (cleanRole || "CUSTOMER") as Role,
        status: (data.status || (data.enabled ? "ACTIVE" : "INACTIVE")) as UserStatus,
      };

      const storedAuth = readStoredAuth();
      if (storedAuth.token) {
        persistAuth(storedAuth.token, updatedUser, true);
      }
      setUser(updatedUser);
      return updatedUser;
    } catch {
      return user;
    }
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      login: (input) => authenticate("login", input),
      signup: (input) => authenticate("signup", input),
      selectRole,
      refreshProfile,
      logout: () => {
        clearAuth();
        setUser(null);
      },
      hasRole: (roles) => {
        if (!user || user.status === "PENDING_ROLE" || user.status === "PENDING_APPROVAL") return false;
        const targetRoles = Array.isArray(roles) ? roles : [roles];
        return targetRoles.includes(user.role);
      },
    }),
    [user, isLoading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
};
