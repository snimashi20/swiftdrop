import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import type { Role } from "../features/auth/auth.types";
export function ProtectedRoute({ allowedRoles }: { allowedRoles?: Role[] }) { const { user } = useAuth(); const location = useLocation(); if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />; if (allowedRoles && !allowedRoles.includes(user.role)) return <Navigate to="/dashboard" replace />; return <Outlet />; }