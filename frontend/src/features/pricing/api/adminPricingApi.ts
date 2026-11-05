import { api } from "../../../lib/api";

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
  const res = await api.get<Zone[]>("/admin/pricing/zones");
  return res.data;
};

export const createZone = async (data: { name: string; district: string }): Promise<Zone> => {
  const res = await api.post<Zone>("/admin/pricing/zones", data);
  return res.data;
};

export const fetchRules = async (): Promise<PricingRule[]> => {
  const res = await api.get<PricingRule[]>("/admin/pricing/rules");
  return res.data;
};

export const updateRule = async (id: number, data: Partial<PricingRule>): Promise<PricingRule> => {
  const res = await api.put<PricingRule>(`/admin/pricing/rules/${id}`, data);
  return res.data;
};
