import React, { useEffect, useState } from "react";
import { AlertTriangle, Check, Shield, X } from "lucide-react";
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

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/40 backdrop-blur-xs animate-in fade-in duration-200">
      <div
        className="w-full max-w-md bg-white rounded-2xl shadow-xl border border-ink/10 overflow-hidden transform transition-all"
        role="dialog"
        aria-modal="true"
        aria-labelledby="role-modal-title"
      >
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-ink/10 bg-mist/50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-ink/5 text-ink">
              <Shield size={18} />
            </div>
            <div>
              <h3 id="role-modal-title" className="text-base font-bold text-ink">
                Update User Role
              </h3>
              <p className="text-xs text-ink/50">Adjust system permissions</p>
            </div>
          </div>
          <button
            onClick={onClose}
            disabled={submitting}
            className="p-1.5 rounded-lg text-ink/40 hover:text-ink hover:bg-ink/5 transition"
            aria-label="Close modal"
          >
            <X size={18} />
          </button>
        </div>

        {/* Content */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          {/* User info card */}
          <div className="p-3.5 rounded-xl bg-ink/5 border border-ink/10 space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold text-ink">{user.fullName}</span>
              <RoleBadge role={user.role} size="sm" />
            </div>
            <p className="text-xs text-ink/60 truncate">{user.email}</p>
          </div>

          {isCurrentUser && (
            <div className="flex items-start gap-2.5 p-3 rounded-xl bg-amber-50 text-amber-800 text-xs border border-amber-200">
              <AlertTriangle size={16} className="shrink-0 mt-0.5" />
              <span>
                <strong>Warning:</strong> You are editing your own user role. Changing your role from ADMIN will immediately revoke your administrative privileges.
              </span>
            </div>
          )}

          {error && (
            <div className="p-3 rounded-xl bg-rose-50 text-rose-700 text-xs border border-rose-200">
              {error}
            </div>
          )}

          {/* Role Selection */}
          <div className="space-y-1.5">
            <label className="block text-xs font-bold uppercase tracking-wider text-ink/60">
              Assign New Role
            </label>
            <select
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value as Role)}
              disabled={submitting}
              className="w-full rounded-xl border border-ink/15 bg-white px-3.5 py-2.5 text-sm font-medium text-ink outline-none transition focus:border-coral focus:ring-2 focus:ring-coral/20"
            >
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r.charAt(0) + r.slice(1).toLowerCase()} ({r})
                </option>
              ))}
            </select>
            <p className="text-xs text-ink/60 pt-1">
              {ROLE_DESCRIPTIONS[selectedRole]}
            </p>
          </div>

          {/* Footer Actions */}
          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-ink/10">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-ink/70 hover:text-ink hover:bg-ink/5 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting || selectedRole === user.role}
              className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-ink text-white text-xs font-semibold hover:bg-ink/90 transition shadow-sm disabled:opacity-50"
            >
              <Check size={14} />
              {submitting ? "Updating..." : "Save Role"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
