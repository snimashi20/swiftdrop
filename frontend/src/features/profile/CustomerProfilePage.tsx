import React, { useEffect, useState, useCallback } from "react";
import {
  User,
  Mail,
  Phone,
  ShieldCheck,
  MapPin,
  Plus,
  Edit2,
  Trash2,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Home,
  Building2,
  Warehouse,
  Bookmark,
  Check,
  X,
  RefreshCw,
  Sparkles,
  Camera,
  Upload,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import {
  fetchUserProfile,
  updateUserProfile,
  fetchUserAddresses,
  createUserAddress,
  updateUserAddress,
  deleteUserAddress,
  type UserProfile,
  type Address,
  type CreateAddressRequest,
} from "./profile.api";

// Sri Lankan Mobile Regex: 07XXXXXXXX or +947XXXXXXXX (10 or 12 characters)
const SRI_LANKAN_PHONE_REGEX = /^(?:0|\+94)7[0-9]{8}$/;

const SRI_LANKAN_DISTRICTS = [
  "Colombo",
  "Gampaha",
  "Kalutara",
  "Kandy",
  "Matale",
  "Nuwara Eliya",
  "Galle",
  "Matara",
  "Hambantota",
  "Jaffna",
  "Kilinochchi",
  "Mannar",
  "Vavuniya",
  "Mullaitivu",
  "Batticaloa",
  "Ampara",
  "Trincomalee",
  "Kurunegala",
  "Puttalam",
  "Anuradhapura",
  "Polonnaruwa",
  "Badulla",
  "Monaragala",
  "Ratnapura",
  "Kegalle",
];

const LABEL_ICONS: Record<string, typeof Home> = {
  Home: Home,
  Office: Building2,
  Warehouse: Warehouse,
  Other: Bookmark,
};

export const CustomerProfilePage: React.FC = () => {
  const { user: authUser } = useAuth();

  // Profile State
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [fullName, setFullName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [isProfileLoading, setIsProfileLoading] = useState(true);
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [profileBanner, setProfileBanner] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // Addresses State
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [isAddressesLoading, setIsAddressesLoading] = useState(true);
  const [addressBanner, setAddressBanner] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // Inline Expandable Address Form State (Strictly Zero Modals)
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingAddressId, setEditingAddressId] = useState<number | null>(null);
  const [formLabel, setFormLabel] = useState("Home");
  const [formStreet, setFormStreet] = useState("");
  const [formCity, setFormCity] = useState("");
  const [formDistrict, setFormDistrict] = useState("Colombo");
  const [formPostalCode, setFormPostalCode] = useState("");
  const [formIsDefault, setFormIsDefault] = useState(false);
  const [isSavingAddress, setIsSavingAddress] = useState(false);

  // Inline Delete Confirmation State (Address ID currently confirming delete)
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const [isDeletingId, setIsDeletingId] = useState<number | null>(null);

  // Profile Picture (Avatar) State & Handlers
  const [avatar, setAvatar] = useState<string | null>(null);
  const [isAvatarMenuOpen, setIsAvatarMenuOpen] = useState(false);
  const [isLiveCameraActive, setIsLiveCameraActive] = useState(false);
  const [cameraStream, setCameraStream] = useState<MediaStream | null>(null);

  const fileInputRef = React.useRef<HTMLInputElement>(null);
  const cameraInputRef = React.useRef<HTMLInputElement>(null);
  const avatarMenuRef = React.useRef<HTMLDivElement>(null);
  const videoRef = React.useRef<HTMLVideoElement>(null);

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

  // Auto-dismiss banners after 30 seconds
  useEffect(() => {
    if (!profileBanner) return;
    const timer = setTimeout(() => {
      setProfileBanner(null);
    }, 30000); // 30 seconds
    return () => clearTimeout(timer);
  }, [profileBanner]);

  useEffect(() => {
    if (!addressBanner) return;
    const timer = setTimeout(() => {
      setAddressBanner(null);
    }, 30000); // 30 seconds
    return () => clearTimeout(timer);
  }, [addressBanner]);

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
      message: "Photo captured and saved as profile picture!",
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

  // Load Profile
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
        message: "Failed to load customer profile. Please check your connection.",
      });
    } finally {
      setIsProfileLoading(false);
    }
  }, []);

  // Load Addresses
  const loadAddresses = useCallback(async () => {
    setIsAddressesLoading(true);
    try {
      const data = await fetchUserAddresses();
      setAddresses(data);
    } catch {
      setAddressBanner({
        type: "error",
        message: "Failed to load saved addresses.",
      });
    } finally {
      setIsAddressesLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
    loadAddresses();
  }, [loadProfile, loadAddresses]);

  // Handle Profile Update
  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileBanner(null);

    // Validation
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
        message: "Your profile details have been updated successfully.",
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

  // Open Inline Form for New Address
  const handleOpenAddForm = () => {
    setEditingAddressId(null);
    setFormLabel("Home");
    setFormStreet("");
    setFormCity("");
    setFormDistrict("Colombo");
    setFormPostalCode("");
    setFormIsDefault(addresses.length === 0);
    setIsFormOpen(true);
    setAddressBanner(null);
  };

  // Open Inline Form for Editing Address
  const handleOpenEditForm = (addr: Address) => {
    setEditingAddressId(addr.id);
    setFormLabel(addr.addressLabel || addr.label || "Home");
    setFormStreet(addr.streetAddress);
    setFormCity(addr.city);
    setFormDistrict(addr.district || "Colombo");
    setFormPostalCode(addr.postalCode || "");
    setFormIsDefault(Boolean(addr.isDefault));
    setIsFormOpen(true);
    setAddressBanner(null);
  };

  // Close Inline Address Form
  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingAddressId(null);
  };

  // Handle Save Address (Create or Edit)
  const handleSaveAddress = async (e: React.FormEvent) => {
    e.preventDefault();
    setAddressBanner(null);

    if (!formStreet.trim() || !formCity.trim() || !formDistrict.trim()) {
      setAddressBanner({
        type: "error",
        message: "Street address, city, and district are required.",
      });
      return;
    }

    const payload: CreateAddressRequest = {
      addressLabel: formLabel,
      streetAddress: formStreet.trim(),
      city: formCity.trim(),
      district: formDistrict.trim(),
      postalCode: formPostalCode.trim() || undefined,
      isDefault: formIsDefault,
    };

    setIsSavingAddress(true);
    try {
      if (editingAddressId !== null) {
        await updateUserAddress(editingAddressId, payload);
        setAddressBanner({
          type: "success",
          message: "Address updated successfully.",
        });
      } else {
        await createUserAddress(payload);
        setAddressBanner({
          type: "success",
          message: "New address added to your address book.",
        });
      }
      handleCloseForm();
      await loadAddresses();
    } catch {
      setAddressBanner({
        type: "error",
        message: "Failed to save address. Please verify your details and try again.",
      });
    } finally {
      setIsSavingAddress(false);
    }
  };

  // Inline Delete Address
  const handleConfirmDelete = async (id: number) => {
    setIsDeletingId(id);
    setAddressBanner(null);
    try {
      await deleteUserAddress(id);
      setAddresses((prev) => prev.filter((a) => a.id !== id));
      setAddressBanner({
        type: "success",
        message: "Address removed from your address book.",
      });
      setConfirmDeleteId(null);
    } catch {
      setAddressBanner({
        type: "error",
        message: "Failed to delete address. Please try again.",
      });
    } finally {
      setIsDeletingId(null);
    }
  };

  const isPhoneValid = !phoneNumber.trim() || SRI_LANKAN_PHONE_REGEX.test(phoneNumber.trim());

  return (
    <div className="font-sans text-ink">
      {/* Header — Real-world user profile header */}
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
                  alt={fullName || "Profile"}
                  className="h-20 w-20 rounded-2xl object-cover border-2 border-coral/30 shadow-sm transition group-hover:opacity-90"
                />
              ) : (
                <div className="grid h-20 w-20 place-items-center rounded-2xl bg-coral/10 font-display text-3xl font-bold text-coral border-2 border-coral/20 transition group-hover:border-coral/40">
                  {(fullName || authUser?.name || "C")[0]?.toUpperCase()}
                </div>
              )}
              <div
                className="absolute -bottom-1.5 -right-1.5 grid h-7 w-7 place-items-center rounded-full bg-ink text-white shadow-md transition group-hover:bg-coral"
              >
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
              <div className="absolute left-0 top-full mt-2 z-30 w-52 rounded-2xl border border-ink/10 bg-white p-2 shadow-xl">
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
                    onClick={handleRemoveAvatar}
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
                Customer Profile
              </p>
              <span className="rounded-md bg-mint/40 px-2 py-0.5 text-[11px] font-bold text-emerald-800">
                {profile?.status || "ACTIVE"}
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-3 mt-1">
              <h1 className="font-display text-3xl font-bold tracking-tight text-ink sm:text-4xl">
                {fullName || authUser?.name || "Customer Account"}
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

        <button
          onClick={() => {
            loadProfile();
            loadAddresses();
          }}
          disabled={isProfileLoading || isAddressesLoading}
          className="flex items-center justify-center gap-2 rounded-xl border border-ink/10 bg-white px-4 py-3 text-sm font-semibold text-ink/70 transition hover:border-coral/40 hover:text-ink disabled:opacity-50 self-start sm:self-auto"
        >
          <RefreshCw
            size={16}
            className={isProfileLoading || isAddressesLoading ? "animate-spin text-coral" : ""}
          />
          <span>Refresh</span>
        </button>
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
        {/* SECTION A: Customer Personal Details Card */}
        <section className="rounded-2xl border border-ink/10 bg-white p-6 shadow-xs sm:p-8">
          {/* Section A Card Header */}
          <div className="mb-6 flex flex-col gap-4 border-b border-ink/10 pb-6 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">
                Personal Information
              </p>
              <h2 className="mt-1 font-display text-2xl font-bold text-ink">
                Contact & Identity Details
              </h2>
              <p className="mt-1 text-xs text-ink/55">
                Update your display name and contact phone number for parcel handoffs and dispatch calls.
              </p>
            </div>

            <div className="flex items-center gap-2 self-start rounded-xl border border-ink/10 bg-mist/50 px-3.5 py-2 text-xs font-semibold text-ink/70 sm:self-auto">
              <ShieldCheck size={16} className="text-coral" />
              <span>Customer RBAC Protected</span>
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

          {/* Profile Details Form (Strictly Inline, No Modals) */}
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
                    <span>Email Address (Read-only)</span>
                  </label>
                  <input
                    type="email"
                    value={profile?.email || ""}
                    disabled
                    className="mt-2 w-full cursor-not-allowed rounded-xl border border-ink/10 bg-ink/5 px-4 py-3 text-sm font-medium text-ink/60 outline-none"
                    title="Account email address cannot be changed"
                  />
                  <p className="mt-1.5 text-xs text-ink/40">
                    Linked to your primary SwiftDrop account.
                  </p>
                </div>

                {/* Account Role (Read-only) */}
                <div>
                  <label className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-ink/50">
                    <User size={14} />
                    <span>Assigned Role</span>
                  </label>
                  <input
                    type="text"
                    value={profile?.role || "CUSTOMER"}
                    disabled
                    className="mt-2 w-full cursor-not-allowed rounded-xl border border-ink/10 bg-ink/5 px-4 py-3 text-sm font-medium text-ink/60 outline-none uppercase"
                  />
                  <p className="mt-1.5 text-xs text-ink/40">
                    Platform role: Customer delivery recipient & sender.
                  </p>
                </div>

                {/* Full Name (Editable) */}
                <div>
                  <label className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-ink/70">
                    <User size={14} className="text-coral" />
                    <span>Full Name *</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="e.g. John Perera"
                    className="mt-2 w-full rounded-xl border border-ink/15 bg-white px-4 py-3 text-sm font-semibold text-ink placeholder:text-ink/30 outline-none transition focus:border-coral focus:ring-2 focus:ring-coral/20"
                  />
                  <p className="mt-1.5 text-xs text-ink/40">
                    Displayed on parcel labels and delivery receipts.
                  </p>
                </div>

                {/* Phone Number (Editable with Sri Lankan Validation) */}
                <div>
                  <div className="flex items-center justify-between">
                    <label className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-ink/70">
                      <Phone size={14} className="text-coral" />
                      <span>Phone Number</span>
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
                    <span className="font-mono">+947XXXXXXXX</span>. Used for courier dispatch calls.
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

        {/* SECTION B: Integrated Saved Address Book Card (Zero Modals) */}
        <section className="rounded-2xl border border-ink/10 bg-white p-6 shadow-xs sm:p-8">
          {/* Section Header */}
          <div className="flex flex-col justify-between gap-4 border-b border-ink/10 pb-6 sm:flex-row sm:items-center">
            <div>
              <div className="flex items-center gap-2.5">
                <p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">
                  Delivery Book
                </p>
                <span className="rounded-full bg-ink/5 px-2.5 py-0.5 text-xs font-bold text-ink/70">
                  {addresses.length} saved
                </span>
              </div>
              <h2 className="mt-1 font-display text-2xl font-bold text-ink sm:text-3xl">
                Saved Delivery Addresses
              </h2>
              <p className="mt-1 text-xs text-ink/55">
                Quickly select your pickup and drop-off locations when booking parcels.
              </p>
            </div>

            {!isFormOpen && (
              <button
                type="button"
                onClick={handleOpenAddForm}
                className="flex items-center justify-center gap-2 rounded-xl bg-ink px-4 py-3 text-sm font-semibold text-white shadow-xs transition hover:bg-ink/90 self-start sm:self-auto"
              >
                <Plus size={16} />
                <span>+ Add New Address</span>
              </button>
            )}
          </div>

          {/* Address Banner Feedback */}
          {addressBanner && (
            <div
              className={`my-6 flex items-center justify-between rounded-xl border p-4 text-sm font-medium ${
                addressBanner.type === "success"
                  ? "border-emerald-200 bg-emerald-50 text-emerald-900"
                  : "border-coral/20 bg-coral/10 text-coral"
              }`}
            >
              <div className="flex items-center gap-2.5">
                {addressBanner.type === "success" ? (
                  <CheckCircle2 size={18} />
                ) : (
                  <AlertCircle size={18} />
                )}
                <span>{addressBanner.message}</span>
              </div>
              <button
                type="button"
                onClick={() => setAddressBanner(null)}
                className="rounded-lg p-1 text-ink/40 transition hover:bg-black/5 hover:text-ink cursor-pointer"
                title="Close"
              >
                <X size={16} />
              </button>
            </div>
          )}

          {/* Inline Expandable Form (Zero Modals) */}
          {isFormOpen && (
            <div className="my-6 rounded-2xl border-2 border-coral/30 bg-mist/60 p-6">
              <div className="mb-4 flex items-center justify-between border-b border-ink/10 pb-3">
                <div className="flex items-center gap-2">
                  <span className="grid h-7 w-7 place-items-center rounded-lg bg-coral/10 text-coral">
                    {editingAddressId ? <Edit2 size={14} /> : <Plus size={14} />}
                  </span>
                  <h3 className="font-display text-lg font-bold text-ink">
                    {editingAddressId ? "Edit Address Details" : "Add New Saved Address"}
                  </h3>
                </div>
                <button
                  type="button"
                  onClick={handleCloseForm}
                  className="rounded-lg p-1 text-ink/40 hover:bg-ink/5 hover:text-ink"
                  title="Close form"
                >
                  <X size={18} />
                </button>
              </div>

              <form onSubmit={handleSaveAddress} className="space-y-4">
                <div className="grid gap-4 sm:grid-cols-2">
                  {/* Address Label */}
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-ink/60">
                      Address Label *
                    </label>
                    <select
                      value={formLabel}
                      onChange={(e) => setFormLabel(e.target.value)}
                      className="mt-1.5 w-full rounded-xl border border-ink/15 bg-white px-3.5 py-2.5 text-sm font-semibold text-ink outline-none transition focus:border-coral focus:ring-2 focus:ring-coral/20"
                    >
                      <option value="Home">Home</option>
                      <option value="Office">Office</option>
                      <option value="Warehouse">Warehouse</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>

                  {/* District (Sri Lankan Dropdown) */}
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-ink/60">
                      District *
                    </label>
                    <select
                      value={formDistrict}
                      onChange={(e) => setFormDistrict(e.target.value)}
                      className="mt-1.5 w-full rounded-xl border border-ink/15 bg-white px-3.5 py-2.5 text-sm font-semibold text-ink outline-none transition focus:border-coral focus:ring-2 focus:ring-coral/20"
                    >
                      {SRI_LANKAN_DISTRICTS.map((d) => (
                        <option key={d} value={d}>
                          {d}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* City */}
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-ink/60">
                      City / Suburb *
                    </label>
                    <input
                      type="text"
                      required
                      value={formCity}
                      onChange={(e) => setFormCity(e.target.value)}
                      placeholder="e.g. Colombo 03, Dehiwala, Kandy"
                      className="mt-1.5 w-full rounded-xl border border-ink/15 bg-white px-3.5 py-2.5 text-sm font-semibold text-ink placeholder:text-ink/30 outline-none transition focus:border-coral focus:ring-2 focus:ring-coral/20"
                    />
                  </div>

                  {/* Postal Code (Optional) */}
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-ink/60">
                      Postal Code (Optional)
                    </label>
                    <input
                      type="text"
                      value={formPostalCode}
                      onChange={(e) => setFormPostalCode(e.target.value)}
                      placeholder="e.g. 00300"
                      className="mt-1.5 w-full rounded-xl border border-ink/15 bg-white px-3.5 py-2.5 text-sm font-semibold text-ink placeholder:text-ink/30 outline-none transition focus:border-coral focus:ring-2 focus:ring-coral/20"
                    />
                  </div>
                </div>

                {/* Street Address */}
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-ink/60">
                    Street Address & Building / Unit *
                  </label>
                  <input
                    type="text"
                    required
                    value={formStreet}
                    onChange={(e) => setFormStreet(e.target.value)}
                    placeholder="e.g. 142/A Galle Road, Apt 4B"
                    className="mt-1.5 w-full rounded-xl border border-ink/15 bg-white px-3.5 py-2.5 text-sm font-semibold text-ink placeholder:text-ink/30 outline-none transition focus:border-coral focus:ring-2 focus:ring-coral/20"
                  />
                </div>

                {/* Set as Default Toggle */}
                <div className="flex items-center gap-3 pt-2">
                  <input
                    type="checkbox"
                    id="formIsDefault"
                    checked={formIsDefault}
                    onChange={(e) => setFormIsDefault(e.target.checked)}
                    className="h-4 w-4 rounded border-ink/20 text-coral accent-coral focus:ring-coral/20"
                  />
                  <label htmlFor="formIsDefault" className="text-xs font-semibold text-ink select-none">
                    Set this as my default delivery and pickup address
                  </label>
                </div>

                {/* Form Action Controls */}
                <div className="flex items-center justify-end gap-3 border-t border-ink/10 pt-4">
                  <button
                    type="button"
                    onClick={handleCloseForm}
                    disabled={isSavingAddress}
                    className="rounded-xl border border-ink/10 bg-white px-4 py-2.5 text-xs font-semibold text-ink/70 transition hover:bg-mist disabled:opacity-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isSavingAddress || !formStreet.trim() || !formCity.trim()}
                    className="flex items-center gap-1.5 rounded-xl bg-coral px-5 py-2.5 text-xs font-semibold text-white shadow-xs transition hover:bg-coral/90 disabled:opacity-50"
                  >
                    {isSavingAddress ? (
                      <>
                        <RefreshCw size={14} className="animate-spin" />
                        <span>Saving...</span>
                      </>
                    ) : (
                      <>
                        <Check size={14} />
                        <span>{editingAddressId ? "Update Address" : "Save Address"}</span>
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* Saved Addresses List */}
          {isAddressesLoading ? (
            <div className="grid gap-4 sm:grid-cols-2 pt-4">
              {[1, 2].map((n) => (
                <div key={n} className="h-32 w-full animate-pulse rounded-2xl bg-ink/5" />
              ))}
            </div>
          ) : addresses.length === 0 ? (
            <div className="py-12 text-center">
              <div className="mx-auto mb-3 grid h-12 w-12 place-items-center rounded-full bg-ink/5 text-ink/50">
                <MapPin size={24} />
              </div>
              <h3 className="font-display text-base font-bold text-ink">No Saved Addresses Yet</h3>
              <p className="mt-1 max-w-sm mx-auto text-xs text-ink/50">
                Add your home or office address to speed up your next parcel booking.
              </p>
              {!isFormOpen && (
                <button
                  type="button"
                  onClick={handleOpenAddForm}
                  className="mt-4 inline-flex items-center gap-1.5 rounded-xl bg-coral px-4 py-2 text-xs font-semibold text-white shadow-xs transition hover:bg-coral/90"
                >
                  <Plus size={14} />
                  <span>Add First Address</span>
                </button>
              )}
            </div>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 pt-4">
              {addresses.map((addr) => {
                const label = addr.addressLabel || addr.label || "Address";
                const IconComponent = LABEL_ICONS[label] || MapPin;
                const isConfirming = confirmDeleteId === addr.id;
                const isDeleting = isDeletingId === addr.id;

                return (
                  <div
                    key={addr.id}
                    className={`flex flex-col justify-between rounded-2xl border p-5 transition ${
                      addr.isDefault
                        ? "border-coral/40 bg-coral/[0.02] shadow-xs"
                        : "border-ink/10 bg-white hover:border-ink/20"
                    }`}
                  >
                    <div>
                      {/* Top Label & Badges */}
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="grid h-7 w-7 place-items-center rounded-lg bg-coral/10 text-coral">
                            <IconComponent size={14} />
                          </span>
                          <span className="text-xs font-bold uppercase tracking-wider text-ink">
                            {label}
                          </span>
                        </div>
                        {addr.isDefault && (
                          <span className="flex items-center gap-1 rounded-full bg-mint/50 px-2.5 py-0.5 text-[11px] font-bold text-emerald-900">
                            <Sparkles size={11} />
                            <span>Default</span>
                          </span>
                        )}
                      </div>

                      {/* Address Details */}
                      <div className="mt-3">
                        <p className="text-sm font-semibold text-ink leading-snug">
                          {addr.streetAddress}
                        </p>
                        <p className="mt-0.5 text-xs text-ink/65">
                          {addr.city}, {addr.district}
                        </p>
                        {addr.postalCode && (
                          <p className="mt-1 font-mono text-[11px] text-ink/40">
                            Postal Code: {addr.postalCode}
                          </p>
                        )}
                      </div>
                    </div>

                    {/* Inline Action Controls (Zero Modals) */}
                    <div className="mt-5 border-t border-ink/5 pt-3">
                      {isConfirming ? (
                        <div className="flex items-center justify-between rounded-xl bg-rose-50 p-2.5">
                          <span className="text-xs font-semibold text-rose-800">
                            Delete address?
                          </span>
                          <div className="flex items-center gap-2">
                            <button
                              type="button"
                              onClick={() => handleConfirmDelete(addr.id)}
                              disabled={isDeleting}
                              className="rounded-lg bg-rose-600 px-3 py-1 text-xs font-bold text-white shadow-xs hover:bg-rose-700 disabled:opacity-50"
                            >
                              {isDeleting ? "Deleting..." : "Confirm"}
                            </button>
                            <button
                              type="button"
                              onClick={() => setConfirmDeleteId(null)}
                              disabled={isDeleting}
                              className="rounded-lg border border-ink/10 bg-white px-2.5 py-1 text-xs font-semibold text-ink/70 hover:bg-mist"
                            >
                              Cancel
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div className="flex items-center justify-between text-xs">
                          <button
                            type="button"
                            onClick={() => handleOpenEditForm(addr)}
                            className="inline-flex items-center gap-1 font-semibold text-ink/60 hover:text-coral transition"
                          >
                            <Edit2 size={13} />
                            <span>Edit Address</span>
                          </button>

                          <button
                            type="button"
                            onClick={() => setConfirmDeleteId(addr.id)}
                            className="inline-flex items-center gap-1 font-semibold text-rose-600 hover:text-rose-800 transition"
                          >
                            <Trash2 size={13} />
                            <span>Delete</span>
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default CustomerProfilePage;

