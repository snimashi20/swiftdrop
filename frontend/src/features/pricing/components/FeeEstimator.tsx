import { useState, useEffect } from 'react';
import { fetchQuote, QuoteResponse } from '../api/pricingApi';

interface FeeEstimatorProps {
  pickupDistrict: string;
  dropoffDistrict: string;
  size: 'S' | 'M' | 'L';
  weightKg: number;
}

export const FeeEstimator = ({ 
  pickupDistrict, 
  dropoffDistrict, 
  size, 
  weightKg 
}: FeeEstimatorProps) => {
  const [quote, setQuote] = useState<QuoteResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!pickupDistrict || !dropoffDistrict || !weightKg || weightKg <= 0) {
      setQuote(null);
      return;
    }

    const getQuote = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchQuote({
          pickupDistrict,
          dropoffDistrict,
          size,
          weightKg,
        });
        setQuote(data);
      } catch (err: any) {
        if (err.response?.status === 404) {
          setError(err.response.data.message || 'Zone not found for the selected district.');
        } else {
          setError('Could not calculate fee. Please try again.');
        }
        setQuote(null);
      } finally {
        setLoading(false);
      }
    };

    const timeoutId = setTimeout(() => {
      getQuote();
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [pickupDistrict, dropoffDistrict, size, weightKg]);

  if (loading) {
    return <div className="text-ink/50 text-sm animate-pulse font-sans">Calculating fee...</div>;
  }

  if (error) {
    return <div className="text-coral text-sm font-sans">{error}</div>;
  }

  if (!quote) {
    return null;
  }

  return (
    <div className="bg-mist border border-ink/10 rounded-lg p-4 mt-4 shadow-sm">
      <h3 className="text-lg font-display font-semibold text-ink">Estimated Fee</h3>
      <p className="text-3xl font-display font-bold text-ink">Rs. {quote.fee.toFixed(2)}</p>
      
      <div className="mt-2 text-sm text-ink/70 font-sans">
        <p>From: <span className="font-medium text-ink">{quote.pickupZone}</span></p>
        <p>To: <span className="font-medium text-ink">{quote.dropoffZone}</span></p>
        {quote.interZone && (
          <p className="text-coral font-medium mt-1">* Includes inter-zone surcharge</p>
        )}
      </div>
    </div>
  );
};
