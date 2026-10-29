import React, { useState } from 'react';
import type { CourierDelivery } from '../types';
import { deliverParcelWithProof } from '../api';

interface Props {
  isOpen: boolean;
  parcel: CourierDelivery | null;
  onClose: () => void;
  onSuccess: () => void;
}

export const DeliveryProofModal: React.FC<Props> = ({ isOpen, parcel, onClose, onSuccess }) => {
  const [recipientName, setRecipientName] = useState('');
  const [note, setNote] = useState('');
  const [photo, setPhoto] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen || !parcel) return null;

  const handlePhotoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      if (file.size > 5 * 1024 * 1024) {
        setError('Photo must be smaller than 5MB');
        return;
      }
      setPhoto(file);
      setPreviewUrl(URL.createObjectURL(file));
      setError('');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!recipientName.trim()) {
      setError('Recipient name is required');
      return;
    }

    try {
      setLoading(true);
      setError('');
      await deliverParcelWithProof(parcel.parcelId, recipientName.trim(), note.trim(), photo);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to submit delivery proof');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60 p-0 sm:p-4 backdrop-blur-sm">
      <div className="w-full max-w-lg rounded-t-2xl sm:rounded-2xl bg-white p-6 shadow-xl animate-in slide-in-from-bottom duration-200">
        <div className="flex justify-between items-center border-b pb-3 mb-4">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Complete Delivery</h2>
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
              Recipient Name *
            </label>
            <input
              type="text"
              required
              placeholder="e.g. Mrs. Silva"
              value={recipientName}
              onChange={(e) => setRecipientName(e.target.value)}
              className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Proof Photo (Optional)
            </label>
            <input
              type="file"
              accept="image/jpeg,image/png"
              capture="environment"
              onChange={handlePhotoChange}
              className="w-full text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-emerald-50 file:text-emerald-700 hover:file:bg-emerald-100 cursor-pointer"
            />
            {previewUrl && (
              <div className="mt-2 relative">
                <img src={previewUrl} alt="Proof preview" className="w-full h-36 object-cover rounded-lg border border-slate-200" />
                <button
                  type="button"
                  onClick={() => { setPhoto(null); setPreviewUrl(null); }}
                  className="absolute top-2 right-2 bg-black/70 text-white rounded-full px-2 py-0.5 text-xs"
                >
                  Remove
                </button>
              </div>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Remarks / Notes
            </label>
            <textarea
              rows={2}
              placeholder="e.g. Left with security guard"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              className="w-full px-3.5 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 text-sm"
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
              className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-sm disabled:opacity-50"
            >
              {loading ? 'Submitting...' : 'Confirm Delivered'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};