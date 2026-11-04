import React, { useState } from "react";
import { AlertTriangle, Power, X } from "lucide-react";
import type { AdminUser } from "../admin.types";

interface StatusConfirmModalProps {
  user: AdminUser | null;
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (userId: number, nextStatus: boolean) => Promise<void>;
  isCurrentUser: boolean;
}

export const StatusConfirmModal: React.FC<StatusConfirmModalProps> = ({
  user,
  isOpen,
  onClose,
  onConfirm,
  isCurrentUser,
}) => {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen || !user) return null;

  const nextStatus = !user.enabled;
  const isDisabling = !nextStatus;

  const handleConfirm = async () => {
    if (isCurrentUser && isDisabling) {
      setError("Self-deactivation is disabled for security reasons.");
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await onConfirm(user.id, nextStatus);
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to toggle status.";
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
        aria-labelledby="status-modal-title"
      >
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-ink/10 bg-mist/50">
          <div className="flex items-center gap-2.5">
            <div
              className={`p-2 rounded-xl ${
                isDisabling
                  ? "bg-rose-50 text-rose-600"
                  : "bg-emerald-50 text-emerald-600"
              }`}
            >
              <Power size={18} />
            </div>
            <div>
              <h3 id="status-modal-title" className="text-base font-bold text-ink">
                {isDisabling ? "Disable User Account" : "Activate User Account"}
              </h3>
              <p className="text-xs text-ink/50">Account access control</p>
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

        {/* Body */}
        <div className="p-5 space-y-4">
          <p className="text-sm text-ink/80">
            {isDisabling ? (
              <>
                Are you sure you want to deactivate{" "}
                <strong className="text-ink font-semibold">{user.fullName}</strong> (
                {user.email})?
              </>
            ) : (
              <>
                Are you sure you want to reactivate{" "}
                <strong className="text-ink font-semibold">{user.fullName}</strong> (
                {user.email})?
              </>
            )}
          </p>

          {isDisabling ? (
            <div className="p-3.5 rounded-xl bg-amber-50 text-amber-900 text-xs border border-amber-200/80 space-y-1">
              <div className="flex items-center gap-1.5 font-semibold">
                <AlertTriangle size={15} />
                <span>Notice on Account Deactivation</span>
              </div>
              <p className="text-amber-800">
                The user will be immediately signed out and blocked from logging into the SwiftDrop network until an administrator re-enables their account.
              </p>
            </div>
          ) : (
            <div className="p-3.5 rounded-xl bg-emerald-50 text-emerald-900 text-xs border border-emerald-200/80 space-y-1">
              <p className="text-emerald-800">
                The user will regain login access immediately using their existing authentication credentials.
              </p>
            </div>
          )}

          {isCurrentUser && isDisabling && (
            <div className="p-3 rounded-xl bg-rose-50 text-rose-700 text-xs border border-rose-200 font-medium">
              You cannot deactivate your own account while signed in.
            </div>
          )}

          {error && (
            <div className="p-3 rounded-xl bg-rose-50 text-rose-700 text-xs border border-rose-200">
              {error}
            </div>
          )}

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
              type="button"
              onClick={handleConfirm}
              disabled={submitting || (isCurrentUser && isDisabling)}
              className={`px-4 py-2 rounded-xl text-white text-xs font-semibold shadow-sm transition disabled:opacity-50 ${
                isDisabling
                  ? "bg-rose-600 hover:bg-rose-700"
                  : "bg-emerald-600 hover:bg-emerald-700"
              }`}
            >
              {submitting
                ? "Processing..."
                : isDisabling
                ? "Confirm Deactivation"
                : "Confirm Activation"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
