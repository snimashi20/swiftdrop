import { useState } from "react";
import type { FormEvent } from "react";
import { getApiErrorMessage } from "../../../lib/api";
import { dispatchApi } from "../api";
import type { CourierWorkload, VehicleType, ZoneOption } from "../types";
import { VEHICLES } from "../vehicles";
import { Modal } from "./Modal";

const inputClass =
  "w-full rounded-xl border border-ink/15 bg-white px-3.5 py-3 text-sm outline-none transition focus:border-coral focus:ring-4 focus:ring-coral/10";

type EditCourierDialogProps = {
  courier: CourierWorkload;
  zones: ZoneOption[];
  onClose: () => void;
  onSaved: (message: string) => void;
};

export function EditCourierDialog({ courier, zones, onClose, onSaved }: EditCourierDialogProps) {
  const [vehicleType, setVehicleType] = useState<VehicleType>(courier.vehicleType);
  const [zoneId, setZoneId] = useState(String(courier.zoneId));
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const name = courier.courierName ?? `Courier #${courier.courierProfileId}`;

  const save = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      await dispatchApi.updateCourier(courier.courierProfileId, { vehicleType, zoneId: Number(zoneId) });
      onSaved(`${name} updated.`);
    } catch (err) {
      setError(getApiErrorMessage(err, "We couldn't update this courier. Please try again."));
      setSaving(false);
    }
  };

  return (
    <Modal title="Edit courier" onClose={onClose}>
      <p className="mb-4 text-sm text-ink/60">{name}</p>
      <form onSubmit={save} className="space-y-4">
        <label className="block space-y-1.5">
          <span className="text-sm font-semibold text-ink/80">Vehicle</span>
          <select className={inputClass} value={vehicleType} onChange={(e) => setVehicleType(e.target.value as VehicleType)}>
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
            {zones.map((zone) => (
              <option key={zone.id} value={zone.id}>
                {zone.name} ({zone.district})
              </option>
            ))}
          </select>
        </label>
        {error && <p className="rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral">{error}</p>}
        <div className="flex justify-end gap-3">
          <button type="button" onClick={onClose} className="rounded-xl border border-ink/15 px-4 py-2.5 text-sm font-semibold">
            Cancel
          </button>
          <button disabled={saving} className="rounded-xl bg-ink px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50 transition hover:bg-coral">
            {saving ? "Saving…" : "Save changes"}
          </button>
        </div>
      </form>
    </Modal>
  );
}