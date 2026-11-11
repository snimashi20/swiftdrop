import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { decodeJwt, persistAuth } from "./auth.storage";
import type { User, UserStatus } from "./auth.types";

export const OAuthCallbackPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const token = searchParams.get("token");
    const error = searchParams.get("error");
    const isNewUserParam = searchParams.get("newUser") === "true";
    const statusParam = searchParams.get("status");

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

        // Routing logic based on user role and approval status
        if (isNewUserParam || user.status === "PENDING_ROLE") {
          window.location.href = "/select-role";
        } else if (user.status === "PENDING_APPROVAL" || statusParam === "PENDING_APPROVAL") {
          window.location.href = "/waiting-approval";
        } else {
          window.location.href = "/dashboard";
        }
      } catch (e) {
        console.error("Failed to decode token", e);
        navigate("/login?error=token_invalid", { replace: true });
      }
    } else if (error) {
      console.error("OAuth Authentication Error:", error);
      navigate(`/login?error=${encodeURIComponent(error)}`, { replace: true });
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
