import axios from 'axios';
import {
  WebhookEndpoint,
  UpdateWebhookEndpointRequest,
  WebhookDelivery,
  TestPingResponse
} from './types';

// Use relative URL so Vite dev proxy or Nginx in Docker handles routing to core-api:8080
const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor to attach JWT token if present in localStorage
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const webhookApi = {
  getEndpoint: async (): Promise<WebhookEndpoint> => {
    const response = await apiClient.get<WebhookEndpoint>('/webhooks/endpoint');
    return response.data;
  },

  updateEndpoint: async (data: UpdateWebhookEndpointRequest): Promise<WebhookEndpoint> => {
    const response = await apiClient.put<WebhookEndpoint>('/webhooks/endpoint', data);
    return response.data;
  },

  regenerateSecret: async (): Promise<{ secret: string }> => {
    const response = await apiClient.post<{ secret: string }>('/webhooks/endpoint/secret');
    return response.data;
  },

  sendTestPing: async (): Promise<TestPingResponse> => {
    const response = await apiClient.post<TestPingResponse>('/webhooks/test-ping');
    return response.data;
  },

  getDeliveries: async (page = 0, size = 10): Promise<{ content: WebhookDelivery[]; totalElements: number }> => {
    const response = await apiClient.get('/webhooks/deliveries', {
      params: { page, size },
    });
    return response.data;
  },

  resendDelivery: async (deliveryId: number): Promise<WebhookDelivery> => {
    const response = await apiClient.post<WebhookDelivery>(`/webhooks/deliveries/${deliveryId}/resend`);
    return response.data;
  },
};
