import { ArrowRight, Download, Package, Plus, SearchX } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getApiErrorMessage } from "../../lib/api";
import { useAuth } from "../auth/AuthContext";
import {
  CANCEL_REASONS,
  cancelParcel,
  cancelReasonLabel,
  downloadReceipt,
  itemTypeLabel,
  listParcels,
  STATUS_LABELS,
  STATUSES,
} from "./parcels.api";
import type { CancelReason, ParcelPage, ParcelStatus } from "./parcels.api";

const statusClass: Record<ParcelStatus, string> = {
  CREATED: "bg-ink/10 text-ink",
  PICKED_UP: "bg-mint text-ink",
  IN_TRANSIT: "bg-mint text-ink",
  OUT_FOR_DELIVERY: "bg-mint text-ink",
  DELIVERED: "bg-ink text-white",
  FAILED: "bg-coral text-white",
  CANCELLED: "bg-coral/15 text-coral",
};

// The normal journey of a parcel, used for the progress bar on each card.
const JOURNEY: ParcelStatus[] = ["CREATED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED"];

const formatDate = (iso: string) =>
  new Date(iso).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" });

// Shows the town the customer typed plus its district, e.g. "Moratuwa, Colombo".
// When both are the same (or the town is empty) it shows the district once.
function place(a: { city?: string | null; district: string }): string {
  const city = a.city?.trim();
  return city && city.toLowerCase() !== a.district.toLowerCase() ? `${city}, ${a.district}` : a.district;
}

function Progress({ status }: { status: ParcelStatus }) {
  const reached = JOURNEY.indexOf(status);
  if (reached < 0) return null; // failed or cancelled parcels are not on the journey
  return (
    <div className="mt-3 flex gap-1" role="img" aria-label={`Step ${reached + 1} of ${JOURNEY.length}`}>
      {JOURNEY.map((step, i) => (
        <span key={step} className={`h-1.5 flex-1 rounded-full ${i <= reached ? "bg-coral" : "bg-ink/10"}`} />
      ))}
    </div>
  );
}

