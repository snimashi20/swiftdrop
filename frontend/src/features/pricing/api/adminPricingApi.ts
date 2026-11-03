import axios from 'axios';

const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1';

// Types
export interface Zone {
  id: number;
  name: string;
  district: string;
  isActive: boolean;
}

export interface PricingRule {
  id: number;
  zoneId: number;
  zoneName: string;
  baseFee: number;
  perKgFee: number;
  interZoneSurcharge: number;
  sizeFactorS: number;
  sizeFactorM: number;
  sizeFactorL: number;
}

// API Calls
export const fetchZones = async (): Promise<Zone[]> => {
  const res = await axios.get<Zone[]>(`${baseUrl}/admin/pricing/zones`);
  return res.data;
};

export const createZone = async (data: { name: string; district: string }): Promise<Zone> => {
  const res = await axios.post<Zone>(`${baseUrl}/admin/pricing/zones`, data);
  return res.data;
};

export const fetchRules = async (): Promise<PricingRule[]> => {
  const res = await axios.get<PricingRule[]>(`${baseUrl}/admin/pricing/rules`);
  return res.data;
};

export const updateRule = async (id: number, data: Partial<PricingRule>): Promise<PricingRule> => {
  const res = await axios.put<PricingRule>(`${baseUrl}/admin/pricing/rules/${id}`, data);
  return res.data;
};
