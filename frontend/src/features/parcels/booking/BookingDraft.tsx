import { createContext, useContext, useState } from "react";
import type { ReactNode } from "react";
import type { ParcelAddress } from "../parcels.api";
import type { LatLng } from "./mapService";

export type BookingMode = "send" | "receive";
export type BookingTarget = "pickup" | "drop";

// A location the customer has chosen. The position is null for a saved address.
export interface ChosenLocation {
  address: ParcelAddress;
  position: LatLng | null;
}

interface BookingDraft {
  mode: BookingMode | null;
  setMode: (mode: BookingMode) => void;
  pickup: ChosenLocation | null;
  dropoff: ChosenLocation | null;
  setChosen: (target: BookingTarget, chosen: ChosenLocation) => void;
  target: BookingTarget; // which row the customer is filling in
  setTarget: (target: BookingTarget) => void;
  startAt: LatLng | null; // where the confirm map opens
  setStartAt: (position: LatLng | null) => void;
}

const BookingDraftContext = createContext<BookingDraft | null>(null);

// Keeps the booking choices while the customer moves between the booking pages.
// It is created when they enter /parcels/new and dropped when they leave.
export function BookingDraftProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<BookingMode | null>(null);
  const [pickup, setPickup] = useState<ChosenLocation | null>(null);
  const [dropoff, setDropoff] = useState<ChosenLocation | null>(null);
  const [target, setTarget] = useState<BookingTarget>("pickup");
  const [startAt, setStartAt] = useState<LatLng | null>(null);

  const setChosen = (t: BookingTarget, chosen: ChosenLocation) => (t === "pickup" ? setPickup(chosen) : setDropoff(chosen));

  return (
    <BookingDraftContext.Provider
      value={{ mode, setMode, pickup, dropoff, setChosen, target, setTarget, startAt, setStartAt }}
    >
      {children}
    </BookingDraftContext.Provider>
  );
}

export function useBookingDraft(): BookingDraft {
  const draft = useContext(BookingDraftContext);
  if (!draft) throw new Error("useBookingDraft must be used inside BookingDraftProvider");
  return draft;
}
