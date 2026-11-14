import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { getApiErrorMessage } from "../../../lib/api";
import { dispatchApi } from "../api";
import { EditCourierDialog } from "../components/EditCourierDialog";
import { PageHeader } from "../components/PageHeader";
import type { CourierCandidate, CourierWorkload, VehicleType, ZoneOption } from "../types";
import { VEHICLES, vehicleLabel } from "../vehicles";

type Filter = "all" | "active" | "inactive";

const FILTERS: { value: Filter; label: string }[] = [
  { value: "all", label: "All" },
  { value: "active", label: "Available" },
  { value: "inactive", label: "Unavailable" },
];

const inputClass =
  "w-full rounded-xl border border-ink/15 bg-white px-3.5 py-3 text-sm outline-none transition placeholder:text-ink/30 focus:border-coral focus:ring-4 focus:ring-coral/10";

export function CouriersPage() {
  const [filter, setFilter] = useState<Filter>("all");
  const [couriers, setCouriers] = useState<CourierWorkload[]>([]);
  const [zones, setZones] = useState<ZoneOption[]>([]);
  const [candidates, setCandidates] = useState<CourierCandidate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busyId, setBusyId] = useState<number | null>(null);
  const [editing, setEditing] = useState<CourierWorkload | null>(null);

  const [userId, setUserId] = useState("");
  const [zoneId, setZoneId] = useState("");
  const [vehicleType, setVehicleType] = useState<VehicleType>("MOTORBIKE");
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);

  const zoneName = (id: number) => zones.find((zone) => zone.id === id)?.name ?? "Unknown area";

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const active = filter === "all" ? undefined : filter === "active";
      setCouriers(await dispatchApi.workload({ active }));
    } catch (err) {
      setError(getApiErrorMessage(err, "We couldn't load the couriers. Please refresh the page."));
    } finally {
      setLoading(false);
    }
  }, [filter]);

  const loadLookups = useCallback(async () => {
    try {
      const [zoneList, candidateList] = await Promise.all([dispatchApi.zones(), dispatchApi.courierCandidates()]);
      setZones(zoneList);
      setCandidates(candidateList);
    } catch (err) {
      setFormError(getApiErrorMessage(err, "We couldn't load the couriers and delivery areas. Please refresh the page."));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    void loadLookups();
  }, [loadLookups]);

  const toggleActive = async (courier: CourierWorkload) => {
    setBusyId(courier.courierProfileId);
    setError("");
    setNotice("");
    try {
      await dispatchApi.updateCourier(courier.courierProfileId, { active: !courier.active });
      setNotice(
        `${courier.courierName ?? `Courier #${courier.courierProfileId}`} is now ${courier.active ? "unavailable" : "available"}.`,
      );
      await load();
    } catch (err) {
      setError(getApiErrorMessage(err, "We couldn't update this courier. Please try again."));
    } finally {
      setBusyId(null);
    }
  };

  const createCourier = async (event: FormEvent) => {
    event.preventDefault();
    setFormError("");
    setNotice("");
    const userIdNumber = Number(userId);
    const zoneIdNumber = Number(zoneId);
    if (!userId || !zoneId || !Number.isInteger(userIdNumber) || !Number.isInteger(zoneIdNumber)) {
      setFormError("Please choose a courier and a delivery area.");
      return;
    }
    setSaving(true);
    try {
      await dispatchApi.createCourier({ userId: userIdNumber, vehicleType, zoneId: zoneIdNumber });
      setNotice("Courier added.");
      setUserId("");
      setZoneId("");
      await Promise.all([load(), loadLookups()]);
    } catch (err) {
      setFormError(getApiErrorMessage(err, "We couldn't add this courier. Please try again."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <PageHeader
        eyebrow="Operations desk"
        title="Couriers"
        text="See your couriers, how many deliveries each one has right now, and who is available."
      />

      {notice && <p className="mb-4 rounded-lg bg-mint/40 px-3 py-2 text-sm text-ink">{notice}</p>}
      {error && <p className="mb-4 rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral">{error}</p>}

      <section className="mb-8 rounded-2xl border border-ink/10 bg-white p-5">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="font-display text-xl font-bold">
            Courier list {!loading && <span className="text-sm font-normal text-ink/45">({couriers.length})</span>}
          </h2>
          <div className="inline-flex rounded-xl border border-ink/10 p-1">
            {FILTERS.map((option) => (
              <button
                key={option.value}
                onClick={() => setFilter(option.value)}
                className={`rounded-lg px-3 py-1.5 text-sm font-semibold transition ${
                  filter === option.value ? "bg-ink text-white" : "text-ink/60 hover:text-ink"
                }`}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <p className="text-sm text-ink/50">Loading couriers…</p>
        ) : couriers.length === 0 ? (
          error ? null : (
            <p className="rounded-xl border border-dashed border-ink/20 p-6 text-sm text-ink/55">
              No couriers found. Add a courier below.
            </p>
          )
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[34rem] text-left text-sm">
              <thead>
                <tr className="border-b border-ink/10 text-xs uppercase tracking-wider text-ink/45">
                  <th className="py-2 pr-4 font-semibold">Courier</th>
                  <th className="py-2 pr-4 font-semibold">Vehicle</th>
                  <th className="py-2 pr-4 font-semibold">Delivery area</th>
                  <th className="py-2 pr-4 font-semibold">Active deliveries</th>
                  <th className="py-2 pr-4 font-semibold">Status</th>
                  <th className="py-2 font-semibold" />
                </tr>
              </thead>
              <tbody>
                {couriers.map((courier) => (
                  <tr key={courier.courierProfileId} className="border-b border-ink/5 last:border-0">
                    <td className="py-3 pr-4 font-medium">{courier.courierName ?? "Courier"}</td>
                    <td className="py-3 pr-4">{vehicleLabel(courier.vehicleType)}</td>
                    <td className="py-3 pr-4">{zoneName(courier.zoneId)}</td>
                    <td className="py-3 pr-4 font-semibold">{courier.activeDeliveries}</td>
                    <td className="py-3 pr-4">
                      <span
                        className={`rounded-full px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider ${
                          courier.active ? "bg-mint text-ink" : "bg-ink/10 text-ink/55"
                        }`}
                      >
                        {courier.active ? "Available" : "Unavailable"}
                      </span>
                    </td>
                    <td className="py-3 text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => setEditing(courier)}
                          className="rounded-lg border border-ink/15 px-3 py-1.5 text-xs font-semibold hover:bg-ink hover:text-white transition"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => toggleActive(courier)}
                          disabled={busyId === courier.courierProfileId}
                          className="rounded-lg border border-ink/15 px-3 py-1.5 text-xs font-semibold hover:bg-ink hover:text-white transition disabled:opacity-50"
                        >
                          {courier.active ? "Make unavailable" : "Make available"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="mt-3 text-xs text-ink/40">Couriers with the fewest deliveries are listed first.</p>
      </section>

      <section className="rounded-2xl border border-ink/10 bg-white p-5">
        <h2 className="font-display text-xl font-bold">Add a courier</h2>
        <p className="mt-1 text-sm text-ink/55">
          Choose a courier, then set their vehicle and delivery area.
        </p>
        <form onSubmit={createCourier} className="mt-5 grid gap-4 sm:grid-cols-3">
          <label className="block space-y-1.5">
            <span className="text-sm font-semibold text-ink/80">Courier</span>
            <select className={inputClass} value={userId} onChange={(e) => setUserId(e.target.value)}>
              <option value="">Choose a courier</option>
              {candidates.map((candidate) => (
                <option key={candidate.userId} value={candidate.userId}>
                  {candidate.fullName ?? "Unnamed courier"}
                </option>
              ))}
            </select>
            {candidates.length === 0 && (
              <span className="block text-xs text-ink/45">
                Everyone with a courier account has already been added. Ask an administrator to create a new courier account first.
              </span>
            )}
          </label>
          <label className="block space-y-1.5">
            <span className="text-sm font-semibold text-ink/80">Vehicle</span>
            <select
              className={inputClass}
              value={vehicleType}
              onChange={(e) => setVehicleType(e.target.value as VehicleType)}
            >
              {VEHICLES.map((vehicle) => (
                <option key={vehicle.value} value={vehicle.value}>
                  {vehicle.label}
                </option>
              ))}
            </select>
          </label>
          <label className="block space-y-1.5">
            <span className="text-sm font-semibold text-ink/80">Delivery area</span>
            <select className={inputClass} value={zoneId} onChange={(e) => setZoneId(e.target.value)}>
              <option value="">Choose a delivery area</option>
              {zones.map((zone) => (
                <option key={zone.id} value={zone.id}>
                  {zone.name} ({zone.district})
                </option>
              ))}
            </select>
          </label>
          {formError && (
            <p className="rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral sm:col-span-3">{formError}</p>
          )}
          <button
            disabled={saving}
            className="rounded-xl bg-ink px-4 py-3 font-semibold text-white disabled:opacity-60 sm:col-span-3 sm:w-fit transition hover:bg-coral"
          >
            {saving ? "Adding…" : "Add courier"}
          </button>
        </form>
      </section>

      {editing && (
        <EditCourierDialog
          courier={editing}
          zones={zones}
          onClose={() => setEditing(null)}
          onSaved={(message) => {
            setEditing(null);
            setNotice(message);
            void load();
          }}
        />
      )}
    </div>
  );
}