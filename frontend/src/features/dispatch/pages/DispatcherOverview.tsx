import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowUpRight, CircleCheck, Package, Truck, Users } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { dispatchApi } from "../api";
import { assignedParcelsApi } from "../assigned";
import type { AssignedParcel } from "../assigned";
import type { CourierWorkload, Page, ReportSummary } from "../types";
import { vehicleLabel } from "../vehicles";

const REFRESH_MS = 60_000;

// Same colours as the Reports page, so the two charts look alike.
const COLORS = { delivered: "#22C55E", failed: "#EF4444", cancelled: "#6B7280" };

const STAGES = [
  { status: "CREATED", label: "Waiting for pickup" },
  { status: "PICKED_UP", label: "Picked up" },
  { status: "IN_TRANSIT", label: "In transit" },
  { status: "OUT_FOR_DELIVERY", label: "Out for delivery" },
];

type Numbers = {
  waiting: number | null;
  onTheirWay: Page<AssignedParcel> | null;
  couriers: CourierWorkload[] | null;
  report: ReportSummary | null;
};

const EMPTY: Numbers = { waiting: null, onTheirWay: null, couriers: null, report: null };

const show = (value: number | null | undefined) => (value === null || value === undefined ? "—" : String(value));

export function DispatcherOverview() {
  const [numbers, setNumbers] = useState<Numbers>(EMPTY);
  const [loading, setLoading] = useState(true);
  const [someFailed, setSomeFailed] = useState(false);

  const load = useCallback(async () => {
    const results = await Promise.allSettled([
      dispatchApi.unassignedParcels({ size: 1 }),
      assignedParcelsApi.list({ activeOnly: true, size: 100 }),
      dispatchApi.workload(),
      dispatchApi.reportSummary(7),
    ]);
    const [waiting, onTheirWay, couriers, report] = results;
    setNumbers({
      waiting: waiting.status === "fulfilled" ? waiting.value.totalElements : null,
      onTheirWay: onTheirWay.status === "fulfilled" ? onTheirWay.value : null,
      couriers: couriers.status === "fulfilled" ? couriers.value : null,
      report: report.status === "fulfilled" ? report.value : null,
    });
    setSomeFailed(results.some((result) => result.status === "rejected"));
    setLoading(false);
  }, []);

  useEffect(() => {
    void load();
    const timer = window.setInterval(() => void load(), REFRESH_MS);
    return () => window.clearInterval(timer);
  }, [load]);

  const activeParcels = numbers.onTheirWay?.content ?? [];
  const stages = STAGES.map((stage) => ({
    ...stage,
    count: activeParcels.filter((parcel) => parcel.status === stage.status).length,
  }));
  const biggest = Math.max(1, ...stages.map((stage) => stage.count));

  const couriers = numbers.couriers ?? [];
  const availableCouriers = couriers.filter((courier) => courier.active).length;
  const busiest = [...couriers].sort((a, b) => b.activeDeliveries - a.activeDeliveries).slice(0, 6);
  const mostDeliveries = Math.max(1, ...busiest.map((courier) => courier.activeDeliveries));

  const daily = (numbers.report?.daily ?? []).map((day) => ({ ...day, label: day.date.slice(5) }));
  const outcomes = numbers.report?.outcomes;
  const successRate = outcomes?.successRatePercent;

  const cards = [
    {
      label: "Waiting for a courier",
      value: show(numbers.waiting),
      note: "Parcels that still need someone to deliver them",
      to: "/fleet/unassigned",
      icon: Package,
    },
    {
      label: "On their way",
      value: show(numbers.onTheirWay?.totalElements),
      note: "Parcels with a courier that are not delivered yet",
      to: "/fleet/unassigned",
      icon: Truck,
    },
    {
      label: "Couriers available",
      value: numbers.couriers ? `${availableCouriers} of ${couriers.length}` : "—",
      note: "Couriers ready to take parcels",
      to: "/fleet/couriers",
      icon: Users,
    },
    {
      label: "Delivered, last 7 days",
      value: show(outcomes?.delivered),
      note:
        successRate === null || successRate === undefined
          ? "No finished deliveries yet"
          : `${Math.round(successRate)}% of finished parcels were delivered`,
      to: "/fleet/reports",
      icon: CircleCheck,
    },
  ];

  return (
    <div>
      <div className="mb-8 flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
        <div>
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.2em] text-coral">Operations desk</p>
          <h1 className="font-display text-4xl font-bold tracking-tight sm:text-5xl">Your day at a glance.</h1>
          <p className="mt-3 max-w-xl text-ink/55">
            See what is waiting, what is on the road and how your couriers are doing. The numbers refresh every minute.
          </p>
        </div>
        <Link
          to="/fleet"
          className="flex items-center justify-center gap-2 rounded-xl bg-coral px-4 py-3 font-semibold text-white shadow-lg shadow-coral/20 transition hover:bg-ink"
        >
          Open dispatch desk
          <ArrowUpRight size={17} />
        </Link>
      </div>

      {someFailed && (
        <p className="mb-4 rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral">
          We couldn't load some of the numbers. Please refresh the page in a moment.
        </p>
      )}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map(({ label, value, note, to, icon: Icon }) => (
          <Link
            key={label}
            to={to}
            className="rounded-2xl border border-ink/10 bg-white p-5 transition hover:border-coral/40"
          >
            <div className="mb-6 flex justify-between text-ink/45">
              <span className="text-sm">{label}</span>
              <Icon size={18} />
            </div>
            <p className="font-display text-4xl font-bold">{loading ? "…" : value}</p>
            <p className="mt-1 text-xs text-ink/40">{note}</p>
          </Link>
        ))}
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <section className="rounded-2xl border border-ink/10 bg-white p-6">
          <h2 className="font-display text-xl font-bold">Where the parcels are</h2>
          <p className="mt-1 text-sm text-ink/50">Parcels that have a courier and are not delivered yet.</p>
          {!loading && activeParcels.length === 0 ? (
            <p className="mt-6 rounded-xl border border-dashed border-ink/20 p-5 text-sm text-ink/55">
              Nothing is on its way right now.
            </p>
          ) : (
            <ul className="mt-5 space-y-4">
              {stages.map((stage) => (
                <li key={stage.status}>
                  <div className="mb-1 flex justify-between text-sm">
                    <span className="text-ink/70">{stage.label}</span>
                    <span className="font-semibold">{loading ? "…" : stage.count}</span>
                  </div>
                  <div className="h-2 rounded-full bg-mist">
                    <div
                      className="h-2 rounded-full bg-coral"
                      style={{ width: `${(stage.count / biggest) * 100}%` }}
                    />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="rounded-2xl border border-ink/10 bg-white p-6">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 className="font-display text-xl font-bold">How busy are your couriers</h2>
              <p className="mt-1 text-sm text-ink/50">Parcels each courier is delivering right now.</p>
            </div>
            <Link to="/fleet/couriers" className="shrink-0 text-sm font-semibold text-coral hover:underline">
              See all
            </Link>
          </div>
          {!loading && busiest.length === 0 ? (
            <p className="mt-6 rounded-xl border border-dashed border-ink/20 p-5 text-sm text-ink/55">
              No couriers yet. Add one on the Couriers page.
            </p>
          ) : (
            <ul className="mt-5 space-y-4">
              {busiest.map((courier) => (
                <li key={courier.courierProfileId}>
                  <div className="mb-1 flex justify-between gap-3 text-sm">
                    <span className="truncate text-ink/70">
                      {courier.courierName ?? "Courier"} · {vehicleLabel(courier.vehicleType)}
                      {!courier.active && <span className="ml-2 text-xs text-ink/40">(unavailable)</span>}
                    </span>
                    <span className="font-semibold">{courier.activeDeliveries}</span>
                  </div>
                  <div className="h-2 rounded-full bg-mist">
                    <div
                      className="h-2 rounded-full bg-mint"
                      style={{ width: `${(courier.activeDeliveries / mostDeliveries) * 100}%` }}
                    />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <section className="mt-6 rounded-2xl border border-ink/10 bg-white p-6">
        <h2 className="font-display text-xl font-bold">Deliveries each day</h2>
        <p className="mt-1 text-sm text-ink/50">Parcels that finished in the last 7 days.</p>
        {!loading && daily.length === 0 ? (
          <p className="mt-6 rounded-xl border border-dashed border-ink/20 p-5 text-sm text-ink/55">
            There is nothing to show yet.
          </p>
        ) : (
          <div className="mt-5 h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={daily}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="label" fontSize={12} />
                <YAxis allowDecimals={false} fontSize={12} />
                <Tooltip />
                <Legend />
                <Bar dataKey="delivered" name="Delivered" stackId="day" fill={COLORS.delivered} />
                <Bar dataKey="failed" name="Could not be delivered" stackId="day" fill={COLORS.failed} />
                <Bar dataKey="cancelled" name="Cancelled" stackId="day" fill={COLORS.cancelled} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </section>

      <section className="mt-6 rounded-2xl border border-ink/10 bg-ink p-6 text-white">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-sm text-white/60">Last 7 days</p>
            <h2 className="mt-1 font-display text-2xl">How the deliveries went</h2>
          </div>
          <Link to="/fleet/reports" className="text-sm font-semibold text-mint hover:underline">
            Open the full report
          </Link>
        </div>
        <dl className="mt-6 grid grid-cols-2 gap-6 sm:grid-cols-4">
          {[
            { label: "Delivered", value: show(outcomes?.delivered) },
            { label: "Could not be delivered", value: show(outcomes?.failed) },
            { label: "Cancelled", value: show(outcomes?.cancelled) },
            {
              label: "Success rate",
              value: successRate === null || successRate === undefined ? "—" : `${Math.round(successRate)}%`,
            },
          ].map((item) => (
            <div key={item.label}>
              <dt className="text-sm text-white/60">{item.label}</dt>
              <dd className="mt-1 font-display text-3xl font-bold">{loading ? "…" : item.value}</dd>
            </div>
          ))}
        </dl>
      </section>
    </div>
  );
}