import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Check, MapPin, Pencil, Power, RefreshCw, Truck, User, UserCheck, UserX, Users } from "lucide-react";
import { getApiErrorMessage } from "../../../lib/api";
import { dispatchApi } from "../api";
import { EditCourierDialog } from "../components/EditCourierDialog";
import { PageHeader } from "../components/PageHeader";
import { Field, SectionCard, fieldClass, ghostButtonClass, primaryButtonClass } from "../components/Section";
import type { CourierCandidate, CourierWorkload, VehicleType, ZoneOption } from "../types";
import { VEHICLES, vehicleLabel } from "../vehicles";

type Filter = "all" | "active" | "inactive";

const FILTERS: { value: Filter; label: string }[] = [
  { value: "all", label: "All" },
  { value: "active", label: "Available" },
  { value: "inactive", label: "Unavailable" },
];

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
      setCouriers(await dispatchApi.workload());
    } catch (err) {
      setError(getApiErrorMessage(err, "We couldn't load the couriers. Please refresh the page."));
    } finally {
      setLoading(false);
    }
  }, []);

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

  const visible = couriers.filter(
    (courier) => filter === "all" || (filter === "active" ? courier.active : !courier.active),
  );
  const availableCount = couriers.filter((courier) => courier.active).length;
  const inProgress = couriers.reduce((sum, courier) => sum + courier.activeDeliveries, 0);
  const stats = [
    { label: "Total couriers", value: couriers.length, note: "Everyone you can assign parcels to", icon: Users, tone: "bg-mist text-ink" },
    { label: "Available", value: availableCount, note: "Ready to take parcels", icon: UserCheck, tone: "bg-mint text-ink" },
    { label: "Unavailable", value: couriers.length - availableCount, note: "Not taking parcels right now", icon: UserX, tone: "bg-coral/10 text-coral" },
    { label: "Deliveries in progress", value: inProgress, note: "Across all couriers", icon: Truck, tone: "bg-ink/5 text-ink" },
  ];

  const toggleActive = async (courier: CourierWorkload) => {
    setBusyId(courier.courierProfileId);
    setError("");
    setNotice("");
    try {
      await dispatchApi.updateCourier(courier.courierProfileId, { active: !courier.active });
      setNotice(
        `${courier.courierName ?? "The courier"} is now ${courier.active ? "unavailable" : "available"}.`,
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
        icon={Truck}
        eyebrow="Operations desk"
        badges={[
          { label: `${couriers.length} couriers` },
          { label: `${availableCount} available`, tone: "mint" },
        ]}
        title="Couriers"
        text="See your couriers, how many deliveries each one has right now, and who is available."
        actions={
          <button
            type="button"
            onClick={() => {
              void load();
              void loadLookups();
            }}
            className={ghostButtonClass}
          >
            <RefreshCw size={16} />
            Refresh
          </button>
        }
      />

      {notice && <p className="mb-4 rounded-xl bg-mint/40 px-4 py-3 text-sm text-ink">{notice}</p>}
      {error && <p className="mb-4 rounded-xl bg-coral/10 px-4 py-3 text-sm text-coral">{error}</p>}

      <div className="mb-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {stats.map(({ label, value, note, icon: Icon, tone }) => (
          <div key={label} className="rounded-3xl border border-ink/10 bg-white p-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-ink/50">{label}</span>
              <span className={`grid h-9 w-9 place-items-center rounded-xl ${tone}`}>
                <Icon size={17} />
              </span>
            </div>
            <p className="mt-4 font-display text-4xl font-bold">{loading ? "…" : value}</p>
            <p className="mt-1 text-xs text-ink/45">{note}</p>
          </div>
        ))}
      </div>

      <SectionCard
        eyebrow="Courier team"
        title={`Courier list${loading ? "" : ` (${visible.length})`}`}
        description="Couriers with the fewest deliveries are listed first."
        aside={
          <div className="inline-flex rounded-2xl border border-ink/10 p-1">
            {FILTERS.map((option) => (
              <button
                key={option.value}
                onClick={() => setFilter(option.value)}
                className={`rounded-xl px-4 py-2 text-sm font-semibold transition ${
                  filter === option.value ? "bg-ink text-white" : "text-ink/60 hover:text-ink"
                }`}
              >
                {option.label}
              </button>
            ))}
          </div>
        }
      >
        {loading ? (
          <p className="text-sm text-ink/50">Loading couriers…</p>
        ) : visible.length === 0 ? (
          error ? null : (
            <p className="rounded-2xl border border-dashed border-ink/20 p-6 text-sm text-ink/55">
              No couriers to show yet. Add one below.
            </p>
          )
        ) : (
          <div className="overflow-x-auto rounded-2xl border border-ink/10">
            <table className="w-full min-w-[46rem] text-left text-sm">
              <thead>
                <tr className="border-b border-ink/10 bg-mist/60 text-xs uppercase tracking-wider text-ink/45">
                  <th className="px-4 py-3 font-semibold">ID</th>
                  <th className="px-4 py-3 font-semibold">Courier</th>
                  <th className="px-4 py-3 font-semibold">Vehicle</th>
                  <th className="px-4 py-3 font-semibold">Deliveries now</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 text-right font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((courier) => (
                  <tr key={courier.courierProfileId} className="border-b border-ink/5 last:border-0">
                    <td className="px-4 py-4 font-mono text-xs text-ink/45">#{courier.courierProfileId}</td>
                    <td className="px-4 py-4">
                      <div className="flex items-center gap-3">
                        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-mint font-bold text-ink">
                          {(courier.courierName ?? "C").charAt(0).toUpperCase()}
                        </span>
                        <div>
                          <p className="font-semibold">{courier.courierName ?? "Courier"}</p>
                          <p className="font-mono text-xs text-ink/50">{zoneName(courier.zoneId)}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-4">
                      <span className="inline-flex items-center gap-1.5 rounded-full border border-coral/25 bg-coral/5 px-2.5 py-1 text-xs font-semibold text-coral">
                        <Truck size={13} />
                        {vehicleLabel(courier.vehicleType)}
                      </span>
                    </td>
                    <td className="px-4 py-4">
                      <span className="inline-flex min-w-8 justify-center rounded-full bg-ink/5 px-2.5 py-1 text-xs font-bold">
                        {courier.activeDeliveries}
                      </span>
                    </td>
                    <td className="px-4 py-4">
                      <span
                        className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-semibold ${
                          courier.active ? "border-mint bg-mint/40 text-ink" : "border-coral/30 bg-coral/10 text-coral"
                        }`}
                      >
                        <span className={`h-1.5 w-1.5 rounded-full ${courier.active ? "bg-ink" : "bg-coral"}`} />
                        {courier.active ? "Available" : "Unavailable"}
                      </span>
                    </td>
                    <td className="px-4 py-4 text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => setEditing(courier)}
                          className="inline-flex items-center gap-1.5 rounded-xl border border-ink/15 px-3 py-2 text-xs font-semibold transition hover:bg-ink hover:text-white"
                        >
                          <Pencil size={13} />
                          Edit
                        </button>
                        <button
                          onClick={() => toggleActive(courier)}
                          disabled={busyId === courier.courierProfileId}
                          className={`inline-flex items-center gap-1.5 rounded-xl border px-3 py-2 text-xs font-semibold transition disabled:opacity-50 ${
                            courier.active
                              ? "border-coral/30 bg-coral/5 text-coral hover:bg-coral hover:text-white"
                              : "border-ink/15 hover:bg-ink hover:text-white"
                          }`}
                        >
                          <Power size={13} />
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
      </SectionCard>

      <SectionCard
        eyebrow="New courier"
        title="Add a courier"
        description="Choose a courier, then set their vehicle and delivery area."
      >
        <form onSubmit={createCourier} className="grid gap-6 sm:grid-cols-3">
          <Field
            label="Courier"
            icon={User}
            hint={
              candidates.length === 0
                ? "Everyone with a courier account has already been added. Ask an administrator to create a new courier account first."
                : "People who have a courier account but are not added yet."
            }
          >
            <select className={fieldClass} value={userId} onChange={(e) => setUserId(e.target.value)}>
              <option value="">Choose a courier</option>
              {candidates.map((candidate) => (
                <option key={candidate.userId} value={candidate.userId}>
                  {candidate.fullName ?? "Unnamed courier"}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Vehicle" icon={Truck} hint="What this courier delivers with.">
            <select
              className={fieldClass}
              value={vehicleType}
              onChange={(e) => setVehicleType(e.target.value as VehicleType)}
            >
              {VEHICLES.map((vehicle) => (
                <option key={vehicle.value} value={vehicle.value}>
                  {vehicle.label}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Delivery area" icon={MapPin} hint="Parcels from this area are recommended to this courier.">
            <select className={fieldClass} value={zoneId} onChange={(e) => setZoneId(e.target.value)}>
              <option value="">Choose a delivery area</option>
              {zones.map((zone) => (
                <option key={zone.id} value={zone.id}>
                  {zone.name} ({zone.district})
                </option>
              ))}
            </select>
          </Field>
          {formError && (
            <p className="rounded-xl bg-coral/10 px-4 py-3 text-sm text-coral sm:col-span-3">{formError}</p>
          )}
          <div className="flex justify-end border-t border-ink/10 pt-6 sm:col-span-3">
            <button disabled={saving} className={primaryButtonClass}>
              <Check size={17} />
              {saving ? "Adding…" : "Add courier"}
            </button>
          </div>
        </form>
      </SectionCard>

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