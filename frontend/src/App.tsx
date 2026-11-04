import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AuthProvider, useAuth } from "./features/auth/AuthContext";
import { LoginPage } from "./features/auth/LoginPage";
import { SignupPage } from "./features/auth/SignupPage";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { PlaceholderPage } from "./features/dashboard/PlaceholderPage";
import { ParcelDetailPage, PublicTrackingLayout, TrackLookupPage, TrackingPage } from "./features/tracking";
import { OAuthCallbackPage } from "./features/auth/OAuthCallbackPage";
import { WebhookSettingsPage } from "./features/webhook/pages/WebhookSettingsPage";
import { UserProfilePage } from "./features/profile/UserProfilePage";
import { AddressBookPage } from "./features/profile/AddressBookPage";
import { AdminPricingPage } from './features/pricing/pages/AdminPricingPage';
import {
  CouriersPage,
  DispatcherDashboardPage,
  NotificationLogPage,
  ReportsPage,
  UnassignedParcelsPage,
} from "./features/dispatch";
import { AdminUsersPage } from "./features/admin";

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

        <Route element={<PublicTrackingLayout />}>
          <Route path="/track" element={<TrackLookupPage />} />
          <Route path="/track/:trackingNumber" element={<TrackingPage />} />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/profile" element={<UserProfilePage />} />
            <Route path="/profile/addresses" element={<AddressBookPage />} />
            <Route path="/webhooks" element={<WebhookSettingsPage />} />
            <Route path="/parcels" element={<PlaceholderPage />} />
            <Route path="/parcels/:trackingNumber" element={<ParcelDetailPage />} />
            <Route path="/fleet" element={<ProtectedRoute allowedRoles={["DISPATCHER", "ADMIN"]} />}>
              <Route index element={<DispatcherDashboardPage />} />
              <Route path="unassigned" element={<UnassignedParcelsPage />} />
              <Route path="couriers" element={<CouriersPage />} />
              <Route path="reports" element={<ReportsPage />} />
              <Route path="notifications" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
                <Route index element={<NotificationLogPage />} />
              </Route>
            </Route>
            <Route path="/people" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route index element={<AdminUsersPage />} />
            </Route>
            <Route path="/admin/users" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route index element={<AdminUsersPage />} />
            </Route>
            
            <Route path="/admin/pricing" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route index element={<AdminPricingPage />} />
            </Route>

          </Route>
        </Route>

        <Route path="*" element={<PublicRedirect />} />
      </Routes>
    </AuthProvider>
  );
}
