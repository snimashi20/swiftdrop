import axios from 'axios';
import {
  WebhookEndpoint,
  UpdateWebhookEndpointRequest,
  WebhookDelivery,
  TestPingResponse
} from './types';

// Relative URL for proxy/nginx
const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor to attach JWT token if present in localStorage
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('swiftdrop_token') || localStorage.getItem('token') || localStorage.getItem('accessToken');
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

// ----------------------------------------------------------------------
// User Profile (M2) Types & API Functions (webhookApi එකෙන් පිටත)
// ----------------------------------------------------------------------

export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  phoneNumber?: string;
  provider: string;
  role: string;
}

export interface UpdateProfileRequest {
  fullName: string;
  phoneNumber?: string;
}

export const fetchUserProfile = async (): Promise<UserProfile> => {
  const response = await apiClient.get<UserProfile>('/users/me');
  return response.data;
};

export const updateUserProfile = async (data: UpdateProfileRequest): Promise<UserProfile> => {
  const response = await apiClient.put<UserProfile>('/users/me', data);
  return response.data;
};

// ----------------------------------------------------------------------
// Saved Address Book (M2) Types & API Functions
// ----------------------------------------------------------------------

export interface Address {
  id: number;
  addressLabel?: string;
  label?: string; // support both
  recipientName?: string;
  phoneNumber?: string;
  streetAddress: string;
  city: string;
  district: string;
  postalCode?: string;
  isDefault?: boolean;
}

export interface CreateAddressRequest {
  addressLabel: string;
  streetAddress: string;
  city: string;
  district: string;
  postalCode?: string;
  isDefault?: boolean;
}

// Addresses list
export const fetchUserAddresses = async (): Promise<Address[]> => {
  const response = await apiClient.get<Address[]>('/users/addresses');
  return response.data;
};

// add new Address 
export const createUserAddress = async (data: CreateAddressRequest): Promise<Address> => {
  const response = await apiClient.post<Address>('/users/addresses', data);
  return response.data;
};

// remove an Address  (IDOR protected)
export const deleteUserAddress = async (addressId: number): Promise<void> => {
  await apiClient.delete(`/users/addresses/${addressId}`);
};