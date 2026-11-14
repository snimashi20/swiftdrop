import React, { useEffect, useState } from "react";
import { AlertTriangle, Check, Info, Loader2, Shield, X } from "lucide-react";
import type { Role } from "../../auth/auth.types";
import { ROLES } from "../../auth/auth.types";
import type { AdminUser } from "../admin.types";
import { RoleBadge } from "./RoleBadge";

interface RoleUpdateModalProps {
  user: AdminUser | null;
  isOpen: boolean;
  onClose: () => void;
  onSave: (userId: number, role: Role) => Promise<void>;
  isCurrentUser: boolean;
}

const ROLE_DESCRIPTIONS: Record<Role, string> = {
  CUSTOMER: "Can place orders, track incoming parcels, and manage personal address books.",
  MERCHANT: "Can create bulk shipments, view merchant analytics, and configure delivery webhooks.",
  COURIER: "Has access to courier mobile flows, parcel pickups, and delivery confirmation routes.",
  DISPATCHER: "Can view dispatch boards, monitor active fleet, and assign unassigned parcels.",
  ADMIN: "Full system administration: manages user accounts, roles, fleet, and system logs.",
};

export const RoleUpdateModal: React.FC<RoleUpdateModalProps> = ({
  user,
  isOpen,
  onClose,
  onSave,
  isCurrentUser,
}) => {
  const [selectedRole, setSelectedRole] = useState<Role>("CUSTOMER");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (user) {
      setSelectedRole(user.role);
      setError(null);
    }
  }, [user]);

  // Keyboard shortcut: Escape to dismiss
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !submitting) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, submitting, onClose]);

  if (!isOpen || !user) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedRole === user.role) {
      onClose();
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await onSave(user.id, selectedRole);
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to update role.";
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const userInitials = (user.fullName || user.email || "U")
    .split(" ")
    .map((part) => part[0])
    .filter(Boolean)
    .slice(0, 2)
    .join("")
    .toUpperCase();

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/60 backdrop-blur-md transition-all animate-in fade-in duration-200"
      onClick={(e) => {
        if (e.target === e.currentTarget && !submitting) {
          onClose();
        }
      }}
    >
      <div
        className="w-full max-w-md bg-white rounded-3xl shadow-2xl border border-ink/10 overflow-hidden transform transition-all duration-200 animate-in zoom-in-95"
        role="dialog"
        aria-modal="true"
        aria-labelledby="role-modal-title"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-ink/10 bg-gradient-to-r from-mist/60 via-mist/30 to-white">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-2xl bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200">
              <Shield size={18} strokeWidth={2.2} />
            </div>
            <div>
              <h3 id="role-modal-title" className="text-base font-bold text-ink">
                Update User Role
              </h3>
              <p className="text-xs text-ink/50 mt-0.5">Adjust system permissions</p>
            </div>
          </div>
          <button
            onClick={onClose}
            disabled={submitting}
            className="p-1.5 rounded-xl text-ink/40 hover:text-ink hover:bg-ink/5 transition-colors disabled:opacity-50"
            aria-label="Close modal"
          >
            <X size={18} />
          </button>
        </div>

        {/* Content */}
        <form onSubmit={handleSubmit}>
          <div className="p-6 space-y-3.5">
            {/* User info card */}
            <div className="flex items-center gap-3 p-3.5 rounded-2xl bg-ink/[0.03] border border-ink/10">
              <div className="w-10 h-10 rounded-xl bg-ink/10 text-ink/80 flex items-center justify-center font-bold text-xs shrink-0">
                {userInitials}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-sm font-bold text-ink truncate">{user.fullName}</span>
                  <RoleBadge role={user.role} size="sm" />
                </div>
                <p className="text-xs text-ink/50 truncate mt-0.5">{user.email}</p>
              </div>
            </div>

            {isCurrentUser && (
              <div className="flex items-start gap-2.5 p-3.5 rounded-2xl bg-amber-50 text-amber-900 text-xs border border-amber-200/80 animate-in fade-in duration-150">
                <AlertTriangle size={16} className="shrink-0 mt-0.5 text-amber-600" />
                <span className="leading-relaxed">
                  <strong>Warning:</strong> You are editing your own user role. Changing your role from ADMIN will immediately revoke your administrative privileges.
                </span>
              </div>
            )}

            {error && (
              <div className="p-3.5 rounded-2xl bg-rose-50 text-rose-800 text-xs border border-rose-200/80 animate-in fade-in duration-150">
                {error}
              </div>
            )}

            {/* Role Selection */}
            <div className="space-y-1.5">
              <label className="block text-[11px] font-bold uppercase tracking-wider text-ink/70">
                Assign New Role
              </label>
              <div className="relative">
                <select
                  value={selectedRole}
                  onChange={(e) => setSelectedRole(e.target.value as Role)}
                  disabled={submitting}
                  className="w-full rounded-xl border border-ink/15 bg-white px-3.5 py-2.5 text-sm font-semibold text-ink outline-none transition focus:border-coral focus:ring-2 focus:ring-coral/20 hover:border-ink/25 cursor-pointer disabled:cursor-not-allowed"
                >
                  {ROLES.map((r) => (
                    <option key={r} value={r}>
                      {r.charAt(0) + r.slice(1).toLowerCase()} ({r})
                    </option>
                  ))}
                </select>
              </div>

              {/* Role description card */}
              <div className="flex items-start gap-2 p-3 rounded-xl bg-ink/[0.02] border border-ink/10 text-xs text-ink/65 mt-2">
                <Info size={14} className="shrink-0 mt-0.5 text-coral" />
                <span className="leading-relaxed">{ROLE_DESCRIPTIONS[selectedRole]}</span>
              </div>
            </div>
          </div>

          {/* Footer Actions */}
          <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-ink/10 bg-white">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="px-4 py-2 rounded-xl border border-ink/15 text-xs font-semibold text-ink/70 hover:text-ink hover:bg-ink/5 transition-colors disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting || selectedRole === user.role}
              className="flex items-center gap-2 px-5 py-2 rounded-xl bg-coral hover:bg-coral-hover text-white text-xs font-semibold shadow-md shadow-coral/20 active:scale-[0.98] transition-all disabled:opacity-50"
            >
              {submitting ? (
                <>
                  <Loader2 size={14} className="animate-spin" />
                  <span>Updating...</span>
                </>
              ) : (
                <>
                  <Check size={14} strokeWidth={2.5} />
                  <span>Save Role</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
