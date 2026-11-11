import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AuthProvider, useAuth } from "./features/auth/AuthContext";
import { LoginPage } from "./features/auth/LoginPage";
import { SignupPage } from "./features/auth/SignupPage";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { BookParcelPage } from "./features/parcels/BookParcelPage";
import { ParcelHistoryPage } from "./features/parcels/ParcelHistoryPage";
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
import { TodayDeliveriesPage } from "./features/courier";
import { MerchantApprovalsPage } from "./features/merchants/pages/MerchantApprovalsPage";
import { MerchantRegisterPage } from "./features/merchants/pages/MerchantRegisterPage";
import { MerchantApiKeysPage } from "./features/merchants/pages/MerchantApiKeysPage";
import { MerchantOrdersPage } from "./features/merchants/pages/MerchantOrdersPage";
import { RoleSelectionPage } from "./features/auth/RoleSelectionPage";
import { WaitingApprovalPage } from "./features/auth/WaitingApprovalPage";
import { MyNotificationsPage } from "./features/notifications/MyNotificationsPage";

function PublicRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.status === "PENDING_ROLE") return <Navigate to="/select-role" replace />;
  if (user.status === "PENDING_APPROVAL") return <Navigate to="/waiting-approval" replace />;
  return <Navigate to="/dashboard" replace />;
}

export function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/merchant/register" element={<MerchantRegisterPage />} />
        <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
        <Route path="/select-role" element={<RoleSelectionPage />} />
        <Route path="/waiting-approval" element={<WaitingApprovalPage />} />

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
            <Route path="/courier/today" element={<ProtectedRoute allowedRoles={["COURIER"]} />}>
              <Route index element={<TodayDeliveriesPage />} />
            </Route>
            <Route path="/notifications" element={<MyNotificationsPage />} />
            <Route path="/parcels" element={<ParcelHistoryPage />} />
            <Route path="/parcels/new" element={<BookParcelPage />} />
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
            <Route path="/admin/merchants" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route index element={<MerchantApprovalsPage />} />
            </Route>
            <Route path="/merchant/api-keys" element={<ProtectedRoute allowedRoles={["MERCHANT"]} />}>
              <Route index element={<MerchantApiKeysPage />} />
            </Route>
            <Route path="/merchant/orders" element={<ProtectedRoute allowedRoles={["MERCHANT"]} />}>
              <Route index element={<MerchantOrdersPage />} />
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
