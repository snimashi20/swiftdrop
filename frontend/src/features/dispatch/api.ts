import { api } from "../../lib/api";
import type {
  AssignedParcel,
  CourierCandidate,
  CourierProfile,
  CourierWorkload,
  NotificationChannel,
  NotificationDeliveryStatus,
  NotificationPage,
  Page,
  ParcelAssignment,
  ReportSummary,
  UnassignedParcel,
  VehicleType,
  ZoneOption,
} from "./types";

export const dispatchApi = {
  unassignedParcels: async (params: { zoneId?: number; district?: string; from?: string; to?: string; page?: number; size?: number } = {}) =>
    (await api.get<Page<UnassignedParcel>>("/dispatch/parcels/unassigned", { params })).data,

  assignedParcels: async (params: { courierId?: number; activeOnly?: boolean; page?: number; size?: number } = {}) =>
    (await api.get<Page<AssignedParcel>>("/dispatch/parcels/assigned", { params })).data,

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

  courierCandidates: async () =>
    (await api.get<CourierCandidate[]>("/dispatch/lookups/courier-candidates")).data,

  zones: async () => (await api.get<ZoneOption[]>("/dispatch/lookups/zones")).data,

  reportSummary: async (days: 7 | 30) =>
    (await api.get<ReportSummary>("/dispatch/reports/summary", { params: { days } })).data,
};

// GET /api/v1/notifications/admin (notification-service, ADMIN and DISPATCHER).
// The channel filter is called "type" in the API.
export const notificationsApi = {
  list: async (params: {
    page?: number;
    size?: number;
    channel?: NotificationChannel;
    status?: NotificationDeliveryStatus;
    trackingNumber?: string;
  } = {}) => {
    const { channel, ...rest } = params;
    return (await api.get<NotificationPage>("/notifications/admin", { params: { ...rest, type: channel } })).data;
  },
};