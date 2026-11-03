import axios from 'axios'; //If M1 has a shared axios client, import from rontend/src/lib/axios.ts. Otherwise use default.

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
  // Use base URL. In Docker via Nginx, this is just /api/v1
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1';
  
  const response = await axios.post<QuoteResponse>(
    `${baseUrl}/pricing/quote`, 
    request
  );
  return response.data;
};
