export type DeliveryStatus = 'PENDING' | 'RETRYING' | 'SUCCESS' | 'FAILED';

export interface WebhookEndpoint {
  id?: number;
  merchantId: number;
  url: string;
  secret: string;
  active: boolean;
  createdAt?: string;
}

export interface UpdateWebhookEndpointRequest {
  url: string;
  active: boolean;
}

export interface WebhookDelivery {
  id: number;
  endpointId: number;
  eventId: string;
  trackingNumber: string;
  payload: string;
  status: DeliveryStatus;
  attempts: number;
  lastError?: string;
  lastAttemptAt?: string;
  nextRetryAt?: string;
  createdAt: string;
}

export interface TestPingResponse {
  success: boolean;
  httpStatus: number;
  message: string;
  durationMs: number;
}
