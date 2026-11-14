import React, { useEffect, useState, useCallback } from "react";
import { Link } from "react-router-dom";
import {
  ArrowUpRight,
  Clock3,
  Store,
  Truck,
  Users,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RefreshCw,
  ExternalLink,
} from "lucide-react";
import { fetchAdminUsers, approveUser, rejectUser } from "../admin.api";
import type { AdminUser } from "../admin.types";
import { RoleBadge } from "../components/RoleBadge";
import { StatusBadge } from "../components/StatusBadge";

export const AdminOverviewPage: React.FC = () => {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<Record<number, "approve" | "reject" | null>>({});
  const [banner, setBanner] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const loadData = useCallback(async (isSilentRefresh = false) => {
    if (isSilentRefresh) {
      setIsRefreshing(true);
    } else {
      setIsLoading(true);
    }
    setError(null);

    try {
      const fetchedUsers = await fetchAdminUsers();
      setUsers(fetchedUsers);
    } catch {
      setError("Unable to load live system metrics. Please check server connectivity.");
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Derived live KPI metrics
  const totalUsersCount = users.length;
  const pendingApprovals = users.filter(
    (u) =>
      u.status === "PENDING_APPROVAL" ||
      (!u.enabled && (u.role === "MERCHANT" || u.role === "COURIER"))
  );
  const activeMerchantsCount = users.filter((u) => u.role === "MERCHANT" && u.enabled).length;
  const activeCouriersCount = users.filter((u) => u.role === "COURIER" && u.enabled).length;
  const customersCount = users.filter((u) => u.role === "CUSTOMER").length;
  const dispatchersCount = users.filter((u) => u.role === "DISPATCHER").length;

  // Inline approval handler (Strictly NO modals)
  const handleInlineApprove = async (targetUser: AdminUser) => {
    setActionLoading((prev) => ({ ...prev, [targetUser.id]: "approve" }));
    setBanner(null);
    try {
      const updated = await approveUser(targetUser.id);
      setUsers((prev) => prev.map((u) => (u.id === targetUser.id ? updated : u)));
      setBanner({
        type: "success",
        text: `Account for ${targetUser.fullName || targetUser.email} has been approved.`,
      });
    } catch {
      setBanner({
        type: "error",
        text: `Failed to approve account for ${targetUser.fullName || targetUser.email}. Please try again.`,
      });
    } finally {
      setActionLoading((prev) => ({ ...prev, [targetUser.id]: null }));
    }
  };

  // Inline reject handler (Strictly NO modals)
  const handleInlineReject = async (targetUser: AdminUser) => {
    setActionLoading((prev) => ({ ...prev, [targetUser.id]: "reject" }));
    setBanner(null);
    try {
      const updated = await rejectUser(targetUser.id);
      setUsers((prev) => prev.map((u) => (u.id === targetUser.id ? updated : u)));
      setBanner({
        type: "success",
        text: `Account for ${targetUser.fullName || targetUser.email} has been rejected.`,
      });
    } catch {
      setBanner({
        type: "error",
        text: `Failed to reject account for ${targetUser.fullName || targetUser.email}. Please try again.`,
      });
    } finally {
      setActionLoading((prev) => ({ ...prev, [targetUser.id]: null }));
    }
  };

  // Standard SwiftDrop metric card definitions (matching DashboardPage exactly)
  const metricCards = [
    {
      label: "Total accounts",
      value: isLoading ? "—" : totalUsersCount.toString(),
      hint: `${customersCount} customers · ${dispatchersCount} dispatchers`,
      icon: Users,
    },
    {
      label: "Pending approvals",
      value: isLoading ? "—" : pendingApprovals.length.toString(),
      hint: pendingApprovals.length > 0 ? "Awaiting admin verification" : "All partners verified",
      icon: Clock3,
    },
    {
      label: "Active merchants",
      value: isLoading ? "—" : activeMerchantsCount.toString(),
      hint: "Verified store integrations",
      icon: Store,
    },
    {
      label: "Active couriers",
      value: isLoading ? "—" : activeCouriersCount.toString(),
      hint: "Ready for automated dispatch",
      icon: Truck,
    },
  ];

  return (
    <div>
      {/* 1. Standard Dashboard Header (Matches DashboardPage layout & typography) */}
      <div className="mb-8 flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
        <div>
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.2em] text-coral">
            System overview
          </p>
          <h1 className="font-display text-4xl font-bold tracking-tight sm:text-5xl">
            Good morning, operator.
          </h1>
          <p className="mt-3 max-w-xl text-ink/55">
            Manage people, policies, and the operational health of the SwiftDrop network.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => loadData(true)}
            disabled={isLoading || isRefreshing}
            className="flex items-center justify-center gap-2 rounded-xl border border-ink/10 bg-white px-4 py-3 text-sm font-semibold text-ink/70 transition hover:border-coral/40 hover:text-ink disabled:opacity-50"
            title="Refresh dashboard metrics"
          >
            <RefreshCw size={16} className={isRefreshing ? "animate-spin text-coral" : ""} />
            <span>{isRefreshing ? "Refreshing..." : "Refresh"}</span>
          </button>
          <Link
            to="/admin/users"
            className="flex items-center justify-center gap-2 rounded-xl bg-coral px-4 py-3 font-semibold text-white shadow-lg shadow-coral/20"
          >
            Manage users
            <ArrowUpRight size={17} />
          </Link>
        </div>
      </div>

      {/* Notifications / Alerts */}
      {banner && (
        <div
          className={`mb-6 flex items-center justify-between rounded-xl border p-4 text-sm font-medium ${
            banner.type === "success"
              ? "border-emerald-200 bg-emerald-50 text-emerald-900"
              : "border-coral/20 bg-coral/10 text-coral"
          }`}
        >
          <div className="flex items-center gap-2.5">
            {banner.type === "success" ? <CheckCircle2 size={18} /> : <XCircle size={18} />}
            <span>{banner.text}</span>
          </div>
          <button
            onClick={() => setBanner(null)}
            className="text-xs font-bold uppercase tracking-wider opacity-70 hover:opacity-100"
          >
            Dismiss
          </button>
        </div>
      )}

      {error && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
          <AlertTriangle size={18} className="shrink-0 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {/* 2. KPI Cards Grid (Matches DashboardPage card style exactly) */}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {metricCards.map(({ label, value, hint, icon: Icon }) => (
          <div key={label} className="rounded-2xl border border-ink/10 bg-white p-5">
            <div className="mb-6 flex justify-between text-ink/45">
              <span className="text-sm">{label}</span>
              <Icon size={18} />
            </div>
            <p className="font-display text-4xl font-bold text-ink">{value}</p>
            <p className="mt-1 text-xs text-ink/40">{hint}</p>
          </div>
        ))}
      </div>

      {/* 3. Partner Review Section (Full Width, NO Modals) */}
      <section className="mt-6 rounded-2xl border border-ink/10 bg-white p-6">
        <div className="flex items-center justify-between border-b border-ink/10 pb-4">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">Partner review</p>
            <h2 className="mt-1 font-display text-2xl font-bold text-ink">
              Pending registration approvals
            </h2>
          </div>
          <Link
            to="/admin/users"
            className="flex items-center gap-1.5 text-xs font-semibold text-coral hover:underline"
          >
            <span>View all ({totalUsersCount})</span>
            <ArrowUpRight size={14} />
          </Link>
        </div>

        {isLoading ? (
          <div className="space-y-3 py-8">
            {[1, 2, 3].map((n) => (
              <div key={n} className="h-12 w-full animate-pulse rounded-xl bg-ink/5" />
            ))}
          </div>
        ) : pendingApprovals.length === 0 ? (
          <div className="py-12 text-center">
            <div className="mx-auto mb-3 grid h-12 w-12 place-items-center rounded-full bg-emerald-50 text-emerald-600">
              <CheckCircle2 size={24} />
            </div>
            <h3 className="font-display text-base font-bold text-ink">All Partners Verified</h3>
            <p className="mt-1 max-w-sm mx-auto text-xs text-ink/50">
              There are currently no Merchant or Courier registrations waiting for admin approval.
            </p>
            <Link
              to="/admin/users"
              className="mt-4 inline-flex items-center gap-1.5 rounded-xl border border-ink/10 bg-white px-3.5 py-2 text-xs font-semibold text-ink shadow-2xs hover:bg-mist"
            >
              Browse full user directory &rarr;
            </Link>
          </div>
        ) : (
          <div className="overflow-x-auto pt-2">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-ink/5 text-[11px] font-bold uppercase tracking-wider text-ink/40">
                  <th className="py-3 px-2">Partner</th>
                  <th className="py-3 px-2">Role</th>
                  <th className="py-3 px-2">Provider</th>
                  <th className="py-3 px-2">Status</th>
                  <th className="py-3 px-2 text-right">Inline Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-y-ink/5">
                {pendingApprovals.map((pUser) => {
                  const isApproveLoading = actionLoading[pUser.id] === "approve";
                  const isRejectLoading = actionLoading[pUser.id] === "reject";
                  const isBusy = isApproveLoading || isRejectLoading;

                  return (
                    <tr key={pUser.id} className="transition hover:bg-mist/50">
                      <td className="py-3.5 px-2">
                        <Link
                          to={`/admin/users/${pUser.id}/review`}
                          className="font-semibold text-ink hover:text-coral transition"
                          title="Click to view full application details"
                        >
                          {pUser.fullName || "Unspecified Name"}
                        </Link>
                        <div className="font-mono text-xs text-ink/50">{pUser.email}</div>
                      </td>

                      <td className="py-3.5 px-2">
                        <RoleBadge role={pUser.role} size="sm" />
                      </td>

                      <td className="py-3.5 px-2">
                        <span className="rounded-md bg-ink/5 px-2 py-0.5 text-xs font-medium uppercase tracking-wider text-ink/65">
                          {pUser.provider}
                        </span>
                      </td>

                      <td className="py-3.5 px-2">
                        <StatusBadge enabled={pUser.enabled} status={pUser.status} size="sm" />
                      </td>

                      <td className="py-3.5 px-2 text-right">
                        <div className="inline-flex items-center gap-2">
                          <Link
                            to={`/admin/users/${pUser.id}/review`}
                            className="inline-flex items-center gap-1 rounded-lg border border-ink/10 bg-white px-2.5 py-1.5 text-xs font-semibold text-ink transition hover:border-coral hover:text-coral shadow-xs"
                            title="Inspect full registration documents & fleet details"
                          >
                            <ExternalLink size={13} />
                            <span>Review</span>
                          </Link>

                          <button
                            onClick={() => handleInlineApprove(pUser)}
                            disabled={isBusy}
                            className="inline-flex items-center gap-1 rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-bold text-white shadow-xs transition hover:bg-emerald-700 disabled:opacity-50"
                          >
                            <CheckCircle2 size={13} />
                            <span>{isApproveLoading ? "Approving..." : "Approve"}</span>
                          </button>

                          <button
                            onClick={() => handleInlineReject(pUser)}
                            disabled={isBusy}
                            className="inline-flex items-center gap-1 rounded-lg border border-ink/10 bg-white px-2.5 py-1.5 text-xs font-semibold text-rose-600 transition hover:border-rose-200 hover:bg-rose-50 disabled:opacity-50"
                          >
                            <XCircle size={13} />
                            <span>{isRejectLoading ? "Rejecting..." : "Reject"}</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
};

export default AdminOverviewPage;
