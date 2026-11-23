import { useState } from "react";
import { Link } from "react-router-dom";
import { BookOpen, ChevronRight, KeyRound, Lock, Package, User, Webhook } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { api, getApiErrorMessage } from "../../lib/api";

export function SettingsPage() {
  const { user } = useAuth();

  const [showPasswordForm, setShowPasswordForm] = useState(false);

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-2xl font-bold">Settings</h1>

      {/* Account */}
      <Section title="Account">
        <SettingsLink to="/profile" icon={User} label="Edit profile" desc="Name, phone number, email" />
        <SettingsLink to="/profile/addresses" icon={BookOpen} label="Address book" desc="Manage saved addresses" />
      </Section>

      {/* Security — only for LOCAL auth users */}
      <Section title="Security">
        {showPasswordForm ? (
          <ChangePasswordForm onDone={() => setShowPasswordForm(false)} />
        ) : (
          <button
            onClick={() => setShowPasswordForm(true)}
            className="flex w-full items-center gap-4 rounded-xl px-4 py-3 text-left transition hover:bg-ink/5"
          >
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-ink/5">
              <Lock size={18} className="text-ink/60" />
            </span>
            <span className="flex-1">
              <span className="block text-sm font-medium text-ink">Change password</span>
              <span className="block text-xs text-ink/50">Update your account password</span>
            </span>
            <ChevronRight size={16} className="text-ink/30" />
          </button>
        )}
      </Section>

      {/* Merchant-only */}
      {user?.role === "MERCHANT" && (
        <Section title="Merchant">
          <SettingsLink to="/merchant/api-keys" icon={KeyRound} label="API keys" desc="Manage your integration keys" />
          <SettingsLink to="/webhooks" icon={Webhook} label="Webhooks" desc="Configure event notifications" />
          <SettingsLink to="/merchant/orders" icon={Package} label="Orders" desc="View and manage orders" />
        </Section>
      )}
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-2xl border border-ink/5 bg-white shadow-sm">
      <h2 className="px-5 pt-4 pb-1 text-xs font-bold uppercase tracking-[0.15em] text-ink/40">{title}</h2>
      <div className="divide-y divide-ink/5">{children}</div>
    </div>
  );
}

function SettingsLink({ to, icon: Icon, label, desc }: { to: string; icon: typeof User; label: string; desc: string }) {
  return (
    <Link to={to} className="flex items-center gap-4 rounded-xl px-4 py-3 transition hover:bg-ink/5">
      <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-ink/5">
        <Icon size={18} className="text-ink/60" />
      </span>
      <span className="flex-1">
        <span className="block text-sm font-medium text-ink">{label}</span>
        <span className="block text-xs text-ink/50">{desc}</span>
      </span>
      <ChevronRight size={16} className="text-ink/30" />
    </Link>
  );
}

function ChangePasswordForm({ onDone }: { onDone: () => void }) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (newPassword.length < 8) {
      setError("New password must be at least 8 characters.");
      return;
    }
    if (newPassword !== confirm) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);
    try {
      await api.put("/users/me/password", { currentPassword, newPassword });
      setSuccess("Password updated successfully.");
      setCurrentPassword("");
      setNewPassword("");
      setConfirm("");
      setTimeout(onDone, 1500);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number } };
      if (axiosErr.response?.status === 403) {
        setError("Current password is incorrect.");
      } else {
        setError(getApiErrorMessage(err, "Failed to change password."));
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3 px-5 py-4">
      {error && <p className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700 border border-rose-200">{error}</p>}
      {success && <p className="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700 border border-emerald-200">{success}</p>}
      <div>
        <label className="block text-sm font-medium text-ink/70">Current password</label>
        <input
          type="password"
          value={currentPassword}
          onChange={(e) => setCurrentPassword(e.target.value)}
          required
          className="mt-1 w-full rounded-xl border border-ink/15 p-2.5 text-sm focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-ink/70">New password</label>
        <input
          type="password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          required
          minLength={8}
          className="mt-1 w-full rounded-xl border border-ink/15 p-2.5 text-sm focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-ink/70">Confirm new password</label>
        <input
          type="password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
          required
          className="mt-1 w-full rounded-xl border border-ink/15 p-2.5 text-sm focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
        />
      </div>
      <div className="flex gap-2 pt-1">
        <button
          type="submit"
          disabled={loading}
          className="rounded-xl bg-ink px-4 py-2 text-sm font-semibold text-white transition hover:bg-ink/90 disabled:opacity-50"
        >
          {loading ? "Saving…" : "Update password"}
        </button>
        <button
          type="button"
          onClick={onDone}
          className="rounded-xl px-4 py-2 text-sm font-medium text-ink/60 transition hover:bg-ink/5"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
