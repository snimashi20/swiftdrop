import { ExternalLink, Package, RefreshCw, Search } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { getApiErrorMessage } from "../../../lib/api";
import { fetchDashboardOrders } from "../api";
import type { MerchantOrder, Page, ParcelStatus } from "../types";

const statuses: (ParcelStatus | "ALL")[] = ["ALL", "CREATED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "FAILED", "CANCELLED"];
const labelStatus = (status: string) => status.replace(/_/g, " ");
const statusStyle: Record<ParcelStatus, string> = {
  CREATED: "bg-ink/5 text-ink/65", PICKED_UP: "bg-mint text-ink", IN_TRANSIT: "bg-mint text-ink",
  OUT_FOR_DELIVERY: "bg-coral/10 text-coral", DELIVERED: "bg-mint text-ink", FAILED: "bg-coral/10 text-coral",
  CANCELLED: "bg-ink/5 text-ink/65",
};

export function MerchantOrdersPage() {
  const [data, setData] = useState<Page<MerchantOrder> | null>(null);
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<ParcelStatus | "ALL">("ALL");
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [sortDirection, setSortDirection] = useState<"ASC" | "DESC">("DESC");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const requestGeneration = useRef(0);
  const load = useCallback(async () => {
    const generation = ++requestGeneration.current;
    setLoading(true);
    setError("");
    try {
      const orders = await fetchDashboardOrders(filter === "ALL" ? undefined : filter, page, search || undefined, sortDirection);
      if (generation === requestGeneration.current) setData(orders);
    }
    catch (requestError) {
      if (generation === requestGeneration.current) setError(getApiErrorMessage(requestError, "Could not load merchant orders."));
    }
    finally {
      if (generation === requestGeneration.current) setLoading(false);
    }
  }, [filter, page, search, sortDirection]);
  useEffect(() => { void load(); }, [load]);
  useEffect(() => () => { requestGeneration.current += 1; }, []);
  useEffect(() => {
    const timer = window.setTimeout(() => setSearch(searchInput.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [searchInput]);
  const visibleOrders = data?.content ?? [];

  return <section className="space-y-6 font-sans text-ink">
    <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end"><div><p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">Merchant workspace</p><h1 className="mt-2 font-display text-3xl font-bold sm:text-4xl">Orders</h1><p className="mt-2 text-sm text-ink/55">Track the deliveries created through your merchant integration.</p></div><button onClick={() => void load()} className="inline-flex items-center justify-center gap-2 rounded-xl border border-ink/10 bg-white px-4 py-2.5 text-sm font-semibold"><RefreshCw size={16} />Refresh</button></header>
    <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto]">
      <label className="flex min-w-0 items-center gap-2 rounded-xl border border-ink/10 bg-white px-3.5 py-2.5 text-ink/50">
        <Search size={17} aria-hidden="true" />
        <input type="search" aria-label="Search by order ID" value={searchInput} onChange={(event) => { setSearchInput(event.target.value); setPage(0); }} placeholder="Store order ID or SwiftDrop tracking number" className="min-w-0 flex-1 bg-transparent text-sm text-ink outline-none placeholder:text-ink/40" />
      </label>
      <label className="flex items-center gap-2 rounded-xl border border-ink/10 bg-white px-3.5 py-2.5 text-sm text-ink/60">
        <span className="whitespace-nowrap">Sort by date</span>
        <select aria-label="Sort orders by date" value={sortDirection} onChange={(event) => { setSortDirection(event.target.value as "ASC" | "DESC"); setPage(0); }} className="bg-transparent font-semibold text-ink outline-none">
          <option value="DESC">Latest first</option>
          <option value="ASC">Oldest first</option>
        </select>
      </label>
    </div>
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
