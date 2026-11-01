import React, { useEffect, useState } from "react";
import { fetchUserProfile, updateUserProfile, UserProfile } from "./profile.api";

export const UserProfilePage: React.FC = () => {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [fullName, setFullName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    fetchUserProfile()
      .then((data) => {
        setProfile(data);
        setFullName(data.fullName || "");
        setPhoneNumber(data.phoneNumber || "");
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setErrorMessage("Failed to load user profile.");
        setLoading(false);
      });
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage("");
    setErrorMessage("");
    try {
      const updated = await updateUserProfile({ fullName, phoneNumber });
      setProfile(updated);
      setMessage("Profile updated successfully!");
    } catch (err) {
      console.error(err);
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
    <div className="max-w-xl mx-auto p-6 bg-white rounded-2xl shadow-sm border border-ink/5 mt-6">
      <h1 className="text-2xl font-bold mb-6 text-ink">User Profile</h1>
      {message && <div className="mb-4 p-3 bg-emerald-50 text-emerald-800 text-sm rounded-lg border border-emerald-200">{message}</div>}
      {errorMessage && <div className="mb-4 p-3 bg-rose-50 text-rose-800 text-sm rounded-lg border border-rose-200">{errorMessage}</div>}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-ink/70">Email Address (Read-only)</label>
          <input
            type="email"
            value={profile?.email || ""}
            disabled
            className="w-full mt-1.5 p-3 bg-ink/5 border border-ink/10 rounded-xl text-ink/70 cursor-not-allowed"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-ink/70">Auth Provider</label>
            <input
              type="text"
              value={profile?.provider || "LOCAL"}
              disabled
              className="w-full mt-1.5 p-3 bg-ink/5 border border-ink/10 rounded-xl text-ink/70 cursor-not-allowed"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-ink/70">Role</label>
            <input
              type="text"
              value={profile?.role || "CUSTOMER"}
              disabled
              className="w-full mt-1.5 p-3 bg-ink/5 border border-ink/10 rounded-xl text-ink/70 cursor-not-allowed"
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-ink/80">Full Name</label>
          <input
            type="text"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            required
            placeholder="John Doe"
            className="w-full mt-1.5 p-3 border border-ink/15 rounded-xl text-ink focus:outline-none focus:ring-2 focus:ring-coral/20 focus:border-coral"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-ink/80">Phone Number</label>
          <input
            type="text"
            value={phoneNumber}
            onChange={(e) => setPhoneNumber(e.target.value)}
            className="w-full mt-1.5 p-3 border border-ink/15 rounded-xl text-ink focus:outline-none focus:ring-2 focus:ring-coral/20 focus:border-coral"
            placeholder="+947XXXXXXXX or 07XXXXXXXX"
          />
        </div>

        <button
          type="submit"
          className="w-full bg-ink text-white py-3 rounded-xl font-semibold hover:bg-ink/90 transition shadow-sm"
        >
          Update Profile
        </button>
      </form>
    </div>
  );
};
