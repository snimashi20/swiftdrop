import React, { useState } from "react";
import { Clock, ShieldCheck, RefreshCw, LogOut, ArrowRight, Building2, Truck } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";

export const WaitingApprovalPage: React.FC = () => {
  const { user, refreshProfile, logout } = useAuth();
  const navigate = useNavigate();
  const [checking, setChecking] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const roleLabel =
    user?.role === "MERCHANT"
      ? "Merchant"
      : user?.role === "COURIER"
      ? "Courier"
      : "Partner";

  const handleCheckStatus = async () => {
    setChecking(true);
    setStatusMessage(null);
    try {
      const updatedUser = await refreshProfile();
      if (updatedUser?.status === "ACTIVE") {
        setStatusMessage("Great news! Your account has been approved. Redirecting...");
        setTimeout(() => {
          navigate("/dashboard", { replace: true });
        }, 1200);
      } else {
        setStatusMessage("Your account is still under review. Please check back shortly.");
      }
    } catch {
      setStatusMessage("Could not check status right now. Please try again.");
    } finally {
      setChecking(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="min-h-screen bg-[#fbfcfa] flex flex-col justify-between p-4 sm:p-8">
      {/* Top Navbar */}
      <header className="max-w-4xl mx-auto w-full flex items-center justify-between py-4">
        <Link to="/" className="font-display text-2xl font-bold text-ink">
          swift<span className="text-coral">drop</span>
        </Link>
        <button
          onClick={handleLogout}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-ink/10 text-xs font-semibold text-ink/70 hover:bg-ink/5 transition"
        >
          <LogOut size={14} />
          <span>Sign Out</span>
        </button>
      </header>

      {/* Main Card */}
      <main className="max-w-xl mx-auto w-full my-auto py-8">
        <div className="bg-white rounded-3xl border border-ink/10 p-6 sm:p-10 shadow-xl text-center space-y-6">
          {/* Animated Clock / Review Icon */}
          <div className="relative mx-auto w-20 h-20 rounded-3xl bg-amber-50 border border-amber-200 flex items-center justify-center">
            <Clock size={40} className="text-amber-600 animate-pulse" />
            <span className="absolute -top-1.5 -right-1.5 flex h-4 w-4">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75" />
              <span className="relative inline-flex rounded-full h-4 w-4 bg-amber-500" />
            </span>
          </div>

          {/* Heading */}
          <div className="space-y-2">
            <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-amber-100/70 text-amber-900 border border-amber-200">
              Pending Admin Review
            </span>
            <h1 className="font-display text-3xl sm:text-4xl font-bold text-ink">
              Waiting for Admin's approval...
            </h1>
            <p className="text-sm text-ink/60 max-w-md mx-auto">
              Your <strong>{roleLabel}</strong> registration was submitted successfully.
              Before accessing your workspace, an administrator must review and approve your account.
            </p>
          </div>

          {/* Account Details Box */}
          <div className="rounded-2xl bg-mist/30 border border-ink/5 p-4 text-left space-y-2.5 text-xs text-ink/70">
            <div className="flex justify-between items-center py-1 border-b border-ink/5">
              <span className="text-ink/50">Registered Name</span>
              <span className="font-semibold text-ink">{user?.name || "User"}</span>
            </div>
            <div className="flex justify-between items-center py-1 border-b border-ink/5">
              <span className="text-ink/50">Email Address</span>
              <span className="font-mono text-ink">{user?.email}</span>
            </div>
            <div className="flex justify-between items-center py-1 border-b border-ink/5">
              <span className="text-ink/50">Requested Access</span>
              <span className="inline-flex items-center gap-1 font-semibold text-coral">
                {user?.role === "MERCHANT" ? <Building2 size={13} /> : <Truck size={13} />}
                {roleLabel} Workspace
              </span>
            </div>
            <div className="flex justify-between items-center py-1">
              <span className="text-ink/50">Status</span>
              <span className="font-bold text-amber-700 uppercase tracking-wider">
                Pending Approval
              </span>
            </div>
          </div>

          {statusMessage && (
            <div
              className={`p-3.5 rounded-xl text-xs font-semibold ${
                statusMessage.includes("approved")
                  ? "bg-emerald-50 text-emerald-800 border border-emerald-200"
                  : "bg-amber-50 text-amber-900 border border-amber-200"
              }`}
            >
              {statusMessage}
            </div>
          )}

          {/* Action Buttons */}
          <div className="space-y-3 pt-2">
            <button
              onClick={handleCheckStatus}
              disabled={checking}
              className="w-full flex items-center justify-center gap-2 rounded-xl bg-ink px-4 py-3.5 font-semibold text-white transition hover:bg-ink/90 disabled:opacity-60 shadow-md text-sm"
            >
              <RefreshCw size={16} className={checking ? "animate-spin text-coral" : ""} />
              <span>{checking ? "Checking approval status..." : "Check Approval Status"}</span>
            </button>

            <p className="text-xs text-ink/40">
              Once approved by an Admin, you will be able to log in and access your {roleLabel.toLowerCase()} dashboard.
            </p>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="text-center py-4 text-xs text-ink/40">
        &copy; {new Date().getFullYear()} SwiftDrop Logistics Network. All rights reserved.
      </footer>
    </div>
  );
};

export default WaitingApprovalPage;

