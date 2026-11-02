import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { getApiErrorMessage } from "../../../lib/api";
import { dispatchApi } from "../api";
import { PageHeader } from "../components/PageHeader";
import type { CourierWorkload, VehicleType } from "../types";

const VEHICLES: { value: VehicleType; label: string }[] = [
  { value: "BICYCLE", label: "Bicycle" },
  { value: "MOTORBIKE", label: "Motorbike" },
  { value: "THREE_WHEELER", label: "Three-wheeler" },
  { value: "CAR", label: "Car" },
  { value: "VAN", label: "Van" },
];

const vehicleLabel = (type: VehicleType) => VEHICLES.find((v) => v.value === type)?.label ?? type;

type Filter = "all" | "active" | "inactive";

const FILTERS: { value: Filter; label: string }[] = [
  { value: "all", label: "All" },
  { value: "active", label: "Active" },
  { value: "inactive", label: "Inactive" },
];

const inputClass =
  "w-full rounded-xl border border-ink/15 bg-white px-3.5 py-3 text-sm outline-none transition placeholder:text-ink/30 focus:border-coral focus:ring-4 focus:ring-coral/10";

export function CouriersPage() {
  const [filter, setFilter] = useState<Filter>("all");
  const [couriers, setCouriers] = useState<CourierWorkload[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busyId, setBusyId] = useState<number | null>(null);

  const [userId, setUserId] = useState("");
  const [zoneId, setZoneId] = useState("");
  const [vehicleType, setVehicleType] = useState<VehicleType>("MOTORBIKE");
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const active = filter === "all" ? undefined : filter === "active";
      setCouriers(await dispatchApi.workload({ active }));
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not load couriers."));
    } finally {
      setLoading(false);
    }
  }, [filter]);

  useEffect(() => {
    void load();
  }, [load]);

  const toggleActive = async (courier: CourierWorkload) => {
    setBusyId(courier.courierProfileId);
    setError("");
    setNotice("");
    try {
      await dispatchApi.updateCourier(courier.courierProfileId, { active: !courier.active });
      setNotice(`Courier #${courier.courierProfileId} is now ${courier.active ? "inactive" : "active"}.`);
      await load();
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not update the courier."));
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
    if (!Number.isInteger(userIdNumber) || userIdNumber <= 0 || !Number.isInteger(zoneIdNumber) || zoneIdNumber <= 0) {
      setFormError("Enter a valid user ID and zone ID (whole numbers above 0).");
      return;
    }
    setSaving(true);
    try {
      await dispatchApi.createCourier({ userId: userIdNumber, vehicleType, zoneId: zoneIdNumber });
      setNotice("Courier profile created.");
      setUserId("");
      setZoneId("");
      await load();
    } catch (err) {
      setFormError(getApiErrorMessage(err, "Could not create the courier profile."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <PageHeader
        eyebrow="Operations desk"
        title="Couriers"
        text="Courier profiles, how busy each courier is, and who is available for assignments."
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
          <p className="rounded-xl border border-dashed border-ink/20 p-6 text-sm text-ink/55">
            No couriers found. Create a courier profile below.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[34rem] text-left text-sm">
              <thead>
                <tr className="border-b border-ink/10 text-xs uppercase tracking-wider text-ink/45">
                  <th className="py-2 pr-4 font-semibold">Courier</th>
                  <th className="py-2 pr-4 font-semibold">Vehicle</th>
                  <th className="py-2 pr-4 font-semibold">Zone</th>
                  <th className="py-2 pr-4 font-semibold">Active deliveries</th>
                  <th className="py-2 pr-4 font-semibold">Status</th>
                  <th className="py-2 font-semibold" />
                </tr>
              </thead>
              <tbody>
                {couriers.map((courier) => (
                  <tr key={courier.courierProfileId} className="border-b border-ink/5 last:border-0">
                    <td className="py-3 pr-4 font-medium">User #{courier.userId}</td>
                    <td className="py-3 pr-4">{vehicleLabel(courier.vehicleType)}</td>
                    <td className="py-3 pr-4">Zone #{courier.zoneId}</td>
                    <td className="py-3 pr-4 font-semibold">{courier.activeDeliveries}</td>
                    <td className="py-3 pr-4">
                      <span
                        className={`rounded-full px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider ${
                          courier.active ? "bg-mint text-ink" : "bg-ink/10 text-ink/55"
                        }`}
                      >
                        {courier.active ? "Active" : "Inactive"}
                      </span>
                    </td>
                    <td className="py-3 text-right">
                      <button
                        onClick={() => toggleActive(courier)}
                        disabled={busyId === courier.courierProfileId}
                        className="rounded-lg border border-ink/15 px-3 py-1.5 text-xs font-semibold hover:bg-ink/5 disabled:opacity-50"
                      >
                        {courier.active ? "Deactivate" : "Activate"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="mt-3 text-xs text-ink/40">Sorted by workload: the least busy courier comes first.</p>
      </section>

      <section className="rounded-2xl border border-ink/10 bg-white p-5">
        <h2 className="font-display text-xl font-bold">Create a courier profile</h2>
        <p className="mt-1 text-sm text-ink/55">
          The user must already have an account with the COURIER role (an admin can create one on the Users page).
        </p>
        <form onSubmit={createCourier} className="mt-5 grid gap-4 sm:grid-cols-3">
          <label className="block space-y-1.5">
            <span className="text-sm font-semibold text-ink/80">User ID</span>
            <input
              className={inputClass}
              inputMode="numeric"
              placeholder="e.g. 12"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
            />
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
            <span className="text-sm font-semibold text-ink/80">Zone ID</span>
            <input
              className={inputClass}
              inputMode="numeric"
              placeholder="e.g. 1"
              value={zoneId}
              onChange={(e) => setZoneId(e.target.value)}
            />
          </label>
          {formError && (
            <p className="rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral sm:col-span-3">{formError}</p>
          )}
          <button
            disabled={saving}
            className="rounded-xl bg-ink px-4 py-3 font-semibold text-white disabled:opacity-60 sm:col-span-3 sm:w-fit"
          >
            {saving ? "Creating…" : "Create profile"}
          </button>
        </form>
      </section>
    </div>
  );
}