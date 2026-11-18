import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  Check,
  CheckCircle2,
  Lock,
  Mail,
  Phone,
  Radio,
  Shield,
  Truck,
  User,
  UserCheck,
  UserPlus,
  Users,
  X,
  XCircle,
} from "lucide-react";
import { createStaffAccount } from "../admin.api";
import type { CreateStaffRequestDto } from "../admin.types";
import { getApiErrorMessage } from "../../../lib/api";

const SRI_LANKAN_PHONE_REGEX = /^(?:0|\+94)7[0-9]{8}$/;

export const CreateStaffPage: React.FC = () => {
  const navigate = useNavigate();

  const [role, setRole] = useState<"COURIER" | "DISPATCHER">("COURIER");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [banner, setBanner] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // Auto-dismiss banner after 30 seconds
  useEffect(() => {
    if (!banner) return;
    const timer = setTimeout(() => {
      setBanner(null);
    }, 30000);
    return () => clearTimeout(timer);
  }, [banner]);

  const isPhoneValid = !phoneNumber.trim() || SRI_LANKAN_PHONE_REGEX.test(phoneNumber.trim());
  const isPasswordValid = password.length >= 6;
  const doPasswordsMatch = password === confirmPassword;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBanner(null);

    if (!fullName.trim() || fullName.trim().length < 2) {
      setBanner({
        type: "error",
        message: "Full name must be at least 2 characters long.",
      });
      return;
    }

    if (!email.trim() || !email.includes("@")) {
      setBanner({
        type: "error",
        message: "Please enter a valid official email address.",
      });
      return;
    }

    if (phoneNumber.trim() && !isPhoneValid) {
      setBanner({
        type: "error",
        message: "Invalid Sri Lankan phone number. Format: 07XXXXXXXX or +947XXXXXXXX (e.g. 0771234567).",
      });
      return;
    }

    if (!isPasswordValid) {
      setBanner({
        type: "error",
        message: "Temporary password must be at least 6 characters.",
      });
      return;
    }

    if (!doPasswordsMatch) {
      setBanner({
        type: "error",
        message: "Password confirmation does not match the entered password.",
      });
      return;
    }

    setIsSubmitting(true);

    try {
      const payload: CreateStaffRequestDto = {
        fullName: fullName.trim(),
        email: email.trim().toLowerCase(),
        phoneNumber: phoneNumber.trim() || undefined,
        role,
        password,
      };

      const created = await createStaffAccount(payload);
      setBanner({
        type: "success",
        message: `Staff account for ${created.fullName} (${created.role}) has been created successfully! Redirecting to user directory...`,
      });

      // Brief delay so admin can read the inline success confirmation
      setTimeout(() => {
        navigate("/admin/users", {
          state: { message: `Staff member ${created.fullName} registered successfully.` },
        });
      }, 1500);
    } catch (err: unknown) {
      const msg = getApiErrorMessage(
        err,
        "Failed to provision staff account. A user with this email may already exist."
      );
      setBanner({ type: "error", message: msg });
    } finally {
      setIsSubmitting(false);
    }
  };

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
            <span className="text-ink/70">Create Staff</span>
          </div>
          <h1 className="font-display text-2xl font-bold tracking-tight text-ink md:text-3xl">
            Provision Staff Member
          </h1>
          <p className="mt-1 text-sm text-ink/60">
            Create an active internal staff account for logistics operations with zero customer signup gates.
          </p>
        </div>

        <Link
          to="/admin/users"
          className="inline-flex items-center gap-2 rounded-xl border border-ink/10 bg-white px-4 py-2.5 text-xs font-semibold text-ink shadow-xs transition hover:bg-ink/5"
        >
          <ArrowLeft size={14} />
          <span>Back to Users</span>
        </Link>
      </div>

      {/* Inline Feedback Banner (Strictly Zero Modals, 30s auto-dismiss + X close) */}
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

      {/* Main Provisioning Form Card */}
      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Role Selection Cards */}
        <div className="rounded-3xl border border-ink/10 bg-white p-6 shadow-sm">
          <label className="block font-display text-base font-bold text-ink mb-1">
            1. Select Operational Staff Role
          </label>
          <p className="text-xs text-ink/50 mb-4">
            Select the internal responsibility profile assigned to this staff account.
          </p>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {/* Courier Card */}
            <div
              onClick={() => setRole("COURIER")}
              className={`cursor-pointer rounded-2xl border-2 p-5 transition ${
                role === "COURIER"
                  ? "border-coral bg-coral/5 shadow-xs"
                  : "border-ink/10 bg-mist/20 hover:border-ink/20"
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div
                    className={`grid h-10 w-10 place-items-center rounded-xl font-bold ${
                      role === "COURIER"
                        ? "bg-coral text-white"
                        : "bg-ink/5 text-ink/60"
                    }`}
                  >
                    <Truck size={20} />
                  </div>
                  <div>
                    <h3 className="font-display font-bold text-ink">Field Courier</h3>
                    <span className="font-mono text-[10px] font-semibold uppercase tracking-wider text-coral">
                      ROLE_COURIER
                    </span>
                  </div>
                </div>
                <div
                  className={`grid h-5 w-5 place-items-center rounded-full border ${
                    role === "COURIER"
                      ? "border-coral bg-coral text-white"
                      : "border-ink/20 bg-white"
                  }`}
                >
                  {role === "COURIER" && <Check size={12} strokeWidth={3} />}
                </div>
              </div>
              <p className="mt-3 text-xs text-ink/65 leading-relaxed">
                Field deliverer handling last-mile transit, live mobile parcel pickup/dropoff confirmations, and real-time status updates.
              </p>
            </div>

            {/* Dispatcher Card */}
            <div
              onClick={() => setRole("DISPATCHER")}
              className={`cursor-pointer rounded-2xl border-2 p-5 transition ${
                role === "DISPATCHER"
                  ? "border-coral bg-coral/5 shadow-xs"
                  : "border-ink/10 bg-mist/20 hover:border-ink/20"
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div
                    className={`grid h-10 w-10 place-items-center rounded-xl font-bold ${
                      role === "DISPATCHER"
                        ? "bg-coral text-white"
                        : "bg-ink/5 text-ink/60"
                    }`}
                  >
                    <Radio size={20} />
                  </div>
                  <div>
                    <h3 className="font-display font-bold text-ink">Fleet Dispatcher</h3>
                    <span className="font-mono text-[10px] font-semibold uppercase tracking-wider text-coral">
                      ROLE_DISPATCHER
                    </span>
                  </div>
                </div>
                <div
                  className={`grid h-5 w-5 place-items-center rounded-full border ${
                    role === "DISPATCHER"
                      ? "border-coral bg-coral text-white"
                      : "border-ink/20 bg-white"
                  }`}
                >
                  {role === "DISPATCHER" && <Check size={12} strokeWidth={3} />}
                </div>
              </div>
              <p className="mt-3 text-xs text-ink/65 leading-relaxed">
                Logistics operator managing fleet routes, unassigned delivery queues, regional delivery zones, and customer dispatch notifications.
              </p>
            </div>
          </div>
        </div>

        {/* Identity & Contact Details Card */}
        <div className="rounded-3xl border border-ink/10 bg-white p-6 shadow-sm">
          <label className="block font-display text-base font-bold text-ink mb-1">
            2. Personal & Contact Identity
          </label>
          <p className="text-xs text-ink/50 mb-5">
            Internal identification and Sri Lankan contact details for dispatch communications.
          </p>

          <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
            {/* Full Name */}
            <div>
              <label className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold text-ink">
                <User size={14} className="text-ink/60" />
                <span>Full Name *</span>
              </label>
              <input
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="e.g. Kasun Fernando"
                className="w-full rounded-xl border border-ink/15 bg-white px-3.5 py-2.5 text-sm text-ink placeholder:text-ink/30 transition focus:border-coral focus:outline-hidden focus:ring-2 focus:ring-coral/20"
              />
              <span className="mt-1 block text-[11px] text-ink/40">
                Official name as shown on Sri Lankan NIC / driving license.
              </span>
            </div>

            {/* Email Address */}
            <div>
              <label className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold text-ink">
                <Mail size={14} className="text-ink/60" />
                <span>Official Work Email *</span>
              </label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="staff.name@swiftdrop.lk"
                className="w-full rounded-xl border border-ink/15 bg-white px-3.5 py-2.5 text-sm text-ink placeholder:text-ink/30 transition focus:border-coral focus:outline-hidden focus:ring-2 focus:ring-coral/20"
              />
              <span className="mt-1 block text-[11px] text-ink/40">
                Used as the unique login credential. Must be unique in the system.
              </span>
            </div>

            {/* Sri Lankan Phone Number */}
            <div className="md:col-span-2">
              <label className="mb-1.5 flex items-center justify-between text-xs font-semibold text-ink">
                <span className="flex items-center gap-1.5">
                  <Phone size={14} className="text-ink/60" />
                  <span>Sri Lankan Phone Number</span>
                </span>
                {phoneNumber && (
                  <span
                    className={`font-mono text-[11px] font-medium ${
                      isPhoneValid ? "text-emerald-600" : "text-rose-600"
                    }`}
                  >
                    {isPhoneValid ? "✓ Valid Sri Lankan format" : "✕ Must match 07XXXXXXXX or +947XXXXXXXX"}
                  </span>
                )}
              </label>
              <input
                type="tel"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="e.g. 0771234567 or +94771234567"
                className={`w-full rounded-xl border px-3.5 py-2.5 text-sm font-mono text-ink placeholder:text-ink/30 transition focus:outline-hidden focus:ring-2 ${
                  phoneNumber && !isPhoneValid
                    ? "border-rose-400 bg-rose-50/20 focus:border-rose-500 focus:ring-rose-200"
                    : "border-ink/15 bg-white focus:border-coral focus:ring-coral/20"
                }`}
              />
              <p className="mt-1 text-[11px] text-ink/40">
                Valid prefixes: Dialog (077/076/074), Mobitel (071/070), Airtel (075), Hutch (078/072).
              </p>
            </div>
          </div>
        </div>

        {/* Credentials Card */}
        <div className="rounded-3xl border border-ink/10 bg-white p-6 shadow-sm">
          <label className="block font-display text-base font-bold text-ink mb-1">
            3. Account Credentials
          </label>
          <p className="text-xs text-ink/50 mb-5">
            Initial temporary password for first sign-in. The staff member will be prompted to change it.
          </p>

          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
            {/* Password */}
            <div>
              <label className="mb-1.5 flex items-center justify-between text-xs font-semibold text-ink">
                <span className="flex items-center gap-1.5">
                  <Lock size={14} className="text-ink/60" />
                  <span>Temporary Password *</span>
                </span>
                {password && (
                  <span
                    className={`text-[11px] font-medium ${
                      isPasswordValid ? "text-emerald-600" : "text-rose-600"
                    }`}
                  >
                    {isPasswordValid ? "✓ Min 6 chars" : "✕ Too short"}
                  </span>
                )}
              </label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="At least 6 characters"
                className={`w-full rounded-xl border px-3.5 py-2.5 text-sm text-ink placeholder:text-ink/30 transition focus:outline-hidden focus:ring-2 ${
                  password && !isPasswordValid
                    ? "border-rose-400 bg-rose-50/20 focus:border-rose-500 focus:ring-rose-200"
                    : "border-ink/15 bg-white focus:border-coral focus:ring-coral/20"
                }`}
              />
            </div>

            {/* Confirm Password */}
            <div>
              <label className="mb-1.5 flex items-center justify-between text-xs font-semibold text-ink">
                <span className="flex items-center gap-1.5">
                  <Lock size={14} className="text-ink/60" />
                  <span>Confirm Password *</span>
                </span>
                {confirmPassword && (
                  <span
                    className={`text-[11px] font-medium ${
                      doPasswordsMatch ? "text-emerald-600" : "text-rose-600"
                    }`}
                  >
                    {doPasswordsMatch ? "✓ Matches" : "✕ Does not match"}
                  </span>
                )}
              </label>
              <input
                type="password"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Re-enter temporary password"
                className={`w-full rounded-xl border px-3.5 py-2.5 text-sm text-ink placeholder:text-ink/30 transition focus:outline-hidden focus:ring-2 ${
                  confirmPassword && !doPasswordsMatch
                    ? "border-rose-400 bg-rose-50/20 focus:border-rose-500 focus:ring-rose-200"
                    : "border-ink/15 bg-white focus:border-coral focus:ring-coral/20"
                }`}
              />
            </div>
          </div>
        </div>

        {/* Actions Bar */}
        <div className="flex flex-wrap items-center justify-end gap-3 pt-2">
          <Link
            to="/admin/users"
            className="rounded-xl border border-ink/10 bg-white px-5 py-2.5 text-sm font-semibold text-ink/70 transition hover:bg-ink/5"
          >
            Cancel
          </Link>
          <button
            type="submit"
            disabled={isSubmitting || !fullName || !email || !isPasswordValid || !doPasswordsMatch || (!!phoneNumber && !isPhoneValid)}
            className="inline-flex items-center gap-2 rounded-xl bg-ink px-6 py-2.5 text-sm font-bold text-white shadow-sm transition hover:bg-coral disabled:cursor-not-allowed disabled:opacity-50"
          >
            <UserPlus size={16} />
            <span>{isSubmitting ? "Creating Staff Account..." : "Create Staff Account"}</span>
          </button>
        </div>
      </form>
    </div>
  );
};

