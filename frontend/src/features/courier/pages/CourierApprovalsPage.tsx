import { Check, ChevronLeft, ChevronRight, RefreshCw, X } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { getApiErrorMessage } from "../../../lib/api";
import { approveCourierApplication, fetchCourierApplications, rejectCourierApplication } from "../admin.api";
import type { CourierApplication, CourierApplicationStatus } from "../types";

const statusStyle: Record<CourierApplicationStatus, string> = {
  PENDING: "bg-coral/10 text-coral",
  APPROVED: "bg-mint text-ink",
  REJECTED: "bg-ink/10 text-ink/60",
};

const vehicleLabel: Record<string, string> = {
  BICYCLE: "Bicycle",
  MOTORBIKE: "Motorbike",
  THREE_WHEELER: "Three wheeler",
  CAR: "Car",
  VAN: "Van",
};

export function CourierApprovalsPage() {
  const [status, setStatus] = useState<CourierApplicationStatus | "ALL">("PENDING");
  const [couriers, setCouriers] = useState<CourierApplication[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const queryRef = useRef({ status, page });
  const latestRequestRef = useRef(0);
  const lifecycleRef = useRef(0);
  const mountedRef = useRef(false);
  const mutationInFlightRef = useRef(false);
  queryRef.current = { status, page };

  useEffect(() => {
    mountedRef.current = true;
    lifecycleRef.current += 1;

    return () => {
      mountedRef.current = false;
      lifecycleRef.current += 1;
      latestRequestRef.current += 1;
    };
  }, []);

  const load = useCallback(async () => {
    const query = queryRef.current;
    const lifecycleId = lifecycleRef.current;
    const requestId = ++latestRequestRef.current;
    setLoading(true);
    setError("");
    try {
      const result = await fetchCourierApplications(query.status === "ALL" ? undefined : query.status, query.page);
      if (!mountedRef.current || lifecycleId !== lifecycleRef.current || requestId !== latestRequestRef.current) return;
      setCouriers(result.content);
      setTotalPages(result.totalPages);
    } catch (requestError) {
      if (mountedRef.current && lifecycleId === lifecycleRef.current && requestId === latestRequestRef.current) {
        setError(getApiErrorMessage(requestError, "Could not load courier applications."));
      }
    } finally {
      if (mountedRef.current && lifecycleId === lifecycleRef.current && requestId === latestRequestRef.current) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => { void load(); }, [load, page, status]);

  const review = async (courier: CourierApplication, action: "approve" | "reject") => {
    if (!mountedRef.current || mutationInFlightRef.current) return;
    let reason = "";
    if (action === "reject") {
      reason = window.prompt(`Reason for rejecting ${courier.fullName}:`)?.trim() ?? "";
      if (!reason) return;
    }
    const lifecycleId = lifecycleRef.current;
    mutationInFlightRef.current = true;
    setBusyId(courier.id);
    setError("");
    setNotice("");
    try {
      const updatedCourier = action === "approve"
        ? await approveCourierApplication(courier.id)
        : await rejectCourierApplication(courier.id, reason);
      if (!mountedRef.current || lifecycleId !== lifecycleRef.current) return;
      const currentStatus = queryRef.current.status;
      setCouriers((current) => {
        if (currentStatus !== "ALL" && updatedCourier.status !== currentStatus) {
          return current.filter((item) => item.id !== updatedCourier.id);
        }
        return current.map((item) => item.id === updatedCourier.id ? updatedCourier : item);
      });
      setNotice(`${courier.fullName} ${action === "approve" ? "approved" : "rejected"}.`);
      await load();
    } catch (requestError) {
      if (mountedRef.current && lifecycleId === lifecycleRef.current) {
        setError(getApiErrorMessage(requestError, "Could not update this courier."));
      }
    } finally {
      mutationInFlightRef.current = false;
      if (mountedRef.current && lifecycleId === lifecycleRef.current) setBusyId(null);
    }
  };

  return <section className="space-y-6 font-sans text-ink">
    <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
      <div><p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">Administration</p><h1 className="mt-2 font-display text-3xl font-bold sm:text-4xl">Courier approvals</h1><p className="mt-2 text-sm text-ink/55">Review rider applications before they can sign in and take deliveries.</p></div>
      <button onClick={() => void load()} className="inline-flex items-center justify-center gap-2 rounded-xl border border-ink/10 bg-white px-4 py-2.5 text-sm font-semibold"><RefreshCw size={16} />Refresh</button>
    </header>
    <div className="flex flex-wrap gap-2">
      {(["PENDING", "APPROVED", "REJECTED", "ALL"] as const).map((item) => <button key={item} onClick={() => setStatus(item)} className={`rounded-full px-4 py-2 text-sm font-semibold transition ${status === item ? "bg-ink text-white" : "bg-white text-ink/60 hover:bg-ink/5"}`}>{item === "ALL" ? "All couriers" : item}</button>)}
    </div>
    {notice && <p role="status" className="rounded-xl bg-mint px-4 py-3 text-sm text-ink">{notice}</p>}
    {error && <p role="alert" className="rounded-xl bg-coral/10 px-4 py-3 text-sm text-coral">{error}</p>}
    <div className="overflow-hidden rounded-2xl border border-ink/10 bg-white">
      {loading ? <p className="p-8 text-center text-sm text-ink/55">Loading courier applications…</p> : couriers.length === 0 ? <p className="p-8 text-center text-sm text-ink/55">No couriers found for this filter.</p> : <div className="divide-y divide-ink/10">
        {couriers.map((courier) => <article key={courier.id} className="grid gap-4 p-5 md:grid-cols-[1fr_auto] md:items-center">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2"><h2 className="font-display text-xl font-bold">{courier.fullName}</h2><span className={`rounded-full px-2.5 py-1 text-[10px] font-bold tracking-wide ${statusStyle[courier.status]}`}>{courier.status}</span></div>
            <p className="mt-1 text-sm text-ink/65">{courier.contactEmail}{courier.contactPhone ? ` · ${courier.contactPhone}` : ""}</p>
            <p className="mt-1 text-xs text-ink/45">{courier.vehicleType ? `${vehicleLabel[courier.vehicleType] ?? courier.vehicleType} · ` : ""}{courier.licenseNumber ? `Licence ${courier.licenseNumber} · ` : ""}Applied {new Date(courier.createdAt).toLocaleDateString()}</p>
            {courier.rejectionReason && <p className="mt-2 text-sm text-ink/60">Reason: {courier.rejectionReason}</p>}
          </div>
          {courier.status === "PENDING" && <div className="flex gap-2">
            <button disabled={busyId === courier.id} onClick={() => void review(courier, "approve")} className="inline-flex flex-1 items-center justify-center gap-2 rounded-xl bg-ink px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50"><Check size={16} />Approve</button>
            <button disabled={busyId === courier.id} onClick={() => void review(courier, "reject")} className="inline-flex flex-1 items-center justify-center gap-2 rounded-xl border border-coral/30 px-4 py-2.5 text-sm font-semibold text-coral disabled:opacity-50"><X size={16} />Reject</button>
          </div>}
        </article>)}
      </div>}
      <footer className="flex items-center justify-between border-t border-ink/10 px-5 py-3 text-sm text-ink/50"><span>Page {page + 1}{totalPages ? ` of ${totalPages}` : ""}</span><div className="flex gap-2"><button aria-label="Previous page" disabled={page === 0 || loading} onClick={() => setPage((value) => Math.max(0, value - 1))} className="rounded-lg border border-ink/10 p-2 disabled:opacity-40"><ChevronLeft size={16} /></button><button aria-label="Next page" disabled={loading || page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)} className="rounded-lg border border-ink/10 p-2 disabled:opacity-40"><ChevronRight size={16} /></button></div></footer>
    </div>
  </section>;
}
