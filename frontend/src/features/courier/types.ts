export type ParcelStatus = 
  | 'CREATED' 
  | 'PICKED_UP' 
  | 'IN_TRANSIT' 
  | 'OUT_FOR_DELIVERY' 
  | 'DELIVERED' 
  | 'FAILED';

export type FailureReason = 
  | 'RECIPIENT_NOT_AVAILABLE'
  | 'WRONG_ADDRESS'
  | 'REFUSED'
  | 'DAMAGED'
  | 'OTHER';

export interface CourierDelivery {
  parcelId: number;
  trackingNumber: string;
  recipientName: string;
  recipientPhone: string;
  deliveryAddress: string;
  status: ParcelStatus;
  weightKg: number;
  deliveryInstructions?: string | null;
  assignedAt?: string | null;
  proofPhotoUrl?: string | null;
}

export interface UpdateStatusRequest {
  status: string;
  note?: string;
}

export interface DeliverParcelRequest {
  recipientName: string;
  note?: string;
}

export interface FailParcelRequest {
  reason: FailureReason;
  note?: string;
}