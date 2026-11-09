import React, { useState } from 'react';
import type { CourierDelivery, FailureReason } from '../types';
import { failParcelDelivery } from '../api';

interface Props {
  isOpen: boolean;
  parcel: CourierDelivery | null;
  onClose: () => void;
  onSuccess: () => void;
}

const FAILURE_REASONS: { value: FailureReason; label: string }[] = [
  { value: 'RECIPIENT_NOT_AVAILABLE', label: 'Recipient Not Available' },
  { value: 'WRONG_ADDRESS', label: 'Wrong Address / Cannot Locate' },
  { value: 'REFUSED', label: 'Customer Refused Delivery' },
  { value: 'DAMAGED', label: 'Package Damaged' },
  { value: 'OTHER', label: 'Other Reason' },
];

export const FailDeliveryModal: React.FC<Props> = ({ isOpen, parcel, onClose, onSuccess }) => {
  const [reason, setReason] = useState<FailureReason>('RECIPIENT_NOT_AVAILABLE');
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen || !parcel) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setLoading(true);
      setError('');
      await failParcelDelivery(parcel.parcelId, reason, note.trim());
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to record delivery failure');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60 p-0 sm:p-4 backdrop-blur-sm">
      <div className="w-full max-w-lg rounded-t-2xl sm:rounded-2xl bg-white p-6 shadow-xl animate-in slide-in-from-bottom duration-200">
        <div className="flex justify-between items-center border-b pb-3 mb-4">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Report Delivery Failure</h2>
            <p className="text-xs text-slate-500 font-mono mt-0.5">{parcel.trackingNumber}</p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600 text-2xl font-light">
            &times;
          </button>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Failure Reason *
            </label>
            <select
              value={reason}
              onChange={(e) => setReason(e.target.value as FailureReason)}
              className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 text-sm bg-white"
            >
              {FAILURE_REASONS.map((r) => (
                <option key={r.value} value={r.value}>{r.label}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Explanation / Remarks
            </label>
            <textarea
              rows={3}
              placeholder="Provide extra details (e.g. called customer twice, phone switched off)"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              className="w-full px-3.5 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 text-sm"
            />
          </div>

          <div className="grid grid-cols-2 gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="w-full py-2.5 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-sm font-semibold shadow-sm disabled:opacity-50"
            >
              {loading ? 'Recording...' : 'Confirm Failed'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};