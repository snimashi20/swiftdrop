import { useEffect, useState } from 'react';
import type { LatLng } from './geo';

const cache = new Map<string, LatLng[]>();

/**
 * Route between two points. Starts as a straight line, then is replaced by the
 * real driving route from the public OSRM demo server (fine for a project demo;
 * self-host OSRM or use a paid routing API for production).
 */
export function useRoute(from: LatLng | null, to: LatLng | null): LatLng[] | null {
  const key = from && to ? `${from.join(',')}|${to.join(',')}` : '';
  const [path, setPath] = useState<LatLng[] | null>(null);

  useEffect(() => {
    if (!from || !to) {
      setPath(null);
      return;
    }
    const cached = cache.get(key);
    if (cached) {
      setPath(cached);
      return;
    }
    setPath([from, to]);

    const controller = new AbortController();
    const url =
      `https://router.project-osrm.org/route/v1/driving/` +
      `${from[1]},${from[0]};${to[1]},${to[0]}?overview=full&geometries=geojson`;

    fetch(url, { signal: controller.signal })
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error('route failed'))))
      .then((data) => {
        const coords = data?.routes?.[0]?.geometry?.coordinates as [number, number][] | undefined;
        if (!coords?.length) return;
        const route = coords.map(([lng, lat]) => [lat, lng] as LatLng);
        cache.set(key, route);
        setPath(route);
      })
      .catch(() => undefined); // keep the straight line

    return () => controller.abort();
  }, [key]);

  return path;
}
