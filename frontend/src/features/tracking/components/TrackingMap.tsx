import { useEffect, useMemo } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { MapContainer, Marker, Polyline, TileLayer, useMap } from 'react-leaflet';
import { coordsFor } from '../cities';
import { progressFor, splitAt } from '../geo';
import type { LatLng } from '../geo';
import { useRoute } from '../useRoute';
import type { ParcelStatus, TimelineEntry } from '../types';

interface Props {
  pickupCity: string;
  dropoffCity: string;
  status: ParcelStatus;
  timeline: TimelineEntry[];
  live: boolean;
  expanded: boolean;
}

const escapeHtml = (s: string) =>
  s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c] as string);

function pinIcon(city: string, role: string, color: string) {
  return L.divIcon({
    className: 'sd-marker',
    iconSize: [150, 86],
    iconAnchor: [75, 86],
    html:
      `<div class="sd-pin"><div class="sd-pin-card"><strong>${escapeHtml(city)}</strong><span>${role}</span></div>` +
      `<svg width="30" height="38" viewBox="0 0 24 30" aria-hidden="true"><path d="M12 0C5.4 0 0 5.2 0 11.6 0 20 12 30 12 30s12-10 12-18.4C24 5.2 18.6 0 12 0z" fill="${color}"/><circle cx="12" cy="11.5" r="4.5" fill="#fff"/></svg></div>`,
  });
}

function truckIcon(live: boolean) {
  return L.divIcon({
    className: 'sd-marker',
    iconSize: [44, 44],
    iconAnchor: [22, 22],
    html:
      `<div class="sd-truck ${live ? 'sd-pulse' : ''}"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">` +
      `<path d="M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2"/><path d="M15 18H9"/>` +
      `<path d="M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14"/>` +
      `<circle cx="17" cy="18" r="2"/><circle cx="7" cy="18" r="2"/></svg></div>`,
  });
}

function FitRoute({ path, expanded }: { path: LatLng[]; expanded: boolean }) {
  const map = useMap();
  useEffect(() => {
    const id = requestAnimationFrame(() => {
      map.invalidateSize();
      map.fitBounds(L.latLngBounds(path), { padding: [90, 90] });
    });
    return () => cancelAnimationFrame(id);
  }, [map, path, expanded]);
  return null;
}

function MapControls({ path }: { path: LatLng[] }) {
  const map = useMap();
  const btn = 'flex h-10 w-10 items-center justify-center bg-white text-xl text-slate-700 hover:bg-slate-50';
  return (
    <div
      ref={(el) => {
        if (el) {
          L.DomEvent.disableClickPropagation(el);
          L.DomEvent.disableScrollPropagation(el);
        }
      }}
      className="absolute right-4 top-1/2 z-[1000] flex -translate-y-1/2 flex-col divide-y divide-slate-200 overflow-hidden rounded-lg border border-slate-200 shadow-md"
    >
      <button type="button" aria-label="Zoom in" onClick={() => map.zoomIn()} className={btn}>
        +
      </button>
      <button type="button" aria-label="Zoom out" onClick={() => map.zoomOut()} className={btn}>
        −
      </button>
      <button
        type="button"
        aria-label="Show the whole route"
        onClick={() => map.fitBounds(L.latLngBounds(path), { padding: [90, 90] })}
        className={btn}
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <circle cx="12" cy="12" r="3" />
          <circle cx="12" cy="12" r="8" />
          <path d="M12 2v3M12 19v3M2 12h3M19 12h3" />
        </svg>
      </button>
    </div>
  );
}

export default function TrackingMap({ pickupCity, dropoffCity, status, timeline, live, expanded }: Props) {
  const from = coordsFor(pickupCity);
  const to = coordsFor(dropoffCity);
  const path = useRoute(from, to);

  const split = useMemo(
    () => (path ? splitAt(path, progressFor(status, timeline)) : null),
    [path, status, timeline],
  );
  const pickup = useMemo(() => pinIcon(pickupCity, 'Pickup', '#16A34A'), [pickupCity]);
  const dropoff = useMemo(() => pinIcon(dropoffCity, 'Delivery', '#DC2626'), [dropoffCity]);
  const truck = useMemo(() => truckIcon(live && status !== 'DELIVERED'), [live, status]);

  if (!from || !to || !path || !split) {
    return (
      <div className="flex h-full items-center justify-center bg-slate-100 p-6 text-center text-slate-600">
        The map isn’t available for this route yet.
      </div>
    );
  }

  return (
    <MapContainer
      center={from}
      zoom={9}
      zoomControl={false}
      scrollWheelZoom
      className="h-full w-full"
      aria-label={`Map: ${pickupCity} to ${dropoffCity}`}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <Polyline positions={split.remaining} pathOptions={{ color: '#93c5fd', weight: 5, dashArray: '8 10' }} />
      <Polyline positions={split.done} pathOptions={{ color: '#2563eb', weight: 6, lineCap: 'round' }} />
      <Marker position={from} icon={pickup} alt={`Pickup: ${pickupCity}`} />
      <Marker position={to} icon={dropoff} alt={`Delivery: ${dropoffCity}`} />
      <Marker position={split.position} icon={truck} zIndexOffset={1000} alt="Parcel position (estimated)" />
      <FitRoute path={path} expanded={expanded} />
      <MapControls path={path} />
    </MapContainer>
  );
}
