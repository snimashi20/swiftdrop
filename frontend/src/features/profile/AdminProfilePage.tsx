import React, { useEffect, useState, useCallback, useRef } from "react";
import { Link } from "react-router-dom";
import {
  User,
  Mail,
  Phone,
  ShieldCheck,
  ShieldAlert,
  Users,
  Store,
  Truck,
  DollarSign,
  CheckCircle2,
  XCircle,
  Check,
  X,
  RefreshCw,
  Camera,
  Upload,
  Trash2,
  ArrowUpRight,
  Lock,
  ChevronRight,
  Radio,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import {
  fetchUserProfile,
  updateUserProfile,
  type UserProfile,
} from "./profile.api";

// Sri Lankan Mobile Regex: 07XXXXXXXX or +947XXXXXXXX (10 or 12 characters)
const SRI_LANKAN_PHONE_REGEX = /^(?:0|\+94)7[0-9]{8}$/;

export const AdminProfilePage: React.FC = () => {
  const { user: authUser } = useAuth();

  // Profile Data State
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [fullName, setFullName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [isProfileLoading, setIsProfileLoading] = useState(true);
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [profileBanner, setProfileBanner] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // Avatar / Profile Picture State & Handlers
  const [avatar, setAvatar] = useState<string | null>(null);
  const [isAvatarMenuOpen, setIsAvatarMenuOpen] = useState(false);
  const [isLiveCameraActive, setIsLiveCameraActive] = useState(false);
  const [cameraStream, setCameraStream] = useState<MediaStream | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);
  const avatarMenuRef = useRef<HTMLDivElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);

  // Read stored avatar
  useEffect(() => {
    const email = profile?.email || authUser?.email;
    if (email) {
      const stored = localStorage.getItem(`swiftdrop_avatar_${email}`);
      setAvatar(stored);
    }
  }, [profile?.email, authUser?.email]);

  // Close avatar options popover on click outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (avatarMenuRef.current && !avatarMenuRef.current.contains(e.target as Node)) {
        setIsAvatarMenuOpen(false);
      }
    };
    if (isAvatarMenuOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isAvatarMenuOpen]);

  // Connect live camera stream to video element
  useEffect(() => {
    if (isLiveCameraActive && cameraStream && videoRef.current) {
      videoRef.current.srcObject = cameraStream;
    }
  }, [isLiveCameraActive, cameraStream]);

  // Auto-dismiss banner after 30 seconds
  useEffect(() => {
    if (!profileBanner) return;
    const timer = setTimeout(() => {
      setProfileBanner(null);
    }, 30000); // 30 seconds
    return () => clearTimeout(timer);
  }, [profileBanner]);

  // Load Profile from API
  const loadProfile = useCallback(async () => {
    setIsProfileLoading(true);
    try {
      const data = await fetchUserProfile();
      setProfile(data);
      setFullName(data.fullName || "");
      setPhoneNumber(data.phoneNumber || "");
    } catch {
      setProfileBanner({
        type: "error",
        message: "Failed to load administrator profile. Please check your connection.",
      });
    } finally {
      setIsProfileLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  // Handle Profile Update
  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileBanner(null);

    const trimmedName = fullName.trim();
    if (!trimmedName) {
      setProfileBanner({ type: "error", message: "Full name cannot be blank." });
      return;
    }

    const trimmedPhone = phoneNumber.trim();
    if (trimmedPhone && !SRI_LANKAN_PHONE_REGEX.test(trimmedPhone)) {
      setProfileBanner({
        type: "error",
        message:
          "Invalid phone format. Please enter a valid Sri Lankan mobile number (e.g., 07XXXXXXXX or +947XXXXXXXX).",
      });
      return;
    }

    setIsSavingProfile(true);
    try {
      const updated = await updateUserProfile({
        fullName: trimmedName,
        phoneNumber: trimmedPhone || undefined,
      });
      setProfile(updated);
      setProfileBanner({
        type: "success",
        message: "Administrator profile details updated successfully.",
      });
    } catch {
      setProfileBanner({
        type: "error",
        message: "Failed to save profile changes. Please verify phone number format and try again.",
      });
    } finally {
      setIsSavingProfile(false);
    }
  };

  // Camera & Photo Handlers
  const handleStartCamera = async () => {
    setIsAvatarMenuOpen(false);
    try {
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { width: { ideal: 640 }, height: { ideal: 640 }, facingMode: "user" },
        });
        setCameraStream(stream);
        setIsLiveCameraActive(true);
      } else {
        cameraInputRef.current?.click();
      }
    } catch {
      cameraInputRef.current?.click();
    }
  };

  const handleCapturePhoto = () => {
    if (!videoRef.current) return;
    const video = videoRef.current;
    const canvas = document.createElement("canvas");
    canvas.width = video.videoWidth || 400;
    canvas.height = video.videoHeight || 400;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    const dataUrl = canvas.toDataURL("image/jpeg", 0.9);

    setAvatar(dataUrl);
    const email = profile?.email || authUser?.email;
    if (email) {
      localStorage.setItem(`swiftdrop_avatar_${email}`, dataUrl);
      window.dispatchEvent(new Event("avatarUpdated"));
    }
    handleStopCamera();
    setProfileBanner({
      type: "success",
      message: "Profile photo captured and updated successfully!",
    });
  };

  const handleStopCamera = () => {
    if (cameraStream) {
      cameraStream.getTracks().forEach((track) => track.stop());
      setCameraStream(null);
    }
    setIsLiveCameraActive(false);
  };

  const handleAvatarUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    setIsAvatarMenuOpen(false);
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setProfileBanner({
        type: "error",
        message: "Please select an image file (PNG, JPG, or WebP).",
      });
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setProfileBanner({
        type: "error",
        message: "Image size exceeds 5MB limit. Please choose a smaller photo.",
      });
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      setAvatar(result);
      const email = profile?.email || authUser?.email;
      if (email) {
        localStorage.setItem(`swiftdrop_avatar_${email}`, result);
        window.dispatchEvent(new Event("avatarUpdated"));
      }
      setProfileBanner({
        type: "success",
        message: "Profile picture updated successfully!",
      });
    };
    reader.readAsDataURL(file);
  };

  const handleRemoveAvatar = () => {
    setIsAvatarMenuOpen(false);
    const email = profile?.email || authUser?.email;
    if (email) {
      localStorage.removeItem(`swiftdrop_avatar_${email}`);
      window.dispatchEvent(new Event("avatarUpdated"));
    }
    setAvatar(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
    if (cameraInputRef.current) {
      cameraInputRef.current.value = "";
    }
    setProfileBanner({
      type: "success",
      message: "Profile picture removed.",
    });
  };

  const isPhoneValid = !phoneNumber.trim() || SRI_LANKAN_PHONE_REGEX.test(phoneNumber.trim());

  return (
    <div className="font-sans text-ink">
      {/* Header — Real-world Admin Identity & Governance Header */}
      <div className="mb-8 flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
        <div className="flex flex-col sm:flex-row sm:items-center gap-5">
          {/* Avatar with Photo Options Popover */}
          <div className="relative shrink-0 self-start sm:self-auto" ref={avatarMenuRef}>
            <div
              onClick={() => setIsAvatarMenuOpen((prev) => !prev)}
              className="relative group cursor-pointer"
              title="Click to manage profile photo"
            >
              {avatar ? (
                <img
                  src={avatar}
                  alt={fullName || "Administrator"}
                  className="h-20 w-20 rounded-2xl object-cover border-2 border-coral/30 shadow-sm transition group-hover:opacity-90"
                />
              ) : (
                <div className="grid h-20 w-20 place-items-center rounded-2xl bg-coral/10 font-display text-3xl font-bold text-coral border-2 border-coral/20 transition group-hover:border-coral/40">
                  {(fullName || authUser?.name || "A")[0]?.toUpperCase()}
                </div>
              )}
              <div className="absolute -bottom-1.5 -right-1.5 grid h-7 w-7 place-items-center rounded-full bg-ink text-white shadow-md transition group-hover:bg-coral">
                <Camera size={13} />
              </div>
            </div>

            {/* Hidden file inputs */}
            <input
              type="file"
              ref={fileInputRef}
              accept="image/png,image/jpeg,image/webp"
              onChange={handleAvatarUpload}
              className="hidden"
            />
            <input
              type="file"
              ref={cameraInputRef}
              accept="image/*"
              capture="user"
              onChange={handleAvatarUpload}
              className="hidden"
            />

            {/* Profile Picture Options Popover Menu */}
            {isAvatarMenuOpen && (
              <div className="absolute left-0 top-full mt-2 z-30 w-52 rounded-2xl border border-ink/10 bg-white p-2 shadow-xl animate-in fade-in zoom-in-95 duration-100">
                <div className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-ink/40">
                  Profile Photo
                </div>
                <button
                  type="button"
                  onClick={handleStartCamera}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold text-ink transition hover:bg-mist cursor-pointer"
                >
                  <Camera size={15} className="text-coral" />
                  <span>Take Photo</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setIsAvatarMenuOpen(false);
                    fileInputRef.current?.click();
                  }}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold text-ink transition hover:bg-mist cursor-pointer"
                >
                  <Upload size={15} className="text-coral" />
                  <span>Upload Photo</span>
                </button>
                {avatar && (
                  <button
                    type="button"
                    onClick={() => {
                      setIsAvatarMenuOpen(false);
                      handleRemoveAvatar();
                    }}
                    className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold text-rose-600 transition hover:bg-rose-50 cursor-pointer"
                  >
                    <Trash2 size={15} />
                    <span>Remove Photo</span>
                  </button>
                )}
              </div>
            )}
          </div>

          <div>
            <div className="flex items-center gap-2">
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">
                System Operator
              </p>
              <span className="rounded-md bg-coral/10 px-2.5 py-0.5 text-[11px] font-bold uppercase tracking-wider text-coral">
                Role Admin
              </span>
              <span className="rounded-md bg-mint/40 px-2 py-0.5 text-[11px] font-bold text-emerald-800">
                {profile?.status || "ACTIVE"}
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-3 mt-1">
              <h1 className="font-display text-3xl font-bold tracking-tight text-ink sm:text-4xl">
                {fullName || authUser?.name || "System Administrator"}
              </h1>
            </div>
            <p className="mt-1 text-sm text-ink/60 font-mono">
              {profile?.email || authUser?.email}
              <span className="mx-2 text-ink/20">·</span>
              <span className="font-sans text-xs uppercase tracking-wider text-ink/50">
                {profile?.provider || "LOCAL"} Auth
              </span>
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3 self-start sm:self-auto">
          <Link
            to="/admin/overview"
            className="flex items-center justify-center gap-2 rounded-xl border border-ink/10 bg-white px-4 py-3 text-sm font-semibold text-ink/70 transition hover:border-coral/40 hover:text-ink"
          >
            <span>&larr; Admin Overview</span>
          </Link>
          <button
            onClick={() => loadProfile()}
            disabled={isProfileLoading}
            className="flex items-center justify-center gap-2 rounded-xl border border-ink/10 bg-white px-4 py-3 text-sm font-semibold text-ink/70 transition hover:border-coral/40 hover:text-ink disabled:opacity-50"
            title="Refresh profile"
          >
            <RefreshCw
              size={16}
              className={isProfileLoading ? "animate-spin text-coral" : ""}
            />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* Live Camera Viewfinder (Inline, Strictly Zero Modals) */}
      {isLiveCameraActive && (
        <div className="mb-8 rounded-2xl border-2 border-coral/40 bg-ink p-5 text-white shadow-xl">
          <div className="mb-3 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="h-2.5 w-2.5 rounded-full bg-rose-500 animate-ping" />
              <span className="text-xs font-bold uppercase tracking-wider text-white/80">
                Live Camera Capture
              </span>
            </div>
            <button
              type="button"
              onClick={handleStopCamera}
              className="rounded-lg p-1 text-white/60 hover:bg-white/10 hover:text-white cursor-pointer"
            >
              <X size={18} />
            </button>
          </div>
          <div className="relative mx-auto aspect-square max-w-xs overflow-hidden rounded-2xl bg-black border border-white/10 shadow-inner">
            <video ref={videoRef} autoPlay playsInline className="h-full w-full object-cover" />
          </div>
          <div className="mt-4 flex items-center justify-center gap-3">
            <button
              type="button"
              onClick={handleStopCamera}
              className="rounded-xl border border-white/20 bg-white/10 px-4 py-2.5 text-xs font-semibold text-white transition hover:bg-white/20 cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleCapturePhoto}
              className="flex items-center gap-2 rounded-xl bg-coral px-5 py-2.5 text-xs font-bold text-white shadow-md transition hover:bg-coral/90 cursor-pointer"
            >
              <Camera size={15} />
              <span>Capture Photo</span>
            </button>
          </div>
        </div>
      )}

      <div className="space-y-8">
        {/* SECTION A: Administrator Contact & Identity Details Card */}
        <section className="rounded-2xl border border-ink/10 bg-white p-6 shadow-xs sm:p-8">
          {/* Card Header */}
          <div className="mb-6 flex flex-col gap-4 border-b border-ink/10 pb-6 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">
                Operator Identity
              </p>
              <h2 className="mt-1 font-display text-2xl font-bold text-ink">
                Personal & Security Details
              </h2>
              <p className="mt-1 text-xs text-ink/55">
                Manage your administrator contact information and communication credentials.
              </p>
            </div>

            <div className="flex items-center gap-2 self-start rounded-xl border border-emerald-200/80 bg-emerald-50 px-3.5 py-2 text-xs font-semibold text-emerald-800 sm:self-auto">
              <ShieldCheck size={16} className="text-emerald-700" />
              <span>Full System Governance Authority</span>
            </div>
          </div>

          {/* Profile Banner Feedback */}
          {profileBanner && (
            <div
              className={`mb-6 flex items-center justify-between rounded-xl border p-4 text-sm font-medium ${
                profileBanner.type === "success"
                  ? "border-emerald-200 bg-emerald-50 text-emerald-900"
                  : "border-coral/20 bg-coral/10 text-coral"
              }`}
            >
              <div className="flex items-center gap-2.5">
                {profileBanner.type === "success" ? (
                  <CheckCircle2 size={18} />
                ) : (
                  <XCircle size={18} />
                )}
                <span>{profileBanner.message}</span>
              </div>
              <button
                type="button"
                onClick={() => setProfileBanner(null)}
                className="rounded-lg p-1 text-ink/40 transition hover:bg-black/5 hover:text-ink cursor-pointer"
                title="Close"
              >
                <X size={16} />
              </button>
            </div>
          )}

          {/* Form */}
          {isProfileLoading ? (
            <div className="space-y-4 py-6">
              {[1, 2, 3].map((n) => (
                <div key={n} className="h-12 w-full animate-pulse rounded-xl bg-ink/5" />
              ))}
            </div>
          ) : (
            <form onSubmit={handleSaveProfile} className="space-y-6">
              <div className="grid gap-6 sm:grid-cols-2">
                {/* Email Address (Read-only) */}
                <div>
                  <label className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-ink/50">
                    <Mail size={14} />
                    <span>Administrator Email (Read-only)</span>
                  </label>
                  <div className="relative mt-2">
                    <input
                      type="email"
                      value={profile?.email || ""}
                      disabled
                      className="w-full cursor-not-allowed rounded-xl border border-ink/10 bg-ink/5 pl-4 pr-10 py-3 text-sm font-medium text-ink/60 outline-none"
                    />
                    <Lock
                      size={15}
                      className="absolute right-3.5 top-1/2 -translate-y-1/2 text-ink/30"
                    />
                  </div>
                  <p className="mt-1.5 text-xs text-ink/40">
                    Primary operator account email. Locked for system audit safety.
                  </p>
                </div>

                {/* Assigned Role (Read-only) */}
                <div>
                  <label className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-ink/50">
                    <ShieldAlert size={14} />
                    <span>Administrative Role</span>
                  </label>
                  <input
                    type="text"
                    value={profile?.role || "ROLE_ADMIN"}
                    disabled
                    className="mt-2 w-full cursor-not-allowed rounded-xl border border-ink/10 bg-ink/5 px-4 py-3 text-sm font-medium text-ink/60 outline-none uppercase font-mono"
                  />
                  <p className="mt-1.5 text-xs text-ink/40">
                    Highest security clearance with complete RBAC administrative privileges.
                  </p>
                </div>

                {/* Full Name (Editable) */}
                <div>
                  <label className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-ink/70">
                    <User size={14} className="text-coral" />
                    <span>Administrator Name *</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="e.g. Sadeepa Perera"
                    className="mt-2 w-full rounded-xl border border-ink/15 bg-white px-4 py-3 text-sm font-semibold text-ink placeholder:text-ink/30 outline-none transition focus:border-coral focus:ring-2 focus:ring-coral/20"
                  />
                  <p className="mt-1.5 text-xs text-ink/40">
                    Recorded in audit logs for system configuration events.
                  </p>
                </div>

                {/* Phone Number (Editable with Sri Lankan Validation) */}
                <div>
                  <div className="flex items-center justify-between">
                    <label className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-ink/70">
                      <Phone size={14} className="text-coral" />
                      <span>Contact Mobile</span>
                    </label>
                    {phoneNumber.trim() && (
                      <span
                        className={`text-xs font-semibold ${
                          isPhoneValid ? "text-emerald-700" : "text-coral"
                        }`}
                      >
                        {isPhoneValid ? "✓ Valid Sri Lankan Mobile" : "✗ Invalid Format"}
                      </span>
                    )}
                  </div>
                  <input
                    type="tel"
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    placeholder="e.g. 0771234567 or +94771234567"
                    className={`mt-2 w-full rounded-xl border bg-white px-4 py-3 text-sm font-semibold text-ink placeholder:text-ink/30 outline-none transition focus:ring-2 ${
                      isPhoneValid
                        ? "border-ink/15 focus:border-coral focus:ring-coral/20"
                        : "border-coral focus:border-coral focus:ring-coral/30"
                    }`}
                  />
                  <p className="mt-1.5 text-xs text-ink/40">
                    Format: <span className="font-mono">07XXXXXXXX</span> or{" "}
                    <span className="font-mono">+947XXXXXXXX</span>. Used for incident escalations.
                  </p>
                </div>
              </div>

              {/* Submit Action */}
              <div className="flex items-center justify-end border-t border-ink/10 pt-4">
                <button
                  type="submit"
                  disabled={isSavingProfile || !fullName.trim() || !isPhoneValid}
                  className="flex items-center justify-center gap-2 rounded-xl bg-coral px-6 py-3 text-sm font-semibold text-white shadow-lg shadow-coral/20 transition hover:bg-coral/90 disabled:opacity-50"
                >
                  {isSavingProfile ? (
                    <>
                      <RefreshCw size={16} className="animate-spin" />
                      <span>Saving Changes...</span>
                    </>
                  ) : (
                    <>
                      <Check size={16} />
                      <span>Save Profile Changes</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          )}
        </section>

        {/* SECTION B: Admin Privileges & System Governance Overview Card */}
        <section className="rounded-2xl border border-ink/10 bg-white p-6 shadow-xs sm:p-8">
          <div className="border-b border-ink/10 pb-4">
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">
              System Clearance
            </p>
            <h2 className="mt-1 font-display text-2xl font-bold text-ink">
              Administrative Capabilities & Privileges
            </h2>
            <p className="mt-1 text-xs text-ink/55">
              Summary of assigned system governance authorities across SwiftDrop infrastructure.
            </p>
          </div>

          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {/* Privilege 1: User Management */}
            <div className="flex flex-col justify-between rounded-2xl border border-ink/10 bg-mist/30 p-5 transition hover:border-coral/40">
              <div>
                <span className="grid h-10 w-10 place-items-center rounded-xl bg-coral/10 text-coral">
                  <Users size={18} />
                </span>
                <h3 className="mt-4 font-display text-base font-bold text-ink">
                  User Directory & Staff
                </h3>
                <p className="mt-1.5 text-xs text-ink/60 leading-relaxed">
                  Provision new Courier and Dispatcher staff accounts, modify platform roles, and toggle access states.
                </p>
              </div>
              <Link
                to="/admin/users"
                className="mt-4 inline-flex items-center gap-1 text-xs font-bold text-coral hover:underline"
              >
                <span>Open Users</span>
                <ArrowUpRight size={13} />
              </Link>
            </div>

            {/* Privilege 2: Partner Approvals */}
            <div className="flex flex-col justify-between rounded-2xl border border-ink/10 bg-mist/30 p-5 transition hover:border-emerald-300">
              <div>
                <span className="grid h-10 w-10 place-items-center rounded-xl bg-emerald-50 text-emerald-700">
                  <Store size={18} />
                </span>
                <h3 className="mt-4 font-display text-base font-bold text-ink">
                  Partner Approvals
                </h3>
                <p className="mt-1.5 text-xs text-ink/60 leading-relaxed">
                  Review and verify incoming Merchant business storefronts and freelance Courier registrations.
                </p>
              </div>
              <Link
                to="/admin/merchants"
                className="mt-4 inline-flex items-center gap-1 text-xs font-bold text-emerald-700 hover:underline"
              >
                <span>Merchant Queue</span>
                <ArrowUpRight size={13} />
              </Link>
            </div>

            {/* Privilege 3: Fleet & Dispatch Desk */}
            <div className="flex flex-col justify-between rounded-2xl border border-ink/10 bg-mist/30 p-5 transition hover:border-sky-300">
              <div>
                <span className="grid h-10 w-10 place-items-center rounded-xl bg-sky-50 text-sky-700">
                  <Truck size={18} />
                </span>
                <h3 className="mt-4 font-display text-base font-bold text-ink">
                  Operations & Fleet
                </h3>
                <p className="mt-1.5 text-xs text-ink/60 leading-relaxed">
                  Monitor network bottlenecks, track driver routes, and inspect unassigned delivery queues.
                </p>
              </div>
              <Link
                to="/fleet"
                className="mt-4 inline-flex items-center gap-1 text-xs font-bold text-sky-700 hover:underline"
              >
                <span>Fleet Console</span>
                <ArrowUpRight size={13} />
              </Link>
            </div>

            {/* Privilege 4: Pricing & Zones */}
            <div className="flex flex-col justify-between rounded-2xl border border-ink/10 bg-mist/30 p-5 transition hover:border-indigo-300">
              <div>
                <span className="grid h-10 w-10 place-items-center rounded-xl bg-indigo-50 text-indigo-700">
                  <DollarSign size={18} />
                </span>
                <h3 className="mt-4 font-display text-base font-bold text-ink">
                  Dynamic Tariffs & Zones
                </h3>
                <p className="mt-1.5 text-xs text-ink/60 leading-relaxed">
                  Configure distance-based calculation models, island-wide zones, and weight pricing tiers.
                </p>
              </div>
              <Link
                to="/admin/pricing"
                className="mt-4 inline-flex items-center gap-1 text-xs font-bold text-indigo-700 hover:underline"
              >
                <span>Manage Pricing</span>
                <ArrowUpRight size={13} />
              </Link>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
};

export default AdminProfilePage;

