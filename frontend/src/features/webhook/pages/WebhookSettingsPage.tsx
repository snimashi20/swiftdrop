import React, { useState, useEffect } from 'react';
import { WebhookEndpointCard } from '../components/WebhookEndpointCard';
import { WebhookDeliveryTable } from '../components/WebhookDeliveryTable';
import { WebhookEndpoint, WebhookDelivery, TestPingResponse } from '../types';
import { webhookApi } from '../api';

export const WebhookSettingsPage: React.FC = () => {
  const [endpoint, setEndpoint] = useState<WebhookEndpoint | null>(null);
  const [deliveries, setDeliveries] = useState<WebhookDelivery[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const loadData = async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const [ep, del] = await Promise.all([
        webhookApi.getEndpoint(),
        webhookApi.getDeliveries()
      ]);
      setEndpoint(ep);
      setDeliveries(del?.content || []);
    } catch (err: any) {
      console.error('Failed to load webhook configuration', err);
      const errMsg = err?.response?.data?.message || err?.message || "Couldn't load your webhook settings. Please make sure you are signed in as a registered merchant or verify backend connectivity.";
      setLoadError(errMsg);
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
    } catch (err: any) {
      setMessage({ type: 'error', text: err?.response?.data?.message || 'Failed to update webhook endpoint.' });
    }
    setTimeout(() => setMessage(null), 4000);
  };

  const handleRegenerateSecret = async () => {
    try {
      const res = await webhookApi.regenerateSecret();
      setMessage({ type: 'success', text: 'Webhook secret regenerated successfully.' });
      return res.secret;
    } catch (err: any) {
      setMessage({ type: 'error', text: err?.response?.data?.message || 'Failed to regenerate webhook secret.' });
      throw err;
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
    } catch (err: any) {
      setMessage({ type: 'error', text: err?.response?.data?.message || `Failed to re-dispatch delivery #${deliveryId}.` });
    }
    setTimeout(() => setMessage(null), 4000);
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

      {/* Load Error Banner */}
      {loadError && (
        <div className="bg-rose-50 border border-rose-200 text-rose-800 p-4 rounded-xl text-sm mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 shadow-sm">
          <div className="flex items-center gap-2">
            <span className="text-base">⚠️</span>
            <span className="font-medium">{loadError}</span>
          </div>
          <button
            onClick={loadData}
            className="px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-semibold self-start sm:self-auto transition-colors whitespace-nowrap"
          >
            Try Again
          </button>
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
