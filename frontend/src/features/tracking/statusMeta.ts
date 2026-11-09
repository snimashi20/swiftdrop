import type { ParcelStatus } from './types';

/** The happy path, in order. FAILED and CANCELLED are shown separately. */
export const MAIN_STEPS: ParcelStatus[] = [
  'CREATED',
  'PICKED_UP',
  'IN_TRANSIT',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
];

export const STATUS_LABEL: Record<ParcelStatus, string> = {
  CREATED: 'Created',
  PICKED_UP: 'Picked up',
  IN_TRANSIT: 'In transit',
  OUT_FOR_DELIVERY: 'Out for delivery',
  DELIVERED: 'Delivered',
  FAILED: 'Delivery failed',
  CANCELLED: 'Cancelled',
};

export const STATUS_BADGE: Record<ParcelStatus, string> = {
  CREATED: 'bg-ink/10 text-ink',
  PICKED_UP: 'bg-sky-100 text-sky-900',
  IN_TRANSIT: 'bg-amber-100 text-amber-900',
  OUT_FOR_DELIVERY: 'bg-amber-100 text-amber-900',
  DELIVERED: 'bg-green-100 text-green-900',
  FAILED: 'bg-red-100 text-red-900',
  CANCELLED: 'bg-slate-200 text-slate-700',
};

export const TRACKING_NUMBER_REGEX = /^SD[A-Z0-9]{10}$/;
