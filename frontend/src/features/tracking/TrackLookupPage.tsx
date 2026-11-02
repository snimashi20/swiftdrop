import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from './components/PageHeader';
import { TRACKING_NUMBER_REGEX } from './statusMeta';

export default function TrackLookupPage() {
  const [value, setValue] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    const trackingNumber = value.trim().toUpperCase();
    if (!TRACKING_NUMBER_REGEX.test(trackingNumber)) {
      setError('Tracking numbers look like SD7K2M9QX4LP (SD + 10 letters or digits).');
      return;
    }
    setError('');
    navigate(`/track/${trackingNumber}`);
  };

  return (
    <div className="space-y-5 p-4 md:p-8">
      <PageHeader title="Track Parcel" subtitle="Enter your tracking number to see where your parcel is" />
      <section className="mx-auto mt-6 max-w-xl rounded-xl border border-slate-200 bg-white p-6 shadow-sm md:mt-12">
        <h2 className="text-xl font-semibold text-slate-900">Where is your parcel?</h2>
        <p className="mb-5 mt-1 text-sm text-slate-600">No account needed. You’ll find the number on your receipt.</p>
        <form onSubmit={onSubmit} className="flex flex-col gap-3 sm:flex-row" noValidate>
          <label htmlFor="tracking-number" className="sr-only">
            Tracking number
          </label>
          <input
            id="tracking-number"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            placeholder="SD7K2M9QX4LP"
            autoComplete="off"
            autoCapitalize="characters"
            maxLength={12}
            className="min-w-0 flex-1 rounded-lg border border-slate-300 bg-white p-3 font-semibold uppercase tracking-wider"
          />
          <button type="submit" className="rounded-lg bg-blue-600 px-6 py-3 font-semibold text-white hover:bg-blue-700">
            Track
          </button>
        </form>
        {error && (
          <p role="alert" className="mt-3 text-sm text-red-600">
            {error}
          </p>
        )}
      </section>
    </div>
  );
}
