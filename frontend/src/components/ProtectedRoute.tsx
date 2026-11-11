import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import type { Role } from "../features/auth/auth.types";

export function ProtectedRoute({ allowedRoles }: { allowedRoles?: Role[] }) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  // If user hasn't chosen a role yet, redirect to role selection
  if (user.status === "PENDING_ROLE") {
    return <Navigate to="/select-role" replace />;
  }

  // If user is awaiting admin approval, block dashboard and send to waiting screen
  if (user.status === "PENDING_APPROVAL") {
    return <Navigate to="/waiting-approval" replace />;
  }

  // If role not permitted for this route, redirect to general dashboard
  if (allowedRoles && !allowedRoles.includes(user.role as Role)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}
