import { useEffect, useState } from "react";
import { getApiErrorMessage } from "../../../lib/api";
import { dispatchApi } from "../api";
import type { ParcelSummary } from "../assigned";
import type { CourierWorkload, VehicleType, ZoneOption } from "../types";
import { Modal } from "./Modal";

const VEHICLE_LABELS: Record<VehicleType, string> = {
  BICYCLE: "Bicycle",
  MOTORBIKE: "Motorbike",
  THREE_WHEELER: "Three-wheeler",
  CAR: "Car",
  VAN: "Van",
};

type AssignDialogProps = {
  parcel: ParcelSummary;
  /** Set when the parcel already has a courier: the dialog then reassigns. */
  currentCourierId?: number;
  onClose: () => void;
  onAssigned: (message: string) => void;
};

export function AssignDialog({ parcel, currentCourierId, onClose, onAssigned }: AssignDialogProps) {
  const [couriers, setCouriers] = useState<CourierWorkload[]>([]);
  const [zones, setZones] = useState<ZoneOption[]>([]);
  const [suggested, setSuggested] = useState<CourierWorkload | null>(null);
  const [selected, setSelected] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let ignore = false;
    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const [list, suggestion, zoneList] = await Promise.all([
          dispatchApi.workload({ active: true }),
          // No zone or no active courier in the zone gives a 404: that is fine, there is just no suggestion.
          dispatchApi.suggestedCourier(parcel.id).catch(() => null),
          dispatchApi.zones().catch(() => [] as ZoneOption[]),
        ]);
        if (ignore) return;
        setCouriers(list);
        setZones(zoneList);
        setSuggested(suggestion);
        setSelected(suggestion ? suggestion.courierProfileId : null);
      } catch (err) {
        if (!ignore) setError(getApiErrorMessage(err, "Could not load couriers."));
      } finally {
        if (!ignore) setLoading(false);
      }
    };
    void load();
    return () => {
      ignore = true;
    };
  }, [parcel.id]);

  // Couriers of the suggested zone first, then the least busy.
  const zoneId = suggested?.zoneId;
  const ordered = [...couriers].sort(
    (a, b) =>
      Number(b.zoneId === zoneId) - Number(a.zoneId === zoneId) || a.activeDeliveries - b.activeDeliveries,
  );

  const zoneName = (id: number) => zones.find((zone) => zone.id === id)?.name ?? "Unknown area";
  const courierName = (courier: CourierWorkload) => courier.courierName ?? "Courier";

  const assign = async () => {
    if (selected === null) return;
    setSaving(true);
    setError("");
    try {
      await dispatchApi.assignParcel(parcel.id, selected);
      const chosen = couriers.find((c) => c.courierProfileId === selected);
      onAssigned(`Parcel ${parcel.trackingNumber} ${currentCourierId ? "reassigned" : "assigned"} to ${chosen ? courierName(chosen) : `courier #${selected}`}.`);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not assign the parcel."));
      setSaving(false);
    }
  };

  return (
    <Modal title={currentCourierId ? "Reassign parcel" : "Assign parcel"} onClose={onClose}>
      <div className="mb-4 rounded-xl bg-mist p-3 text-sm">
        <p className="font-semibold">{parcel.trackingNumber}</p>
        <p className="text-ink/60">
          {parcel.pickupCity ?? "—"} ({parcel.pickupDistrict ?? "—"}) → {parcel.dropoffCity ?? "—"} ·{" "}
          {parcel.size} · {parcel.weightKg} kg
        </p>
      </div>

      {loading && <p className="text-sm text-ink/50">Loading couriers…</p>}
      {error && <p className="mb-3 rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral">{error}</p>}

      {!loading && (
        <>
          {suggested ? (
            <p className="mb-3 rounded-lg bg-mint/40 px-3 py-2 text-sm">
              Recommended: {courierName(suggested)}, who has the fewest deliveries in {zoneName(suggested.zoneId)} ({suggested.activeDeliveries} right now).
            </p>
          ) : (
            <p className="mb-3 rounded-lg bg-ink/5 px-3 py-2 text-sm text-ink/60">
              We could not pick a courier for you because nobody available covers this pickup area. Please choose one from the list.
            </p>
          )}

          {ordered.length === 0 ? (
            <p className="rounded-xl border border-dashed border-ink/20 p-4 text-sm text-ink/55">
              No couriers are available right now. Add a courier, or mark one as available, on the Couriers page.
            </p>
          ) : (
            <ul className="max-h-72 space-y-2 overflow-y-auto">
              {ordered.map((courier) => {
                const isSuggested = courier.courierProfileId === suggested?.courierProfileId;
                return (
                  <li key={courier.courierProfileId}>
                    <label
                      className={`flex cursor-pointer items-center gap-3 rounded-xl border p-3 text-sm transition ${
                        selected === courier.courierProfileId
                          ? "border-coral bg-coral/5"
                          : "border-ink/10 hover:border-coral hover:bg-coral/5"
                      }`}
                    >
                      <input
                        type="radio"
                        name="courier"
                        checked={selected === courier.courierProfileId}
                        onChange={() => setSelected(courier.courierProfileId)}
                      />
                      <span className="flex-1">
                        <span className="font-semibold">{courierName(courier)}</span>{" "}
                        <span className="text-ink/50">
                          · {VEHICLE_LABELS[courier.vehicleType]} · {zoneName(courier.zoneId)}
                        </span>
                      </span>
                      <span className="text-right">
                        <span className="block font-semibold">{courier.activeDeliveries}</span>
                        <span className="block text-[10px] uppercase tracking-wider text-ink/40">active</span>
                      </span>
                      {isSuggested && (
                        <span className="rounded-full bg-mint px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider">
                          Recommended
                        </span>
                      )}
                      {courier.courierProfileId === currentCourierId && (
                        <span className="rounded-full bg-ink/10 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider">
                          Current
                        </span>
                      )}
                    </label>
                  </li>
                );
              })}
            </ul>
          )}
        </>
      )}

      <div className="mt-5 flex justify-end gap-3">
        <button onClick={onClose} className="rounded-xl border border-ink/15 px-4 py-2.5 text-sm font-semibold">
          Cancel
        </button>
        <button
          onClick={assign}
          disabled={selected === null || saving}
          className="rounded-xl bg-ink px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50 transition hover:bg-coral"
        >
          {saving ? "Saving…" : currentCourierId ? "Reassign parcel" : "Assign parcel"}
        </button>
      </div>
    </Modal>
  );
}