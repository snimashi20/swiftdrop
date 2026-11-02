import axios, { isAxiosError } from 'axios';
import type { TrackingResult } from './types';

// The public tracking API needs no JWT. Relative base URL: Vite proxy in dev,
// Nginx in Docker. TODO: switch to M8's shared Axios client once it exists.
const client = axios.create({ baseURL: '/api/v1', timeout: 10_000 });

// DEV ONLY: open /track/SD7K2M9QX4LP?mock to preview the page without a backend.
// Remove this block when GET /api/v1/tracking/{trackingNumber} is merged.
const MOCK: TrackingResult = {
  trackingNumber: 'SD7K2M9QX4LP',
  status: 'IN_TRANSIT',
  receiverName: 'N. Perera',
  pickupCity: 'Colombo',
  dropoffCity: 'Kandy',
  timeline: [
    { status: 'CREATED', changedAt: '2026-10-28T08:00:00Z', note: 'Booking confirmed' },
    { status: 'PICKED_UP', changedAt: '2026-10-28T10:15:00Z', note: null },
    { status: 'IN_TRANSIT', changedAt: '2026-10-28T13:40:00Z', note: 'Left Colombo hub' },
  ],
};

export async function getTracking(trackingNumber: string): Promise<TrackingResult> {
  if (import.meta.env.DEV && new URLSearchParams(window.location.search).has('mock')) {
    return { ...MOCK, trackingNumber };
  }
  const response = await client.get<TrackingResult>(
    `/tracking/${encodeURIComponent(trackingNumber)}`,
  );
  return response.data;
}

export function isNotFound(error: unknown): boolean {
  return isAxiosError(error) && error.response?.status === 404;
}
