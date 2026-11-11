import { useCallback, useEffect, useState } from "react";
import { getApiErrorMessage } from "../../../lib/api";
import { dispatchApi } from "../api";
import type { AssignedParcel, Page } from "../types";
import { AssignDialog } from "./AssignDialog";

const PAGE_SIZE = 10;

export function AssignedParcelsPanel() {
  const [activeOnly, setActiveOnly] = useState(true);
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<AssignedParcel> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [reassigning, setReassigning] = useState<AssignedParcel | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setData(await dispatchApi.assignedParcels({ activeOnly, page, size: PAGE_SIZE }));
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not load the assigned parcels."));
    } finally {
      setLoading(false);
    }
  }, [activeOnly, page]);

  useEffect(() => {
    void load();
  }, [load]);

  const parcels = data?.content ?? [];

  return (
    <div>
      {notice && <p className="mb-4 rounded-lg bg-mint/40 px-3 py-2 text-sm text-ink">{notice}</p>}
      {error && <p className="mb-4 rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral">{error}</p>}

      <label className="mb-4 flex items-center gap-2 text-sm text-ink/70">
        <input
          type="checkbox"
          checked={activeOnly}
          onChange={(e) => {
            setPage(0);
            setActiveOnly(e.target.checked);
          }}
        />
        Only parcels still being delivered
      </label>

      <section className="rounded-2xl border border-ink/10 bg-white p-5">
        {loading && !data ? (
          <p className="text-sm text-ink/50">Loading parcels…</p>
        ) : parcels.length === 0 ? (
          error ? null : (
            <p className="rounded-xl border border-dashed border-ink/20 p-6 text-sm text-ink/55">
              No assigned parcels to show.
            </p>
          )
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[44rem] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink/10 text-xs uppercase tracking-wider text-ink/45">
                    <th className="py-2 pr-4 font-semibold">Tracking no.</th>
                    <th className="py-2 pr-4 font-semibold">Route</th>
                    <th className="py-2 pr-4 font-semibold">Status</th>
                    <th className="py-2 pr-4 font-semibold">Courier</th>
                    <th className="py-2 pr-4 font-semibold">Assigned</th>
                    <th className="py-2 font-semibold" />
                  </tr>
                </thead>
                <tbody>
                  {parcels.map((parcel) => (
                    <tr key={parcel.id} className="border-b border-ink/5 last:border-0">
                      <td className="py-3 pr-4 font-semibold">{parcel.trackingNumber}</td>
                      <td className="py-3 pr-4">
                        {parcel.pickupCity ?? "—"} → {parcel.dropoffCity ?? "—"}
                      </td>
                      <td className="py-3 pr-4">
                        <span className="rounded-full bg-ink/5 px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider text-ink/70">
                          {parcel.status.replace(/_/g, " ")}
                        </span>
                      </td>
                      <td className="py-3 pr-4 font-medium">
                        {parcel.courierName ?? `Courier #${parcel.assignedCourierId}`}
                      </td>
                      <td className="py-3 pr-4 text-ink/60">{new Date(parcel.assignedAt).toLocaleString()}</td>
                      <td className="py-3 text-right">
                        {["CREATED", "PICKED_UP"].includes(parcel.status) && (
                          <button
                            onClick={() => setReassigning(parcel)}
                            className="rounded-lg border border-ink/15 px-3 py-1.5 text-xs font-semibold hover:bg-ink/5"
                          >
                            Reassign
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="mt-4 flex items-center justify-between text-sm text-ink/60">
              <span>
                Page {(data?.number ?? 0) + 1} of {Math.max(data?.totalPages ?? 1, 1)} · {data?.totalElements ?? 0}{" "}
                parcels
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage(page - 1)}
                  disabled={page === 0 || loading}
                  className="rounded-lg border border-ink/15 px-3 py-1.5 font-semibold disabled:opacity-40"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage(page + 1)}
                  disabled={!data || page + 1 >= data.totalPages || loading}
                  className="rounded-lg border border-ink/15 px-3 py-1.5 font-semibold disabled:opacity-40"
                >
                  Next
                </button>
              </div>
            </div>
          </>
        )}
      </section>

      {reassigning && (
        <AssignDialog
          parcel={reassigning}
          currentCourierId={reassigning.assignedCourierId}
          onClose={() => setReassigning(null)}
          onAssigned={(message) => {
            setReassigning(null);
            setNotice(message);
            void load();
          }}
        />
      )}
    </div>
  );
}