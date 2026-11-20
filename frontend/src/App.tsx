import { Navigate, Outlet, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AuthProvider, useAuth } from "./features/auth/AuthContext";
import { LoginPage } from "./features/auth/LoginPage";
import { SignupPage } from "./features/auth/SignupPage";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { BookParcelPage } from "./features/parcels/BookParcelPage";
import { BookingDraftProvider } from "./features/parcels/booking/BookingDraft";
import { BookModePage } from "./features/parcels/booking/BookModePage";
import { ConfirmAddressPage } from "./features/parcels/booking/ConfirmAddressPage";
import { LocationPage } from "./features/parcels/booking/LocationPage";
import { SavedAddressesPage } from "./features/parcels/booking/SavedAddressesPage";
import { ParcelHistoryPage } from "./features/parcels/ParcelHistoryPage";
import { ParcelDetailPage, PublicTrackingLayout, TrackLookupPage, TrackingPage } from "./features/tracking";
import { OAuthCallbackPage } from "./features/auth/OAuthCallbackPage";
import { WebhookSettingsPage } from "./features/webhook/pages/WebhookSettingsPage";
import { UserProfilePage } from "./features/profile/UserProfilePage";
import { AddressBookPage } from "./features/profile/AddressBookPage";
import { CustomerProfilePage } from "./features/profile/CustomerProfilePage";
import { AdminProfilePage } from "./features/profile/AdminProfilePage";
import { AdminPricingPage } from './features/pricing/pages/AdminPricingPage';
import { ChatWidget } from './features/ai/ChatWidget';
import { PriceCalculator } from './features/pricing/components/PriceCalculator';
import { CourierRegisterPage } from './features/courier';
import {
  CouriersPage,
  DispatcherDashboardPage,
  NotificationLogPage,
  ReportsPage,
  UnassignedParcelsPage,
} from "./features/dispatch";
import {
  AdminOverviewPage,
  AdminUsersPage,
  ApprovalReviewPage,
  CreateStaffPage,
} from "./features/admin";
import { CourierApprovalsPage, TodayDeliveriesPage } from "./features/courier";
import { MerchantApprovalsPage } from "./features/merchants/pages/MerchantApprovalsPage";
import { MerchantRegisterPage } from "./features/merchants/pages/MerchantRegisterPage";
import { MerchantApiKeysPage } from "./features/merchants/pages/MerchantApiKeysPage";
import { MerchantOrdersPage } from "./features/merchants/pages/MerchantOrdersPage";
import { RoleSelectionPage } from "./features/auth/RoleSelectionPage";
import { WaitingApprovalPage } from "./features/auth/WaitingApprovalPage";
import { MyNotificationsPage } from "./features/notifications/MyNotificationsPage";
import { SettingsPage } from "./features/settings/SettingsPage";

function PublicRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.status === "PENDING_ROLE") return <Navigate to="/select-role" replace />;
  if (user.status === "PENDING_APPROVAL") return <Navigate to="/waiting-approval" replace />;
  if (user.role === "ADMIN") return <Navigate to="/admin/overview" replace />;
  return <Navigate to="/dashboard" replace />;
}

export function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/merchant/register" element={<MerchantRegisterPage />} />
        <Route path="/merchant/register" element={<MerchantRegisterPage />} />
        <Route path="/courier/register" element={<CourierRegisterPage />} />
        <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
        <Route path="/calculator" element={<PriceCalculator />} />
        <Route path="/select-role" element={<RoleSelectionPage />} />
        <Route path="/waiting-approval" element={<WaitingApprovalPage />} />

        <Route element={<PublicTrackingLayout />}>
          <Route path="/track" element={<TrackLookupPage />} />
          <Route path="/track/:trackingNumber" element={<TrackingPage />} />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/profile" element={<UserProfilePage />} />
            <Route path="/profile/addresses" element={<ProtectedRoute allowedRoles={["CUSTOMER"]} />}>
              <Route index element={<AddressBookPage />} />
            </Route>
            <Route path="/customer/profile" element={<ProtectedRoute allowedRoles={["CUSTOMER"]} />}>
              <Route index element={<CustomerProfilePage />} />
            </Route>
            <Route path="/admin/profile" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route index element={<AdminProfilePage />} />
            </Route>
            <Route path="/webhooks" element={<WebhookSettingsPage />} />
            <Route path="/courier/today" element={<ProtectedRoute allowedRoles={["COURIER"]} />}>
              <Route index element={<TodayDeliveriesPage />} />
            </Route>
            <Route path="/notifications" element={<MyNotificationsPage />} />
             <Route path="/parcels" element={<ProtectedRoute allowedRoles={["CUSTOMER"]} />}>
              <Route index element={<ParcelHistoryPage />} />
            </Route>
            <Route path="/parcels/start" element={<ProtectedRoute allowedRoles={["CUSTOMER"]} />}>
              <Route index element={<BookModePage />} />
            </Route>
            <Route path="/parcels/new" element={<ProtectedRoute allowedRoles={["CUSTOMER"]} />}>
              <Route element={<BookingDraftProvider><Outlet /></BookingDraftProvider>}>
                <Route index element={<BookParcelPage />} />
                <Route path="location" element={<LocationPage />} />
                <Route path="confirm" element={<ConfirmAddressPage />} />
                <Route path="saved" element={<SavedAddressesPage />} />
              </Route>
            </Route>
            <Route path="/parcels/:trackingNumber" element={<ProtectedRoute allowedRoles={["CUSTOMER"]} />}>
              <Route index element={<ParcelDetailPage />} />
            </Route>
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
            <Route path="/admin/couriers" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route index element={<CourierApprovalsPage />} />
            </Route>
            <Route path="/merchant/api-keys" element={<ProtectedRoute allowedRoles={["MERCHANT"]} />}>
              <Route index element={<MerchantApiKeysPage />} />
            </Route>
            <Route path="/merchant/orders" element={<ProtectedRoute allowedRoles={["MERCHANT"]} />}>
              <Route index element={<MerchantOrdersPage />} />
            </Route>
            <Route path="/admin/overview" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route index element={<AdminOverviewPage />} />
            </Route>
            <Route path="/people" element={<Navigate to="/admin/users" replace />} />
            <Route path="/admin/users" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route index element={<AdminUsersPage />} />
            </Route>
            <Route path="/admin/users/new" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route index element={<CreateStaffPage />} />
            </Route>
            <Route path="/admin/users/:id/review" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route index element={<ApprovalReviewPage />} />
            </Route>
            <Route path="/admin/pricing" element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route index element={<AdminPricingPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<PublicRedirect />} />
      </Routes>
      <ChatWidget />
    </AuthProvider>
  );
}
