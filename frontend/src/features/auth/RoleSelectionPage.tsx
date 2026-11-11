import React, { useState } from "react";
import { Check, ArrowRight, Package, Building2, Truck, Sparkles, Clock, AlertCircle, type LucideIcon } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import type { SelfRegisterRole } from "./auth.types";
import { getApiErrorMessage } from "../../lib/api";

interface RoleOption {
  id: SelfRegisterRole;
  title: string;
  tagline: string;
  description: string;
  icon: LucideIcon;
  approvalRequired: boolean;
  badge: string;
}

const ROLE_OPTIONS: RoleOption[] = [
  {
    id: "CUSTOMER",
    title: "Customer",
    tagline: "Send & track personal packages",
    description: "Book deliveries, manage address books, and track packages in real-time across the island.",
    icon: Package,
    approvalRequired: false,
    badge: "Instant Access",
  },
  {
    id: "MERCHANT",
    title: "Merchant",
    tagline: "Business shipping & API integration",
    description: "Manage high-volume shipments, generate store API keys, and streamline customer fulfillment.",
    icon: Building2,
    approvalRequired: true,
    badge: "Admin Approval Required",
  },
  {
    id: "COURIER",
    title: "Courier",
    tagline: "Delivery fleet driver / rider",
    description: "Accept assigned delivery routes, provide proofs of delivery, and manage daily dropoffs.",
    icon: Truck,
    approvalRequired: true,
    badge: "Admin Approval Required",
  },
];

export const RoleSelectionPage: React.FC = () => {
  const { user, selectRole, isLoading } = useAuth();
  const navigate = useNavigate();
  const [selectedRole, setSelectedRole] = useState<SelfRegisterRole>("CUSTOMER");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    try {
      const updatedUser = await selectRole(selectedRole);

      if (updatedUser.status === "PENDING_APPROVAL" || selectedRole === "MERCHANT" || selectedRole === "COURIER") {
        navigate("/waiting-approval", { replace: true });
      } else {
        navigate("/dashboard", { replace: true });
      }
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, "Failed to set account role. Please try again."));
    }
  };

  return (
    <div className="min-h-screen bg-[#fbfcfa] flex flex-col justify-between p-4 sm:p-8">
      {/* Header */}
      <header className="max-w-4xl mx-auto w-full py-4">
        <Link to="/" className="font-display text-2xl font-bold text-ink">
          swift<span className="text-coral">drop</span>
        </Link>
      </header>

      {/* Main Content */}
      <main className="max-w-3xl mx-auto w-full my-auto py-8">
        <div className="bg-white rounded-3xl border border-ink/10 p-6 sm:p-12 shadow-xl space-y-8">
          {/* Page Intro */}
          <div className="space-y-2 text-center max-w-xl mx-auto">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-coral/10 text-coral">
              Google Account Connected
            </span>
            <h1 className="font-display text-3xl sm:text-4xl font-bold text-ink">
              Choose your account type
            </h1>
            <p className="text-sm text-ink/60">
              Welcome{user?.name ? `, ${user.name}` : ""}! Please select how you plan to use SwiftDrop to complete your registration.
            </p>
          </div>

          {error && (
            <div className="flex items-center gap-2 p-3.5 rounded-xl bg-coral/10 border border-coral/20 text-xs font-semibold text-coral">
              <AlertCircle size={16} className="shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Role Cards Form */}
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid gap-4 sm:grid-cols-3">
              {ROLE_OPTIONS.map((option) => {
                const isSelected = selectedRole === option.id;
                const Icon = option.icon;

                return (
                  <div
                    key={option.id}
                    onClick={() => setSelectedRole(option.id)}
                    className={`relative cursor-pointer rounded-2xl border-2 p-5 flex flex-col justify-between transition-all ${
                      isSelected
                        ? "border-coral bg-coral/[0.02] shadow-md shadow-coral/10 scale-[1.02]"
                        : "border-ink/10 bg-white hover:border-ink/25 hover:bg-ink/[0.01]"
                    }`}
                  >
                    {/* Selected Checkmark Indicator */}
                    <div className="flex items-start justify-between mb-4">
                      <div
                        className={`p-2.5 rounded-xl ${
                          isSelected ? "bg-coral text-white" : "bg-ink/5 text-ink/70"
                        }`}
                      >
                        <Icon size={20} />
                      </div>
                      <div
                        className={`h-5 w-5 rounded-full border flex items-center justify-center transition-colors ${
                          isSelected
                            ? "border-coral bg-coral text-white"
                            : "border-ink/20 bg-white"
                        }`}
                      >
                        {isSelected && <Check size={12} strokeWidth={3} />}
                      </div>
                    </div>

                    <div className="space-y-1.5 mb-4">
                      <h3 className="font-display text-lg font-bold text-ink">
                        {option.title}
                      </h3>
                      <p className="text-xs font-semibold text-ink/70">
                        {option.tagline}
                      </p>
                      <p className="text-[11px] text-ink/50 leading-relaxed">
                        {option.description}
                      </p>
                    </div>

                    {/* Access Status Badge */}
                    <div className="pt-2 border-t border-ink/5">
                      <span
                        className={`inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-md ${
                          option.approvalRequired
                            ? "bg-amber-50 text-amber-800 border border-amber-200"
                            : "bg-emerald-50 text-emerald-700 border border-emerald-200"
                        }`}
                      >
                        {option.approvalRequired ? (
                          <Clock size={11} className="shrink-0" />
                        ) : (
                          <Sparkles size={11} className="shrink-0" />
                        )}
                        <span>{option.badge}</span>
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Note banner based on selection */}
            <div className="rounded-2xl p-4 bg-mist/40 border border-ink/10 text-xs text-ink/70 flex items-start gap-3">
              {selectedRole === "CUSTOMER" ? (
                <>
                  <Sparkles size={18} className="text-emerald-600 shrink-0 mt-0.5" />
                  <div>
                    <strong className="text-ink font-semibold">Immediate access:</strong> Customer accounts can book pickups and track deliveries instantly without waiting for administrator approval.
                  </div>
                </>
              ) : (
                <>
                  <Clock size={18} className="text-amber-600 shrink-0 mt-0.5" />
                  <div>
                    <strong className="text-ink font-semibold">Admin approval flow:</strong> Choosing{" "}
                    <strong>{selectedRole === "MERCHANT" ? "Merchant" : "Courier"}</strong> will submit your account to our administration team for review. You will be redirected to an approval waiting page.
                  </div>
                </>
              )}
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full flex items-center justify-center gap-2 rounded-xl bg-ink px-4 py-4 font-semibold text-white transition hover:bg-ink/90 disabled:opacity-60 shadow-lg text-sm"
            >
              <span>{isLoading ? "Saving your selection..." : "Complete Registration & Continue"}</span>
              <ArrowRight size={18} />
            </button>
          </form>
        </div>
      </main>

      {/* Footer */}
      <footer className="text-center py-4 text-xs text-ink/40">
        &copy; {new Date().getFullYear()} SwiftDrop Logistics Network.
      </footer>
    </div>
  );
};

export default RoleSelectionPage;
