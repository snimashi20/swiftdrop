import type { ParcelStatus } from './types';

/** The happy path, in order. FAILED is shown separately. */
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
};

export const STATUS_BADGE: Record<ParcelStatus, string> = {
  CREATED: 'bg-slate-100 text-slate-700',
  PICKED_UP: 'bg-blue-100 text-blue-800',
  IN_TRANSIT: 'bg-amber-100 text-amber-800',
  OUT_FOR_DELIVERY: 'bg-amber-100 text-amber-800',
  DELIVERED: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
};

export const TRACKING_NUMBER_REGEX = /^SD[A-Z0-9]{10}$/;
