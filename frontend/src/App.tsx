import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AuthProvider, useAuth } from "./features/auth/AuthContext";
import { LoginPage } from "./features/auth/LoginPage";
import { SignupPage } from "./features/auth/SignupPage";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { PlaceholderPage } from "./features/dashboard/PlaceholderPage";
function PublicRedirect() { const { user } = useAuth(); return <Navigate to={user ? "/dashboard" : "/login"} replace />; }
export function App() { return <AuthProvider><Routes><Route path="/login" element={<LoginPage />} /><Route path="/signup" element={<SignupPage />} /><Route element={<ProtectedRoute />}><Route element={<AppShell />}><Route path="/dashboard" element={<DashboardPage />} /><Route path="/parcels" element={<PlaceholderPage />} /><Route path="/fleet" element={<ProtectedRoute allowedRoles={["DISPATCHER", "ADMIN"]} />}><Route index element={<PlaceholderPage />} /></Route><Route path="/people" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}><Route index element={<PlaceholderPage />} /></Route></Route></Route><Route path="*" element={<PublicRedirect />} /></Routes></AuthProvider>; }