import React, { useState, useEffect } from 'react';
import type { CourierDelivery } from '../types';
import { getTodayDeliveries, updateDeliveryStatus } from '../api';
import { DeliveryProofModal } from '../components/DeliveryProofModal';
import { FailDeliveryModal } from '../components/FailDeliveryModal';

type FilterTab = 'ALL' | 'ACTIVE' | 'COMPLETED';

export const TodayDeliveriesPage: React.FC = () => {
  const [deliveries, setDeliveries] = useState<CourierDelivery[]>([]);
  const [filter, setFilter] = useState<FilterTab>('ALL');
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);

  // Modals state
  const [selectedParcel, setSelectedParcel] = useState<CourierDelivery | null>(null);
  const [proofModalOpen, setProofModalOpen] = useState(false);
  const [failModalOpen, setFailModalOpen] = useState(false);

  const fetchDeliveries = async () => {
    try {
      setLoading(true);
      const data = await getTodayDeliveries();
      setDeliveries(data);
    } catch (err) {
      console.error('Failed to load deliveries', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDeliveries();
  }, []);

  const handleQuickStatusUpdate = async (parcelId: number, nextStatus: string) => {
    try {
      setActionLoadingId(parcelId);
      await updateDeliveryStatus(parcelId, nextStatus);
      await fetchDeliveries();
    } catch (err) {
      alert('Failed to update status');
    } finally {
      setActionLoadingId(null);
    }
  };

  const filtered = deliveries.filter((d) => {
    if (filter === 'ACTIVE') {
      return ['PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY'].includes(d.status);
    }
    if (filter === 'COMPLETED') {
      return ['DELIVERED', 'FAILED'].includes(d.status);
    }
    return true;
  });

  return (
    <div className="max-w-md mx-auto min-h-screen bg-slate-50 pb-20">
      {/* Sticky Mobile Header */}
      <header className="bg-blue-600 text-white p-4 sticky top-0 z-10 shadow-md">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-xl font-bold">Today's Deliveries</h1>
            <p className="text-xs text-blue-100">{deliveries.length} total parcels assigned</p>
          </div>
          <button 
            onClick={fetchDeliveries}
            className="text-xs bg-blue-700 hover:bg-blue-800 px-3 py-1.5 rounded-lg border border-blue-500"
          >
            ↻ Refresh
          </button>
        </div>

        {/* Filter Pills */}
        <div className="flex gap-2 mt-3">
          {(['ALL', 'ACTIVE', 'COMPLETED'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setFilter(tab)}
              className={`px-3 py-1 rounded-full text-xs font-semibold transition-colors ${
                filter === tab ? 'bg-white text-blue-600 shadow-sm' : 'bg-blue-500/60 text-white'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>
      </header>

      {/* Main Delivery List */}
      <main className="p-4 space-y-3">
        {loading && <p className="text-center text-sm text-slate-500 py-8">Loading deliveries...</p>}

        {!loading && filtered.length === 0 && (
          <div className="text-center py-12 bg-white rounded-xl border border-slate-200 p-6">
            <p className="text-sm font-semibold text-slate-700">No deliveries found</p>
            <p className="text-xs text-slate-500 mt-1">There are no parcels in this category right now.</p>
          </div>
        )}

        {filtered.map((item) => (
          <div 
            key={item.parcelId} 
            className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 space-y-3"
          >
            {/* Top row: Tracking number & Status pill */}
            <div className="flex justify-between items-center">
              <span className="font-mono text-xs font-bold text-blue-700 bg-blue-50 px-2.5 py-1 rounded">
                {item.trackingNumber}
              </span>
              <span className={`text-[11px] font-bold px-2.5 py-0.5 rounded-full ${
                item.status === 'DELIVERED' ? 'bg-emerald-100 text-emerald-800' :
                item.status === 'FAILED' ? 'bg-rose-100 text-rose-800' :
                item.status === 'OUT_FOR_DELIVERY' ? 'bg-amber-100 text-amber-800' :
                'bg-slate-100 text-slate-700'
              }`}>
                {item.status}
              </span>
            </div>

            {/* Recipient Details & Quick Call */}
            <div>
              <div className="flex justify-between items-start">
                <h3 className="font-semibold text-slate-900 text-base">{item.recipientName}</h3>
                {item.recipientPhone && (
                  <a 
                    href={`tel:${item.recipientPhone}`} 
                    className="text-xs bg-slate-100 hover:bg-slate-200 text-slate-700 px-2 py-1 rounded flex items-center gap-1 font-medium"
                  >
                    📞 Call
                  </a>
                )}
              </div>
              <p className="text-xs text-slate-600 mt-1 flex items-start gap-1">
                📍 <span>{item.deliveryAddress}</span>
              </p>
              {item.deliveryInstructions && (
                <p className="text-[11px] text-amber-700 bg-amber-50 p-1.5 rounded mt-2 border border-amber-200">
                  ⚠️ Note: {item.deliveryInstructions}
                </p>
              )}
            </div>

            {/* Action Buttons for Couriers */}
            {item.status !== 'DELIVERED' && item.status !== 'FAILED' && (
              <div className="pt-2 border-t border-slate-100 space-y-2">
                {item.status === 'PICKED_UP' && (
                  <button
                    onClick={() => handleQuickStatusUpdate(item.parcelId, 'IN_TRANSIT')}
                    disabled={actionLoadingId === item.parcelId}
                    className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 rounded-lg text-xs font-semibold shadow-sm"
                  >
                    Start Transit (In Transit)
                  </button>
                )}

                {item.status === 'IN_TRANSIT' && (
                  <button
                    onClick={() => handleQuickStatusUpdate(item.parcelId, 'OUT_FOR_DELIVERY')}
                    disabled={actionLoadingId === item.parcelId}
                    className="w-full bg-amber-600 hover:bg-amber-700 text-white py-2 rounded-lg text-xs font-semibold shadow-sm"
                  >
                    Out for Delivery
                  </button>
                )}

                {item.status === 'OUT_FOR_DELIVERY' && (
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={() => { setSelectedParcel(item); setProofModalOpen(true); }}
                      className="bg-emerald-600 hover:bg-emerald-700 text-white py-2.5 rounded-lg text-xs font-semibold shadow-sm"
                    >
                      ✓ Delivered
                    </button>
                    <button
                      onClick={() => { setSelectedParcel(item); setFailModalOpen(true); }}
                      className="bg-rose-600 hover:bg-rose-700 text-white py-2.5 rounded-lg text-xs font-semibold shadow-sm"
                    >
                      ✕ Failed
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
      </main>

      {/* Modals */}
      <DeliveryProofModal
        isOpen={proofModalOpen}
        parcel={selectedParcel}
        onClose={() => { setProofModalOpen(false); setSelectedParcel(null); }}
        onSuccess={fetchDeliveries}
      />

      <FailDeliveryModal
        isOpen={failModalOpen}
        parcel={selectedParcel}
        onClose={() => { setFailModalOpen(false); setSelectedParcel(null); }}
        onSuccess={fetchDeliveries}
      />
    </div>
  );
};