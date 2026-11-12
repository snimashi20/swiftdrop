import { useState } from 'react';
import { fetchQuote, QuoteResponse } from '../api/pricingApi';

export const PriceCalculator = () => {
  const [pickupDistrict, setPickupDistrict] = useState('Colombo');
  const [dropoffDistrict, setDropoffDistrict] = useState('Kandy');
  const [size, setSize] = useState<'S' | 'M' | 'L'>('M');
  const [weightKg, setWeightKg] = useState(1);
  const [quote, setQuote] = useState<QuoteResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const districts = ['Colombo', 'Gampaha', 'Kandy', 'Galle', 'Jaffna'];

  const handleCalculate = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchQuote({ pickupDistrict, dropoffDistrict, size, weightKg });
      setQuote(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Zone not found for the selected district.');
      setQuote(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-mist border border-ink/10 rounded-2xl p-6 shadow-sm max-w-md mx-auto">
      <h2 className="font-display text-2xl font-bold text-ink mb-4">Instant Price Estimate</h2>
      
      <div className="space-y-3">
        <div>
          <label className="block text-xs font-semibold uppercase tracking-wide text-ink/60 mb-1">From</label>
          <select value={pickupDistrict} onChange={(e) => setPickupDistrict(e.target.value)} className="w-full rounded-xl border border-ink/15 bg-white px-3 py-2.5 text-sm">
            {districts.map(d => <option key={d} value={d}>{d}</option>)}
          </select>
        </div>
        <div>
          <label className="block text-xs font-semibold uppercase tracking-wide text-ink/60 mb-1">To</label>
          <select value={dropoffDistrict} onChange={(e) => setDropoffDistrict(e.target.value)} className="w-full rounded-xl border border-ink/15 bg-white px-3 py-2.5 text-sm">
            {districts.map(d => <option key={d} value={d}>{d}</option>)}
          </select>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wide text-ink/60 mb-1">Size</label>
            <select value={size} onChange={(e) => setSize(e.target.value as 'S' | 'M' | 'L')} className="w-full rounded-xl border border-ink/15 bg-white px-3 py-2.5 text-sm">
              <option value="S">Small</option>
              <option value="M">Medium</option>
              <option value="L">Large</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wide text-ink/60 mb-1">Weight (kg)</label>
            <input type="number" min="0.1" step="0.1" value={weightKg} onChange={(e) => setWeightKg(Number(e.target.value))} className="w-full rounded-xl border border-ink/15 bg-white px-3 py-2.5 text-sm" />
          </div>
        </div>
      </div>

      <button onClick={handleCalculate} disabled={loading} className="w-full mt-4 rounded-xl bg-coral px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60">
        {loading ? 'Calculating...' : 'Calculate Fee'}
      </button>

      {error && <p className="mt-3 text-sm text-coral">{error}</p>}

      {quote && (
        <div className="mt-4 bg-white rounded-xl p-4 border border-ink/10">
          <p className="text-xs font-semibold uppercase text-ink/60">Estimated Fee</p>
          <p className="font-display text-3xl font-bold text-ink">Rs. {quote.fee.toFixed(2)}</p>
          <p className="text-xs text-ink/60 mt-1">{quote.pickupZone} → {quote.dropoffZone} {quote.interZone && '(inter-zone)'}</p>
        </div>
      )}
    </div>
  );
};
