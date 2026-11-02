import type { ParcelStatus, TimelineEntry } from './types';

export type LatLng = [number, number];

/** Where the truck is along the route (0 = pickup, 1 = drop-off), estimated from the status. */
export const STATUS_PROGRESS: Record<ParcelStatus, number> = {
  CREATED: 0,
  PICKED_UP: 0.08,
  IN_TRANSIT: 0.5,
  OUT_FOR_DELIVERY: 0.88,
  DELIVERED: 1,
  FAILED: 0,
};

export function progressFor(status: ParcelStatus, timeline: TimelineEntry[]): number {
  if (status !== 'FAILED') return STATUS_PROGRESS[status];
  // A failed parcel stays where it last was.
  const last = [...timeline].reverse().find((e) => e.status !== 'FAILED');
  return last ? STATUS_PROGRESS[last.status] : 0;
}

function distance(a: LatLng, b: LatLng): number {
  const dx = (b[1] - a[1]) * Math.cos((((a[0] + b[0]) / 2) * Math.PI) / 180);
  const dy = b[0] - a[0];
  return Math.hypot(dx, dy);
}

/** Splits a route at `fraction` of its length: travelled part, remaining part, and the point itself. */
export function splitAt(path: LatLng[], fraction: number) {
  const f = Math.min(1, Math.max(0, fraction));
  if (path.length < 2) {
    return { done: path, remaining: path, position: path[0] };
  }
  const segments = path.slice(1).map((p, i) => distance(path[i], p));
  const target = segments.reduce((a, b) => a + b, 0) * f;

  let walked = 0;
  for (let i = 0; i < segments.length; i++) {
    if (segments[i] > 0 && walked + segments[i] >= target) {
      const t = (target - walked) / segments[i];
      const a = path[i];
      const b = path[i + 1];
      const position: LatLng = [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t];
      return {
        done: [...path.slice(0, i + 1), position],
        remaining: [position, ...path.slice(i + 1)],
        position,
      };
    }
    walked += segments[i];
  }
  const end = path[path.length - 1];
  return { done: path, remaining: [end], position: end };
}
