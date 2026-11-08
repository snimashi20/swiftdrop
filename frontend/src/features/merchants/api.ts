import { api } from "../../lib/api";
import type { ApiKey, GeneratedApiKey, Merchant, MerchantOrder, MerchantStatus, Page, RegisterMerchantRequest, ParcelStatus } from "./types";

export async function registerMerchant(data: RegisterMerchantRequest): Promise<Merchant> {
  return (await api.post<Merchant>("/merchants/register", data)).data;
}

export async function fetchMyMerchant(): Promise<Merchant> {
  return (await api.get<Merchant>("/merchants/me")).data;
}

export async function fetchMerchants(status?: MerchantStatus, page = 0): Promise<Page<Merchant>> {
  return (await api.get<Page<Merchant>>("/admin/merchants", { params: { status, page, size: 20 } })).data;
}

export async function approveMerchant(id: number): Promise<Merchant> {
  return (await api.post<Merchant>(`/admin/merchants/${id}/approve`)).data;
}

export async function rejectMerchant(id: number, reason: string): Promise<Merchant> {
  return (await api.post<Merchant>(`/admin/merchants/${id}/reject`, { reason })).data;
}

export async function fetchApiKeys(): Promise<ApiKey[]> {
  return (await api.get<ApiKey[]>("/merchants/me/api-keys")).data;
}

export async function createApiKey(label: string): Promise<GeneratedApiKey> {
  return (await api.post<GeneratedApiKey>("/merchants/me/api-keys", { label })).data;
}

export async function revokeApiKey(id: number): Promise<ApiKey> {
  return (await api.post<ApiKey>(`/merchants/me/api-keys/${id}/revoke`)).data;
}

export async function fetchDashboardOrders(status?: ParcelStatus, page = 0): Promise<Page<MerchantOrder>> {
  return (await api.get<Page<MerchantOrder>>("/merchants/me/orders", { params: { status: status || undefined, page, size: 20 } })).data;
}

export async function fetchDashboardOrderSummary(): Promise<Record<string, number>> {
  return (await api.get<Record<string, number>>("/merchants/me/orders/summary")).data;
}
