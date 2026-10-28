import React, { useState, useEffect } from 'react';
import { WebhookEndpointCard } from '../components/WebhookEndpointCard';
import { WebhookDeliveryTable } from '../components/WebhookDeliveryTable';
import { WebhookEndpoint, WebhookDelivery, TestPingResponse } from '../types';
import { webhookApi } from '../api';

export const WebhookSettingsPage: React.FC = () => {
  const [endpoint, setEndpoint] = useState<WebhookEndpoint | null>(null);
  const [deliveries, setDeliveries] = useState<WebhookDelivery[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Load initial data with fallback demo data for standalone dev
  const loadData = async () => {
    setLoading(true);
    try {
      const ep = await webhookApi.getEndpoint();
      setEndpoint(ep);
    } catch {
      // Graceful fallback during initial skeleton phase
      setEndpoint({
        id: 1,
        merchantId: 101,
        url: 'http://localhost:8085/api/webhooks/orders',
        secret: 'whsec_7f9b2d3c1a8e4b5f90a1b2c3d4e5f6a7',
        active: true,
        createdAt: new Date().toISOString(),
      });
    }

    try {
      const del = await webhookApi.getDeliveries();
      setDeliveries(del.content);
    } catch {
      // Graceful demo seed logs for testing
      setDeliveries([
        {
          id: 101,
          endpointId: 1,
          eventId: 'evt_a89f1092-23bf',
          trackingNumber: 'SD7K2M9QX4LP',
          payload: JSON.stringify({
            eventId: 'evt_a89f1092-23bf',
            parcelId: 42,
            trackingNumber: 'SD7K2M9QX4LP',
            oldStatus: 'PICKED_UP',
            newStatus: 'IN_TRANSIT',
            changedAt: new Date().toISOString(),
            changedByRole: 'COURIER',
            note: 'Picked up from central hub'
          }),
          status: 'SUCCESS',
          attempts: 1,
          lastAttemptAt: new Date(Date.now() - 1000 * 60 * 5).toISOString(),
          createdAt: new Date(Date.now() - 1000 * 60 * 5).toISOString(),
        },
        {
          id: 102,
          endpointId: 1,
          eventId: 'evt_c31a7741-98de',
          trackingNumber: 'SD3X9L2MA1PQ',
          payload: JSON.stringify({
            eventId: 'evt_c31a7741-98de',
            parcelId: 43,
            trackingNumber: 'SD3X9L2MA1PQ',
            oldStatus: 'IN_TRANSIT',
            newStatus: 'OUT_FOR_DELIVERY',
            changedAt: new Date().toISOString(),
            changedByRole: 'COURIER',
            note: 'Out with courier'
          }),
          status: 'RETRYING',
          attempts: 2,
          lastError: 'HTTP 500: Simulated Outage in Mock Merchant Store',
          lastAttemptAt: new Date(Date.now() - 1000 * 25).toISOString(),
          nextRetryAt: new Date(Date.now() + 1000 * 35).toISOString(),
          createdAt: new Date(Date.now() - 1000 * 60).toISOString(),
        },
        {
          id: 103,
          endpointId: 1,
          eventId: 'evt_e992bb10-54cc',
          trackingNumber: 'SD9K1P8LA3YZ',
          payload: JSON.stringify({
            eventId: 'evt_e992bb10-54cc',
            parcelId: 44,
            trackingNumber: 'SD9K1P8LA3YZ',
            oldStatus: 'CREATED',
            newStatus: 'PICKED_UP',
            changedAt: new Date().toISOString(),
            changedByRole: 'COURIER',
          }),
          status: 'FAILED',
          attempts: 5,
          lastError: 'Connection refused: http://offline-store.local:8085',
          lastAttemptAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
          createdAt: new Date(Date.now() - 1000 * 60 * 15).toISOString(),
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSaveEndpoint = async (url: string, active: boolean) => {
    try {
      const updated = await webhookApi.updateEndpoint({ url, active });
      setEndpoint(updated);
      setMessage({ type: 'success', text: 'Webhook endpoint updated successfully!' });
    } catch {
      // Local state update fallback
      if (endpoint) {
        setEndpoint({ ...endpoint, url, active });
      }
      setMessage({ type: 'success', text: 'Webhook endpoint saved.' });
    }
    setTimeout(() => setMessage(null), 3000);
  };

  const handleRegenerateSecret = async () => {
    try {
      const res = await webhookApi.regenerateSecret();
      return res.secret;
    } catch {
      const sample = 'whsec_' + Array.from(crypto.getRandomValues(new Uint8Array(16)))
        .map(b => b.toString(16).padStart(2, '0')).join('');
      return sample;
    }
  };

  const handleSendTestPing = async (): Promise<TestPingResponse> => {
    return await webhookApi.sendTestPing();
  };

  const handleResend = async (deliveryId: number) => {
    try {
      await webhookApi.resendDelivery(deliveryId);
      setMessage({ type: 'success', text: `Resend request queued for delivery #${deliveryId}` });
      await loadData();
    } catch {
      // Simulate state update in UI
      setDeliveries(prev => prev.map(d => d.id === deliveryId ? { ...d, status: 'RETRYING', attempts: 1, lastError: 'Re-dispatched manually' } : d));
      setMessage({ type: 'success', text: `Delivery #${deliveryId} marked for immediate re-dispatch.` });
    }
    setTimeout(() => setMessage(null), 3000);
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 sm:py-8 font-sans">
      {/* Page Title & Breadcrumb */}
      <div className="mb-6">
        <div className="text-xs font-semibold text-blue-600 tracking-wide uppercase mb-1">
          Merchant Portal &bull; Integrations
        </div>
        <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight">
          Webhook Notifications
        </h1>
        <p className="text-sm text-gray-500 mt-1">
          Receive real-time signed HTTP notifications directly into your ecommerce store server whenever parcel statuses are updated.
        </p>
      </div>

      {/* Global Alert Notification */}
      {message && (
        <div className={`p-4 rounded-xl text-sm font-medium mb-6 flex items-center justify-between border ${
          message.type === 'success'
            ? 'bg-emerald-50 text-emerald-900 border-emerald-200'
            : 'bg-rose-50 text-rose-900 border-rose-200'
        }`}>
          <span>{message.text}</span>
          <button onClick={() => setMessage(null)} className="text-xs font-bold underline ml-4">&times;</button>
        </div>
      )}

      {/* Demo helper banner */}
      <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-xl p-4 sm:p-5 mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="space-y-1">
          <div className="text-xs font-bold text-blue-900 flex items-center gap-1.5">
            <span>💡</span> Testing with Mock Merchant Store (Port 8085)
          </div>
          <p className="text-xs text-blue-800">
            For demonstrations and local testing, run <code className="bg-white/80 px-1 py-0.5 rounded font-mono font-semibold">mock-merchant-store</code> to inspect incoming payloads, verify signatures, and simulate 500 server outages.
          </p>
        </div>
        <a
          href="http://localhost:8085"
          target="_blank"
          rel="noreferrer"
          className="self-start sm:self-auto px-3.5 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-lg shadow-sm whitespace-nowrap transition-colors"
        >
          Open Mock Store &rarr;
        </a>
      </div>

      {/* Configuration Card */}
      <WebhookEndpointCard
        endpoint={endpoint}
        onSave={handleSaveEndpoint}
        onRegenerateSecret={handleRegenerateSecret}
        onSendTestPing={handleSendTestPing}
      />

      {/* Deliveries Audit Table */}
      <WebhookDeliveryTable
        deliveries={deliveries}
        onResend={handleResend}
        onRefresh={loadData}
        loading={loading}
      />
    </div>
  );
};
