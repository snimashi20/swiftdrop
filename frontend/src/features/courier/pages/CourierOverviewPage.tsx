import {
  AlertTriangle,
  ArrowUpRight,
  CheckCircle2,
  Clock3,
  MapPin,
  Package,
  Phone,
  RefreshCw,
  Route,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getTodayDeliveries } from "../api";
import type { CourierDelivery } from "../types";

export function CourierOverviewPage() {
  const [deliveries, setDeliveries] = useState<CourierDelivery[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = async () => {
    try {
      setLoading(true);
      setError("");
      const data = await getTodayDeliveries();
      setDeliveries(data);
    } catch {
      setError("Could not load today's route summary.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const totalCount = deliveries.length;
  const deliveredCount = deliveries.filter((d) => d.status === "DELIVERED").length;
  const failedCount = deliveries.filter((d) => d.status === "FAILED").length;
  const activeDeliveries = deliveries.filter(
    (d) => d.status !== "DELIVERED" && d.status !== "FAILED"
  );
  const outForDeliveryCount = deliveries.filter((d) => d.status === "OUT_FOR_DELIVERY").length;
  const completionRate =
    totalCount > 0 ? `${Math.round((deliveredCount / totalCount) * 100)}%` : "0%";
  const nextStop = activeDeliveries[0] ?? null;

  const kpis = [
    {
      label: "Active parcels",
      value: loading ? "…" : String(activeDeliveries.length),
      hint: `${outForDeliveryCount} out for delivery now`,
      icon: Package,
      to: "/courier/today?filter=ACTIVE",
    },
    {
      label: "Delivered today",
      value: loading ? "…" : String(deliveredCount),
      hint: `${completionRate} completion rate`,
      icon: CheckCircle2,
      to: "/courier/today?filter=DELIVERED",
    },
    {
      label: "Failed / exceptions",
      value: loading ? "…" : String(failedCount),
      hint: failedCount > 0 ? "Re-attempt available" : "No delivery issues",
      icon: AlertTriangle,
      to: "/courier/today?filter=FAILED",
    },
    {
      label: "Total stops today",
      value: loading ? "…" : String(totalCount),
      hint: "Assigned to your route",
      icon: Route,
      to: "/courier/today?filter=ALL",
    },
  ];

  return (
    <div className="font-sans text-ink">
      <div className="mb-8 flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
        <div>
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.2em] text-coral">
            Courier route
          </p>
          <h1 className="font-display text-4xl font-bold tracking-tight sm:text-5xl">
            Ready for the road?
          </h1>
          <p className="mt-3 max-w-xl text-ink/55">
            Live overview of your assigned deliveries, completion progress, and next drop-off stop.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={load}
            disabled={loading}
            className="inline-flex items-center gap-2 rounded-xl border border-ink/15 bg-white px-4 py-3 text-sm font-semibold text-ink transition hover:bg-mist disabled:opacity-50"
          >
            <RefreshCw size={15} className={loading ? "animate-spin" : ""} />
            Refresh
          </button>
          <Link
            to="/courier/today?filter=ALL"
            className="flex items-center justify-center gap-2 rounded-xl bg-coral px-5 py-3 font-semibold text-white shadow-lg shadow-coral/20 transition hover:bg-coral/90"
          >
            View route <ArrowUpRight size={17} />
          </Link>
        </div>
      </div>

      {error && (
        <div className="mb-6 rounded-2xl border border-coral/20 bg-coral/10 px-4 py-3 text-sm font-medium text-coral">
          {error}
        </div>
      )}

      {/* Clickable Live KPI Cards */}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {kpis.map(({ label, value, hint, icon: Icon, to }) => (
          <Link
            key={label}
            to={to}
            className="group block rounded-2xl border border-ink/10 bg-white p-5 shadow-xs transition hover:border-coral/40 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-coral/30"
          >
            <div className="mb-6 flex justify-between text-ink/45">
              <span className="text-sm font-medium group-hover:text-ink transition">{label}</span>
              <Icon size={18} className="text-coral" />
            </div>
            <p className="font-display text-4xl font-bold">{value}</p>
            <div className="mt-2 flex items-center justify-between gap-2">
              <p className="text-xs text-ink/50">{hint}</p>
              <ArrowUpRight
                size={14}
                className="shrink-0 text-ink/30 transition group-hover:translate-x-0.5 group-hover:-translate-y-0.5 group-hover:text-coral"
              />
            </div>
          </Link>
        ))}
      </div>

      {/* Next Delivery & Route Summary */}
      <div className="mt-6 grid gap-6 lg:grid-cols-[1.4fr_0.6fr]">
        <section className="rounded-2xl border border-ink/10 bg-ink p-6 text-white">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-widest text-white/50">
                Next stop on route
              </p>
              <h2 className="mt-1 font-display text-2xl">
                {nextStop ? nextStop.trackingNumber : "All caught up!"}
              </h2>
            </div>
            <span className="rounded-full bg-mint px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-ink">
              {nextStop ? nextStop.status.replace(/_/g, " ") : "Standby"}
            </span>
          </div>

          {nextStop ? (
            <div className="mt-6 space-y-3 border-t border-white/10 pt-5 text-sm">
              <p className="font-semibold text-white">{nextStop.recipientName}</p>
              <p className="flex items-start gap-2 text-white/75">
                <MapPin size={16} className="mt-0.5 shrink-0 text-coral" />
                <span>{nextStop.deliveryAddress}</span>
              </p>
              <div className="flex flex-wrap items-center gap-3 pt-2">
                <a
                  href={`tel:${nextStop.recipientPhone}`}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-white/10 px-3.5 py-2 text-xs font-semibold text-white hover:bg-white/20"
                >
                  <Phone size={14} /> Call {nextStop.recipientPhone}
                </a>
                <Link
                  to="/courier/today?filter=ACTIVE"
                  className="inline-flex items-center gap-1.5 rounded-xl bg-coral px-4 py-2 text-xs font-semibold text-white hover:bg-coral/90"
                >
                  Update status / Deliver <ArrowUpRight size={14} />
                </Link>
              </div>
            </div>
          ) : (
            <div className="mt-10 border-t border-white/10 pt-4 text-sm text-white/55">
              No pending deliveries right now. New assignments from dispatch will appear here automatically.
            </div>
          )}
        </section>

        <section className="rounded-2xl border border-ink/10 bg-white p-6">
          <div className="flex items-center gap-2">
            <Clock3 size={18} className="text-coral" />
            <h2 className="font-semibold">Shift Progress</h2>
          </div>
          <div className="mt-5 space-y-3 text-sm">
            <div className="flex justify-between">
              <span className="text-ink/60">Completed</span>
              <span className="font-bold text-ink">{deliveredCount} / {totalCount}</span>
            </div>
            <div className="h-2.5 w-full overflow-hidden rounded-full bg-mist">
              <div
                className="h-full rounded-full bg-coral transition-all"
                style={{ width: completionRate }}
              />
            </div>
            <p className="pt-2 text-xs leading-5 text-ink/55">
              Open <strong>Today&apos;s Deliveries</strong> to scan/update statuses, upload proof-of-delivery photos, or log delivery exceptions.
            </p>
          </div>
        </section>
      </div>
    </div>
  );
}