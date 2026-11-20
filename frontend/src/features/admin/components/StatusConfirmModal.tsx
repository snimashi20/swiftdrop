import React, { useEffect, useState } from "react";
import { AlertCircle, AlertTriangle, CheckCircle2, Loader2, Power, X } from "lucide-react";
import type { AdminUser } from "../admin.types";
import { RoleBadge } from "./RoleBadge";

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
        aria-labelledby="status-modal-title"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-ink/10 bg-gradient-to-r from-mist/60 via-mist/30 to-white">
          <div className="flex items-center gap-3">
            <div
              className={`p-2 rounded-2xl ring-1 ${
                isDisabling
                  ? "bg-rose-50 text-rose-600 ring-rose-200"
                  : "bg-emerald-50 text-emerald-600 ring-emerald-200"
              }`}
            >
              <Power size={18} strokeWidth={2.2} />
            </div>
            <div>
              <h3 id="status-modal-title" className="text-base font-bold text-ink">
                {isDisabling ? "Disable User Account" : "Activate User Account"}
              </h3>
              <p className="text-xs text-ink/50 mt-0.5">Account access control</p>
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

        {/* Body */}
        <div>
          <div className="p-6 space-y-3.5">
            {/* User info snippet */}
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

            <p className="text-xs text-ink/75 leading-relaxed">
              {isDisabling ? (
                <>
                  Are you sure you want to deactivate <strong className="text-ink font-semibold">{user.fullName}</strong>?
                </>
              ) : (
                <>
                  Are you sure you want to reactivate <strong className="text-ink font-semibold">{user.fullName}</strong>?
                </>
              )}
            </p>

            {isDisabling ? (
              <div className="flex items-start gap-2.5 p-3.5 rounded-2xl bg-amber-50 text-amber-900 text-xs border border-amber-200/80 animate-in fade-in duration-150">
                <AlertTriangle size={16} className="shrink-0 mt-0.5 text-amber-600" />
                <div className="space-y-1">
                  <span className="font-bold text-amber-950">Notice on Account Deactivation</span>
                  <p className="text-amber-800/90 leading-relaxed">
                    The user will be immediately blocked from logging into the SwiftDrop network until an administrator re-enables their account.
                  </p>
                </div>
              </div>
            ) : (
              <div className="flex items-start gap-2.5 p-3.5 rounded-2xl bg-emerald-50 text-emerald-900 text-xs border border-emerald-200/80 animate-in fade-in duration-150">
                <CheckCircle2 size={16} className="shrink-0 mt-0.5 text-emerald-600" />
                <p className="text-emerald-800 leading-relaxed">
                  The user will regain login access immediately using their existing authentication credentials.
                </p>
              </div>
            )}

            {isCurrentUser && isDisabling && (
              <div className="flex items-start gap-2 p-3.5 rounded-2xl bg-rose-50 text-rose-800 text-xs border border-rose-200 font-medium">
                <AlertCircle size={15} className="shrink-0 mt-0.5 text-rose-600" />
                <span>You cannot deactivate your own account while signed in.</span>
              </div>
            )}

            {error && (
              <div className="p-3.5 rounded-2xl bg-rose-50 text-rose-800 text-xs border border-rose-200/80 animate-in fade-in duration-150">
                {error}
              </div>
            )}
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
              type="button"
              onClick={handleConfirm}
              disabled={submitting || (isCurrentUser && isDisabling)}
              className={`flex items-center gap-2 px-5 py-2 rounded-xl text-white text-xs font-semibold shadow-md active:scale-[0.98] transition-all disabled:opacity-50 ${
                isDisabling
                  ? "bg-rose-600 hover:bg-rose-700 shadow-rose-600/20"
                  : "bg-emerald-600 hover:bg-emerald-700 shadow-emerald-600/20"
              }`}
            >
              {submitting ? (
                <>
                  <Loader2 size={14} className="animate-spin" />
                  <span>Processing...</span>
                </>
              ) : (
                <>
                  <Power size={14} strokeWidth={2.2} />
                  <span>
                    {isDisabling ? "Confirm Deactivation" : "Confirm Activation"}
                  </span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
