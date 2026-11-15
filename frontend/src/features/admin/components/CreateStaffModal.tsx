import React, { useEffect, useState } from "react";
import { AlertCircle, Check, Eye, EyeOff, Loader2, Radio, Truck, UserPlus, X } from "lucide-react";
import { getApiErrorMessage } from "../../../lib/api";
import { createStaffAccount } from "../admin.api";
import type { AdminUser, CreateStaffRequestDto } from "../admin.types";

interface CreateStaffModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (createdUser: AdminUser) => void;
}

// Sri Lankan mobile number format: 07XXXXXXXX or +947XXXXXXXX (or 7XXXXXXXX)
const SRI_LANKAN_PHONE_REGEX = /^(?:\+94|0)?7[0-9]{8}$/;

export const CreateStaffModal: React.FC<CreateStaffModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [role, setRole] = useState<"COURIER" | "DISPATCHER">("COURIER");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  const resetForm = () => {
    setFullName("");
    setEmail("");
    setPhoneNumber("");
    setRole("COURIER");
    setPassword("");
    setShowPassword(false);
    setServerError(null);
    setValidationErrors({});
  };

  const handleClose = () => {
    if (submitting) return;
    resetForm();
    onClose();
  };

  // Keyboard shortcut: Escape to dismiss
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !submitting) {
        handleClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, submitting]);

  if (!isOpen) return null;

  const validate = (): boolean => {
    const errors: Record<string, string> = {};

    if (!fullName.trim()) {
      errors.fullName = "Full name is required";
    }

    if (!email.trim()) {
      errors.email = "Email address is required";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errors.email = "Please enter a valid email address";
    }

    if (phoneNumber.trim()) {
      const sanitizedPhone = phoneNumber.trim().replace(/[\s-]/g, "");
      if (!SRI_LANKAN_PHONE_REGEX.test(sanitizedPhone)) {
        errors.phoneNumber = "Must be a valid Sri Lankan mobile number (e.g. 07XXXXXXXX or +947XXXXXXXX)";
      }
    }

    if (!password) {
      errors.password = "Password is required";
    } else if (password.length < 8) {
      errors.password = "Password must be at least 8 characters long";
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerError(null);

    if (!validate()) return;

    setSubmitting(true);
    try {
      const sanitizedPhone = phoneNumber.trim()
        ? phoneNumber.trim().replace(/[\s-]/g, "")
        : undefined;

      const payload: CreateStaffRequestDto = {
        fullName: fullName.trim(),
        email: email.trim().toLowerCase(),
        phoneNumber: sanitizedPhone,
        role,
        password,
      };

      const created = await createStaffAccount(payload);
      resetForm();
      onSuccess(created);
      onClose();
    } catch (err: unknown) {
      const msg = getApiErrorMessage(err, "Failed to create staff account.");
      setServerError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/60 backdrop-blur-md transition-all animate-in fade-in duration-200"
      onClick={(e) => {
        if (e.target === e.currentTarget && !submitting) {
          handleClose();
        }
      }}
    >
      <div
        className="w-full max-w-lg bg-white rounded-3xl shadow-2xl border border-ink/10 overflow-hidden transform transition-all duration-200 animate-in zoom-in-95"
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-staff-title"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-ink/10 bg-gradient-to-r from-mist/60 via-mist/30 to-white">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-2xl bg-coral/10 text-coral ring-1 ring-coral/20">
              <UserPlus size={18} strokeWidth={2.2} />
            </div>
            <div>
              <h3 id="create-staff-title" className="text-base font-bold text-ink">
                Create Staff Account
              </h3>
              <p className="text-xs text-ink/50 mt-0.5">
                Register a new courier or dispatcher to the network
              </p>
            </div>
          </div>
          <button
            onClick={handleClose}
            disabled={submitting}
            className="p-1.5 rounded-xl text-ink/40 hover:text-ink hover:bg-ink/5 transition-colors disabled:opacity-50"
            aria-label="Close modal"
          >
            <X size={18} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit}>
          {/* Form Body - Clean 2-column layout to fit comfortably without scrollbars */}
          <div className="p-6 space-y-3.5">
            {serverError && (
              <div className="flex items-start gap-2.5 p-3 rounded-2xl bg-rose-50 text-rose-800 text-xs border border-rose-200/80 animate-in fade-in duration-150">
                <AlertCircle size={15} className="shrink-0 mt-0.5 text-rose-600" />
                <span className="leading-relaxed">{serverError}</span>
              </div>
            )}

            {/* Full Name */}
            <div className="space-y-1">
              <label className="block text-[11px] font-bold uppercase tracking-wider text-ink/70">
                Full Name <span className="text-coral">*</span>
              </label>
              <input
                type="text"
                placeholder="e.g. Nuwan Pradeep"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                disabled={submitting}
                className={`w-full rounded-xl border px-3.5 py-2 text-sm text-ink outline-none transition placeholder:text-ink/30 focus:ring-2 focus:ring-coral/20 ${
                  validationErrors.fullName
                    ? "border-rose-400 focus:border-rose-500 bg-rose-50/20"
                    : "border-ink/15 focus:border-coral bg-white hover:border-ink/25"
                }`}
              />
              {validationErrors.fullName && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {validationErrors.fullName}
                </p>
              )}
            </div>

            {/* Email & Phone side-by-side on desktop/tablet for a compact, non-scrolling layout */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {/* Email Address */}
              <div className="space-y-1">
                <label className="block text-[11px] font-bold uppercase tracking-wider text-ink/70">
                  Email Address <span className="text-coral">*</span>
                </label>
                <input
                  type="email"
                  placeholder="e.g. nuwan@swiftdrop.lk"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={submitting}
                  className={`w-full rounded-xl border px-3.5 py-2 text-sm text-ink outline-none transition placeholder:text-ink/30 focus:ring-2 focus:ring-coral/20 ${
                    validationErrors.email
                      ? "border-rose-400 focus:border-rose-500 bg-rose-50/20"
                      : "border-ink/15 focus:border-coral bg-white hover:border-ink/25"
                  }`}
                />
                {validationErrors.email && (
                  <p className="text-[11px] text-rose-600 font-medium">
                    {validationErrors.email}
                  </p>
                )}
              </div>

              {/* Phone Number */}
              <div className="space-y-1">
                <div className="flex items-center justify-between">
                  <label className="block text-[11px] font-bold uppercase tracking-wider text-ink/70">
                    Phone Number
                  </label>
                  <span className="text-[10px] font-medium text-ink/40">07X..</span>
                </div>
                <input
                  type="text"
                  placeholder="07XXXXXXXX or +947XXXXXXXX"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  disabled={submitting}
                  className={`w-full rounded-xl border px-3.5 py-2 text-sm text-ink outline-none transition placeholder:text-ink/30 focus:ring-2 focus:ring-coral/20 ${
                    validationErrors.phoneNumber
                      ? "border-rose-400 focus:border-rose-500 bg-rose-50/20"
                      : "border-ink/15 focus:border-coral bg-white hover:border-ink/25"
                  }`}
                />
                {validationErrors.phoneNumber && (
                  <p className="text-[11px] text-rose-600 font-medium">
                    {validationErrors.phoneNumber}
                  </p>
                )}
              </div>
            </div>

            {/* Role Selection */}
            <div className="space-y-1.5">
              <label className="block text-[11px] font-bold uppercase tracking-wider text-ink/70">
                Staff Role <span className="text-coral">*</span>
              </label>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setRole("COURIER")}
                  disabled={submitting}
                  className={`relative flex items-center gap-2.5 p-3 rounded-2xl border text-left transition-all ${
                    role === "COURIER"
                      ? "border-amber-400 bg-amber-50/70 ring-2 ring-amber-400/30 text-amber-950 font-semibold shadow-xs"
                      : "border-ink/10 hover:border-ink/20 hover:bg-ink/[0.02] text-ink/70"
                  }`}
                >
                  <div
                    className={`p-2 rounded-xl transition-colors ${
                      role === "COURIER"
                        ? "bg-amber-500 text-white shadow-xs"
                        : "bg-ink/5 text-ink/50"
                    }`}
                  >
                    <Truck size={16} />
                  </div>
                  <div>
                    <div className="text-xs font-bold">Courier</div>
                    <div className="text-[10px] text-ink/50 leading-tight">
                      Deliveries & parcels
                    </div>
                  </div>
                  {role === "COURIER" && (
                    <div className="absolute top-2.5 right-2.5 w-4 h-4 rounded-full bg-amber-500 text-white flex items-center justify-center">
                      <Check size={10} strokeWidth={3} />
                    </div>
                  )}
                </button>

                <button
                  type="button"
                  onClick={() => setRole("DISPATCHER")}
                  disabled={submitting}
                  className={`relative flex items-center gap-2.5 p-3 rounded-2xl border text-left transition-all ${
                    role === "DISPATCHER"
                      ? "border-sky-400 bg-sky-50/70 ring-2 ring-sky-400/30 text-sky-950 font-semibold shadow-xs"
                      : "border-ink/10 hover:border-ink/20 hover:bg-ink/[0.02] text-ink/70"
                  }`}
                >
                  <div
                    className={`p-2 rounded-xl transition-colors ${
                      role === "DISPATCHER"
                        ? "bg-sky-500 text-white shadow-xs"
                        : "bg-ink/5 text-ink/50"
                    }`}
                  >
                    <Radio size={16} />
                  </div>
                  <div>
                    <div className="text-xs font-bold">Dispatcher</div>
                    <div className="text-[10px] text-ink/50 leading-tight">
                      Fleet & routes control
                    </div>
                  </div>
                  {role === "DISPATCHER" && (
                    <div className="absolute top-2.5 right-2.5 w-4 h-4 rounded-full bg-sky-500 text-white flex items-center justify-center">
                      <Check size={10} strokeWidth={3} />
                    </div>
                  )}
                </button>
              </div>
            </div>

            {/* Password */}
            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <label className="block text-[11px] font-bold uppercase tracking-wider text-ink/70">
                  Temporary Password <span className="text-coral">*</span>
                </label>
                <span className="text-[10px] font-medium text-ink/40">Min. 8 characters</span>
              </div>
              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  placeholder="At least 8 characters"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={submitting}
                  className={`w-full rounded-xl border pl-3.5 pr-10 py-2 text-sm text-ink outline-none transition placeholder:text-ink/30 focus:ring-2 focus:ring-coral/20 ${
                    validationErrors.password
                      ? "border-rose-400 focus:border-rose-500 bg-rose-50/20"
                      : "border-ink/15 focus:border-coral bg-white hover:border-ink/25"
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  tabIndex={-1}
                  className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-md text-ink/40 hover:text-ink transition-colors"
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </div>
              {validationErrors.password && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {validationErrors.password}
                </p>
              )}
            </div>
          </div>

          {/* Footer Actions */}
          <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-ink/10 bg-white">
            <button
              type="button"
              onClick={handleClose}
              disabled={submitting}
              className="px-4 py-2 rounded-xl border border-ink/15 text-xs font-semibold text-ink/70 hover:text-ink hover:bg-ink/5 transition-colors disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex items-center gap-2 px-5 py-2 rounded-xl bg-coral hover:bg-coral-hover text-white text-xs font-semibold shadow-md shadow-coral/20 active:scale-[0.98] transition-all disabled:opacity-50"
            >
              {submitting ? (
                <>
                  <Loader2 size={14} className="animate-spin" />
                  <span>Creating Account...</span>
                </>
              ) : (
                <>
                  <UserPlus size={14} strokeWidth={2.2} />
                  <span>Create Account</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
