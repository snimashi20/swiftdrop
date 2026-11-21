import { addressFromReverse, reverseFromPhoton } from "./placeAddress";
import type { PhotonProperties, ReverseResult } from "./placeAddress";
import type { ParcelAddress } from "../parcels.api";

export type LatLng = [number, number];

// Free services, no key needed: Photon (search while typing) and Nominatim (address of a map point).
const SEARCH_URL = "https://photon.komoot.io/api/";
const PHOTON_REVERSE_URL = "https://photon.komoot.io/reverse";
const REVERSE_URL = "https://nominatim.openstreetmap.org/reverse";

export interface Suggestion {
  main: string;
  secondary: string;
  position: LatLng;
}

interface PhotonFeature {
  geometry: { coordinates: [number, number] };
  properties: {
    name?: string;
    street?: string;
    housenumber?: string;
    district?: string;
    city?: string;
    county?: string;
    state?: string;
    countrycode?: string;
  };
}

// Place suggestions while the customer types, Sri Lanka only.
export async function searchSuggestions(input: string): Promise<Suggestion[]> {
  const url = `${SEARCH_URL}?q=${encodeURIComponent(input)}&limit=8&lang=en&lat=7.87&lon=80.77`;
  const res = await fetch(url);
  if (!res.ok) throw new Error("Search failed");
  const data = (await res.json()) as { features: PhotonFeature[] };
  return data.features
    .filter((f) => f.properties.countrycode === "LK")
    .map((f) => {
      const p = f.properties;
      const main = p.name ?? [p.housenumber, p.street].filter(Boolean).join(" ");
      const secondary = [p.street, p.district, p.city, p.county]
        .filter((part, i, all) => part && part !== main && all.indexOf(part) === i)
        .join(", ");
      // Photon gives [longitude, latitude]; the map wants [latitude, longitude].
      return { main, secondary, position: [f.geometry.coordinates[1], f.geometry.coordinates[0]] as LatLng };
    })
    .filter((s) => s.main);
}

// Finds the address of a map point. Photon first; if it is busy, Nominatim is the backup.
export async function reverseGeocode(lat: number, lng: number): Promise<ParcelAddress | null> {
  try {
    const res = await fetch(`${PHOTON_REVERSE_URL}?lat=${lat}&lon=${lng}&lang=en`);
    if (!res.ok) throw new Error("Photon failed");
    const data = (await res.json()) as { features: { properties: PhotonProperties }[] };
    const first = data.features[0];
    return first ? addressFromReverse(reverseFromPhoton(first.properties)) : null;
  } catch {
    const url = `${REVERSE_URL}?format=jsonv2&addressdetails=1&zoom=18&accept-language=en&lat=${lat}&lon=${lng}`;
    const res = await fetch(url);
    if (!res.ok) throw new Error("Address lookup failed");
    return addressFromReverse((await res.json()) as ReverseResult);
  }
}

// Asks the browser for the customer's current position.
export function currentPosition(): Promise<LatLng> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error("Your browser cannot share your location."));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (p) => resolve([p.coords.latitude, p.coords.longitude]),
      () => reject(new Error("Could not get your location. Please search for the address instead.")),
    );
  });
}
