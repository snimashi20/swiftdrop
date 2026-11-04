import { api } from "../../lib/api";
import type {
  CourierProfile,
  CourierWorkload,
  Page,
  ParcelAssignment,
  ReportSummary,
  UnassignedParcel,
  VehicleType,
} from "./types";

export const dispatchApi = {
  unassignedParcels: async (params: { district?: string; from?: string; to?: string; page?: number; size?: number } = {}) =>
    (await api.get<Page<UnassignedParcel>>("/dispatch/parcels/unassigned", { params })).data,

  suggestedCourier: async (parcelId: number) =>
    (await api.get<CourierWorkload>(`/dispatch/parcels/${parcelId}/suggested-courier`)).data,

  assignParcel: async (parcelId: number, courierProfileId: number) =>
    (await api.put<ParcelAssignment>(`/dispatch/parcels/${parcelId}/assign`, { courierProfileId })).data,

  workload: async (params: { zoneId?: number; active?: boolean } = {}) =>
    (await api.get<CourierWorkload[]>("/dispatch/couriers/workload", { params })).data,

  couriers: async (params: { zoneId?: number; active?: boolean; page?: number; size?: number } = {}) =>
    (await api.get<Page<CourierProfile>>("/dispatch/couriers", { params })).data,

  createCourier: async (body: { userId: number; vehicleType: VehicleType; zoneId: number; active?: boolean }) =>
    (await api.post<CourierProfile>("/dispatch/couriers", body)).data,

  updateCourier: async (id: number, body: { vehicleType?: VehicleType; zoneId?: number; active?: boolean }) =>
    (await api.patch<CourierProfile>(`/dispatch/couriers/${id}`, body)).data,

  reportSummary: async (days: 7 | 30) =>
    (await api.get<ReportSummary>("/dispatch/reports/summary", { params: { days } })).data,
};