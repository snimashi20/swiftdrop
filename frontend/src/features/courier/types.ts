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

export type VehicleType = 'BICYCLE' | 'MOTORBIKE' | 'THREE_WHEELER' | 'CAR' | 'VAN';

export interface ZoneOption {
  id: number;
  name: string;
  district: string;
}

export interface RegisterCourierRequest {
  fullName: string;
  email: string;
  password: string;
  phoneNumber: string;
  nicNumber: string;
  dateOfBirth: string;
  residentialAddress: string;
  vehicleType: VehicleType;
  vehiclePlateNumber: string;
  drivingLicenseNumber: string;
  drivingLicenseExpiry: string;
  zoneId: number;
  emergencyContactName: string;
  emergencyContactPhone: string;
}

export interface CourierRegistrationResponse {
  userId: number;
  courierProfileId: number;
  fullName: string;
  email: string;
  vehicleType: VehicleType;
  zoneId: number;
  status: string;
  nicFrontPhotoUrl: string;
  nicBackPhotoUrl: string;
}