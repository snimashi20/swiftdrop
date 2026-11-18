import React, { useEffect, useState, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import {
  AlertTriangle,
  ArrowLeft,
  Building2,
  Calendar,
  CheckCircle2,
  Clock,
  CreditCard,
  FileText,
  Mail,
  MapPin,
  Phone,
  Power,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Store,
  Truck,
  User,
  X,
  XCircle,
} from "lucide-react";
import {
  approveUser,
  fetchAdminUserDetails,
  rejectUser,
  toggleUserStatus,
  updateUserRole,
} from "../admin.api";
import type { AdminUserDetails } from "../admin.types";
import { RoleBadge } from "../components/RoleBadge";
import { StatusBadge } from "../components/StatusBadge";
import { api, getApiErrorMessage } from "../../../lib/api";
import { ROLES, type Role } from "../../auth/auth.types";

function ProtectedDocumentImage({ url, alt }: { url: string; alt: string }) {
  const [src, setSrc] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let objectUrl: string | null = null;
    const relativePath = url.replace(/^\/api\/v1/, "");
    api
      .get(relativePath, { responseType: "blob" })
      .then((res) => {
        objectUrl = URL.createObjectURL(res.data);
        setSrc(objectUrl);
      })
      .catch(() => setFailed(true));

    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [url]);

  if (failed) {
    return (
      <div className="flex h-44 items-center justify-center rounded-xl border border-rose-200 bg-rose-50 text-xs text-rose-700">
        Unable to load {alt}
      </div>
    );
  }
  if (!src) {
    return (
      <div className="flex h-44 items-center justify-center rounded-xl bg-mist/40 text-xs text-ink/50">
        Loading {alt}...
      </div>
    );
  }
  return (
    <a
      href={src}
      target="_blank"
      rel="noreferrer"
      className="block overflow-hidden rounded-xl border border-ink/10"
      title={`Open full-size ${alt}`}
    >
      <img
        src={src}
        alt={alt}
        className="h-44 w-full object-cover transition hover:scale-[1.02]"
      />
    </a>
  );
}

