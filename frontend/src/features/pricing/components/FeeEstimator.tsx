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
    // Don't fetch if required fields are missing
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
        // Handle our custom NotFoundException (404) from the backend
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

    // Debounce the API call so it doesn't fire on every keystroke
    const timeoutId = setTimeout(() => {
      getQuote();
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [pickupDistrict, dropoffDistrict, size, weightKg]);

  if (loading) {
    return <div className="text-gray-500 text-sm animate-pulse">Calculating fee...</div>;
  }

  if (error) {
    return <div className="text-red-500 text-sm">{error}</div>;
  }

  if (!quote) {
    return null;
  }

  return (
    <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mt-4">
      <h3 className="text-lg font-semibold text-blue-900">Estimated Fee</h3>
      <p className="text-3xl font-bold text-blue-700">Rs. {quote.fee.toFixed(2)}</p>
      
      <div className="mt-2 text-sm text-gray-600">
        <p>From: <span className="font-medium">{quote.pickupZone}</span></p>
        <p>To: <span className="font-medium">{quote.dropoffZone}</span></p>
        {quote.interZone && (
          <p className="text-orange-600 font-medium mt-1">* Includes inter-zone surcharge</p>
        )}
      </div>
    </div>
  );
};
