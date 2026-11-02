import axios from 'axios';
import type { 
  CourierDelivery, 
  UpdateStatusRequest, 
  DeliverParcelRequest, 
  FailParcelRequest 
} from './types';

// Connects to backend API using JWT from localStorage
const client = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = 
    localStorage.getItem('swiftdrop_access_token') || 
    localStorage.getItem('token') || 
    localStorage.getItem('jwt');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const getTodayDeliveries = async (): Promise<CourierDelivery[]> => {
  const { data } = await client.get<CourierDelivery[]>('/courier/deliveries/today');
  return data;
};

export const updateDeliveryStatus = async (
  parcelId: number, 
  status: string, 
  note?: string
): Promise<CourierDelivery> => {
  const payload: UpdateStatusRequest = { status, note };
  const { data } = await client.patch<CourierDelivery>(`/courier/deliveries/${parcelId}/status`, payload);
  return data;
};

export const deliverParcelWithProof = async (
  parcelId: number,
  recipientName: string,
  note?: string,
  photo?: File | null
): Promise<CourierDelivery> => {
  const formData = new FormData();
  const requestData: DeliverParcelRequest = { recipientName, note };
  
  formData.append(
    'data', 
    new Blob([JSON.stringify(requestData)], { type: 'application/json' })
  );
  
  if (photo) {
    formData.append('photo', photo);
  }

  const { data } = await client.post<CourierDelivery>(
    `/courier/deliveries/${parcelId}/deliver`, 
    formData, 
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  return data;
};

export const failParcelDelivery = async (
  parcelId: number,
  reason: FailParcelRequest['reason'],
  note?: string
): Promise<CourierDelivery> => {
  const payload: FailParcelRequest = { reason, note };
  const { data } = await client.post<CourierDelivery>(`/courier/deliveries/${parcelId}/fail`, payload);
  return data;
};