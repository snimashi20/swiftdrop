import React, { useEffect, useState, useCallback } from "react";
import { Link } from "react-router-dom";
import {
  ArrowUpRight,
  Package,
  Clock3,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Search,
  Plus,
  ArrowRight,
  MapPin,
  FileText,
  DollarSign,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { listParcels, STATUS_LABELS } from "../parcels/parcels.api";
import type { ParcelDetail, ParcelStatus } from "../parcels/parcels.api";
import { getApiErrorMessage } from "../../lib/api";

const statusBadgeClasses: Record<ParcelStatus, string> = {
  CREATED: "bg-ink/10 text-ink",
  PICKED_UP: "bg-mint text-ink",
  IN_TRANSIT: "bg-mint text-ink",
  OUT_FOR_DELIVERY: "bg-mint text-ink",
  DELIVERED: "bg-ink text-white",
  FAILED: "bg-coral text-white",
  CANCELLED: "bg-coral/15 text-coral",
};

export const CustomerOverviewPage: React.FC = () => {
  const { user } = useAuth();
  const [parcels, setParcels] = useState<ParcelDetail[]>([]);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [activeCount, setActiveCount] = useState<number>(0);
  const [deliveredCount, setDeliveredCount] = useState<number>(0);
  const [totalSpent, setTotalSpent] = useState<number>(0);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async (isSilentRefresh = false) => {
    if (isSilentRefresh) {
      setIsRefreshing(true);
    } else {
      setIsLoading(true);
    }
    setError(null);

    try {
      // Fetch latest page (defaults to 10 parcels per page)
      const res = await listParcels(0);
      const items = res?.content ?? [];
      const total = res?.totalElements ?? items.length;

      setParcels(items);
      setTotalElements(total);

      // Derive metrics from recent parcels & total
      const activeStatuses: ParcelStatus[] = ["CREATED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY"];
      const active = items.filter((p) => activeStatuses.includes(p.status)).length;
      const delivered = items.filter((p) => p.status === "DELIVERED").length;
      const spent = items.reduce((sum, p) => sum + (p.fee || 0), 0);

      setActiveCount(active);
      setDeliveredCount(delivered);
      setTotalSpent(spent);
    } catch (err) {
      setError(getApiErrorMessage(err, "Unable to load your delivery overview. Please try again."));
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const recentParcels = parcels.slice(0, 5);

  const metricCards = [
    {
      label: "Total parcels",
      value: isLoading ? "—" : totalElements.toString(),
      hint: totalElements === 1 ? "1 shipment recorded" : `${totalElements} shipments recorded`,
      icon: Package,
    },
    {
      label: "Active now",
      value: isLoading ? "—" : activeCount.toString(),
      hint: activeCount > 0 ? "In transit (latest 10)" : "No parcels in transit",
      icon: Clock3,
    },
    {
      label: "Recently delivered",
      value: isLoading ? "—" : deliveredCount.toString(),
      hint: "Completed in last 10",
      icon: CheckCircle2,
    },
    {
      label: "Spent on recent shipments",
      value: isLoading ? "—" : `LKR ${totalSpent.toLocaleString()}`,
      hint: "Across latest 10 parcels",
      icon: DollarSign,
    },
  ];

  return (
    <div className="space-y-6 font-sans text-ink">
      {/* 1. Header Section */}
      <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
        <div>
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.2em] text-coral">
            Customer workspace
          </p>
          <h1 className="font-display text-4xl font-bold tracking-tight sm:text-5xl">
            {user?.name ? `Hello, ${user.name.split(" ")[0]}.` : "Welcome back."}
          </h1>
          <p className="mt-3 max-w-xl text-ink/55">
            Track your deliveries, check live parcel progress, and book new pickups anytime.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <button
            onClick={() => loadData(true)}
            disabled={isLoading || isRefreshing}
            className="inline-flex items-center justify-center gap-2 rounded-xl border border-ink/10 bg-white px-4 py-3 text-sm font-semibold text-ink transition hover:bg-mist disabled:opacity-50"
            title="Refresh overview metrics"
            aria-label="Refresh overview"
          >
            <RefreshCw size={16} className={isRefreshing ? "animate-spin text-coral" : ""} />
            <span>Refresh</span>
          </button>

          <Link
            to="/parcels/start"
            className="flex items-center justify-center gap-2 rounded-xl bg-coral px-4 py-3 font-semibold text-white shadow-lg shadow-coral/20 transition hover:bg-coral/90"
          >
            <span>Send a parcel</span>
            <ArrowUpRight size={17} />
          </Link>
        </div>
      </div>

      {/* Error message banner */}
      {error && (
        <div
          role="alert"
          className="flex items-center gap-3 rounded-xl border border-coral/30 bg-coral/10 p-4 text-sm text-coral"
        >
          <AlertCircle size={18} className="shrink-0" />
          <p className="font-medium">{error}</p>
        </div>
      )}

      {/* 2. KPI Summary Cards */}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {metricCards.map(({ label, value, hint, icon: Icon }) => (
          <div key={label} className="rounded-2xl border border-ink/10 bg-white p-5 shadow-sm">
            <div className="mb-5 flex items-center justify-between text-ink/45">
              <span className="text-sm font-medium">{label}</span>
              <Icon size={18} className="text-coral" />
            </div>
            <p className="font-display text-4xl font-bold">
              {isLoading ? (
                <span className="inline-block h-9 w-16 animate-pulse rounded bg-ink/10" />
              ) : (
                value
              )}
            </p>
            <p className="mt-1 text-xs text-ink/45">{hint}</p>
          </div>
        ))}
      </div>

      {/* 3. Main Content: Recent Deliveries & Quick Actions */}
      <div className="grid gap-6 lg:grid-cols-[1.6fr_0.9fr]">
        {/* Recent Parcels Section */}
        <section className="rounded-2xl border border-ink/10 bg-white p-5 sm:p-6 shadow-sm">
          <div className="flex items-center justify-between border-b border-ink/10 pb-4">
            <div>
              <h2 className="font-display text-2xl font-bold">Recent shipments</h2>
              <p className="mt-1 text-sm text-ink/50">Your latest parcel orders and status updates</p>
            </div>
            <Link
              to="/parcels"
              className="inline-flex items-center gap-1.5 text-sm font-semibold text-coral hover:underline"
            >
              <span>View all</span>
              <ArrowRight size={16} />
            </Link>
          </div>

          {isLoading ? (
            <div className="space-y-3 pt-4">
              {[1, 2, 3].map((i) => (
                <div
                  key={i}
                  className="flex animate-pulse items-center justify-between rounded-xl border border-ink/5 bg-mist/60 p-4"
                >
                  <div className="space-y-2">
                    <div className="h-4 w-28 rounded bg-ink/10" />
                    <div className="h-3 w-44 rounded bg-ink/5" />
                  </div>
                  <div className="h-6 w-20 rounded-full bg-ink/10" />
                </div>
              ))}
            </div>
          ) : recentParcels.length === 0 ? (
            /* Empty State */
            <div className="grid h-64 place-items-center text-center">
              <div className="max-w-xs space-y-3">
                <div className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-coral/10 text-coral">
                  <Package size={24} />
                </div>
                <h3 className="font-display text-lg font-bold">No parcels booked yet</h3>
                <p className="text-sm text-ink/50">
                  Ready to send your first item? Book now and track its route live across the island.
                </p>
                <div className="pt-2">
                  <Link
                    to="/parcels/start"
                    className="inline-flex items-center gap-2 rounded-xl bg-coral px-4 py-2.5 text-sm font-semibold text-white shadow-md shadow-coral/20 hover:bg-coral/90"
                  >
                    <Plus size={16} />
                    <span>Book your first delivery</span>
                  </Link>
                </div>
              </div>
            </div>
          ) : (
            /* List of Recent Parcels */
            <div className="divide-y divide-ink/10 pt-2">
              {recentParcels.map((parcel) => {
                const dateStr = new Date(parcel.createdAt).toLocaleDateString("en-GB", {
                  day: "numeric",
                  month: "short",
                  year: "numeric",
                });
                const statusLabel = STATUS_LABELS[parcel.status] || parcel.status;
                const badgeStyle = statusBadgeClasses[parcel.status] || "bg-ink/10 text-ink";

                return (
                  <div
                    key={parcel.id}
                    className="flex flex-col justify-between gap-3 py-4 sm:flex-row sm:items-center"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <Link
                          to={`/parcels/${parcel.trackingNumber}`}
                          className="font-mono text-sm font-bold text-coral hover:underline"
                        >
                          {parcel.trackingNumber}
                        </Link>
                        <span
                          className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${badgeStyle}`}
                        >
                          {statusLabel}
                        </span>
                      </div>
                      <p className="text-xs text-ink/55">
                        <span className="font-medium text-ink">To:</span> {parcel.receiverName} ·{" "}
                        {parcel.dropoff.city}, {parcel.dropoff.district}
                      </p>
                    </div>

                    <div className="flex items-center justify-between gap-4 sm:justify-end">
                      <div className="text-right">
                        <p className="text-xs font-medium text-ink/50">{dateStr}</p>
                        <p className="text-xs font-bold text-ink">
                          LKR {parcel.fee ? parcel.fee.toLocaleString() : "—"}
                        </p>
                      </div>
                      <Link
                        to={`/parcels/${parcel.trackingNumber}`}
                        className="inline-flex items-center justify-center rounded-lg border border-ink/10 bg-white p-2 text-ink/70 transition hover:bg-ink hover:text-white"
                        title="View tracking and delivery details"
                      >
                        <ArrowUpRight size={16} />
                      </Link>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>

        {/* Quick Actions & Help Aside */}
        <aside className="space-y-6">
          {/* Quick Track Card */}
          <div className="rounded-2xl border border-ink/10 bg-ink p-6 text-white shadow-sm">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-xs font-bold uppercase tracking-wider text-coral">Direct lookup</p>
                <h3 className="mt-1 font-display text-2xl font-bold">Track a parcel</h3>
              </div>
              <div className="grid h-10 w-10 place-items-center rounded-xl bg-white/10 text-mint">
                <Search size={20} />
              </div>
            </div>
            <p className="mt-3 text-sm leading-relaxed text-white/65">
              Have a tracking number handy? Check real-time road dispatch and delivery coordinates.
            </p>
            <div className="mt-5">
              <Link
                to="/track"
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-coral px-4 py-2.5 text-sm font-semibold text-white shadow-md shadow-coral/30 transition hover:bg-coral/90"
              >
                <span>Open tracking page</span>
                <ArrowRight size={15} />
              </Link>
            </div>
          </div>

          {/* Customer Helpful Shortcuts */}
          <div className="rounded-2xl border border-ink/10 bg-white p-6 shadow-sm">
            <h3 className="font-display text-lg font-bold">Quick actions</h3>
            <p className="mt-1 text-xs text-ink/50">Manage your address preferences and parcel bookings</p>

            <div className="mt-4 space-y-2">
              <Link
                to="/parcels/start"
                className="flex items-center justify-between rounded-xl border border-ink/5 bg-mist/60 p-3 text-sm font-semibold text-ink transition hover:bg-mist hover:text-coral"
              >
                <div className="flex items-center gap-2.5">
                  <Plus size={16} className="text-coral" />
                  <span>Send a new parcel</span>
                </div>
                <ArrowUpRight size={15} className="text-ink/40" />
              </Link>

              <Link
                to="/profile/addresses"
                className="flex items-center justify-between rounded-xl border border-ink/5 bg-mist/60 p-3 text-sm font-semibold text-ink transition hover:bg-mist hover:text-coral"
              >
                <div className="flex items-center gap-2.5">
                  <MapPin size={16} className="text-coral" />
                  <span>Saved address book</span>
                </div>
                <ArrowUpRight size={15} className="text-ink/40" />
              </Link>

              <Link
                to="/parcels"
                className="flex items-center justify-between rounded-xl border border-ink/5 bg-mist/60 p-3 text-sm font-semibold text-ink transition hover:bg-mist hover:text-coral"
              >
                <div className="flex items-center gap-2.5">
                  <FileText size={16} className="text-coral" />
                  <span>Shipment history & receipts</span>
                </div>
                <ArrowUpRight size={15} className="text-ink/40" />
              </Link>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
};

