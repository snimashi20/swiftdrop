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

  const [shareError, setShareError] = useState<string | null>(null);

  const copyFallback = (text: string): boolean => {
    const el = document.createElement('textarea');
    el.value = text;
    el.setAttribute('readonly', '');
    el.style.position = 'fixed';
    el.style.opacity = '0';
    document.body.appendChild(el);
    el.select();
    try {
      return document.execCommand('copy');
    } catch {
      return false;
    } finally {
      document.body.removeChild(el);
    }
  };

  const shareLink = async () => {
    const url = `${window.location.origin}/track/${normalized}`;
    setShareError(null);
    let ok = false;
    try {
      await navigator.clipboard.writeText(url);
      ok = true;
    } catch {
      ok = copyFallback(url); // Clipboard API needs HTTPS; fall back to execCommand
    }
    if (ok) {
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } else {
      setShareError(`Couldn't copy automatically. Copy this link: ${url}`);
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
      {shareError && (
        <p role="alert" className="select-all break-all rounded-xl bg-red-50 px-4 py-2 text-sm text-red-700">
          {shareError}
        </p>
      )}
      <Link to="/parcels" className="inline-block text-sm font-medium text-ink/70 hover:text-ink hover:underline">
        ← Back to my parcels
      </Link>
      <LiveParcelView trackingNumber={normalized} />
    </div>
  );
}
