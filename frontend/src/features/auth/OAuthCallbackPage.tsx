import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { decodeJwt, persistAuth } from "./auth.storage";
import type { User, UserStatus } from "./auth.types";

/**
 * Maps the decoded role to its designated operational portal route in App.tsx
 */
const getRolePortalRoute = (rawRole?: string): string => {
  const role = rawRole?.replace(/^ROLE_/, "").toUpperCase();
  switch (role) {
    case "ADMIN":
      return "/admin/users";
    case "DISPATCHER":
      return "/fleet";
    case "COURIER":
      return "/courier/today";
    case "MERCHANT":
      return "/merchant/orders";
    case "CUSTOMER":
    default:
      return "/dashboard";
  }
};

export const OAuthCallbackPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    // 1. Clean up temporary OAuth registration role state
    sessionStorage.removeItem("swiftdrop_pending_oauth_role");

    const token = searchParams.get("token");
    const error = searchParams.get("error");
    const statusParam = searchParams.get("status");

    // 2. Immediate redirect for PendingAdminApproval (DO NOT save any token)
    if (error === "PendingAdminApproval") {
      navigate("/login?error=PendingAdminApproval", { replace: true });
      return;
    }

    // 3. Handle other OAuth errors (e.g. account_disabled, email_not_verified)
    if (error) {
      console.error("OAuth Authentication Error:", error);
      navigate(`/login?error=${encodeURIComponent(error)}`, { replace: true });
      return;
    }

    // 4. Valid token processing
    if (token) {
      try {
        const decoded = decodeJwt(token);
        const status = (statusParam || decoded.status || "ACTIVE") as UserStatus;
        const user: User = {
          id: decoded.id,
          name: decoded.name || decoded.email?.split("@")[0] || "SwiftDrop user",
          email: decoded.email || "",
          role: decoded.role || "CUSTOMER",
          status,
        };

        // Persist token in swiftdrop_access_token and user info
        persistAuth(token, user, true);

        // Routing logic based on user status and role
        if (user.status === "PENDING_ROLE") {
          window.location.href = "/select-role";
          return;
        }
        if (user.status === "PENDING_APPROVAL" || statusParam === "PENDING_APPROVAL") {
          window.location.href = "/waiting-approval";
          return;
        }

        // Navigate to exact role portal route defined in App.tsx
        window.location.href = getRolePortalRoute(decoded.role || user.role);
      } catch (e) {
        console.error("Failed to decode token", e);
        navigate("/login?error=token_invalid", { replace: true });
      }
    } else {
      navigate("/login", { replace: true });
    }
  }, [searchParams, navigate]);

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-sand">
      <div className="p-8 bg-white rounded-2xl shadow-sm border border-ink/5 text-center max-w-sm w-full mx-4">
        <div className="w-10 h-10 border-4 border-coral border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
        <h2 className="text-lg font-bold text-ink">Completing Sign-In...</h2>
        <p className="text-sm text-ink/60 mt-1">Please wait while we log you into SwiftDrop.</p>
      </div>
    </div>
  );
};

export default OAuthCallbackPage;
