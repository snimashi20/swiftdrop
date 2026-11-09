import axios from 'axios';
import { api } from '../../lib/api'; // the shared Axios client (M8)
import type { TrackingResult } from './types';

// DEV ONLY: open /track/SD7K2M9QX4LP?mock to preview the page without a backend.
// Remove this block when GET /api/v1/tracking/{trackingNumber} is merged.
const MOCK: TrackingResult = {
  trackingNumber: 'SD7K2M9QX4LP',
  status: 'IN_TRANSIT',
  receiverName: 'N. Perera',
  pickupCity: 'Colombo',
  dropoffCity: 'Kandy',
  timeline: [
    { status: 'CREATED', changedAt: '2026-10-28T08:00:00Z' },
    { status: 'PICKED_UP', changedAt: '2026-10-28T10:15:00Z' },
    { status: 'IN_TRANSIT', changedAt: '2026-10-28T13:40:00Z' },
  ],
};

export function isMockMode(): boolean {
  return import.meta.env.DEV && new URLSearchParams(window.location.search).has('mock');
}

export async function getTracking(trackingNumber: string): Promise<TrackingResult> {
  if (isMockMode()) {
    return { ...MOCK, trackingNumber };
  }
  const response = await api.get<TrackingResult>(`/tracking/${encodeURIComponent(trackingNumber)}`);
  return response.data;
}

export function isNotFound(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 404;
}
