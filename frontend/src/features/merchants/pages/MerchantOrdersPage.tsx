import { ExternalLink, Package, RefreshCw } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getApiErrorMessage } from "../../../lib/api";
import { fetchDashboardOrderSummary, fetchDashboardOrders } from "../api";
import type { MerchantOrder, Page, ParcelStatus } from "../types";

const statuses: (ParcelStatus | "ALL")[] = ["ALL", "CREATED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "FAILED"];
const labelStatus = (status: string) => status.replace(/_/g, " ");
const statusStyle: Record<ParcelStatus, string> = {
  CREATED: "bg-ink/5 text-ink/65", PICKED_UP: "bg-mint text-ink", IN_TRANSIT: "bg-mint text-ink",
  OUT_FOR_DELIVERY: "bg-coral/10 text-coral", DELIVERED: "bg-mint text-ink", FAILED: "bg-coral/10 text-coral",
};

export function MerchantOrdersPage() {
  const [data, setData] = useState<Page<MerchantOrder> | null>(null);
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<ParcelStatus | "ALL">("ALL");
  const [summary, setSummary] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [orders, orderSummary] = await Promise.all([
        fetchDashboardOrders(filter === "ALL" ? undefined : filter, page),
        fetchDashboardOrderSummary(),
      ]);
      setData(orders);
      setSummary(orderSummary);
    }
    catch (requestError) { setError(getApiErrorMessage(requestError, "Could not load merchant orders.")); }
    finally { setLoading(false); }
  }, [filter, page]);
  useEffect(() => { void load(); }, [load]);
  const visibleOrders = data?.content ?? [];
  const counts = statuses.slice(1).map((status) => ({ status, count: filter === "ALL" ? (summary[status] ?? 0) : filter === status ? (summary[status] ?? 0) : 0 }));

  return <section className="space-y-6 font-sans text-ink">
    <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end"><div><p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">Merchant workspace</p><h1 className="mt-2 font-display text-3xl font-bold sm:text-4xl">Orders</h1><p className="mt-2 text-sm text-ink/55">Track the deliveries created through your merchant integration.</p></div><button onClick={() => void load()} className="inline-flex items-center justify-center gap-2 rounded-xl border border-ink/10 bg-white px-4 py-2.5 text-sm font-semibold"><RefreshCw size={16} />Refresh</button></header>
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{counts.map(({ status, count }) => <div key={status} className="rounded-2xl border border-ink/10 bg-white p-4"><p className="text-xs font-bold uppercase tracking-wide text-ink/45">{labelStatus(status)}</p><p className="mt-2 font-display text-3xl font-bold">{count}</p><p className="text-xs text-ink/45">{filter === "ALL" ? "all your orders" : "matching status"}</p></div>)}</div>
    <div className="flex gap-2 overflow-x-auto pb-1">{statuses.map((status) => <button key={status} onClick={() => { setPage(0); setFilter(status); }} className={`shrink-0 rounded-full px-4 py-2 text-sm font-semibold ${filter === status ? "bg-ink text-white" : "bg-white text-ink/60 hover:bg-ink/5"}`}>{status === "ALL" ? "All statuses" : labelStatus(status)}</button>)}</div>
    {error && <div role="alert" className="rounded-xl bg-coral/10 px-4 py-3 text-sm text-coral">{error}</div>}
    <div className="overflow-hidden rounded-2xl border border-ink/10 bg-white">
      {loading ? <p className="p-8 text-center text-sm text-ink/55">Loading orders…</p> : visibleOrders.length === 0 ? <div className="p-10 text-center"><Package className="mx-auto text-ink/25" size={32} /><p className="mt-3 font-semibold">No matching orders yet</p><p className="mt-1 text-sm text-ink/50">Orders created through the merchant API will appear here.</p></div> : <div className="divide-y divide-ink/10">
        {visibleOrders.map((order) => <article key={order.trackingNumber} className="flex flex-col gap-3 p-5 sm:flex-row sm:items-center sm:justify-between"><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><Link to={`/track/${order.trackingNumber}`} className="font-semibold text-coral underline-offset-2 hover:underline">{order.trackingNumber}</Link><span className={`rounded-full px-2.5 py-1 text-[10px] font-bold ${statusStyle[order.status]}`}>{labelStatus(order.status)}</span></div><p className="mt-1 text-sm text-ink/50">{order.externalOrderId ? `Store order ${order.externalOrderId} · ` : ""}{new Date(order.createdAt).toLocaleString()}</p></div><div className="flex items-center justify-between gap-4 sm:justify-end"><span className="font-semibold">LKR {Number(order.fee).toFixed(2)}</span><Link aria-label={`Track ${order.trackingNumber}`} to={`/track/${order.trackingNumber}`} className="rounded-lg p-2 text-ink/50 hover:bg-mist"><ExternalLink size={17} /></Link></div></article>)}
      </div>}
      <footer className="flex items-center justify-between border-t border-ink/10 px-5 py-3 text-sm text-ink/50"><span>{data?.totalElements ?? 0} orders</span><div className="flex gap-2"><button disabled={page === 0 || loading} onClick={() => setPage((value) => Math.max(0, value - 1))} className="rounded-lg border border-ink/10 px-3 py-1.5 disabled:opacity-40">Previous</button><span className="px-2 py-1.5">{page + 1}{data?.totalPages ? ` / ${data.totalPages}` : ""}</span><button disabled={!data || data.last || loading} onClick={() => setPage((value) => value + 1)} className="rounded-lg border border-ink/10 px-3 py-1.5 disabled:opacity-40">Next</button></div></footer>
    </div>
  </section>;
}
