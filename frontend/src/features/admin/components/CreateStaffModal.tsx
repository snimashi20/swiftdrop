import React, { useState } from "react";
import { AlertCircle, Eye, EyeOff, Radio, Truck, UserPlus, X } from "lucide-react";
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

  if (!isOpen) return null;

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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/40 backdrop-blur-xs animate-in fade-in duration-200">
      <div
        className="w-full max-w-lg bg-white rounded-2xl shadow-xl border border-ink/10 overflow-hidden transform transition-all"
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-staff-title"
      >
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-ink/10 bg-mist/50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-coral/10 text-coral">
              <UserPlus size={18} />
            </div>
            <div>
              <h3 id="create-staff-title" className="text-base font-bold text-ink">
                Create Staff Account
              </h3>
              <p className="text-xs text-ink/50">
                Register a new courier or dispatcher to the network
              </p>
            </div>
          </div>
          <button
            onClick={handleClose}
            disabled={submitting}
            className="p-1.5 rounded-lg text-ink/40 hover:text-ink hover:bg-ink/5 transition"
            aria-label="Close modal"
          >
            <X size={18} />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {serverError && (
            <div className="flex items-start gap-2.5 p-3 rounded-xl bg-rose-50 text-rose-800 text-xs border border-rose-200">
              <AlertCircle size={16} className="shrink-0 mt-0.5 text-rose-600" />
              <span>{serverError}</span>
            </div>
          )}

          {/* Full Name */}
          <div className="space-y-1">
            <label className="block text-xs font-bold uppercase tracking-wider text-ink/70">
              Full Name <span className="text-coral">*</span>
            </label>
            <input
              type="text"
              placeholder="e.g. Nuwan Pradeep"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              disabled={submitting}
              className={`w-full rounded-xl border px-3.5 py-2.5 text-sm text-ink outline-none transition placeholder:text-ink/30 focus:ring-2 focus:ring-coral/20 ${
                validationErrors.fullName
                  ? "border-rose-400 focus:border-rose-500"
                  : "border-ink/15 focus:border-coral"
              }`}
            />
            {validationErrors.fullName && (
              <p className="text-[11px] text-rose-600 font-medium">
                {validationErrors.fullName}
              </p>
            )}
          </div>

          {/* Email Address */}
          <div className="space-y-1">
            <label className="block text-xs font-bold uppercase tracking-wider text-ink/70">
              Email Address <span className="text-coral">*</span>
            </label>
            <input
              type="email"
              placeholder="e.g. nuwan.courier@swiftdrop.lk"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={submitting}
              className={`w-full rounded-xl border px-3.5 py-2.5 text-sm text-ink outline-none transition placeholder:text-ink/30 focus:ring-2 focus:ring-coral/20 ${
                validationErrors.email
                  ? "border-rose-400 focus:border-rose-500"
                  : "border-ink/15 focus:border-coral"
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
              <label className="block text-xs font-bold uppercase tracking-wider text-ink/70">
                Phone Number
              </label>
              <span className="text-[10px] text-ink/40">Sri Lankan mobile</span>
            </div>
            <input
              type="text"
              placeholder="07XXXXXXXX or +947XXXXXXXX"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              disabled={submitting}
              className={`w-full rounded-xl border px-3.5 py-2.5 text-sm text-ink outline-none transition placeholder:text-ink/30 focus:ring-2 focus:ring-coral/20 ${
                validationErrors.phoneNumber
                  ? "border-rose-400 focus:border-rose-500"
                  : "border-ink/15 focus:border-coral"
              }`}
            />
            {validationErrors.phoneNumber && (
              <p className="text-[11px] text-rose-600 font-medium">
                {validationErrors.phoneNumber}
              </p>
            )}
          </div>

          {/* Role Selection */}
          <div className="space-y-1.5">
            <label className="block text-xs font-bold uppercase tracking-wider text-ink/70">
              Staff Role <span className="text-coral">*</span>
            </label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setRole("COURIER")}
                disabled={submitting}
                className={`flex items-center gap-2.5 p-3 rounded-xl border text-left transition ${
                  role === "COURIER"
                    ? "border-amber-400 bg-amber-50/60 ring-1 ring-amber-400/40 text-amber-950 font-semibold"
                    : "border-ink/10 hover:bg-ink/5 text-ink/70"
                }`}
              >
                <div
                  className={`p-1.5 rounded-lg ${
                    role === "COURIER"
                      ? "bg-amber-100 text-amber-800"
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
              </button>

              <button
                type="button"
                onClick={() => setRole("DISPATCHER")}
                disabled={submitting}
                className={`flex items-center gap-2.5 p-3 rounded-xl border text-left transition ${
                  role === "DISPATCHER"
                    ? "border-sky-400 bg-sky-50/60 ring-1 ring-sky-400/40 text-sky-950 font-semibold"
                    : "border-ink/10 hover:bg-ink/5 text-ink/70"
                }`}
              >
                <div
                  className={`p-1.5 rounded-lg ${
                    role === "DISPATCHER"
                      ? "bg-sky-100 text-sky-800"
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
              </button>
            </div>
          </div>

          {/* Password */}
          <div className="space-y-1">
            <label className="block text-xs font-bold uppercase tracking-wider text-ink/70">
              Temporary Password <span className="text-coral">*</span>
            </label>
            <div className="relative">
              <input
                type={showPassword ? "text" : "password"}
                placeholder="At least 8 characters"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={submitting}
                className={`w-full rounded-xl border pl-3.5 pr-10 py-2.5 text-sm text-ink outline-none transition placeholder:text-ink/30 focus:ring-2 focus:ring-coral/20 ${
                  validationErrors.password
                    ? "border-rose-400 focus:border-rose-500"
                    : "border-ink/15 focus:border-coral"
                }`}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                tabIndex={-1}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-ink/40 hover:text-ink transition"
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {validationErrors.password && (
              <p className="text-[11px] text-rose-600 font-medium">
                {validationErrors.password}
              </p>
            )}
          </div>

          {/* Footer Actions */}
          <div className="flex items-center justify-end gap-2.5 pt-4 border-t border-ink/10">
            <button
              type="button"
              onClick={handleClose}
              disabled={submitting}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-ink/70 hover:text-ink hover:bg-ink/5 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-ink text-white text-xs font-semibold hover:bg-ink/90 transition shadow-sm disabled:opacity-50"
            >
              <UserPlus size={14} />
              <span>{submitting ? "Creating..." : "Create Account"}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
