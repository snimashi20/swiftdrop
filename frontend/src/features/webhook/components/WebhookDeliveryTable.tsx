import React, { useState } from 'react';
import { WebhookDelivery, DeliveryStatus } from '../types';

interface WebhookDeliveryTableProps {
  deliveries: WebhookDelivery[];
  onResend: (deliveryId: number) => Promise<void>;
  onRefresh: () => void;
  loading?: boolean;
}

export const WebhookDeliveryTable: React.FC<WebhookDeliveryTableProps> = ({
  deliveries,
  onResend,
  onRefresh,
  loading = false,
}) => {
  const [resendingId, setResendingId] = useState<number | null>(null);
  const [selectedPayload, setSelectedPayload] = useState<string | null>(null);

  const handleResend = async (id: number) => {
    setResendingId(id);
    try {
      await onResend(id);
    } finally {
      setResendingId(null);
    }
  };

  const getStatusBadge = (status: DeliveryStatus) => {
    switch (status) {
      case 'SUCCESS':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">
            ✓ SUCCESS
          </span>
        );
      case 'RETRYING':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-200">
            ↻ RETRYING
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800 border border-rose-200">
            ✕ FAILED (DLQ)
          </span>
        );
      case 'PENDING':
      default:
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 border border-blue-200">
            ⋯ PENDING
          </span>
        );
    }
  };

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
      {/* Header */}
      <div className="p-5 sm:p-6 border-b border-gray-100 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h3 className="text-base font-bold text-gray-900">Webhook Delivery Audit Log</h3>
          <p className="text-xs text-gray-500">
            Every dispatch attempt, response status, and retry schedule logged in real-time.
          </p>
        </div>
        <button
          onClick={onRefresh}
          disabled={loading}
          className="self-start sm:self-auto px-3 py-1.5 bg-gray-50 hover:bg-gray-100 text-gray-700 text-xs font-medium rounded-lg border border-gray-200 transition-colors disabled:opacity-50 flex items-center gap-1.5"
        >
          <span>↻</span> {loading ? 'Refreshing...' : 'Refresh Logs'}
        </button>
      </div>

      {/* Empty State */}
      {deliveries.length === 0 ? (
        <div className="text-center py-12 px-4">
          <div className="text-3xl mb-2">📦</div>
          <h4 className="text-sm font-semibold text-gray-700">No Webhook Deliveries Yet</h4>
          <p className="text-xs text-gray-400 mt-1 max-w-sm mx-auto">
            When a courier moves a parcel belonging to your store (Picked Up, In Transit, Delivered), a delivery attempt will be logged here.
          </p>
        </div>
      ) : (
        <>
          {/* Desktop Table View (Hidden on mobile) */}
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-left text-xs text-gray-600">
              <thead className="bg-gray-50 text-gray-500 uppercase tracking-wider text-[10px] border-b border-gray-200">
                <tr>
                  <th className="py-3 px-4">Tracking #</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Attempts</th>
                  <th className="py-3 px-4">Last Error / Response</th>
                  <th className="py-3 px-4">Last Attempt</th>
                  <th className="py-3 px-4">Payload</th>
                  <th className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 font-mono">
                {deliveries.map((d) => (
                  <tr key={d.id} className="hover:bg-gray-50/80 transition-colors">
                    <td className="py-3 px-4 font-bold text-gray-900 font-sans">
                      {d.trackingNumber}
                      <span className="block text-[10px] text-gray-400 font-mono">
                        {d.eventId.length > 12 ? d.eventId.substring(0, 12) + '...' : d.eventId}
                      </span>
                    </td>
                    <td className="py-3 px-4 font-sans">{getStatusBadge(d.status)}</td>
                    <td className="py-3 px-4">
                      <span className={`font-semibold ${d.attempts >= 5 ? 'text-rose-600' : 'text-gray-700'}`}>
                        {d.attempts} / 5
                      </span>
                    </td>
                    <td className="py-3 px-4 font-sans max-w-xs truncate text-gray-500">
                      {d.lastError ? (
                        <span className="text-rose-600 font-medium" title={d.lastError}>
                          {d.lastError}
                        </span>
                      ) : (
                        <span className="text-gray-400">None (HTTP 200)</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-gray-500 font-sans">
                      {d.lastAttemptAt ? new Date(d.lastAttemptAt).toLocaleTimeString() : '—'}
                    </td>
                    <td className="py-3 px-4 font-sans">
                      <button
                        onClick={() => setSelectedPayload(d.payload)}
                        className="text-blue-600 hover:text-blue-800 underline text-xs"
                      >
                        Inspect JSON
                      </button>
                    </td>
                    <td className="py-3 px-4 text-right font-sans">
                      {(d.status === 'FAILED' || d.status === 'RETRYING') && (
                        <button
                          onClick={() => handleResend(d.id)}
                          disabled={resendingId === d.id}
                          className="px-2.5 py-1 bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-300 rounded text-xs font-semibold transition-colors disabled:opacity-50"
                        >
                          {resendingId === d.id ? 'Resending...' : 'Resend'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile Card View (Visible on phone screens) */}
          <div className="md:hidden divide-y divide-gray-100">
            {deliveries.map((d) => (
              <div key={d.id} className="p-4 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-sm text-gray-900">{d.trackingNumber}</span>
                  {getStatusBadge(d.status)}
                </div>
                <div className="flex justify-between text-xs text-gray-500">
                  <span>Attempts: <strong className="text-gray-800">{d.attempts}/5</strong></span>
                  <span>{d.lastAttemptAt ? new Date(d.lastAttemptAt).toLocaleTimeString() : ''}</span>
                </div>
                {d.lastError && (
                  <div className="text-xs text-rose-600 bg-rose-50 p-2 rounded border border-rose-100">
                    {d.lastError}
                  </div>
                )}
                <div className="flex items-center justify-between pt-1">
                  <button
                    onClick={() => setSelectedPayload(d.payload)}
                    className="text-xs text-blue-600 underline font-medium"
                  >
                    Inspect JSON Payload
                  </button>
                  {(d.status === 'FAILED' || d.status === 'RETRYING') && (
                    <button
                      onClick={() => handleResend(d.id)}
                      disabled={resendingId === d.id}
                      className="px-3 py-1 bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-300 rounded text-xs font-semibold"
                    >
                      {resendingId === d.id ? 'Resending...' : 'Resend'}
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {/* JSON Payload Inspector Modal */}
      {selectedPayload && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-lg w-full p-5 space-y-3">
            <div className="flex items-center justify-between border-b pb-2">
              <h4 className="text-sm font-bold text-gray-900">Dispatched Payload JSON</h4>
              <button
                onClick={() => setSelectedPayload(null)}
                className="text-gray-400 hover:text-gray-600 text-lg leading-none"
              >
                &times;
              </button>
            </div>
            <pre className="bg-gray-900 text-emerald-400 p-4 rounded-lg text-xs font-mono overflow-x-auto max-h-72">
              {JSON.stringify(JSON.parse(selectedPayload || '{}'), null, 2)}
            </pre>
            <div className="text-right">
              <button
                onClick={() => setSelectedPayload(null)}
                className="px-4 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold rounded-lg"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
