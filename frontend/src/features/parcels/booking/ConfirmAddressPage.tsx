import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { LocateFixed, MapPin } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { ParcelAddress } from "../parcels.api";
import { BookingHeader } from "./BookingHeader";
import { useBookingDraft } from "./BookingDraft";
import { currentPosition, reverseGeocode } from "./mapService";
import type { LatLng } from "./mapService";

const COLOMBO: LatLng = [6.9271, 79.8612];

// Third page: the customer moves the map so the pin sits on their exact spot, then confirms.
export function ConfirmAddressPage() {
  const navigate = useNavigate();
  const { target, startAt, pickup, dropoff, setChosen } = useBookingDraft();
  const mapDiv = useRef<HTMLDivElement>(null);
  const map = useRef<L.Map | null>(null);
  const lookupId = useRef(0); // ignores answers that arrive after the pin has moved again

  const [address, setAddress] = useState<ParcelAddress | null>(null);
  const [center, setCenter] = useState<LatLng | null>(null);
  const [busy, setBusy] = useState(true);
  const [note, setNote] = useState("");

  // Look up the address under the pin.
  const lookup = async (lat: number, lng: number) => {
    const id = ++lookupId.current;
    setBusy(true);
    setNote("");
    try {
      const found = await reverseGeocode(lat, lng);
      if (id !== lookupId.current) return;
      setAddress(found);
      setCenter([lat, lng]);
      if (!found) setNote("We do not deliver to this spot yet. Move the pin inside a delivery district.");
    } catch {
      if (id !== lookupId.current) return;
      setAddress(null);
      setNote("Could not find the address here. Check your connection and try again.");
    } finally {
      if (id === lookupId.current) setBusy(false);
    }
  };

  useEffect(() => {
    if (!mapDiv.current) return;
    const existing = (target === "pickup" ? pickup : dropoff)?.position;
    const start = startAt ?? existing ?? COLOMBO;
    const m = L.map(mapDiv.current).setView(start, startAt || existing ? 17 : 12);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: "&copy; OpenStreetMap contributors",
    }).addTo(m);
    map.current = m;

    // "moveend" fires each time the map stops moving; look up the address under the pin then.
    // It waits a moment so the free address service is not asked on every small move.
    let timer: ReturnType<typeof setTimeout>;
    const onMoveEnd = () => {
      clearTimeout(timer);
      setBusy(true);
      timer = setTimeout(() => {
        const c = m.getCenter();
        lookup(c.lat, c.lng);
      }, 600);
    };
    m.on("moveend", onMoveEnd);
    onMoveEnd();

    return () => {
      clearTimeout(timer);
      m.off("moveend", onMoveEnd);
      m.remove();
      map.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const goToMyLocation = async () => {
    try {
      const [lat, lng] = await currentPosition();
      map.current?.setView([lat, lng], 17);
    } catch (err) {
      setNote(err instanceof Error ? err.message : "Could not get your location.");
    }
  };

  const confirm = () => {
    if (!address || !center) return;
    setChosen(target, { address, position: center });
    navigate("/parcels/new");
  };

  const place = target === "pickup" ? "Pick up" : "Drop";

  return (
    <div className="mx-auto max-w-5xl">
      <BookingHeader
        title="Confirm address"
        subtitle={`${place} location. Move the map so the pin is on the exact spot.`}
        onBack={() => navigate("/parcels/new/location")}
      />

      <div className="grid gap-4 lg:grid-cols-[1fr_340px]">
        <div className="relative h-[50vh] min-h-80 overflow-hidden rounded-2xl border border-ink/5 bg-mist shadow-sm lg:h-[calc(100vh-14rem)] lg:min-h-[420px]">
          <div ref={mapDiv} className="h-full w-full" data-testid="map" />

          {/* The pin stays in the middle; the map moves underneath it. The pin's tip marks the exact spot. */}
          <div className="pointer-events-none absolute left-1/2 top-1/2 z-[1000] -translate-x-1/2 -translate-y-full text-coral drop-shadow-md">
            <svg width="40" height="50" viewBox="0 0 24 30" aria-hidden="true">
              <path
                d="M12 0C5.4 0 0 5.2 0 11.6 0 20 12 30 12 30s12-10 12-18.4C24 5.2 18.6 0 12 0z"
                fill="currentColor"
              />
              <circle cx="12" cy="11.5" r="4.5" fill="white" />
            </svg>
          </div>

          <button
            type="button"
            aria-label="Go to my location"
            onClick={goToMyLocation}
            className="absolute bottom-4 right-4 z-[1000] grid h-11 w-11 place-items-center rounded-full bg-white shadow-md hover:bg-mist"
          >
            <LocateFixed className="h-5 w-5" />
          </button>
        </div>

        <aside className="space-y-4 self-start rounded-2xl border border-ink/5 bg-white p-5 shadow-sm lg:sticky lg:top-24">
          <h2 className="font-display text-xl font-bold">{place} address</h2>
          {note && <p className="rounded-lg border border-coral/30 bg-coral/10 p-3 text-sm">{note}</p>}
          <div className="flex items-start gap-3 rounded-xl bg-mist p-3 text-sm">
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-coral text-white">
              <MapPin className="h-5 w-5" />
            </span>
            <div className="min-w-0 flex-1">
              {busy ? (
                <p className="py-2 text-ink/60">Finding the address...</p>
              ) : address ? (
                <>
                  <p className="font-semibold">{address.line1}</p>
                  <p className="text-ink/60">
                    {address.city === address.district ? address.district : `${address.city}, ${address.district}`}
                  </p>
                </>
              ) : (
                <p className="py-2 text-ink/60">Move the map so the pin is on your exact location.</p>
              )}
            </div>
          </div>
          <button
            type="button"
            disabled={busy || !address}
            onClick={confirm}
            className="w-full rounded-xl bg-coral px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60"
          >
            Confirm
          </button>
        </aside>
      </div>
    </div>
  );
}
