import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AuthProvider, useAuth } from "./features/auth/AuthContext";
import { LoginPage } from "./features/auth/LoginPage";
import { SignupPage } from "./features/auth/SignupPage";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { PlaceholderPage } from "./features/dashboard/PlaceholderPage";
import { OAuthCallbackPage } from "./features/webhook/pages/OAuthCallbackPage";
import { WebhookSettingsPage } from "./features/webhook/pages/WebhookSettingsPage";
import { UserProfilePage } from "./features/webhook/pages/UserProfilePage";
import { AddressBookPage } from "./features/webhook/pages/AddressBookPage";

function PublicRedirect() {
  const { user } = useAuth();
  return <Navigate to={user ? "/dashboard" : "/login"} replace />;
}

export function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/oauth/callback" element={<OAuthCallbackPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/profile" element={<UserProfilePage />} />
            <Route path="/profile/addresses" element={<AddressBookPage />} />
            <Route path="/webhooks" element={<WebhookSettingsPage />} />
            <Route path="/parcels" element={<PlaceholderPage />} />
            <Route path="/fleet" element={<ProtectedRoute allowedRoles={["DISPATCHER", "ADMIN"]} />}>
              <Route index element={<PlaceholderPage />} />
            </Route>
            <Route path="/people" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route index element={<PlaceholderPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<PublicRedirect />} />
      </Routes>
    </AuthProvider>
  );
}
