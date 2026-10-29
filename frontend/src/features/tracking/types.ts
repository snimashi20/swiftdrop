export type ParcelStatus =
  | 'CREATED'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'FAILED';

export interface TimelineEntry {
  status: ParcelStatus;
  changedAt: string; // ISO-8601
  note?: string | null;
}

/** Response of GET /api/v1/tracking/{trackingNumber}. Public: no personal data. */
export interface TrackingResult {
  trackingNumber: string;
  status: ParcelStatus;
  receiverName: string; // short form, e.g. "N. Perera"
  pickupCity: string;
  dropoffCity: string;
  timeline: TimelineEntry[];
}

/** Message pushed to /topic/tracking/{trackingNumber}. */
export interface TrackingUpdateMessage {
  trackingNumber: string;
  status: ParcelStatus;
  changedAt: string;
  note?: string | null;
}
