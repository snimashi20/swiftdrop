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
export type CourierApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export type VehicleType = 'BICYCLE' | 'MOTORBIKE' | 'THREE_WHEELER' | 'CAR' | 'VAN';

export interface CourierApplication {
  id: number;
  userId?: number | null;
  fullName: string;
  contactEmail: string;
  contactPhone?: string | null;
  licenseNumber?: string | null;
  vehicleType?: VehicleType | null;
  preferredZone?: string | null;
  status: CourierApplicationStatus;
  rejectionReason?: string | null;
  createdAt: string;
  reviewedAt?: string | null;
  reviewedBy?: number | null;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
