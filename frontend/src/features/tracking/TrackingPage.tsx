import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import PageHeader from './components/PageHeader';
import ShipmentInfoBar from './components/ShipmentInfoBar';
import StatusTimeline from './components/StatusTimeline';
import TrackingMap from './components/TrackingMap';
import { TRACKING_NUMBER_REGEX } from './statusMeta';
import { useLiveTracking } from './useLiveTracking';

function BackLink() {
  return (
    <Link to="/track" className="text-sm font-medium text-blue-700 hover:underline">
      ← Track another parcel
    </Link>
  );
}

export default function TrackingPage() {
  const { trackingNumber = '' } = useParams<{ trackingNumber: string }>();
  const normalized = trackingNumber.toUpperCase();
  const valid = TRACKING_NUMBER_REGEX.test(normalized);

  const { data, loading, error, live } = useLiveTracking(valid ? normalized : '');
  const [expanded, setExpanded] = useState(false);

  useEffect(() => {
    if (!expanded) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setExpanded(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [expanded]);

  return (
    <div className="space-y-5 p-4 md:p-8">
      <PageHeader title="Tracking Details" subtitle="Real-time updates of your shipment" />
      <BackLink />

      {!valid && (
        <div role="alert" className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-amber-900">
          That isn’t a valid tracking number. Tracking numbers look like SD7K2M9QX4LP.
        </div>
      )}

      {valid && loading && <p className="text-slate-600">Loading…</p>}

      {error === 'not-found' && (
        <div role="alert" className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-amber-900">
          We couldn’t find a parcel with this tracking number. Please check it and try again.
        </div>
      )}

      {error === 'failed' && (
        <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-800">
          Tracking is temporarily unavailable. Please try again in a moment.
        </div>
      )}

      {data && (
        <>
          <ShipmentInfoBar data={data} />

          <section
            aria-label="Live tracking"
            className={
              expanded
                ? 'fixed inset-0 z-[2000] flex flex-col bg-white'
                : 'overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm'
            }
          >
            <div className="flex items-center justify-between gap-3 border-b border-slate-200 px-5 py-3">
              <div className="flex items-center gap-3">
                <h2 className="text-lg font-semibold text-slate-900">Live Tracking</h2>
                <span className="flex items-center gap-1.5 text-xs text-slate-600" aria-live="polite">
                  <span className={`h-2 w-2 rounded-full ${live ? 'bg-green-500' : 'bg-slate-300'}`} />
                  {live ? 'Live' : 'Connecting…'}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setExpanded((v) => !v)}
                className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-slate-50"
              >
                {expanded ? 'Close full map' : 'View full map'}
              </button>
            </div>
            <div className={expanded ? 'min-h-0 flex-1' : 'h-[380px] md:h-[540px]'}>
              <TrackingMap
                pickupCity={data.pickupCity}
                dropoffCity={data.dropoffCity}
                status={data.status}
                timeline={data.timeline}
                live={live}
                expanded={expanded}
              />
            </div>
            <p className="border-t border-slate-200 px-5 py-2 text-xs text-slate-500">
              The truck position is estimated from the parcel status.
            </p>
          </section>

          <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="mb-4 text-lg font-semibold text-slate-900">Shipment progress</h2>
            <StatusTimeline status={data.status} timeline={data.timeline} />
          </section>
        </>
      )}
    </div>
  );
}
