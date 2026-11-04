import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import LiveParcelView from './components/LiveParcelView';
import PageHeader from './components/PageHeader';

/**
 * Logged-in parcel detail page /parcels/:trackingNumber with live status.
 * It reuses the same live view as the public page. When M3's owner-only parcel
 * API exists, show the extra details (addresses, fee) above the live view.
 */
export default function ParcelDetailPage() {
  const { trackingNumber = '' } = useParams<{ trackingNumber: string }>();
  const normalized = trackingNumber.toUpperCase();
  const [copied, setCopied] = useState(false);

  const shareLink = async () => {
    try {
      await navigator.clipboard.writeText(`${window.location.origin}/track/${normalized}`);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      // clipboard not available (e.g. insecure context): nothing to do
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <PageHeader title={`Parcel ${normalized}`} subtitle="Live status of your delivery" />
        <div className="flex gap-2">
          <button
            type="button"
            onClick={shareLink}
            className="rounded-xl border border-ink/15 bg-white px-4 py-2 text-sm font-semibold text-ink hover:bg-ink/5"
          >
            {copied ? 'Link copied' : 'Share tracking link'}
          </button>
          <Link
            to={`/track/${normalized}`}
            className="rounded-xl bg-ink px-4 py-2 text-sm font-semibold text-white hover:bg-ink/90"
          >
            Open public page
          </Link>
        </div>
      </div>
      <Link to="/parcels" className="inline-block text-sm font-medium text-ink/70 hover:text-ink hover:underline">
        ← Back to my parcels
      </Link>
      <LiveParcelView trackingNumber={normalized} />
    </div>
  );
}
