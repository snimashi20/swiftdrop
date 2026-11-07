import { Check, ChevronLeft, ChevronRight, RefreshCw, X } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { getApiErrorMessage } from "../../../lib/api";
import { approveMerchant, fetchMerchants, rejectMerchant } from "../api";
import type { Merchant, MerchantStatus } from "../types";

const statusStyle: Record<MerchantStatus, string> = {
  PENDING: "bg-coral/10 text-coral",
  APPROVED: "bg-mint text-ink",
  REJECTED: "bg-ink/10 text-ink/60",
};

export function MerchantApprovalsPage() {
  const [status, setStatus] = useState<MerchantStatus | "ALL">("PENDING");
  const [merchants, setMerchants] = useState<Merchant[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const result = await fetchMerchants(status === "ALL" ? undefined : status, page);
      setMerchants(result.content);
      setTotalPages(result.totalPages);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Could not load merchant applications."));
    } finally {
      setLoading(false);
    }
  }, [page, status]);

  useEffect(() => { void load(); }, [load]);

  const review = async (merchant: Merchant, action: "approve" | "reject") => {
    let reason = "";
    if (action === "reject") {
      reason = window.prompt(`Reason for rejecting ${merchant.businessName}:`)?.trim() ?? "";
      if (!reason) return;
    }
    setBusyId(merchant.id);
    setError("");
    setNotice("");
    try {
      if (action === "approve") await approveMerchant(merchant.id);
      else await rejectMerchant(merchant.id, reason);
      setNotice(`${merchant.businessName} ${action === "approve" ? "approved" : "rejected"}.`);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Could not update this merchant."));
    } finally {
      setBusyId(null);
    }
  };

  return <section className="space-y-6 font-sans text-ink">
    <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
      <div><p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">Administration</p><h1 className="mt-2 font-display text-3xl font-bold sm:text-4xl">Merchant approvals</h1><p className="mt-2 text-sm text-ink/55">Review business applications before they can create API keys and orders.</p></div>
      <button onClick={() => void load()} className="inline-flex items-center justify-center gap-2 rounded-xl border border-ink/10 bg-white px-4 py-2.5 text-sm font-semibold"><RefreshCw size={16} />Refresh</button>
    </header>
    <div className="flex flex-wrap gap-2">
      {(["PENDING", "APPROVED", "REJECTED", "ALL"] as const).map((item) => <button key={item} onClick={() => setStatus(item)} className={`rounded-full px-4 py-2 text-sm font-semibold transition ${status === item ? "bg-ink text-white" : "bg-white text-ink/60 hover:bg-ink/5"}`}>{item === "ALL" ? "All merchants" : item}</button>)}
    </div>
    {notice && <p role="status" className="rounded-xl bg-mint px-4 py-3 text-sm text-ink">{notice}</p>}
    {error && <p role="alert" className="rounded-xl bg-coral/10 px-4 py-3 text-sm text-coral">{error}</p>}
    <div className="overflow-hidden rounded-2xl border border-ink/10 bg-white">
      {loading ? <p className="p-8 text-center text-sm text-ink/55">Loading merchant applications…</p> : merchants.length === 0 ? <p className="p-8 text-center text-sm text-ink/55">No merchants found for this filter.</p> : <div className="divide-y divide-ink/10">
        {merchants.map((merchant) => <article key={merchant.id} className="grid gap-4 p-5 md:grid-cols-[1fr_auto] md:items-center">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2"><h2 className="font-display text-xl font-bold">{merchant.businessName}</h2><span className={`rounded-full px-2.5 py-1 text-[10px] font-bold tracking-wide ${statusStyle[merchant.status]}`}>{merchant.status}</span></div>
            <p className="mt-1 text-sm text-ink/65">{merchant.contactName} · {merchant.contactEmail} · {merchant.contactPhone}</p>
            <p className="mt-1 text-xs text-ink/45">Registration {merchant.registrationNumber} · Applied {new Date(merchant.createdAt).toLocaleDateString()}</p>
            {merchant.rejectionReason && <p className="mt-2 text-sm text-ink/60">Reason: {merchant.rejectionReason}</p>}
          </div>
          {merchant.status === "PENDING" && <div className="flex gap-2">
            <button disabled={busyId === merchant.id} onClick={() => void review(merchant, "approve")} className="inline-flex flex-1 items-center justify-center gap-2 rounded-xl bg-ink px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50"><Check size={16} />Approve</button>
            <button disabled={busyId === merchant.id} onClick={() => void review(merchant, "reject")} className="inline-flex flex-1 items-center justify-center gap-2 rounded-xl border border-coral/30 px-4 py-2.5 text-sm font-semibold text-coral disabled:opacity-50"><X size={16} />Reject</button>
          </div>}
        </article>)}
      </div>}
      <footer className="flex items-center justify-between border-t border-ink/10 px-5 py-3 text-sm text-ink/50"><span>Page {page + 1}{totalPages ? ` of ${totalPages}` : ""}</span><div className="flex gap-2"><button aria-label="Previous page" disabled={page === 0 || loading} onClick={() => setPage((value) => Math.max(0, value - 1))} className="rounded-lg border border-ink/10 p-2 disabled:opacity-40"><ChevronLeft size={16} /></button><button aria-label="Next page" disabled={loading || page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)} className="rounded-lg border border-ink/10 p-2 disabled:opacity-40"><ChevronRight size={16} /></button></div></footer>
    </div>
  </section>;
}
