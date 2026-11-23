import { api } from "../../lib/api";
import type { CourierApplication, CourierApplicationStatus, Page } from "./types";

export async function fetchCourierApplications(
  status?: CourierApplicationStatus,
  page = 0,
): Promise<Page<CourierApplication>> {
  return (await api.get<Page<CourierApplication>>("/admin/couriers", { params: { status, page, size: 20 } })).data;
}

export async function approveCourierApplication(id: number): Promise<CourierApplication> {
  return (await api.post<CourierApplication>(`/admin/couriers/${id}/approve`)).data;
}

export async function rejectCourierApplication(id: number, reason: string): Promise<CourierApplication> {
  return (await api.post<CourierApplication>(`/admin/couriers/${id}/reject`, { reason })).data;
}
