import type { LatLng } from './geo';

// The public tracking API returns city names only (no addresses). The map
// places them with this table. Add more rows here if a new zone is seeded.
const CITIES: Record<string, LatLng> = {
  colombo: [6.9271, 79.8612],
  gampaha: [7.0873, 79.9925],
  negombo: [7.2083, 79.8358],
  kalutara: [6.5854, 79.9607],
  galle: [6.0535, 80.221],
  matara: [5.9549, 80.555],
  hambantota: [6.1241, 81.1185],
  kandy: [7.2906, 80.6337],
  matale: [7.4675, 80.6234],
  'nuwara eliya': [6.9497, 80.7891],
  kegalle: [7.2513, 80.3464],
  ratnapura: [6.6828, 80.3992],
  kurunegala: [7.4863, 80.3647],
  puttalam: [8.0408, 79.8394],
  anuradhapura: [8.3114, 80.4037],
  polonnaruwa: [7.9403, 81.0188],
  badulla: [6.9934, 81.055],
  monaragala: [6.8728, 81.3507],
  trincomalee: [8.5874, 81.2152],
  batticaloa: [7.731, 81.6747],
  ampara: [7.2912, 81.6724],
  jaffna: [9.6615, 80.0255],
  kilinochchi: [9.3803, 80.377],
  mannar: [8.981, 79.9044],
  vavuniya: [8.7542, 80.4982],
  mullaitivu: [9.2671, 80.8142],
};

export function coordsFor(city: string): LatLng | null {
  return CITIES[city.trim().toLowerCase()] ?? null;
}
