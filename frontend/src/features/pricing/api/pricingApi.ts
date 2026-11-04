import { api } from "../../../lib/api";

export interface QuoteRequest {
  pickupDistrict: string;
  dropoffDistrict: string;
  size: 'S' | 'M' | 'L';
  weightKg: number;
}

export interface QuoteResponse {
  fee: number;
  pickupZone: string;
  dropoffZone: string;
  pickupZoneId: number;
  dropoffZoneId: number;
  interZone: boolean;
}

export const fetchQuote = async (request: QuoteRequest): Promise<QuoteResponse> => {
  const response = await api.post<QuoteResponse>("/pricing/quote", request);
  return response.data;
};
