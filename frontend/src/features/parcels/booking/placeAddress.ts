import { DISTRICTS } from "../parcels.api";
import type { ParcelAddress } from "../parcels.api";

// Only the parts of a Nominatim "reverse" answer that we read.
export interface ReverseResult {
  display_name?: string;
  address?: {
    house_number?: string;
    road?: string;
    neighbourhood?: string;
    suburb?: string;
    village?: string;
    town?: string;
    city?: string;
    county?: string;
    state_district?: string;
    country_code?: string;
  };
}

// Only the parts of a Photon "reverse" feature that we read.
export interface PhotonProperties {
  name?: string;
  housenumber?: string;
  street?: string;
  district?: string;
  city?: string;
  county?: string;
  countrycode?: string;
}

// Photon names the fields differently, so we rename them to match the Nominatim answer.
export function reverseFromPhoton(p: PhotonProperties): ReverseResult {
  const place = [p.name, p.city].filter(Boolean).join(", ");
  return {
    display_name: place || undefined,
    address: {
      house_number: p.housenumber,
      road: p.street,
      suburb: p.district,
      city: p.city,
      county: p.county,
      country_code: p.countrycode?.toLowerCase(),
    },
  };
}

// OpenStreetMap sometimes spells a district differently from our list.
const DISTRICT_ALIASES: Record<string, string> = {
  moneragala: "monaragala",
  kegalla: "kegalle",
  mullaittivu: "mullaitivu",
  nuwaraeliya: "nuwara eliya",
};

// "Colombo District" -> "Colombo". Returns null when it is not a district we deliver to.
export function findDistrict(text: string | undefined | null): string | null {
  if (!text) return null;
  let name = text.toLowerCase().replace(/ district$/, "").trim();
  name = DISTRICT_ALIASES[name] ?? name;
  return DISTRICTS.find((d) => d.toLowerCase() === name) ?? null;
}

// Turns the answer for a map point into our address.
// Returns null when the point is outside Sri Lanka or outside the districts we deliver to.
export function addressFromReverse(result: ReverseResult): ParcelAddress | null {
  const a = result.address;
  if (!a || a.country_code !== "lk") return null;

  const district = [a.state_district, a.county, a.city, a.town].map(findDistrict).find((d) => d !== null);
  if (!district) return null;

  const city = a.city ?? a.town ?? a.village ?? a.suburb ?? district;
  const area = a.suburb ?? a.neighbourhood;
  let line1 = [a.house_number, a.road, area && area !== city ? area : ""].filter(Boolean).join(", ");
  if (!line1 && result.display_name) {
    line1 = result.display_name.split(",").slice(0, 2).join(",").trim();
  }
  if (!line1) return null;

  return { line1: line1.slice(0, 200), city: city.slice(0, 100), district };
}
