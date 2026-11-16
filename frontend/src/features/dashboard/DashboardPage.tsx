import { ArrowUpRight, CircleCheck, Clock3, KeyRound, Package, RefreshCw, Route, Users } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { useAuth } from "../auth/AuthContext";
import type { Role } from "../auth/auth.types";
import { DispatcherOverview } from "../dispatch/pages/DispatcherOverview";
import { getApiErrorMessage } from "../../lib/api";
import { fetchApiKeys, fetchDashboardOrderSummary, fetchDashboardOrderTrend } from "../merchants/api";
import type { ApiKey, MerchantOrderTrendPoint } from "../merchants/types";

const copy: Record<Role, { eyebrow: string; title: string; text: string; action: string }> = {
  CUSTOMER: { eyebrow: "Your deliveries", title: "Everything is on its way.", text: "Track your parcels, book a pickup, and stay close to every handoff.", action: "Book a delivery" },
  MERCHANT: { eyebrow: "Merchant workspace", title: "Your orders, moving well.", text: "Keep fulfillment flowing with a live view of your orders and delivery performance.", action: "Open merchant orders" },
  COURIER: { eyebrow: "Courier route", title: "Ready for the road?", text: "Your assigned route and next handoffs will appear here when dispatch is live.", action: "View route" },
  DISPATCHER: { eyebrow: "Operations desk", title: "The network at a glance.", text: "See active work, resolve exceptions, and keep every route moving.", action: "Open dispatch" },
  ADMIN: { eyebrow: "System overview", title: "Good morning, operator.", text: "Manage people, policies, and the health of the SwiftDrop network.", action: "Manage users" },
};

export function DashboardPage() {
  const { user } = useAuth();

  // Strict Rule: Admins have their own dedicated AdminOverviewPage and must never see the shared DashboardPage
  if (user?.role === "ADMIN") {
    return <Navigate to="/admin/overview" replace />;
  }

  const content = copy[user?.role || "CUSTOMER"];
if (user?.role === "DISPATCHER") return <DispatcherOverview />;
  if (user?.role === "MERCHANT") return <MerchantOverview />;

  return <div>
    <div className="mb-8 flex flex-col justify-between gap-5 sm:flex-row sm:items-end"><div><p className="mb-2 text-xs font-bold uppercase tracking-[0.2em] text-coral">{content.eyebrow}</p><h1 className="font-display text-4xl font-bold tracking-tight sm:text-5xl">{content.title}</h1><p className="mt-3 max-w-xl text-ink/55">{content.text}</p></div><Link to={user?.role === "CUSTOMER" ? "/parcels/new" : "#"} className="flex items-center justify-center gap-2 rounded-xl bg-coral px-4 py-3 font-semibold text-white shadow-lg shadow-coral/20">{content.action}<ArrowUpRight size={17} /></Link></div>
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{[{ label: "Active parcels", value: "—", icon: Package }, { label: "On-time rate", value: "—", icon: CircleCheck }, { label: "In progress", value: "—", icon: Clock3 }, { label: "Routes today", value: "—", icon: Route }].map(({ label, value, icon: Icon }) => <div key={label} className="rounded-2xl border border-ink/10 bg-white p-5"><div className="mb-6 flex justify-between text-ink/45"><span className="text-sm">{label}</span><Icon size={18} /></div><p className="font-display text-4xl font-bold">{value}</p><p className="mt-1 text-xs text-ink/40">Connect your feature module to populate</p></div>)}</div>
    <div className="mt-6 grid gap-6 lg:grid-cols-[1.4fr_0.6fr]"><section className="min-h-64 rounded-2xl border border-ink/10 bg-ink p-6 text-white"><div className="flex items-start justify-between"><div><p className="text-sm text-white/60">Live activity</p><h2 className="mt-1 font-display text-2xl">Your activity feed is ready.</h2></div><span className="rounded-full bg-mint px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-ink">Online</span></div><div className="mt-14 border-t border-white/10 pt-4 text-sm text-white/45">Events from parcels, routes, and notifications will appear here.</div></section><section className="rounded-2xl border border-ink/10 bg-white p-6"><div className="flex items-center gap-2"><Users size={18} className="text-coral" /><h2 className="font-semibold">Quick start</h2></div><p className="mt-4 text-sm leading-6 text-ink/55">This dashboard is the shared home for the {user?.role?.toLowerCase()} experience. Feature teams can add their modules to the workspace navigation.</p></section></div>
  </div>;
}

