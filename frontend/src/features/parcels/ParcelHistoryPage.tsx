import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getApiErrorMessage } from "../../lib/api";
import { useAuth } from "../auth/AuthContext";
import { cancelParcel, downloadReceipt, listParcels, STATUSES } from "./parcels.api";
import type { ParcelPage, ParcelStatus } from "./parcels.api";

const statusClass: Record<ParcelStatus, string> = {
  CREATED: "bg-ink/10 text-ink",
  PICKED_UP: "bg-mint text-ink",
  IN_TRANSIT: "bg-mint text-ink",
  OUT_FOR_DELIVERY: "bg-mint text-ink",
  DELIVERED: "bg-ink text-white",
  FAILED: "bg-coral text-white",
  CANCELLED: "bg-coral/15 text-coral",
};

const label = (status: string) => status.replace(/_/g, " ");

export function ParcelHistoryPage() {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<ParcelStatus | "">("");
  const [data, setData] = useState<ParcelPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

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

      const CANCEL_ENABLED = true;
  const handleCancel = async (id: number) => {

    if (!window.confirm("Cancel this parcel? This cannot be undone.")) return;
    try {
      await cancelParcel(id);
      await load();
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not cancel the parcel."));
    }
  };

  if (user?.role !== "CUSTOMER") {
    return (
      <div className="mx-auto max-w-xl rounded-2xl border border-ink/5 bg-white p-6 text-center shadow-sm">
        <h1 className="font-display text-2xl font-bold">My parcels</h1>
        <p className="mt-2 text-ink/60">Booking and delivery history are available to customer accounts.</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-display text-3xl font-bold">My parcels</h1>
        <Link to="/parcels/new" className="rounded-xl bg-coral px-5 py-2.5 text-center text-sm font-semibold text-white">
          Book a parcel
        </Link>
      </div>

      <div className="my-5 flex items-center gap-3">
        <label className="text-xs font-semibold uppercase tracking-wide text-ink/60">Status</label>
        <select
          className="rounded-xl border border-ink/15 bg-white px-3 py-2 text-sm outline-none focus:border-coral"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as ParcelStatus | "");
            setPage(0);
          }}
        >
          <option value="">All</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {label(s)}
            </option>
          ))}
        </select>
      </div>

      {error && <div className="mb-4 rounded-lg border border-coral/30 bg-coral/10 p-3 text-sm text-ink">{error}</div>}

      {loading ? (
        <div className="flex justify-center p-10">
          <div className="h-6 w-6 animate-spin rounded-full border-2 border-coral border-t-transparent" />
        </div>
      ) : !data || data.content.length === 0 ? (
        <p className="rounded-2xl border border-ink/5 bg-white p-6 text-center italic text-ink/50">
          No parcels found.
        </p>
      ) : (
        <ul className="space-y-3">
          {data.content.map((parcel) => (
            <li key={parcel.id} className="rounded-2xl border border-ink/5 bg-white p-4 shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <Link to={`/parcels/${parcel.trackingNumber}`} className=" text-lg font-bold underline">
                  {parcel.trackingNumber}
                </Link>
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusClass[parcel.status]}`}>
                  {label(parcel.status)}
                </span>
              </div>
              <p className="mt-2 text-sm text-ink/70">
                {parcel.pickup.district} to {parcel.dropoff.district} · for {parcel.receiverName}
              </p>
              <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-sm">
                <span className="text-ink/60">
                  Rs. {Number(parcel.fee).toFixed(2)} · booked {new Date(parcel.createdAt).toLocaleDateString()}
                </span>
                                {CANCEL_ENABLED && parcel.status === "CREATED" && (
                  <button
                    type="button"
                    onClick={() => handleCancel(parcel.id)}
                    className="rounded-xl border border-coral px-4 py-1.5 text-sm font-semibold text-coral"
                  >
                    Cancel
                  </button>
                )}
                <button
                  type="button"
                              onClick={() => downloadReceipt(parcel.id, parcel.trackingNumber)} disabled={parcel.status === "CANCELLED"}      
                  className="rounded-xl border border-ink px-4 py-1.5 text-sm font-semibold text-ink disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Download receipt
                </button>
              </div>
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
            className="rounded-xl border border-ink/15 px-4 py-2 font-semibold disabled:opacity-40"
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
            className="rounded-xl border border-ink/15 px-4 py-2 font-semibold disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}