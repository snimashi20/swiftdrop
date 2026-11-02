import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  Filter,
  Key,
  Power,
  RefreshCw,
  Search,
  Shield,
  ShieldAlert,
  UserCheck,
  UserCog,
  Users,
  UserX,
} from "lucide-react";
import { useAuth } from "../../auth/AuthContext";
import type { Role } from "../../auth/auth.types";
import { ROLES } from "../../auth/auth.types";
import { getApiErrorMessage } from "../../../lib/api";
import { fetchAdminUsers, toggleUserStatus, updateUserRole } from "../admin.api";
import type { AdminUser, RoleFilterOption, ToastMessage } from "../admin.types";
import { RoleBadge } from "../components/RoleBadge";
import { RoleUpdateModal } from "../components/RoleUpdateModal";
import { StatusBadge } from "../components/StatusBadge";
import { StatusConfirmModal } from "../components/StatusConfirmModal";
import { ToastContainer } from "../components/Toast";

export const AdminUsersPage: React.FC = () => {
  const { user: currentUser } = useAuth();

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Filters & search
  const [selectedRoleFilter, setSelectedRoleFilter] = useState<RoleFilterOption>("ALL");
  const [searchQuery, setSearchQuery] = useState<string>("");

  // Modals state
  const [roleModalUser, setRoleModalUser] = useState<AdminUser | null>(null);
  const [statusModalUser, setStatusModalUser] = useState<AdminUser | null>(null);

  // Toast feedback state
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const addToast = useCallback((type: "success" | "error" | "info", title: string, message?: string) => {
    const id = `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
    const newToast: ToastMessage = { id, type, title, message };
    setToasts((prev) => [...prev, newToast]);

    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4500);
  }, []);

  const dismissToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  // Fetch users from backend
  const loadUsers = useCallback(
    async (showRefreshSpinner = false) => {
      if (showRefreshSpinner) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }
      setError(null);

      try {
        const data = await fetchAdminUsers(
          selectedRoleFilter === "ALL" ? undefined : selectedRoleFilter
        );
        setUsers(data);
      } catch (err: unknown) {
        const msg = getApiErrorMessage(err, "Failed to load users list.");
        setError(msg);
        addToast("error", "Error loading users", msg);
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [selectedRoleFilter, addToast]
  );

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  // Client-side search filtering (search by name, email, or ID)
  const filteredUsers = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return users;

    return users.filter((u) => {
      const matchName = u.fullName?.toLowerCase().includes(q);
      const matchEmail = u.email?.toLowerCase().includes(q);
      const matchId = u.id?.toString().includes(q);
      const matchPhone = u.phoneNumber?.toLowerCase().includes(q);
      return matchName || matchEmail || matchId || matchPhone;
    });
  }, [users, searchQuery]);

  // Summary Metrics calculations
  const metrics = useMemo(() => {
    const total = users.length;
    const active = users.filter((u) => u.enabled).length;
    const disabled = total - active;
    const staff = users.filter((u) =>
      ["ADMIN", "DISPATCHER", "COURIER"].includes(u.role)
    ).length;
    return { total, active, disabled, staff };
  }, [users]);

  // Role update handler
  const handleRoleUpdate = async (userId: number, nextRole: Role) => {
    try {
      const updated = await updateUserRole(userId, nextRole);
      setUsers((prev) => prev.map((u) => (u.id === userId ? updated : u)));
      addToast(
        "success",
        "Role Updated Successfully",
        `${updated.fullName} is now assigned the ${nextRole} role.`
      );
    } catch (err: unknown) {
      const msg = getApiErrorMessage(err, "Failed to update role.");
      addToast("error", "Role Update Failed", msg);
      throw new Error(msg);
    }
  };

  // Status toggle handler
  const handleStatusToggle = async (userId: number, nextStatus: boolean) => {
    try {
      const updated = await toggleUserStatus(userId, nextStatus);
      setUsers((prev) => prev.map((u) => (u.id === userId ? updated : u)));
      addToast(
        "success",
        nextStatus ? "Account Activated" : "Account Deactivated",
        `${updated.fullName}'s account has been ${nextStatus ? "activated" : "disabled"}.`
      );
    } catch (err: unknown) {
      const msg = getApiErrorMessage(err, "Failed to change user status.");
      addToast("error", "Status Toggle Failed", msg);
      throw new Error(msg);
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Toast Feedback */}
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />

      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-ink">
              User Management
            </h1>
            <span className="rounded-md bg-coral/10 px-2 py-0.5 text-xs font-bold text-coral">
              Admin RBAC
            </span>
          </div>
          <p className="mt-1 text-sm text-ink/60">
            Monitor accounts, assign access privileges, and manage security state across the SwiftDrop network.
          </p>
        </div>

        <button
          onClick={() => loadUsers(true)}
          disabled={loading || refreshing}
          className="self-start sm:self-auto inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-white border border-ink/10 text-xs font-semibold text-ink shadow-xs hover:bg-ink/5 transition disabled:opacity-50"
        >
          <RefreshCw size={14} className={refreshing ? "animate-spin text-coral" : ""} />
          <span>{refreshing ? "Refreshing..." : "Refresh"}</span>
        </button>
      </div>

      {/* Summary Metrics Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="rounded-2xl border border-ink/10 bg-white p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-ink/50">
              Total Users
            </span>
            <div className="p-2 rounded-xl bg-ink/5 text-ink">
              <Users size={16} />
            </div>
          </div>
          <p className="mt-2 text-2xl font-bold text-ink">{metrics.total}</p>
          <p className="text-[11px] text-ink/50 mt-0.5">Registered accounts</p>
        </div>

        <div className="rounded-2xl border border-ink/10 bg-white p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-700">
              Active Accounts
            </span>
            <div className="p-2 rounded-xl bg-emerald-50 text-emerald-600">
              <UserCheck size={16} />
            </div>
          </div>
          <p className="mt-2 text-2xl font-bold text-emerald-900">{metrics.active}</p>
          <p className="text-[11px] text-emerald-600/75 mt-0.5">Login permitted</p>
        </div>

        <div className="rounded-2xl border border-ink/10 bg-white p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-rose-700">
              Suspended
            </span>
            <div className="p-2 rounded-xl bg-rose-50 text-rose-600">
              <UserX size={16} />
            </div>
          </div>
          <p className="mt-2 text-2xl font-bold text-rose-900">{metrics.disabled}</p>
          <p className="text-[11px] text-rose-600/75 mt-0.5">Access revoked</p>
        </div>

        <div className="rounded-2xl border border-ink/10 bg-white p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-indigo-700">
              Staff & Fleet
            </span>
            <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600">
              <Shield size={16} />
            </div>
          </div>
          <p className="mt-2 text-2xl font-bold text-indigo-900">{metrics.staff}</p>
          <p className="text-[11px] text-indigo-600/75 mt-0.5">Couriers, dispatch, admins</p>
        </div>
      </div>

      {/* Main Table Card */}
      <div className="rounded-2xl border border-ink/10 bg-white shadow-xs overflow-hidden">
        {/* Toolbar: Search & Role Filter */}
        <div className="p-4 sm:p-5 border-b border-ink/10 bg-mist/30 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
          {/* Search Bar */}
          <div className="relative flex-1 max-w-md">
            <Search
              size={16}
              className="absolute left-3.5 top-1/2 -translate-y-1/2 text-ink/40 pointer-events-none"
            />
            <input
              type="text"
              placeholder="Search by name, email, or user ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 text-sm rounded-xl border border-ink/15 bg-white text-ink placeholder:text-ink/40 outline-none transition focus:border-coral focus:ring-2 focus:ring-coral/20"
            />
          </div>

          {/* Role Filter Dropdown */}
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1.5 text-xs font-medium text-ink/60 shrink-0">
              <Filter size={14} />
              <span>Role:</span>
            </div>
            <select
              value={selectedRoleFilter}
              onChange={(e) => setSelectedRoleFilter(e.target.value as RoleFilterOption)}
              className="rounded-xl border border-ink/15 bg-white px-3 py-2 text-xs font-semibold text-ink outline-none transition focus:border-coral focus:ring-2 focus:ring-coral/20"
            >
              <option value="ALL">All Roles ({users.length})</option>
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r.charAt(0) + r.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Error Banner */}
        {error && (
          <div className="p-4 bg-rose-50 border-b border-rose-200 flex items-center justify-between gap-3 text-sm text-rose-800">
            <div className="flex items-center gap-2">
              <AlertCircle size={18} className="shrink-0 text-rose-600" />
              <span>{error}</span>
            </div>
            <button
              onClick={() => loadUsers()}
              className="text-xs font-bold text-rose-700 underline hover:text-rose-900"
            >
              Retry
            </button>
          </div>
        )}

        {/* Loading State */}
        {loading ? (
          <div className="flex flex-col items-center justify-center py-20 px-4 text-center">
            <div className="w-9 h-9 border-3 border-coral border-t-transparent rounded-full animate-spin mb-3" />
            <p className="text-sm font-medium text-ink/70">Loading user accounts...</p>
          </div>
        ) : filteredUsers.length === 0 ? (
          /* Empty State */
          <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
            <div className="p-3.5 rounded-2xl bg-ink/5 text-ink/40 mb-3">
              <Users size={32} />
            </div>
            <h3 className="text-base font-bold text-ink">No users found</h3>
            <p className="text-xs text-ink/50 max-w-sm mt-1">
              {searchQuery
                ? `No accounts matched your search for "${searchQuery}". Try adjusting your keywords or role filter.`
                : "There are currently no registered users matching this filter criteria."}
            </p>
            {(searchQuery || selectedRoleFilter !== "ALL") && (
              <button
                onClick={() => {
                  setSearchQuery("");
                  setSelectedRoleFilter("ALL");
                }}
                className="mt-4 px-3.5 py-1.5 rounded-xl bg-ink/5 text-xs font-semibold text-ink hover:bg-ink/10 transition"
              >
                Clear all filters
              </button>
            )}
          </div>
        ) : (
          <>
            {/* Desktop Table View */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-left text-xs text-ink/70">
                <thead className="bg-mist/60 text-ink/50 uppercase tracking-wider text-[10px] font-bold border-b border-ink/10">
                  <tr>
                    <th className="py-3 px-4">ID</th>
                    <th className="py-3 px-4">User</th>
                    <th className="py-3 px-4">Provider</th>
                    <th className="py-3 px-4">Role</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink/5">
                  {filteredUsers.map((u) => {
                    const isSelf = currentUser?.email?.toLowerCase() === u.email?.toLowerCase();
                    return (
                      <tr
                        key={u.id}
                        className={`hover:bg-mist/40 transition-colors ${
                          !u.enabled ? "bg-rose-50/20" : ""
                        }`}
                      >
                        {/* ID */}
                        <td className="py-3.5 px-4 font-mono text-[11px] text-ink/50">
                          #{u.id}
                        </td>

                        {/* User Identity */}
                        <td className="py-3.5 px-4">
                          <div className="flex items-center gap-3">
                            <div className="h-8 w-8 rounded-full bg-ink/5 border border-ink/10 flex items-center justify-center font-bold text-xs text-ink shrink-0">
                              {u.fullName?.charAt(0)?.toUpperCase() || "U"}
                            </div>
                            <div className="min-w-0">
                              <div className="flex items-center gap-1.5">
                                <span className="font-semibold text-ink truncate text-sm">
                                  {u.fullName}
                                </span>
                                {isSelf && (
                                  <span className="rounded bg-coral/15 px-1.5 py-0.2 text-[10px] font-bold text-coral">
                                    You
                                  </span>
                                )}
                              </div>
                              <p className="text-ink/50 text-[11px] truncate">{u.email}</p>
                              {u.phoneNumber && (
                                <p className="text-ink/40 text-[10px]">{u.phoneNumber}</p>
                              )}
                            </div>
                          </div>
                        </td>

                        {/* Auth Provider */}
                        <td className="py-3.5 px-4">
                          <div className="inline-flex items-center gap-1 text-[11px] font-medium text-ink/60">
                            {u.provider === "GOOGLE" ? (
                              <>
                                <span className="font-bold text-sky-600">G</span>
                                <span>Google</span>
                              </>
                            ) : (
                              <>
                                <Key size={12} className="text-ink/40" />
                                <span>Password</span>
                              </>
                            )}
                          </div>
                        </td>

                        {/* Role Badge */}
                        <td className="py-3.5 px-4">
                          <RoleBadge role={u.role} />
                        </td>

                        {/* Status Badge */}
                        <td className="py-3.5 px-4">
                          <StatusBadge enabled={u.enabled} />
                        </td>

                        {/* Actions */}
                        <td className="py-3.5 px-4 text-right">
                          <div className="inline-flex items-center gap-1.5">
                            {/* Change Role Button */}
                            <button
                              onClick={() => setRoleModalUser(u)}
                              className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-ink/10 bg-white hover:bg-ink/5 text-ink font-semibold text-[11px] transition shadow-xs"
                              title="Update user role"
                            >
                              <UserCog size={13} className="text-ink/60" />
                              <span>Role</span>
                            </button>

                            {/* Status Toggle Button */}
                            <button
                              onClick={() => setStatusModalUser(u)}
                              disabled={isSelf && u.enabled}
                              className={`inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg font-semibold text-[11px] transition shadow-xs ${
                                u.enabled
                                  ? isSelf
                                    ? "bg-ink/5 text-ink/30 cursor-not-allowed border border-ink/10"
                                    : "border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100"
                                  : "border border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
                              }`}
                              title={
                                isSelf && u.enabled
                                  ? "Cannot disable your own active account"
                                  : u.enabled
                                  ? "Disable user account"
                                  : "Activate user account"
                              }
                            >
                              <Power size={13} />
                              <span>{u.enabled ? "Disable" : "Enable"}</span>
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Mobile Card View (Hidden on md+) */}
            <div className="md:hidden divide-y divide-ink/10">
              {filteredUsers.map((u) => {
                const isSelf = currentUser?.email?.toLowerCase() === u.email?.toLowerCase();
                return (
                  <div key={u.id} className="p-4 space-y-3">
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center gap-2.5">
                        <div className="h-9 w-9 rounded-full bg-ink/5 border border-ink/10 flex items-center justify-center font-bold text-sm text-ink shrink-0">
                          {u.fullName?.charAt(0)?.toUpperCase() || "U"}
                        </div>
                        <div>
                          <div className="flex items-center gap-1.5">
                            <h4 className="font-semibold text-ink text-sm">{u.fullName}</h4>
                            {isSelf && (
                              <span className="rounded bg-coral/15 px-1.5 py-0.2 text-[10px] font-bold text-coral">
                                You
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-ink/50">{u.email}</p>
                        </div>
                      </div>
                      <span className="font-mono text-xs text-ink/40">#{u.id}</span>
                    </div>

                    <div className="flex flex-wrap items-center gap-2 pt-1">
                      <RoleBadge role={u.role} size="sm" />
                      <StatusBadge enabled={u.enabled} size="sm" />
                      <span className="text-[11px] text-ink/50 ml-auto">
                        Provider: <strong>{u.provider}</strong>
                      </span>
                    </div>

                    <div className="flex items-center gap-2 pt-2 border-t border-ink/5">
                      <button
                        onClick={() => setRoleModalUser(u)}
                        className="flex-1 flex items-center justify-center gap-1.5 py-2 rounded-xl border border-ink/15 text-xs font-semibold text-ink hover:bg-ink/5"
                      >
                        <UserCog size={14} />
                        Change Role
                      </button>
                      <button
                        onClick={() => setStatusModalUser(u)}
                        disabled={isSelf && u.enabled}
                        className={`flex-1 flex items-center justify-center gap-1.5 py-2 rounded-xl text-xs font-semibold ${
                          u.enabled
                            ? isSelf
                              ? "bg-ink/5 text-ink/30 cursor-not-allowed"
                              : "bg-rose-50 text-rose-700 hover:bg-rose-100"
                            : "bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
                        }`}
                      >
                        <Power size={14} />
                        {u.enabled ? "Disable Account" : "Enable Account"}
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )}

        {/* Footer info banner */}
        <div className="p-4 border-t border-ink/10 bg-mist/20 text-xs text-ink/50 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>
            Showing <strong>{filteredUsers.length}</strong> of <strong>{users.length}</strong> accounts
          </span>
          <div className="flex items-center gap-1 text-[11px]">
            <ShieldAlert size={13} className="text-ink/40" />
            <span>Admin audit logging enabled for all role & status mutations</span>
          </div>
        </div>
      </div>

      {/* Role Update Modal */}
      <RoleUpdateModal
        user={roleModalUser}
        isOpen={Boolean(roleModalUser)}
        onClose={() => setRoleModalUser(null)}
        onSave={handleRoleUpdate}
        isCurrentUser={
          Boolean(
            roleModalUser &&
              currentUser?.email?.toLowerCase() === roleModalUser.email?.toLowerCase()
          )
        }
      />

      {/* Status Toggle Confirmation Modal */}
      <StatusConfirmModal
        user={statusModalUser}
        isOpen={Boolean(statusModalUser)}
        onClose={() => setStatusModalUser(null)}
        onConfirm={handleStatusToggle}
        isCurrentUser={
          Boolean(
            statusModalUser &&
              currentUser?.email?.toLowerCase() === statusModalUser.email?.toLowerCase()
          )
        }
      />
    </div>
  );
};

export default AdminUsersPage;