function MerchantOverview() {
  const [summary, setSummary] = useState<Record<string, number> | null>(null);
  const [trend, setTrend] = useState<MerchantOrderTrendPoint[]>([]);
  const [keys, setKeys] = useState<ApiKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refresh, setRefresh] = useState(0);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError("");
    Promise.allSettled([fetchDashboardOrderSummary(), fetchDashboardOrderTrend(), fetchApiKeys()])
      .then(([summaryResult, trendResult, keysResult]) => {
        if (!current) return;
        if (summaryResult.status === "fulfilled") setSummary(summaryResult.value);
        if (trendResult.status === "fulfilled") setTrend(trendResult.value);
        if (keysResult.status === "fulfilled") setKeys(keysResult.value);
        const failed = [summaryResult, trendResult, keysResult].find((result) => result.status === "rejected");
        if (failed?.status === "rejected") {
          setError(getApiErrorMessage(failed.reason, "Could not load part of your merchant overview."));
        }
      })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [refresh]);

  const counts = summary ?? {};
  const total = Object.values(counts).reduce((sum, count) => sum + count, 0);
  const inProgress = (counts.CREATED ?? 0) + (counts.PICKED_UP ?? 0) + (counts.IN_TRANSIT ?? 0) + (counts.OUT_FOR_DELIVERY ?? 0);
  const activeKeys = keys.filter((key) => key.active).length;
  const metrics = [
    { label: "Total orders", value: total, detail: "All-time orders", icon: Package },
    { label: "In progress", value: inProgress, detail: "Being delivered", icon: Clock3 },
    { label: "Delivered", value: counts.DELIVERED ?? 0, detail: "Successfully completed", icon: CircleCheck },
    { label: "Needs attention", value: counts.FAILED ?? 0, detail: "Failed deliveries", icon: Route },
  ];
  const chartData = trend.map((point) => ({
    ...point,
    day: new Intl.DateTimeFormat(undefined, { weekday: "short", timeZone: "UTC" }).format(new Date(`${point.date}T00:00:00Z`)),
  }));

  return <section className="space-y-6 font-sans text-ink">
    <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
      <div><p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">Merchant workspace</p><h1 className="mt-2 font-display text-4xl font-bold tracking-tight sm:text-5xl">Business overview</h1><p className="mt-3 max-w-2xl text-ink/55">A quick view of delivery activity and store connection health.</p></div>
      <button onClick={() => setRefresh((value) => value + 1)} disabled={loading} className="inline-flex items-center justify-center gap-2 rounded-xl border border-ink/10 bg-white px-4 py-2.5 text-sm font-semibold disabled:opacity-50"><RefreshCw size={16} />Refresh</button>
    </header>
    {error && <div role="alert" className="rounded-xl bg-coral/10 px-4 py-3 text-sm text-coral">{error}</div>}
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {metrics.map(({ label, value, detail, icon: Icon }) => <article key={label} className="rounded-2xl border border-ink/10 bg-white p-5">
        <div className="mb-5 flex items-center justify-between text-ink/45"><span className="text-sm">{label}</span><Icon size={18} className="text-coral" /></div>
        <p className="font-display text-4xl font-bold">{loading ? "—" : value}</p><p className="mt-1 text-xs text-ink/45">{detail}</p>
      </article>)}
    </div>
    <div className="grid gap-6 lg:grid-cols-[1.65fr_0.85fr]">
      <section className="rounded-2xl border border-ink/10 bg-white p-5 sm:p-6">
        <div><h2 className="font-display text-2xl font-bold">Order activity</h2><p className="mt-1 text-sm text-ink/50">Orders created each day over the last seven days</p></div>
        {loading ? <div className="grid h-72 place-items-center text-sm text-ink/45">Loading activity…</div> : chartData.length === 0 || chartData.every((point) => point.count === 0) ? <div className="grid h-72 place-items-center text-center"><div><Package className="mx-auto text-ink/25" size={30} /><p className="mt-3 font-semibold">No recent order activity</p><p className="mt-1 text-sm text-ink/50">New orders will appear here.</p></div></div> : <div className="mt-5 h-72" aria-label="Seven day order activity chart">
          <ResponsiveContainer width="100%" height="100%"><LineChart data={chartData} margin={{ top: 12, right: 14, bottom: 4, left: 0 }}>
            <CartesianGrid stroke="currentColor" className="text-ink/10" strokeDasharray="4 4" vertical={false} />
            <XAxis dataKey="day" tick={{ fill: "currentColor", fontSize: 12 }} tickLine={false} axisLine={false} />
            <YAxis allowDecimals={false} width={32} tick={{ fill: "currentColor", fontSize: 12 }} tickLine={false} axisLine={false} />
            <Tooltip formatter={(value) => [value, "Orders"]} />
            <Line type="monotone" dataKey="count" stroke="currentColor" className="text-coral" strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 6 }} />
          </LineChart></ResponsiveContainer>
        </div>}
      </section>
      <aside className="flex flex-col rounded-2xl border border-ink/10 bg-white p-6">
        <div className="flex items-center gap-3"><div className="grid h-10 w-10 place-items-center rounded-xl bg-mint text-ink"><KeyRound size={19} /></div><div><h2 className="font-display text-xl font-bold">Integration setup</h2><p className="text-sm text-ink/50">Machine access readiness</p></div></div>
        <div className="my-6 rounded-xl bg-mist p-4"><p className="text-sm text-ink/55">Active API keys</p><p className="mt-1 font-display text-3xl font-bold">{loading ? "—" : activeKeys}</p><p className="mt-1 text-xs text-ink/45">An active key is needed for your store to submit orders.</p></div>
        <div className="mt-auto flex items-center justify-between gap-3"><span className={`rounded-full px-3 py-1 text-xs font-bold ${activeKeys > 0 ? "bg-mint text-ink" : "bg-coral/10 text-coral"}`}>{loading ? "Checking" : activeKeys > 0 ? "Key available" : "Setup needed"}</span><Link to="/merchant/api-keys" className="inline-flex items-center gap-1 text-sm font-semibold text-coral hover:underline">Manage keys <ArrowUpRight size={15} /></Link></div>
      </aside>
    </div>
  </section>;
}
