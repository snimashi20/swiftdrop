import React, { useEffect, useMemo } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { MapContainer, Marker, Polyline, Popup, TileLayer, useMap } from 'react-leaflet';
import { Navigation, Phone, X, MapPin, ExternalLink, Route } from 'lucide-react';
import type { CourierDelivery } from '../types';
import { coordsFor } from '../../tracking/cities';
import type { LatLng } from '../../tracking/geo';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  deliveries: CourierDelivery[];
}

/**
 * Builds a universal Google Maps Directions URL for a single stop
 */
export function getGoogleMapsNavigationUrl(address: string): string {
  return `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(address)}`;
}

/**
 * Builds a multi-stop Google Maps Directions URL with destination and waypoints
 */
export function buildMultiStopGoogleMapsUrl(deliveries: CourierDelivery[]): string {
  if (deliveries.length === 0) return 'https://www.google.com/maps';
  if (deliveries.length === 1) {
    return getGoogleMapsNavigationUrl(deliveries[0].deliveryAddress);
  }
  const destination = encodeURIComponent(deliveries[deliveries.length - 1].deliveryAddress);
  const waypoints = deliveries
    .slice(0, -1)
    .map((d) => encodeURIComponent(d.deliveryAddress))
    .join('|');
  return `https://www.google.com/maps/dir/?api=1&destination=${destination}&waypoints=${waypoints}`;
}

function createStopIcon(stopNumber: number, isDelivered: boolean) {
  const bg = isDelivered ? '#10b981' : '#f97316';
  return L.divIcon({
    className: 'sd-route-stop-marker',
    iconSize: [32, 32],
    iconAnchor: [16, 16],
    html: `<div style="background-color: ${bg}; color: white; width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px; box-shadow: 0 4px 10px rgba(0,0,0,0.3); border: 2.5px solid white;">${stopNumber}</div>`,
  });
}

function FitBounds({ points }: { points: LatLng[] }) {
  const map = useMap();
  useEffect(() => {
    if (points.length > 0) {
      const id = requestAnimationFrame(() => {
        map.invalidateSize();
        if (points.length === 1) {
          map.setView(points[0], 12);
        } else {
          map.fitBounds(L.latLngBounds(points), { padding: [40, 40], maxZoom: 13 });
        }
      });
      return () => cancelAnimationFrame(id);
    }
  }, [map, points]);
  return null;
}

