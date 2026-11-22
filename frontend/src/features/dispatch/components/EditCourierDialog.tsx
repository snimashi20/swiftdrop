import { useState } from "react";
import type { FormEvent } from "react";
import { Check, MapPin, Truck } from "lucide-react";
import { getApiErrorMessage } from "../../../lib/api";
import { dispatchApi } from "../api";
import type { CourierWorkload, VehicleType, ZoneOption } from "../types";
import { VEHICLES } from "../vehicles";
import { Modal } from "./Modal";
import { Field, fieldClass, ghostButtonClass, primaryButtonClass } from "./Section";

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
      <div className="mb-6 flex items-center gap-3">
        <span className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-mint font-bold text-ink">
          {name.charAt(0).toUpperCase()}
        </span>
        <p className="font-semibold">{name}</p>
      </div>
      <form onSubmit={save} className="space-y-5">
        <Field label="Vehicle" icon={Truck} hint="What this courier delivers with.">
          <select className={fieldClass} value={vehicleType} onChange={(e) => setVehicleType(e.target.value as VehicleType)}>
            {VEHICLES.map((vehicle) => (
              <option key={vehicle.value} value={vehicle.value}>
                {vehicle.label}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Delivery area" icon={MapPin} hint="Parcels from this area are recommended to this courier.">
          <select className={fieldClass} value={zoneId} onChange={(e) => setZoneId(e.target.value)}>
            {zones.map((zone) => (
              <option key={zone.id} value={zone.id}>
                {zone.name} ({zone.district})
              </option>
            ))}
          </select>
        </Field>
        {error && <p className="rounded-xl bg-coral/10 px-3 py-2 text-sm text-coral">{error}</p>}
        <div className="flex justify-end gap-3 border-t border-ink/10 pt-5">
          <button type="button" onClick={onClose} className={ghostButtonClass}>
            Cancel
          </button>
          <button disabled={saving} className={primaryButtonClass}>
            <Check size={16} />
            {saving ? "Saving…" : "Save changes"}
          </button>
        </div>
      </form>
    </Modal>
  );
}