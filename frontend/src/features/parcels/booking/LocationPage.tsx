import { ChevronRight, Heart, LocateFixed, MapPinned, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { fetchUserAddresses } from "../../profile/profile.api";
import { BookingHeader } from "./BookingHeader";
import { useBookingDraft } from "./BookingDraft";
import { currentPosition, searchSuggestions } from "./mapService";
import type { LatLng, Suggestion } from "./mapService";

const rowClass = "flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-mist";

// Second page: how does the customer want to set this location?
export function LocationPage() {
  const navigate = useNavigate();
  const { target, setStartAt, pickup, dropoff } = useBookingDraft();
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [savedCount, setSavedCount] = useState(0);
  const [note, setNote] = useState("");

  useEffect(() => {
    fetchUserAddresses()
      .then((list) => setSavedCount(list.length))
      .catch(() => setSavedCount(0));
  }, []);

  // Suggest places as the customer types (waits a moment so we do not search on every key).
  useEffect(() => {
    if (query.trim().length < 3) {
      setSuggestions([]);
      return;
    }
    let cancelled = false;
    const timer = setTimeout(async () => {
      try {
        const found = await searchSuggestions(query.trim());
        if (!cancelled) setSuggestions(found);
      } catch {
        if (!cancelled) setNote("Could not search places right now. Please try again.");
      }
    }, 350);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [query]);

  const openConfirm = (position: LatLng | null) => {
    setStartAt(position);
    navigate("/parcels/new/confirm");
  };

  const choose = (suggestion: Suggestion) => {
    setNote("");
    openConfirm(suggestion.position);
  };

  const useMyLocation = async () => {
    setNote("");
    try {
      openConfirm(await currentPosition());
    } catch (err) {
      setNote(err instanceof Error ? err.message : "Could not get your location.");
    }
  };

  // "Set on map" reopens where this location was set before (if it was).
  const existing = (target === "pickup" ? pickup : dropoff)?.position ?? null;

  const place = target === "pickup" ? "Pick up" : "Drop";

  return (
    <div className="mx-auto max-w-4xl">
      <BookingHeader
        title={`${place} location`}
        subtitle="Search for a place, use where you are now, or pick a saved address."
        onBack={() => navigate("/parcels/new")}
      />

      {note && <p className="mb-4 rounded-lg border border-coral/30 bg-coral/10 p-3 text-sm">{note}</p>}

      <div className="grid gap-4 md:grid-cols-2">
        <section className="rounded-2xl border border-ink/5 bg-white p-5 shadow-sm">
          <h2 className="font-display text-xl font-bold">Search</h2>
          <label className="mt-3 flex items-center gap-3 rounded-xl border border-ink/15 bg-white px-4 py-2.5 focus-within:border-coral">
            <Search className="h-5 w-5 text-ink/50" />
            <input
              className="w-full bg-transparent text-sm outline-none"
              placeholder="Search location"
              value={query}
              maxLength={100}
              onChange={(e) => setQuery(e.target.value)}
            />
          </label>

          {suggestions.length > 0 ? (
            <ul className="mt-3 divide-y divide-ink/10 overflow-hidden rounded-xl border border-ink/10">
              {suggestions.map((s) => (
                <li key={`${s.main}-${s.secondary}`}>
                  <button type="button" onClick={() => choose(s)} className={rowClass}>
                    <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-mist text-coral">
                      <MapPinned className="h-5 w-5" />
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-semibold">{s.main}</span>
                      <span className="block truncate text-xs text-ink/55">{s.secondary}</span>
                    </span>
                    <ChevronRight className="h-5 w-5 text-ink/30" />
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-3 rounded-xl bg-mist p-3 text-sm text-ink/60">
              Type at least 3 letters, for example a road, a town or a landmark in Sri Lanka.
            </p>
          )}
        </section>

        <section className="rounded-2xl border border-ink/5 bg-white p-5 shadow-sm">
          <h2 className="font-display text-xl font-bold">Other ways</h2>
          <ul className="mt-3 divide-y divide-ink/10 overflow-hidden rounded-xl border border-ink/10">
            <li>
              <button type="button" onClick={useMyLocation} className={rowClass}>
                <LocateFixed className="h-5 w-5 text-coral" />
                <span className="flex-1 text-sm font-semibold">Your current location</span>
                <ChevronRight className="h-5 w-5 text-ink/30" />
              </button>
            </li>
            <li>
              <button type="button" onClick={() => openConfirm(existing)} className={rowClass}>
                <MapPinned className="h-5 w-5 text-coral" />
                <span className="flex-1 text-sm font-semibold">Set on map</span>
                <ChevronRight className="h-5 w-5 text-ink/30" />
              </button>
            </li>
            <li>
              <button type="button" onClick={() => navigate("/parcels/new/saved")} className={rowClass}>
                <Heart className="h-5 w-5 text-coral" />
                <span className="flex-1 text-sm font-semibold">Saved addresses</span>
                <span className="rounded-full bg-mist px-2.5 py-0.5 text-xs font-semibold text-ink/60">{savedCount}</span>
                <ChevronRight className="h-5 w-5 text-ink/30" />
              </button>
            </li>
          </ul>
        </section>
      </div>
    </div>
  );
}