export const CourierRouteModal: React.FC<Props> = ({ isOpen, onClose, deliveries }) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Order active stops first, then delivered/failed
  const sortedStops = useMemo(() => {
    const active = deliveries.filter((d) => d.status !== 'DELIVERED' && d.status !== 'FAILED');
    const completed = deliveries.filter((d) => d.status === 'DELIVERED' || d.status === 'FAILED');
    return [...active, ...completed];
  }, [deliveries]);

  // Resolve geographic coordinates for each stop
  const mappedStops = useMemo(() => {
    return sortedStops.map((delivery, index) => {
      const coords = coordsFor(delivery.deliveryAddress);
      return {
        delivery,
        stopNumber: index + 1,
        coords,
      };
    });
  }, [sortedStops]);

  // Points for polyline and bounds
  const validPoints = useMemo(() => {
    return mappedStops
      .filter((s): s is typeof s & { coords: LatLng } => s.coords !== null)
      .map((s) => s.coords);
  }, [mappedStops]);

  const defaultCenter: LatLng = validPoints[0] ?? [6.9271, 79.8612];

  const multiStopGoogleMapsUrl = useMemo(() => {
    const activeOnly = sortedStops.filter((d) => d.status !== 'DELIVERED' && d.status !== 'FAILED');
    return buildMultiStopGoogleMapsUrl(activeOnly.length > 0 ? activeOnly : sortedStops);
  }, [sortedStops]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-3 sm:p-5 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="flex flex-col w-full max-w-2xl max-h-[92vh] bg-white rounded-2xl shadow-2xl overflow-hidden border border-slate-200">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/70">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-coral/10 text-coral">
              <Route size={20} />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-900">Today&apos;s Delivery Route</h2>
              <p className="text-xs text-slate-500">
                {deliveries.length} total stops assigned &bull; {sortedStops.filter((d) => d.status !== 'DELIVERED' && d.status !== 'FAILED').length} pending
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-200 hover:text-slate-700 transition"
          >
            <X size={18} />
          </button>
        </div>

        {/* Interactive Leaflet Map */}
        <div className="relative h-64 sm:h-72 w-full bg-slate-100 border-b border-slate-200">
          <MapContainer
            center={defaultCenter}
            zoom={10}
            scrollWheelZoom={false}
            className="h-full w-full z-0"
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {validPoints.length > 0 && <FitBounds points={validPoints} />}

            {/* Connecting Polyline */}
            {validPoints.length > 1 && (
              <Polyline
                positions={validPoints}
                pathOptions={{ color: '#f97316', weight: 3.5, dashArray: '6, 8', opacity: 0.8 }}
              />
            )}

            {/* Stop Markers */}
            {mappedStops.map(({ delivery, stopNumber, coords }) => {
              if (!coords) return null;
              const isDelivered = delivery.status === 'DELIVERED';
              return (
                <Marker
                  key={delivery.parcelId}
                  position={coords}
                  icon={createStopIcon(stopNumber, isDelivered)}
                >
                  <Popup>
                    <div className="p-1 space-y-1 text-xs">
                      <p className="font-bold text-slate-900">Stop #{stopNumber}: {delivery.recipientName}</p>
                      <p className="text-slate-600 font-mono text-[11px]">{delivery.trackingNumber}</p>
                      <p className="text-slate-500">{delivery.deliveryAddress}</p>
                      <span className={`inline-block px-2 py-0.5 rounded-full font-semibold text-[10px] ${
                        isDelivered ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                      }`}>
                        {delivery.status}
                      </span>
                    </div>
                  </Popup>
                </Marker>
              );
            })}
          </MapContainer>
        </div>

        {/* Route Itinerary List */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-3 bg-slate-50/50">
          <p className="text-xs font-bold uppercase tracking-wider text-slate-400">
            Stop-by-Stop Itinerary ({sortedStops.length})
          </p>

          {sortedStops.length === 0 ? (
            <p className="text-center text-sm text-slate-500 py-6">No delivery stops assigned yet.</p>
          ) : (
            sortedStops.map((item, index) => {
              const isDelivered = item.status === 'DELIVERED';
              const isFailed = item.status === 'FAILED';
              return (
                <div
                  key={item.parcelId}
                  className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-3.5 bg-white rounded-xl border border-slate-200/80 shadow-xs hover:border-slate-300 transition"
                >
                  <div className="flex items-start gap-3">
                    <span
                      className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold text-white shadow-xs ${
                        isDelivered ? 'bg-emerald-500' : isFailed ? 'bg-rose-500' : 'bg-coral'
                      }`}
                    >
                      {index + 1}
                    </span>
                    <div>
                      <div className="flex items-center gap-2">
                        <h4 className="text-sm font-bold text-slate-900">{item.recipientName}</h4>
                        <span className="font-mono text-[11px] text-slate-500">({item.trackingNumber})</span>
                        <span
                          className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                            isDelivered ? 'bg-emerald-100 text-emerald-800' :
                            isFailed ? 'bg-rose-100 text-rose-800' :
                            'bg-amber-100 text-amber-800'
                          }`}
                        >
                          {item.status}
                        </span>
                      </div>
                      <p className="flex items-center gap-1 text-xs text-slate-600 mt-0.5">
                        <MapPin size={13} className="shrink-0 text-slate-400" />
                        <span>{item.deliveryAddress}</span>
                      </p>
                    </div>
                  </div>

                  {/* Actions for this stop */}
                  <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                    {item.recipientPhone && (
                      <a
                        href={`tel:${item.recipientPhone}`}
                        className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-100 transition"
                      >
                        <Phone size={12} /> Call
                      </a>
                    )}
                    <a
                      href={getGoogleMapsNavigationUrl(item.deliveryAddress)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 rounded-lg bg-blue-50 border border-blue-200 px-3 py-1.5 text-xs font-semibold text-blue-700 hover:bg-blue-100 transition"
                    >
                      <Navigation size={12} /> GPS
                    </a>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Footer Toolbar */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 px-6 py-4 border-t border-slate-200 bg-white">
          <p className="text-xs text-slate-500 hidden sm:block">
            Numbered stops follow recommended dispatch sequence.
          </p>
          <div className="flex items-center gap-3 w-full sm:w-auto justify-end">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl transition"
            >
              Close
            </button>
            <a
              href={multiStopGoogleMapsUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 rounded-xl bg-coral px-5 py-2.5 text-xs font-semibold text-white shadow-md shadow-coral/20 hover:bg-coral/90 transition"
            >
              <ExternalLink size={14} /> Open Full Route in Google Maps
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
