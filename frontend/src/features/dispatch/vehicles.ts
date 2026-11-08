import type { VehicleType } from "./types";

export const VEHICLES: { value: VehicleType; label: string }[] = [
  { value: "BICYCLE", label: "Bicycle" },
  { value: "MOTORBIKE", label: "Motorbike" },
  { value: "THREE_WHEELER", label: "Three-wheeler" },
  { value: "CAR", label: "Car" },
  { value: "VAN", label: "Van" },
];

export const vehicleLabel = (type: VehicleType) => VEHICLES.find((v) => v.value === type)?.label ?? type;