export const ApprovalReviewPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const userId = Number(id);

  const [applicant, setApplicant] = useState<AdminUserDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Inline action states (Zero Modals)
  const [isApproving, setIsApproving] = useState(false);
  const [isRejecting, setIsRejecting] = useState(false);
  const [isTogglingStatus, setIsTogglingStatus] = useState(false);
  const [isUpdatingRole, setIsUpdatingRole] = useState(false);

  // Inline confirmation visibility
  const [showApproveConfirm, setShowApproveConfirm] = useState(false);
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [showStatusConfirm, setShowStatusConfirm] = useState(false);
  const [selectedRole, setSelectedRole] = useState<Role | "">("");

  // Dismissible 30s feedback banner
  const [banner, setBanner] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  const loadUserDetails = useCallback(async () => {
    if (!userId || isNaN(userId)) {
      setError("Invalid user identifier in URL.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const data = await fetchAdminUserDetails(userId);
      setApplicant(data);
      setSelectedRole(data.role);
    } catch (err: unknown) {
      const msg = getApiErrorMessage(err, "Failed to load user application details.");
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    loadUserDetails();
  }, [loadUserDetails]);

  // Auto-dismiss banner after 30 seconds
  useEffect(() => {
    if (!banner) return;
    const timer = setTimeout(() => {
      setBanner(null);
    }, 30000);
    return () => clearTimeout(timer);
  }, [banner]);

  // Inline Approve Action
  const handleApprove = async () => {
    if (!applicant) return;
    setIsApproving(true);
    setBanner(null);

    try {
      const updated = await approveUser(applicant.id);
      setApplicant((prev) =>
        prev
          ? {
              ...prev,
              ...updated,
              courierProfile: prev.courierProfile
                ? { ...prev.courierProfile, active: true }
                : prev.courierProfile,
            }
          : null
      );
      setShowApproveConfirm(false);
      setBanner({
        type: "success",
        message: `Account for ${applicant.fullName || applicant.email} has been approved and activated successfully!`,
      });
    } catch (err: unknown) {
      setBanner({
        type: "error",
        message: getApiErrorMessage(err, "Failed to approve account. Please try again."),
      });
    } finally {
      setIsApproving(false);
    }
  };

  // Inline Reject Action
  const handleReject = async () => {
    if (!applicant) return;
    setIsRejecting(true);
    setBanner(null);

    try {
      const updated = await rejectUser(applicant.id, rejectReason.trim() || undefined);
      setApplicant((prev) =>
        prev
          ? {
              ...prev,
              ...updated,
              courierProfile: prev.courierProfile
                ? { ...prev.courierProfile, active: false }
                : prev.courierProfile,
            }
          : null
      );
      setShowRejectForm(false);
      setRejectReason("");
      setBanner({
        type: "success",
        message: `Account for ${applicant.fullName || applicant.email} has been rejected. Notification recorded.`,
      });
    } catch (err: unknown) {
      setBanner({
        type: "error",
        message: getApiErrorMessage(err, "Failed to reject account. Please try again."),
      });
    } finally {
      setIsRejecting(false);
    }
  };

  // Inline Status Toggle Action (Enable / Disable)
  const handleStatusToggle = async () => {
    if (!applicant) return;
    setIsTogglingStatus(true);
    setBanner(null);

    try {
      const targetEnabled = !applicant.enabled;
      const updated = await toggleUserStatus(applicant.id, targetEnabled);
      setApplicant((prev) => (prev ? { ...prev, ...updated } : null));
      setShowStatusConfirm(false);
      setBanner({
        type: "success",
        message: `Account for ${applicant.fullName} has been ${targetEnabled ? "activated" : "disabled"}.`,
      });
    } catch (err: unknown) {
      setBanner({
        type: "error",
        message: getApiErrorMessage(err, "Failed to update account status."),
      });
    } finally {
      setIsTogglingStatus(false);
    }
  };

  // Inline Role Change Action
  const handleRoleChange = async () => {
    if (!applicant || !selectedRole || selectedRole === applicant.role) return;
    setIsUpdatingRole(true);
    setBanner(null);

    try {
      const updated = await updateUserRole(applicant.id, selectedRole);
      setApplicant((prev) => (prev ? { ...prev, ...updated } : null));
      setBanner({
        type: "success",
        message: `Role for ${applicant.fullName} successfully updated to ${updated.role}.`,
      });
    } catch (err: unknown) {
      setBanner({
        type: "error",
        message: getApiErrorMessage(err, "Failed to update user role."),
      });
    } finally {
      setIsUpdatingRole(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-3">
        <RefreshCw className="h-8 w-8 animate-spin text-coral" />
        <p className="text-sm font-medium text-ink/60">Loading registration &amp; review details...</p>
      </div>
    );
  }

  if (error || !applicant) {
    return (
      <div className="font-sans text-ink">
        <div className="mb-6 flex items-center justify-between">
          <Link
            to="/admin/users"
            className="inline-flex items-center gap-2 rounded-xl border border-ink/10 bg-white px-4 py-2.5 text-xs font-semibold text-ink shadow-xs transition hover:bg-ink/5"
          >
            <ArrowLeft size={14} />
            <span>Back to Users</span>
          </Link>
        </div>
        <div className="rounded-3xl border border-rose-200 bg-rose-50 p-8 text-center">
          <AlertTriangle className="mx-auto h-12 w-12 text-rose-500 mb-3" />
          <h2 className="font-display text-xl font-bold text-rose-950 mb-1">
            Unable to Load Applicant Review
          </h2>
          <p className="text-sm text-rose-800 mb-6 max-w-md mx-auto">
            {error || "The requested user account could not be found or has been removed."}
          </p>
          <Link
            to="/admin/users"
            className="inline-flex items-center gap-2 rounded-xl bg-ink px-5 py-2.5 text-xs font-bold text-white transition hover:bg-coral"
          >
            Return to User Directory
          </Link>
        </div>
      </div>
    );
  }

  const isPending = applicant.status === "PENDING_APPROVAL" || (!applicant.enabled && applicant.status !== "REJECTED");
  const isRejected = applicant.status === "REJECTED";
  const isActive = applicant.status === "ACTIVE" && applicant.enabled;

  return (
    <div className="font-sans text-ink">
      {/* Breadcrumb Navigation */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="mb-1 flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-ink/40">
            <Link to="/admin/overview" className="hover:text-coral transition">
              Admin
            </Link>
            <span>/</span>
            <Link to="/admin/users" className="hover:text-coral transition">
              User Directory
            </Link>
            <span>/</span>
            <span className="text-ink/70">Review Applicant #{applicant.id}</span>
          </div>
          <h1 className="font-display text-2xl font-bold tracking-tight text-ink md:text-3xl">
            Application Details Review
          </h1>
          <p className="mt-1 text-sm text-ink/60">
            Inspect applicant credentials, submitted business or fleet documents, and make governance decisions.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={loadUserDetails}
            className="inline-flex items-center gap-1.5 rounded-xl border border-ink/10 bg-white px-3.5 py-2 text-xs font-semibold text-ink shadow-xs transition hover:bg-ink/5"
            title="Refresh applicant data"
          >
            <RefreshCw size={13} />
            <span>Refresh</span>
          </button>
          <Link
            to="/admin/users"
            className="inline-flex items-center gap-2 rounded-xl border border-ink/10 bg-white px-4 py-2 text-xs font-semibold text-ink shadow-xs transition hover:bg-ink/5"
          >
            <ArrowLeft size={14} />
            <span>Back to Users</span>
          </Link>
        </div>
      </div>

      {/* Dismissible Feedback Banner (Strictly Zero Modals, 30s auto-dismiss + X close) */}
      {banner && (
        <div
          role="alert"
          className={`mb-6 flex items-start justify-between gap-3 rounded-2xl border p-4 shadow-xs transition ${
            banner.type === "success"
              ? "border-emerald-200 bg-emerald-50 text-emerald-900"
              : "border-rose-200 bg-rose-50 text-rose-900"
          }`}
        >
          <div className="flex items-start gap-3">
            {banner.type === "success" ? (
              <CheckCircle2 size={18} className="mt-0.5 shrink-0 text-emerald-600" />
            ) : (
              <XCircle size={18} className="mt-0.5 shrink-0 text-rose-600" />
            )}
            <div className="text-sm font-medium leading-relaxed">{banner.message}</div>
          </div>
          <button
            type="button"
            onClick={() => setBanner(null)}
            className="rounded-lg p-1 text-ink/40 transition hover:bg-black/5 hover:text-ink"
            aria-label="Close notification"
          >
            <X size={16} />
          </button>
        </div>
      )}

      {/* Hero Summary Card */}
      <div className="mb-6 rounded-3xl border border-ink/10 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <div className="grid h-16 w-16 place-items-center rounded-2xl bg-coral/10 font-display text-2xl font-bold text-coral border border-coral/20 shrink-0">
              {(applicant.fullName || applicant.email)[0]?.toUpperCase()}
            </div>
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="font-display text-xl font-bold text-ink">
                  {applicant.fullName || "Unspecified Name"}
                </h2>
                <RoleBadge role={applicant.role} size="md" />
                <StatusBadge enabled={applicant.enabled} status={applicant.status} size="md" />
              </div>
              <div className="mt-1 flex flex-wrap items-center gap-3 text-xs text-ink/60">
                <span className="font-mono">{applicant.email}</span>
                <span>•</span>
                <span className="rounded bg-ink/5 px-2 py-0.5 font-semibold text-[10px] uppercase tracking-wider text-ink/70">
                  {applicant.provider} Auth
                </span>
                {applicant.createdAt && (
                  <>
                    <span>•</span>
                    <span className="flex items-center gap-1">
                      <Clock size={12} className="text-ink/40" />
                      Applied: {new Date(applicant.createdAt).toLocaleDateString()}
                    </span>
                  </>
                )}
              </div>
            </div>
          </div>

          {/* Quick Status Pill */}
          <div className="self-start sm:self-center">
            {isPending && (
              <span className="inline-flex items-center gap-1.5 rounded-xl border border-amber-300 bg-amber-50 px-3.5 py-1.5 text-xs font-bold text-amber-800">
                <Clock size={14} className="text-amber-600 animate-pulse" />
                Pending Verification Review
              </span>
            )}
            {isActive && (
              <span className="inline-flex items-center gap-1.5 rounded-xl border border-emerald-300 bg-emerald-50 px-3.5 py-1.5 text-xs font-bold text-emerald-800">
                <CheckCircle2 size={14} className="text-emerald-600" />
                Active Verified Account
              </span>
            )}
            {isRejected && (
              <span className="inline-flex items-center gap-1.5 rounded-xl border border-rose-300 bg-rose-50 px-3.5 py-1.5 text-xs font-bold text-rose-800">
                <XCircle size={14} className="text-rose-600" />
                Application Rejected
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Left 2 Cols: Details & Documents */}
        <div className="space-y-6 lg:col-span-2">
          {/* Section 1: Personal & Contact Identity */}
          <div className="rounded-3xl border border-ink/10 bg-white p-6 shadow-sm">
            <h3 className="mb-1 font-display text-base font-bold text-ink">
              1. Applicant Personal Identity
            </h3>
            <p className="text-xs text-ink/50 mb-4">
              Registered user account and communication credentials.
            </p>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                  <User size={13} />
                  <span>Full Legal Name</span>
                </div>
                <div className="text-sm font-semibold text-ink">
                  {applicant.fullName || "Not provided"}
                </div>
              </div>

              <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                  <Mail size={13} />
                  <span>Verified Email Address</span>
                </div>
                <div className="text-sm font-mono font-medium text-ink truncate">
                  {applicant.email}
                </div>
              </div>

              <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                  <Phone size={13} />
                  <span>Sri Lankan Phone Number</span>
                </div>
                <div className="text-sm font-mono font-semibold text-ink">
                  {applicant.phoneNumber || "Not registered"}
                </div>
              </div>

              <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                  <ShieldCheck size={13} />
                  <span>Authentication Provider</span>
                </div>
                <div className="text-sm font-semibold text-ink flex items-center gap-2">
                  <span>{applicant.provider}</span>
                  <span className="text-[10px] text-ink/40 font-mono">
                    (ID: #{applicant.id})
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Section 2: Partner Profile Details (Contextual Merchant / Courier) */}
          {applicant.role === "MERCHANT" && (
            <div className="rounded-3xl border border-ink/10 bg-white p-6 shadow-sm">
              <div className="flex items-center justify-between mb-1">
                <h3 className="font-display text-base font-bold text-ink flex items-center gap-2">
                  <Store size={18} className="text-coral" />
                  <span>2. Merchant Storefront &amp; Business Details</span>
                </h3>
                {applicant.merchantProfile?.status && (
                  <span className="rounded-full bg-coral/10 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-coral">
                    {applicant.merchantProfile.status}
                  </span>
                )}
              </div>
              <p className="text-xs text-ink/50 mb-5">
                Submitted commercial registration information for storefront verification.
              </p>

              {applicant.merchantProfile ? (
                <div className="space-y-4">
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                      <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                        <Building2 size={13} />
                        <span>Registered Business Name</span>
                      </div>
                      <div className="text-sm font-bold text-ink">
                        {applicant.merchantProfile.businessName}
                      </div>
                    </div>

                    <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                      <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                        <FileText size={13} />
                        <span>Business Reg. No (BR)</span>
                      </div>
                      <div className="text-sm font-mono font-bold text-coral">
                        {applicant.merchantProfile.registrationNumber}
                      </div>
                    </div>

                    <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                      <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                        <User size={13} />
                        <span>Authorized Contact Person</span>
                      </div>
                      <div className="text-sm font-semibold text-ink">
                        {applicant.merchantProfile.contactName}
                      </div>
                    </div>

                    <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                      <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                        <Phone size={13} />
                        <span>Business Contact Phone</span>
                      </div>
                      <div className="text-sm font-mono font-semibold text-ink">
                        {applicant.merchantProfile.contactPhone}
                      </div>
                    </div>
                  </div>

                  {applicant.merchantProfile.rejectionReason && (
                    <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4">
                      <div className="flex items-center gap-1.5 text-xs font-bold text-rose-800 mb-1">
                        <AlertTriangle size={14} />
                        <span>Previous Rejection Reason</span>
                      </div>
                      <p className="text-xs text-rose-700 leading-relaxed">
                        {applicant.merchantProfile.rejectionReason}
                      </p>
                    </div>
                  )}
                </div>
              ) : (
                <div className="rounded-2xl border border-dashed border-ink/15 bg-mist/10 p-5 text-center">
                  <Store size={24} className="mx-auto text-ink/30 mb-2" />
                  <p className="text-xs font-medium text-ink/60">
                    No detailed storefront record has been submitted yet for this merchant account.
                  </p>
                  <span className="text-[11px] text-ink/40">
                    The user has selected the Merchant role during registration.
                  </span>
                </div>
              )}
            </div>
          )}

          {applicant.role === "COURIER" && (
            <div className="rounded-3xl border border-ink/10 bg-white p-6 shadow-sm">
              <div className="flex items-center justify-between mb-1">
                <h3 className="font-display text-base font-bold text-ink flex items-center gap-2">
                  <Truck size={18} className="text-coral" />
                  <span>2. Courier Verification, Fleet &amp; NIC Documents</span>
                </h3>
                {applicant.courierProfile && (
                  <span
                    className={`rounded-full px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider ${
                      applicant.courierProfile.active
                        ? "bg-emerald-100 text-emerald-800"
                        : "bg-amber-100 text-amber-800"
                    }`}
                  >
                    {applicant.courierProfile.active ? "Fleet Active" : "Fleet Standby"}
                  </span>
                )}
              </div>
              <p className="text-xs text-ink/50 mb-5">
                Submitted identity documents, vehicle specification, driving license, and regional dispatch territory.
              </p>

              {applicant.courierProfile ? (
                <div className="space-y-5">
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                      <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                        <Truck size={13} />
                        <span>Transit Vehicle Type</span>
                      </div>
                      <div className="text-sm font-bold text-ink">
                        {applicant.courierProfile.vehicleType}
                        {applicant.courierProfile.vehiclePlateNumber && (
                          <span className="ml-2 font-mono text-xs text-coral">
                            ({applicant.courierProfile.vehiclePlateNumber})
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                      <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                        <MapPin size={13} />
                        <span>Operating Delivery Zone</span>
                      </div>
                      <div className="text-sm font-bold text-coral">
                        {applicant.courierProfile.zoneName || `Zone #${applicant.courierProfile.zoneId}`}
                      </div>
                    </div>

                    {applicant.courierProfile.nicNumber && (
                      <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                        <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                          <CreditCard size={13} />
                          <span>National Identity Card (NIC)</span>
                        </div>
                        <div className="text-sm font-mono font-bold text-ink">
                          {applicant.courierProfile.nicNumber}
                        </div>
                      </div>
                    )}

                    {applicant.courierProfile.dateOfBirth && (
                      <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                        <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                          <Calendar size={13} />
                          <span>Date of Birth</span>
                        </div>
                        <div className="text-sm font-semibold text-ink">
                          {applicant.courierProfile.dateOfBirth}
                        </div>
                      </div>
                    )}

                    {applicant.courierProfile.drivingLicenseNumber && (
                      <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                        <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                          <FileText size={13} />
                          <span>Driving License &amp; Expiry</span>
                        </div>
                        <div className="text-sm font-mono font-bold text-ink">
                          {applicant.courierProfile.drivingLicenseNumber}
                        </div>
                        {applicant.courierProfile.drivingLicenseExpiry && (
                          <div className="mt-0.5 text-[11px] text-ink/55">
                            Valid until: {applicant.courierProfile.drivingLicenseExpiry}
                          </div>
                        )}
                      </div>
                    )}

                    {(applicant.courierProfile.emergencyContactName ||
                      applicant.courierProfile.emergencyContactPhone) && (
                      <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5">
                        <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                          <Phone size={13} />
                          <span>Emergency Contact</span>
                        </div>
                        <div className="text-sm font-semibold text-ink">
                          {applicant.courierProfile.emergencyContactName || "—"}
                        </div>
                        {applicant.courierProfile.emergencyContactPhone && (
                          <div className="text-xs font-mono text-ink/60">
                            {applicant.courierProfile.emergencyContactPhone}
                          </div>
                        )}
                      </div>
                    )}

                    {applicant.courierProfile.residentialAddress && (
                      <div className="rounded-2xl border border-ink/5 bg-mist/20 p-3.5 sm:col-span-2">
                        <div className="flex items-center gap-1.5 text-xs font-semibold text-ink/50 mb-1">
                          <MapPin size={13} />
                          <span>Residential Address</span>
                        </div>
                        <div className="text-sm font-medium text-ink">
                          {applicant.courierProfile.residentialAddress}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* NIC Front & Back Photo Previews (JWT-authenticated) */}
                  {(applicant.courierProfile.nicFrontPhotoUrl ||
                    applicant.courierProfile.nicBackPhotoUrl) && (
                    <div className="border-t border-ink/10 pt-5">
                      <h4 className="mb-3 flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-ink/60">
                        <CreditCard size={14} className="text-coral" />
                        <span>Uploaded National Identity Card (NIC) Photos (Click to Enlarge)</span>
                      </h4>
                      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                        {applicant.courierProfile.nicFrontPhotoUrl && (
                          <div>
                            <p className="mb-1.5 text-xs font-semibold text-ink/60">
                              NIC Front Side
                            </p>
                            <ProtectedDocumentImage
                              url={applicant.courierProfile.nicFrontPhotoUrl}
                              alt="NIC Front Side"
                            />
                          </div>
                        )}
                        {applicant.courierProfile.nicBackPhotoUrl && (
                          <div>
                            <p className="mb-1.5 text-xs font-semibold text-ink/60">
                              NIC Back Side
                            </p>
                            <ProtectedDocumentImage
                              url={applicant.courierProfile.nicBackPhotoUrl}
                              alt="NIC Back Side"
                            />
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="rounded-2xl border border-dashed border-ink/15 bg-mist/10 p-5 text-center">
                  <Truck size={24} className="mx-auto text-ink/30 mb-2" />
                  <p className="text-xs font-medium text-ink/60">
                    No custom vehicle or zone record has been attached to this courier account yet.
                  </p>
                  <span className="text-[11px] text-ink/40">
                    Default fleet assignment will be configured upon approval.
                  </span>
                </div>
              )}
            </div>
          )}

          {applicant.role !== "MERCHANT" && applicant.role !== "COURIER" && (
            <div className="rounded-3xl border border-ink/10 bg-white p-6 shadow-sm">
              <h3 className="mb-1 font-display text-base font-bold text-ink">
                2. Operational Permissions &amp; Access Scope
              </h3>
              <p className="text-xs text-ink/50 mb-4">
                Assigned system boundaries for this internal profile.
              </p>
              <div className="rounded-2xl border border-ink/5 bg-mist/20 p-4">
                <div className="flex items-center gap-2 mb-2">
                  <ShieldCheck size={16} className="text-emerald-600" />
                  <span className="text-sm font-bold text-ink">{applicant.role} Privileges</span>
                </div>
                <p className="text-xs text-ink/65 leading-relaxed">
                  {applicant.role === "ADMIN"
                    ? "Full administrative control across user governance, partner verification, dynamic pricing, and dispatch."
                    : applicant.role === "DISPATCHER"
                    ? "Fleet management, unassigned parcel dispatching, courier routing, and notification tracking."
                    : "Standard customer booking, live tracking, and saved address book management."}
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Right 1 Col: Governance Actions Card (Zero Modals) */}
        <div className="space-y-6">
          <div className="rounded-3xl border border-ink/10 bg-white p-6 shadow-sm">
            <h3 className="mb-1 font-display text-base font-bold text-ink">
              3. Governance Decisions
            </h3>
            <p className="text-xs text-ink/50 mb-5">
              Execute account activation, rejection, or privilege mutations without modals.
            </p>

            {/* Pending Approval Decisions */}
            {isPending && (
              <div className="space-y-4">
                {/* Approve Flow */}
                {!showApproveConfirm && !showRejectForm && (
                  <button
                    type="button"
                    onClick={() => setShowApproveConfirm(true)}
                    className="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-3 text-sm font-bold text-white shadow-xs transition hover:bg-emerald-700"
                  >
                    <CheckCircle2 size={16} />
                    <span>Approve Account</span>
                  </button>
                )}

                {/* Inline Approve Confirmation Box */}
                {showApproveConfirm && (
                  <div className="rounded-2xl border border-emerald-200 bg-emerald-50/70 p-4 space-y-3">
                    <div className="flex items-start gap-2 text-xs font-semibold text-emerald-950">
                      <CheckCircle2 size={16} className="text-emerald-600 shrink-0 mt-0.5" />
                      <span>Confirm approval of {applicant.fullName}?</span>
                    </div>
                    <p className="text-[11px] text-emerald-800 leading-relaxed">
                      This will immediately activate the account, grant role permissions, and activate any linked storefront or courier fleet profile.
                    </p>
                    <div className="flex items-center gap-2 pt-1">
                      <button
                        type="button"
                        onClick={handleApprove}
                        disabled={isApproving}
                        className="flex-1 rounded-xl bg-emerald-600 py-2 text-xs font-bold text-white shadow-xs transition hover:bg-emerald-700 disabled:opacity-50"
                      >
                        {isApproving ? "Approving..." : "Yes, Approve"}
                      </button>
                      <button
                        type="button"
                        onClick={() => setShowApproveConfirm(false)}
                        disabled={isApproving}
                        className="rounded-xl border border-ink/10 bg-white px-3 py-2 text-xs font-semibold text-ink transition hover:bg-ink/5"
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                )}

                {/* Reject Flow */}
                {!showRejectForm && !showApproveConfirm && (
                  <button
                    type="button"
                    onClick={() => setShowRejectForm(true)}
                    className="w-full inline-flex items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-700 transition hover:bg-rose-100"
                  >
                    <XCircle size={16} />
                    <span>Reject Application</span>
                  </button>
                )}

                {/* Inline Reject Form Box */}
                {showRejectForm && (
                  <div className="rounded-2xl border border-rose-200 bg-rose-50/70 p-4 space-y-3">
                    <div className="flex items-start gap-2 text-xs font-semibold text-rose-950">
                      <XCircle size={16} className="text-rose-600 shrink-0 mt-0.5" />
                      <span>Reject Application for {applicant.fullName}</span>
                    </div>
                    <div>
                      <label className="block text-[11px] font-semibold text-rose-900 mb-1">
                        Rejection Reason / Verification Notes:
                      </label>
                      <textarea
                        rows={3}
                        value={rejectReason}
                        onChange={(e) => setRejectReason(e.target.value)}
                        placeholder="e.g. NIC document blurry or expired driving license."
                        className="w-full rounded-xl border border-rose-200 bg-white p-2.5 text-xs text-ink placeholder:text-ink/30 focus:border-rose-400 focus:outline-hidden focus:ring-2 focus:ring-rose-200"
                      />
                    </div>
                    <div className="flex items-center gap-2 pt-1">
                      <button
                        type="button"
                        onClick={handleReject}
                        disabled={isRejecting}
                        className="flex-1 rounded-xl bg-rose-600 py-2 text-xs font-bold text-white shadow-xs transition hover:bg-rose-700 disabled:opacity-50"
                      >
                        {isRejecting ? "Rejecting..." : "Confirm Rejection"}
                      </button>
                      <button
                        type="button"
                        onClick={() => setShowRejectForm(false)}
                        disabled={isRejecting}
                        className="rounded-xl border border-ink/10 bg-white px-3 py-2 text-xs font-semibold text-ink transition hover:bg-ink/5"
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Non-Pending Account Controls (Enable / Disable & Role Mutation) */}
            {!isPending && (
              <div className="space-y-4">
                {/* Account Status Toggle with Inline Confirmation */}
                <div>
                  <label className="block text-xs font-semibold text-ink mb-1.5">
                    Account Status
                  </label>
                  {!showStatusConfirm ? (
                    <button
                      type="button"
                      onClick={() => setShowStatusConfirm(true)}
                      className={`w-full inline-flex items-center justify-center gap-2 rounded-xl py-2.5 px-3 text-xs font-semibold transition ${
                        applicant.enabled
                          ? "border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100"
                          : "border border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
                      }`}
                    >
                      <Power size={14} />
                      <span>{applicant.enabled ? "Disable Account Access" : "Activate Account Access"}</span>
                    </button>
                  ) : (
                    <div className="rounded-2xl border border-ink/10 bg-mist/30 p-3 space-y-2">
                      <p className="text-xs text-ink/70">
                        Are you sure you want to {applicant.enabled ? "disable" : "activate"} this account?
                      </p>
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={handleStatusToggle}
                          disabled={isTogglingStatus}
                          className="flex-1 rounded-lg bg-ink py-1.5 text-xs font-bold text-white hover:bg-coral transition disabled:opacity-50"
                        >
                          {isTogglingStatus ? "Updating..." : "Yes, Confirm"}
                        </button>
                        <button
                          type="button"
                          onClick={() => setShowStatusConfirm(false)}
                          className="rounded-lg border border-ink/10 bg-white px-3 py-1.5 text-xs font-semibold text-ink"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  )}
                </div>

                {/* Change Role Section (Inline, Zero Modals) */}
                <div className="pt-2 border-t border-ink/10">
                  <label className="block text-xs font-semibold text-ink mb-1.5">
                    Update User Role
                  </label>
                  <div className="flex items-center gap-2">
                    <select
                      value={selectedRole}
                      onChange={(e) => setSelectedRole(e.target.value as Role)}
                      className="flex-1 rounded-xl border border-ink/15 bg-white px-3 py-2 text-xs font-semibold text-ink focus:border-coral focus:outline-hidden"
                    >
                      {ROLES.map((r) => (
                        <option key={r} value={r}>
                          {r}
                        </option>
                      ))}
                    </select>
                    <button
                      type="button"
                      onClick={handleRoleChange}
                      disabled={isUpdatingRole || selectedRole === applicant.role}
                      className="rounded-xl bg-ink px-3 py-2 text-xs font-bold text-white transition hover:bg-coral disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      {isUpdatingRole ? "Saving..." : "Save"}
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Audit Notice */}
            <div className="mt-5 rounded-2xl bg-mist/40 p-3 text-[11px] text-ink/50 flex items-start gap-2">
              <ShieldAlert size={14} className="shrink-0 mt-0.5 text-ink/40" />
              <span>
                All review actions, approvals, and rejections are permanently recorded in the administrator audit log.
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};