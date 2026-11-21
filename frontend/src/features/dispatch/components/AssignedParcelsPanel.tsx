import { useCallback, useEffect, useState } from "react";
import { getApiErrorMessage } from "../../../lib/api";
import { assignedParcelsApi } from "../assigned";
import type { AssignedParcel } from "../assigned";
import type { Page } from "../types";
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
      setData(await assignedParcelsApi.list({ activeOnly, page, size: PAGE_SIZE }));
    } catch (err) {
      setError(getApiErrorMessage(err, "We couldn't load the assigned parcels. Please refresh the page."));
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
        Show only parcels that are still on their way
      </label>

      <section className="rounded-3xl border border-ink/10 bg-white p-6">
        {loading && !data ? (
          <p className="text-sm text-ink/50">Loading parcels…</p>
        ) : parcels.length === 0 ? (
          error ? null : (
            <p className="rounded-xl border border-dashed border-ink/20 p-6 text-sm text-ink/55">
              No parcels with a courier to show.
            </p>
          )
        ) : (
          <>
            <div className="overflow-x-auto rounded-xl border border-ink/10">
              <table className="w-full min-w-[44rem] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink/10 bg-mist/60 text-xs uppercase tracking-wider text-ink/45">
                    <th className="px-4 py-3 font-semibold">Tracking no.</th>
                    <th className="px-4 py-3 font-semibold">Route</th>
                    <th className="px-4 py-3 font-semibold">Status</th>
                    <th className="px-4 py-3 font-semibold">Courier</th>
                    <th className="px-4 py-3 font-semibold">Assigned</th>
                    <th className="px-4 py-3 font-semibold" />
                  </tr>
                </thead>
                <tbody>
                  {parcels.map((parcel) => (
                    <tr key={parcel.id} className="border-b border-ink/5 last:border-0">
                      <td className="px-4 py-3 font-mono text-[13px] font-semibold">{parcel.trackingNumber}</td>
                      <td className="px-4 py-3">
                        {parcel.pickupCity ?? "—"} → {parcel.dropoffCity ?? "—"}
                      </td>
                      <td className="px-4 py-3">
                        <span className="inline-flex items-center gap-1.5 rounded-full border border-ink/15 bg-mist px-2.5 py-1 text-xs font-semibold text-ink/80">
                          <span className="h-1.5 w-1.5 rounded-full bg-coral" />
                          {parcel.status.replace(/_/g, " ").toLowerCase().replace(/^\w/, (c) => c.toUpperCase())}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2.5">
                          <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-mint text-xs font-bold text-ink">
                            {(parcel.courierName ?? "C").charAt(0).toUpperCase()}
                          </span>
                          <span className="font-medium">{parcel.courierName ?? "Courier"}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-ink/60">{new Date(parcel.assignedAt).toLocaleString()}</td>
                      <td className="px-4 py-3 text-right">
                        {["CREATED", "PICKED_UP"].includes(parcel.status) && (
                          <button
                            onClick={() => setReassigning(parcel)}
                            className="rounded-lg border border-ink/15 px-3 py-1.5 text-xs font-semibold hover:bg-ink hover:text-white transition"
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