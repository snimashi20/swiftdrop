import React, { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import {
  CreditCard,
  Calendar,
  MapPin,
  Truck,
  Phone,
  User,
  Trash2,
  Upload,
  ShieldCheck,
  CheckCircle2,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import {
  fetchUserProfile,
  updateUserProfile,
  fetchCourierProfile,
  type CourierFullProfile,
} from "./profile.api";
import { api } from "../../lib/api";

function DocumentPreview({ url, alt }: { url: string; alt: string }) {
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
      <div className="flex h-36 items-center justify-center rounded-xl border border-ink/10 bg-mist/30 text-xs text-ink/50">
        Preview unavailable
      </div>
    );
  }
  if (!src) {
    return (
      <div className="flex h-36 items-center justify-center rounded-xl bg-mist/30 text-xs text-ink/40">
        Loading...
      </div>
    );
  }
  return (
    <a href={src} target="_blank" rel="noreferrer" title={`View ${alt}`}>
      <img src={src} alt={alt} className="h-36 w-full rounded-xl object-cover border border-ink/10 hover:opacity-95 transition" />
    </a>
  );
}

export const UserProfilePage: React.FC = () => {
  const { user } = useAuth();

  if (user?.role === "CUSTOMER") return <Navigate to="/customer/profile" replace />;
  if (user?.role === "ADMIN") return <Navigate to="/admin/profile" replace />;

  const [courierData, setCourierData] = useState<CourierFullProfile | null>(null);
  const [fullName, setFullName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [avatar, setAvatar] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    const loadData = async () => {
      try {
        if (user?.role === "COURIER") {
          const cData = await fetchCourierProfile();
          setCourierData(cData);
          setFullName(cData.fullName || "");
          setPhoneNumber(cData.phoneNumber || "");
        } else {
          const uData = await fetchUserProfile();
          setFullName(uData.fullName || "");
          setPhoneNumber(uData.phoneNumber || "");
        }
      } catch (err) {
        console.error(err);
        setErrorMessage("Failed to load profile details.");
      } finally {
        setLoading(false);
      }
    };
    loadData();

    if (user?.email) {
      const stored = localStorage.getItem(`swiftdrop_avatar_${user.email}`);
      setAvatar(stored);
    }
  }, [user?.email, user?.role]);

  const handleAvatarUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      setErrorMessage("Please select an image file (PNG, JPG, or WebP).");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setErrorMessage("Image must be smaller than 5MB.");
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      setAvatar(result);
      if (user?.email) {
        localStorage.setItem(`swiftdrop_avatar_${user.email}`, result);
        window.dispatchEvent(new Event("avatarUpdated"));
      }
      setMessage("Profile photo updated!");
    };
    reader.readAsDataURL(file);
  };

  const handleRemoveAvatar = () => {
    if (user?.email) {
      localStorage.removeItem(`swiftdrop_avatar_${user.email}`);
      window.dispatchEvent(new Event("avatarUpdated"));
    }
    setAvatar(null);
    setMessage("Profile photo removed.");
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage("");
    setErrorMessage("");
    try {
      await updateUserProfile({ fullName, phoneNumber });
      setMessage("Profile updated successfully!");
    } catch {
      setErrorMessage("Failed to update profile. Please verify phone number format.");
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center p-12">
        <div className="w-8 h-8 border-4 border-coral border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto p-6 sm:p-8 bg-white rounded-3xl shadow-sm border border-ink/5 mt-6 space-y-8 font-sans text-ink">
      <header className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-ink/10 pb-6">
        <div>
          <h1 className="text-2xl font-bold font-display text-ink">Courier Profile</h1>
          <p className="text-xs text-ink/60 mt-1">Your verified onboarding identity, vehicle, and contact records.</p>
        </div>
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-mint text-ink">
          <CheckCircle2 size={14} /> Verified Courier
        </span>
      </header>

      {message && <div className="p-3.5 bg-emerald-50 text-emerald-800 text-xs font-medium rounded-xl border border-emerald-200">{message}</div>}
      {errorMessage && <div className="p-3.5 bg-rose-50 text-rose-800 text-xs font-medium rounded-xl border border-rose-200">{errorMessage}</div>}

      {/* Avatar Section */}
      <div className="flex items-center gap-5">
        <div className="relative">
          {avatar ? (
            <img src={avatar} alt={fullName} className="h-20 w-20 rounded-full object-cover border-2 border-coral shadow-sm" />
          ) : (
            <div className="grid h-20 w-20 place-items-center rounded-full bg-mint text-2xl font-bold text-ink">
              {fullName ? fullName[0].toUpperCase() : <User size={30} />}
            </div>
          )}
        </div>
        <div className="space-y-1.5">
          <div className="flex items-center gap-2">
            <label className="cursor-pointer inline-flex items-center gap-1.5 rounded-xl bg-ink px-3.5 py-2 text-xs font-semibold text-white hover:bg-ink/90 transition">
              <Upload size={14} /> Upload photo
              <input type="file" accept="image/png,image/jpeg,image/webp" onChange={handleAvatarUpload} className="hidden" />
            </label>
            {avatar && (
              <button type="button" onClick={handleRemoveAvatar} className="inline-flex items-center gap-1.5 rounded-xl border border-rose-200 px-3 py-2 text-xs font-semibold text-rose-600 hover:bg-rose-50 transition">
                <Trash2 size={14} /> Remove
              </button>
            )}
          </div>
          <p className="text-[11px] text-ink/50">PNG, JPG or WebP (max 5MB)</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Section 1: Personal & Contact */}
        <section className="space-y-4">
          <h2 className="text-sm font-bold uppercase tracking-wider text-ink/60 flex items-center gap-2">
            <User size={15} className="text-coral" /> 1. Personal & Contact Details
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Full Legal Name *</label>
              <input type="text" value={fullName} onChange={(e) => setFullName(e.target.value)} required className="w-full rounded-xl border border-ink/15 bg-white p-3 text-sm text-ink outline-none focus:border-coral focus:ring-2 focus:ring-coral/20" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Mobile Phone Number *</label>
              <input type="text" value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} required className="w-full rounded-xl border border-ink/15 bg-white p-3 text-sm text-ink outline-none focus:border-coral focus:ring-2 focus:ring-coral/20" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Email Address (Read-only)</label>
              <input type="email" value={courierData?.email || user?.email || ""} disabled className="w-full rounded-xl border border-ink/10 bg-mist/50 p-3 text-sm text-ink/60 cursor-not-allowed" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">National Identity Card (NIC)</label>
              <input type="text" value={courierData?.nicNumber || "Verified"} disabled className="w-full rounded-xl border border-ink/10 bg-mist/50 p-3 text-sm font-mono font-bold text-ink/70 cursor-not-allowed" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Date of Birth</label>
              <input type="text" value={courierData?.dateOfBirth || "N/A"} disabled className="w-full rounded-xl border border-ink/10 bg-mist/50 p-3 text-sm text-ink/70 cursor-not-allowed" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Residential Address</label>
              <input type="text" value={courierData?.residentialAddress || "N/A"} disabled className="w-full rounded-xl border border-ink/10 bg-mist/50 p-3 text-sm text-ink/70 cursor-not-allowed" />
            </div>
          </div>
        </section>

        {/* Section 2: Vehicle & Driving License */}
        <section className="space-y-4 pt-4 border-t border-ink/10">
          <h2 className="text-sm font-bold uppercase tracking-wider text-ink/60 flex items-center gap-2">
            <Truck size={15} className="text-coral" /> 2. Vehicle & Driving Credentials
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Registered Vehicle Type</label>
              <input type="text" value={courierData?.vehicleType || "MOTORBIKE"} disabled className="w-full rounded-xl border border-ink/10 bg-mist/50 p-3 text-sm font-semibold text-ink/70 cursor-not-allowed" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Vehicle Plate Number</label>
              <input type="text" value={courierData?.vehiclePlateNumber || "N/A"} disabled className="w-full rounded-xl border border-ink/10 bg-mist/50 p-3 text-sm font-mono font-bold text-ink/70 cursor-not-allowed" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Driving License Number</label>
              <input type="text" value={courierData?.drivingLicenseNumber || "N/A"} disabled className="w-full rounded-xl border border-ink/10 bg-mist/50 p-3 text-sm font-mono font-bold text-ink/70 cursor-not-allowed" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">License Expiry Date</label>
              <input type="text" value={courierData?.drivingLicenseExpiry || "N/A"} disabled className="w-full rounded-xl border border-ink/10 bg-mist/50 p-3 text-sm text-ink/70 cursor-not-allowed" />
            </div>
          </div>
        </section>

        {/* Section 3: Operating Zone & Emergency Contact */}
        <section className="space-y-4 pt-4 border-t border-ink/10">
          <h2 className="text-sm font-bold uppercase tracking-wider text-ink/60 flex items-center gap-2">
            <ShieldCheck size={15} className="text-coral" /> 3. Operating Zone & Emergency Contact
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Assigned Operating Zone</label>
              <input 
                type="text" 
                value={courierData?.zoneName || (courierData?.zoneId ? `Zone #${courierData.zoneId}` : "Assigned District")} 
                disabled 
                className="w-full rounded-xl border border-ink/10 bg-mist/50 p-3 text-sm font-semibold text-ink/80 cursor-not-allowed" 
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Emergency Contact Person</label>
              <input type="text" value={courierData?.emergencyContactName || "N/A"} disabled className="w-full rounded-xl border border-ink/10 bg-mist/50 p-3 text-sm text-ink/70 cursor-not-allowed" />
            </div>
            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold text-ink/70 mb-1">Emergency Phone Number</label>
              <input type="text" value={courierData?.emergencyContactPhone || "N/A"} disabled className="w-full rounded-xl border border-ink/10 bg-mist/50 p-3 text-sm font-mono text-ink/70 cursor-not-allowed" />
            </div>
          </div>
        </section>

        {/* Section 4: Uploaded NIC Documents */}
        {(courierData?.nicFrontPhotoUrl || courierData?.nicBackPhotoUrl) && (
          <section className="space-y-4 pt-4 border-t border-ink/10">
            <h2 className="text-sm font-bold uppercase tracking-wider text-ink/60 flex items-center gap-2">
              <CreditCard size={15} className="text-coral" /> 4. Submitted Identity Documents (NIC)
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {courierData?.nicFrontPhotoUrl && (
                <div>
                  <p className="text-xs font-semibold text-ink/60 mb-1.5">NIC Front Side</p>
                  <DocumentPreview url={courierData.nicFrontPhotoUrl} alt="NIC Front Side" />
                </div>
              )}
              {courierData?.nicBackPhotoUrl && (
                <div>
                  <p className="text-xs font-semibold text-ink/60 mb-1.5">NIC Back Side</p>
                  <DocumentPreview url={courierData.nicBackPhotoUrl} alt="NIC Back Side" />
                </div>
              )}
            </div>
          </section>
        )}

        <button
          type="submit"
          className="w-full bg-ink text-white py-3.5 rounded-xl font-semibold hover:bg-ink/90 transition shadow-sm cursor-pointer"
        >
          Save Changes
        </button>
      </form>
    </div>
  );
};