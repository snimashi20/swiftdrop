import { api } from "../../lib/api";
import {
  WebhookEndpoint,
  UpdateWebhookEndpointRequest,
  WebhookDelivery,
  TestPingResponse,
} from "./types";

export const webhookApi = {
  getEndpoint: async (): Promise<WebhookEndpoint> => {
    const response = await api.get<WebhookEndpoint>("/webhooks/endpoint");
    return response.data;
  },

  updateEndpoint: async (data: UpdateWebhookEndpointRequest): Promise<WebhookEndpoint> => {
    const response = await api.put<WebhookEndpoint>("/webhooks/endpoint", data);
    return response.data;
  },

  regenerateSecret: async (): Promise<{ secret: string }> => {
    const response = await api.post<{ secret: string }>("/webhooks/endpoint/secret");
    return response.data;
  },

  sendTestPing: async (): Promise<TestPingResponse> => {
    const response = await api.post<TestPingResponse>("/webhooks/test-ping");
    return response.data;
  },

  getDeliveries: async (
    page = 0,
    size = 10
  ): Promise<{ content: WebhookDelivery[]; totalElements: number }> => {
    const response = await api.get("/webhooks/deliveries", {
      params: { page, size },
    });
    return response.data;
  },

  resendDelivery: async (deliveryId: number): Promise<WebhookDelivery> => {
    const response = await api.post<WebhookDelivery>(`/webhooks/deliveries/${deliveryId}/resend`);
    return response.data;
  },
};

// Re-export profile & address book types and methods from features/profile/profile.api
export * from "../profile/profile.api";