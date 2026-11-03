import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { getApiErrorMessage } from "../../../lib/api";
import { dispatchApi } from "../api";
import { AssignDialog } from "../components/AssignDialog";
import { PageHeader } from "../components/PageHeader";
import type { Page, UnassignedParcel } from "../types";

const PAGE_SIZE = 10;

type Filters = { district: string; from: string; to: string };
const NO_FILTERS: Filters = { district: "", from: "", to: "" };

const inputClass =
  "w-full rounded-xl border border-ink/15 bg-white px-3.5 py-2.5 text-sm outline-none transition placeholder:text-ink/30 focus:border-coral focus:ring-4 focus:ring-coral/10";

export function UnassignedParcelsPage() {
  const [form, setForm] = useState<Filters>(NO_FILTERS);
  const [filters, setFilters] = useState<Filters>(NO_FILTERS);
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<UnassignedParcel> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [assigning, setAssigning] = useState<UnassignedParcel | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const result = await dispatchApi.unassignedParcels({
        district: filters.district.trim() || undefined,
        from: filters.from || undefined,
        to: filters.to || undefined,
        page,
        size: PAGE_SIZE,
      });
      if (result.content.length === 0 && page > 0) {
        setPage(page - 1); // the last parcel of the last page was just assigned
        return;
      }
      setData(result);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not load the parcels."));
    } finally {
      setLoading(false);
    }
  }, [filters, page]);

  useEffect(() => {
    void load();
  }, [load]);

  const applyFilters = (event: FormEvent) => {
    event.preventDefault();
    setNotice("");
    setPage(0);
    setFilters(form);
  };

  const clearFilters = () => {
    setForm(NO_FILTERS);
    setFilters(NO_FILTERS);
    setPage(0);
  };

  const handleAssigned = (message: string) => {
    setAssigning(null);
    setNotice(message);
    void load();
  };

  const parcels = data?.content ?? [];

  return (
    <div>
      <PageHeader
        eyebrow="Operations desk"
        title="Unassigned parcels"
        text="Parcels that are waiting for a courier, oldest first."
      />

      {notice && <p className="mb-4 rounded-lg bg-mint/40 px-3 py-2 text-sm text-ink">{notice}</p>}
      {error && <p className="mb-4 rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral">{error}</p>}

      <form onSubmit={applyFilters} className="mb-6 grid gap-3 rounded-2xl border border-ink/10 bg-white p-4 sm:grid-cols-4">
        <label className="block space-y-1">
          <span className="text-xs font-semibold text-ink/60">Pickup district</span>
          <input
            className={inputClass}
            placeholder="e.g. Colombo"
            value={form.district}
            onChange={(e) => setForm({ ...form, district: e.target.value })}
          />
        </label>
        <label className="block space-y-1">
          <span className="text-xs font-semibold text-ink/60">Booked from</span>
          <input
            type="date"
            className={inputClass}
            value={form.from}
            onChange={(e) => setForm({ ...form, from: e.target.value })}
          />
        </label>
        <label className="block space-y-1">
          <span className="text-xs font-semibold text-ink/60">Booked until</span>
          <input
            type="date"
            className={inputClass}
            value={form.to}
            onChange={(e) => setForm({ ...form, to: e.target.value })}
          />
        </label>
        <div className="flex items-end gap-2">
          <button className="rounded-xl bg-ink px-4 py-2.5 text-sm font-semibold text-white">Apply</button>
          <button
            type="button"
            onClick={clearFilters}
            className="rounded-xl border border-ink/15 px-4 py-2.5 text-sm font-semibold"
          >
            Clear
          </button>
        </div>
      </form>

      <section className="rounded-2xl border border-ink/10 bg-white p-5">
        {loading && !data ? (
          <p className="text-sm text-ink/50">Loading parcels…</p>
        ) : parcels.length === 0 ? (
          <p className="rounded-xl border border-dashed border-ink/20 p-6 text-sm text-ink/55">
            No unassigned parcels match these filters.
          </p>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[40rem] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink/10 text-xs uppercase tracking-wider text-ink/45">
                    <th className="py-2 pr-4 font-semibold">Tracking no.</th>
                    <th className="py-2 pr-4 font-semibold">Pickup</th>
                    <th className="py-2 pr-4 font-semibold">Drop-off</th>
                    <th className="py-2 pr-4 font-semibold">Parcel</th>
                    <th className="py-2 pr-4 font-semibold">Booked</th>
                    <th className="py-2 font-semibold" />
                  </tr>
                </thead>
                <tbody>
                  {parcels.map((parcel) => (
                    <tr key={parcel.id} className="border-b border-ink/5 last:border-0">
                      <td className="py-3 pr-4 font-semibold">{parcel.trackingNumber}</td>
                      <td className="py-3 pr-4">
                        {parcel.pickupCity ?? "—"}
                        <span className="block text-xs text-ink/45">{parcel.pickupDistrict ?? "—"}</span>
                      </td>
                      <td className="py-3 pr-4">
                        {parcel.dropoffCity ?? "—"}
                        <span className="block text-xs text-ink/45">{parcel.dropoffDistrict ?? "—"}</span>
                      </td>
                      <td className="py-3 pr-4">
                        {parcel.size} · {parcel.weightKg} kg
                      </td>
                      <td className="py-3 pr-4 text-ink/60">{new Date(parcel.createdAt).toLocaleDateString()}</td>
                      <td className="py-3 text-right">
                        <button
                          onClick={() => setAssigning(parcel)}
                          className="rounded-lg bg-coral px-3 py-1.5 text-xs font-semibold text-white"
                        >
                          Assign
                        </button>
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

      {assigning && (
        <AssignDialog parcel={assigning} onClose={() => setAssigning(null)} onAssigned={handleAssigned} />
      )}
    </div>
  );
}