import React, { useState } from 'react';
import { WebhookEndpoint, TestPingResponse } from '../types';

interface WebhookEndpointCardProps {
  endpoint: WebhookEndpoint | null;
  onSave: (url: string, active: boolean) => Promise<void>;
  onRegenerateSecret: () => Promise<string>;
  onSendTestPing: () => Promise<TestPingResponse>;
}

export const WebhookEndpointCard: React.FC<WebhookEndpointCardProps> = ({
  endpoint,
  onSave,
  onRegenerateSecret,
  onSendTestPing,
}) => {
  const [url, setUrl] = useState(endpoint?.url || 'http://localhost:8085/api/webhooks/orders');
  const [active, setActive] = useState(endpoint?.active ?? true);
  const [secret, setSecret] = useState(endpoint?.secret || 'whsec_sample_secret_key_89234710');
  const [showSecret, setShowSecret] = useState(false);
  const [copied, setCopied] = useState(false);
  const [saving, setSaving] = useState(false);
  const [pinging, setPinging] = useState(false);
  const [pingResult, setPingResult] = useState<TestPingResponse | null>(null);

  const handleCopySecret = async () => {
    try {
      await navigator.clipboard.writeText(secret);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await onSave(url, active);
    } finally {
      setSaving(false);
    }
  };

  const handleRegenerate = async () => {
    if (window.confirm('Are you sure you want to regenerate your webhook secret? Your existing integrations will stop working until you update their secret.')) {
      const newSecret = await onRegenerateSecret();
      setSecret(newSecret);
    }
  };

  const handlePing = async () => {
    setPinging(true);
    setPingResult(null);
    try {
      const res = await onSendTestPing();
      setPingResult(res);
    } catch {
      setPingResult({
        success: false,
        httpStatus: 500,
        message: 'Failed to reach endpoint. Ensure mock store or receiver is running.',
        durationMs: 0,
      });
    } finally {
      setPinging(false);
    }
  };

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5 sm:p-6 mb-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-4 border-b border-gray-100 gap-2">
        <div>
          <h2 className="text-lg font-bold text-gray-900">Webhook Endpoint Configuration</h2>
          <p className="text-sm text-gray-500">
            SwiftDrop will send real-time HTTP POST notifications signed with HMAC-SHA256 to this URL whenever a parcel status changes.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
            active ? 'bg-emerald-100 text-emerald-800' : 'bg-gray-100 text-gray-600'
          }`}>
            {active ? '● Endpoint Active' : '○ Endpoint Disabled'}
          </span>
        </div>
      </div>

      <form onSubmit={handleSave} className="mt-5 space-y-4">
        {/* Target URL Field */}
        <div>
          <label className="block text-xs font-semibold uppercase tracking-wider text-gray-700 mb-1">
            Target Webhook URL
          </label>
          <div className="flex flex-col sm:flex-row gap-2">
            <input
              type="url"
              required
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://your-store.com/api/webhooks/orders"
              className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-600 focus:border-transparent"
            />
            <button
              type="button"
              onClick={handlePing}
              disabled={pinging}
              className="px-4 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-semibold rounded-lg border border-indigo-200 transition-colors disabled:opacity-50 whitespace-nowrap"
            >
              {pinging ? 'Pinging...' : '⚡ Send Test Ping'}
            </button>
          </div>
          <p className="text-xs text-gray-400 mt-1">
            Tip: For local testing, use the mock merchant store URL: <code className="bg-gray-100 px-1 py-0.5 rounded text-gray-800">http://localhost:8085/api/webhooks/orders</code>
          </p>
        </div>

        {/* Test Ping Result Banner */}
        {pingResult && (
          <div className={`p-3 rounded-lg text-xs flex items-center justify-between border ${
            pingResult.success
              ? 'bg-emerald-50 text-emerald-900 border-emerald-200'
              : 'bg-rose-50 text-rose-900 border-rose-200'
          }`}>
            <div className="flex items-center gap-2">
              <span className="font-bold">{pingResult.success ? '✓ Ping Succeeded:' : '✕ Ping Failed:'}</span>
              <span>{pingResult.message} (HTTP {pingResult.httpStatus})</span>
            </div>
            {pingResult.durationMs > 0 && (
              <span className="text-gray-500 font-mono">{pingResult.durationMs}ms</span>
            )}
          </div>
        )}

        {/* Signing Secret Field */}
        <div>
          <div className="flex items-center justify-between mb-1">
            <label className="block text-xs font-semibold uppercase tracking-wider text-gray-700">
              Signing Secret (HMAC-SHA256)
            </label>
            <button
              type="button"
              onClick={handleRegenerate}
              className="text-xs text-rose-600 hover:text-rose-800 underline font-medium"
            >
              Regenerate Secret
            </button>
          </div>
          <div className="flex items-center gap-2 bg-gray-50 border border-gray-200 rounded-lg px-3 py-2">
            <input
              type={showSecret ? 'text' : 'password'}
              readOnly
              value={secret}
              className="flex-1 bg-transparent font-mono text-xs text-gray-700 focus:outline-none"
            />
            <button
              type="button"
              onClick={() => setShowSecret(!showSecret)}
              className="text-xs text-gray-500 hover:text-gray-700 px-2 py-1 rounded bg-white border border-gray-200"
            >
              {showSecret ? 'Hide' : 'Reveal'}
            </button>
            <button
              type="button"
              onClick={handleCopySecret}
              className="text-xs text-gray-700 hover:text-gray-900 px-2.5 py-1 rounded bg-white border border-gray-200 font-semibold"
            >
              {copied ? '✓ Copied' : 'Copy'}
            </button>
          </div>
          <p className="text-xs text-gray-400 mt-1">
            Every webhook request includes an <code className="bg-gray-100 px-1 py-0.5 rounded text-gray-800">X-SwiftDrop-Signature</code> header. Use this secret to verify the payload integrity.
          </p>
        </div>

        {/* Active Toggle & Save Button */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pt-3 border-t border-gray-100 gap-3">
          <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={active}
              onChange={(e) => setActive(e.target.checked)}
              className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
            />
            <span>Enable webhook event dispatching for parcel status updates</span>
          </label>

          <button
            type="submit"
            disabled={saving}
            className="w-full sm:w-auto px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold rounded-lg shadow-sm transition-colors disabled:opacity-50"
          >
            {saving ? 'Saving...' : 'Save Configuration'}
          </button>
        </div>
      </form>
    </div>
  );
};