export function ParcelHistoryPage() {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<ParcelStatus | "">("");
  const [data, setData] = useState<ParcelPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [downloadingId, setDownloadingId] = useState<number | null>(null);
  const [confirmingId, setConfirmingId] = useState<number | null>(null);
  const [cancelReason, setCancelReason] = useState<CancelReason | "">("");
  const [cancelComment, setCancelComment] = useState("");
  const [cancelling, setCancelling] = useState(false);

  const startCancel = (id: number) => {
    setConfirmingId(id);
    setCancelReason("");
    setCancelComment("");
  };

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setData(await listParcels(page, status || undefined));
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not load your parcels."));
    } finally {
      setLoading(false);
    }
  }, [page, status]);

  useEffect(() => {
    if (user?.role === "CUSTOMER") {
      load();
    }
  }, [user, load]);

  const chooseStatus = (value: ParcelStatus | "") => {
    setStatus(value);
    setPage(0);
    setConfirmingId(null);
  };

  const handleDownload = async (id: number, trackingNumber: string) => {
    setError("");
    setDownloadingId(id);
    try {
      await downloadReceipt(id, trackingNumber);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not download the receipt."));
    } finally {
      setDownloadingId(null);
    }
  };

  const handleCancel = async (id: number) => {
    if (!cancelReason) return;
    setError("");
    setCancelling(true);
    try {
      await cancelParcel(id, cancelReason, cancelComment);
      setConfirmingId(null);
      await load();
    } catch (err) {
      setConfirmingId(null);
      setError(getApiErrorMessage(err, "Could not cancel the parcel."));
    } finally {
      setCancelling(false);
    }
  };

  const canConfirmCancel = cancelReason !== "" && (cancelReason !== "OTHER" || cancelComment.trim() !== "");

  if (user?.role !== "CUSTOMER") {
    return (
      <div className="mx-auto max-w-xl rounded-2xl border border-ink/5 bg-white p-6 text-center shadow-sm">
        <h1 className="font-display text-2xl font-bold">My parcels</h1>
        <p className="mt-2 text-ink/60">Booking and delivery history are available to customer accounts.</p>
      </div>
    );
  }

  const chip = (active: boolean) =>
    `shrink-0 rounded-full border px-4 py-1.5 text-sm font-semibold transition ${
      active ? "border-ink bg-ink text-white" : "border-ink/15 bg-white text-ink/70 hover:border-ink/40"
    }`;

  return (
    <div className="mx-auto max-w-4xl">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-3xl font-bold">My parcels</h1>
          <p className="text-sm text-ink/60">Track, cancel and get receipts for your deliveries.</p>
        </div>
        <Link
          to="/parcels/start"
          className="flex items-center justify-center gap-2 rounded-xl bg-coral px-5 py-2.5 text-sm font-semibold text-white"
        >
          <Plus className="h-4 w-4" />
          Book a parcel
        </Link>
      </div>

      <div className="-mx-1 my-5 flex gap-2 overflow-x-auto px-1 pb-1" role="group" aria-label="Filter by status">
        <button type="button" aria-pressed={status === ""} onClick={() => chooseStatus("")} className={chip(status === "")}>
          All
        </button>
        {STATUSES.map((s) => (
          <button key={s} type="button" aria-pressed={status === s} onClick={() => chooseStatus(s)} className={chip(status === s)}>
            {STATUS_LABELS[s]}
          </button>
        ))}
      </div>

      {error && <div className="mb-4 rounded-lg border border-coral/30 bg-coral/10 p-3 text-sm text-ink">{error}</div>}

      {loading ? (
        <div className="flex justify-center p-10">
          <div className="h-6 w-6 animate-spin rounded-full border-2 border-coral border-t-transparent" />
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="rounded-2xl border border-ink/5 bg-white p-10 text-center shadow-sm">
          <span className="mx-auto grid h-14 w-14 place-items-center rounded-full bg-mist text-ink/50">
            {status ? <SearchX className="h-7 w-7" /> : <Package className="h-7 w-7" />}
          </span>
          {status ? (
            <>
              <p className="mt-3 font-display text-xl font-bold">No {STATUS_LABELS[status].toLowerCase()} parcels</p>
              <button type="button" onClick={() => chooseStatus("")} className="mt-3 text-sm font-semibold text-coral underline">
                Clear filter
              </button>
            </>
          ) : (
            <>
              <p className="mt-3 font-display text-xl font-bold">No parcels yet</p>
              <p className="mt-1 text-sm text-ink/60">Book your first delivery and it will show up here.</p>
              <Link to="/parcels/start" className="mt-4 inline-block rounded-xl bg-coral px-5 py-2.5 text-sm font-semibold text-white">
                Book a parcel
              </Link>
            </>
          )}
        </div>
      ) : (
        <ul className="space-y-3">
          {data.content.map((parcel) => (
            <li key={parcel.id} className="rounded-2xl border border-ink/5 bg-white p-5 shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <Link to={`/parcels/${parcel.trackingNumber}`} className="text-lg font-bold tracking-wide hover:underline">
                  {parcel.trackingNumber}
                </Link>
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusClass[parcel.status]}`}>
                  {STATUS_LABELS[parcel.status]}
                </span>
              </div>

              <p className="mt-2 flex items-center gap-2 text-sm font-semibold">
                {place(parcel.pickup)}
                <ArrowRight className="h-4 w-4 text-coral" />
                {place(parcel.dropoff)}
              </p>
              <p className="text-sm text-ink/60">For {parcel.receiverName}</p>

              <Progress status={parcel.status} />
              {parcel.status === "FAILED" && (
                <p className="mt-3 rounded-lg bg-coral/10 p-2 text-xs text-ink">
                  The delivery attempt failed. Open the parcel to see what happened.
                </p>
              )}

              <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-ink/60">
                <span className="font-semibold text-ink">Rs. {Number(parcel.fee).toFixed(2)}</span>
                <span>Booked {formatDate(parcel.createdAt)}</span>
                {parcel.itemType && <span>{itemTypeLabel(parcel.itemType)}</span>}
                {parcel.deliveryPaidBy && (
                  <span>Paid by {parcel.deliveryPaidBy === "SENDER" ? "sender" : "receiver"}</span>
                )}
              </div>

              {parcel.status === "CANCELLED" && parcel.cancelReason && (
                <p className="mt-3 text-sm text-ink/60">
                  Reason: {cancelReasonLabel(parcel.cancelReason)}
                  {parcel.cancelComment ? ` - ${parcel.cancelComment}` : ""}
                </p>
              )}

              {confirmingId === parcel.id ? (
                <div className="mt-4 rounded-xl border border-coral/30 bg-coral/10 p-4">
                  <p className="text-sm font-semibold">Why are you cancelling this parcel?</p>
                  <p className="text-xs text-ink/60">This cannot be undone.</p>
                  <div role="radiogroup" aria-label="Reason for cancelling" className="mt-3 grid gap-2 sm:grid-cols-2">
                    {CANCEL_REASONS.map((r) => (
                      <label
                        key={r.value}
                        className={`flex cursor-pointer items-center gap-2 rounded-xl border bg-white px-3 py-2 text-sm ${
                          cancelReason === r.value ? "border-coral" : "border-ink/15"
                        }`}
                      >
                        <input
                          type="radio"
                          name={`cancel-reason-${parcel.id}`}
                          value={r.value}
                          checked={cancelReason === r.value}
                          onChange={() => setCancelReason(r.value)}
                          className="accent-coral"
                        />
                        {r.label}
                      </label>
                    ))}
                  </div>
                  {cancelReason === "OTHER" && (
                    <div className="mt-3">
                      <label htmlFor={`cancel-comment-${parcel.id}`} className="text-sm font-medium">
                        Tell us more
                      </label>
                      <textarea
                        id={`cancel-comment-${parcel.id}`}
                        value={cancelComment}
                        onChange={(e) => setCancelComment(e.target.value)}
                        maxLength={200}
                        rows={2}
                        className="mt-1 w-full rounded-xl border border-ink/15 bg-white px-3 py-2 text-sm"
                      />
                      <p className="text-right text-xs text-ink/50">{cancelComment.length}/200</p>
                    </div>
                  )}
                  <div className="mt-3 flex flex-wrap justify-end gap-2">
                    <button
                      type="button"
                      onClick={() => setConfirmingId(null)}
                      className="rounded-xl border border-ink/15 bg-white px-4 py-1.5 text-sm font-semibold"
                    >
                      Keep parcel
                    </button>
                    <button
                      type="button"
                      onClick={() => handleCancel(parcel.id)}
                      disabled={!canConfirmCancel || cancelling}
                      className="rounded-xl bg-coral px-4 py-1.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      {cancelling ? "Cancelling..." : "Confirm cancellation"}
                    </button>
                  </div>
                </div>
              ) : (
                <div className="mt-4 flex flex-wrap justify-end gap-2 border-t border-ink/10 pt-4">
                  {parcel.status === "CREATED" && (
                    <button
                      type="button"
                      onClick={() => startCancel(parcel.id)}
                      className="rounded-xl border border-coral px-4 py-1.5 text-sm font-semibold text-coral hover:bg-coral/10"
                    >
                      Cancel
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => handleDownload(parcel.id, parcel.trackingNumber)}
                    disabled={parcel.status === "CANCELLED" || downloadingId === parcel.id}
                    className="flex items-center gap-2 rounded-xl border border-ink/15 px-4 py-1.5 text-sm font-semibold hover:bg-mist disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    <Download className="h-4 w-4" />
                    {downloadingId === parcel.id ? "Downloading..." : "Download receipt"}
                  </button>
                  {parcel.status !== "CANCELLED" && (
                    <Link
                      to={`/parcels/${parcel.trackingNumber}`}
                      className="rounded-xl bg-ink px-4 py-1.5 text-sm font-semibold text-white"
                    >
                      Track
                    </Link>
                  )}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      {data && data.totalPages > 1 && (
        <div className="mt-5 flex items-center justify-between text-sm">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
            className="rounded-xl border border-ink/15 bg-white px-4 py-2 font-semibold disabled:opacity-40"
          >
            Previous
          </button>
          <span className="text-ink/60">
            Page {data.number + 1} of {data.totalPages}
          </span>
          <button
            type="button"
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage(page + 1)}
            className="rounded-xl border border-ink/15 bg-white px-4 py-2 font-semibold disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
