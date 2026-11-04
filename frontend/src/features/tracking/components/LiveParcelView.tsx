import { useEffect, useState } from 'react';
import ShipmentInfoBar from './ShipmentInfoBar';
import StatusTimeline from './StatusTimeline';
import TrackingMap from './TrackingMap';
import { TRACKING_NUMBER_REGEX } from '../statusMeta';
import { useLiveTracking } from '../useLiveTracking';

/**
 * Everything below the page title for one parcel: details bar, live map and progress.
 * Shared by the public tracking page and the customer parcel detail page.
 */
export default function LiveParcelView({ trackingNumber }: { trackingNumber: string }) {
  const valid = TRACKING_NUMBER_REGEX.test(trackingNumber);
  const { data, loading, error, live } = useLiveTracking(valid ? trackingNumber : '');
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
    <>
      {!valid && (
        <div role="alert" className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-amber-900">
          That isn’t a valid tracking number. Tracking numbers look like SD7K2M9QX4LP.
        </div>
      )}

      {valid && loading && <p className="text-ink/60">Loading…</p>}

      {error === 'not-found' && (
        <div role="alert" className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-amber-900">
          We couldn’t find a parcel with this tracking number. Please check it and try again.
        </div>
      )}

      {error === 'failed' && (
        <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-4 text-red-800">
          Tracking is temporarily unavailable. Please try again in a moment.
        </div>
      )}

      {data && (
        <div className="space-y-5">
          <ShipmentInfoBar data={data} />

          <section
            aria-label="Live tracking"
            className={
              expanded
                ? 'fixed inset-0 z-[2000] flex flex-col bg-white'
                : 'overflow-hidden rounded-2xl border border-ink/10 bg-white'
            }
          >
            <div className="flex items-center justify-between gap-3 border-b border-ink/10 px-5 py-3">
              <div className="flex items-center gap-3">
                <h2 className="text-lg font-semibold text-ink">Live tracking</h2>
                <span className="flex items-center gap-1.5 text-xs text-ink/60" aria-live="polite">
                  <span className={`h-2 w-2 rounded-full ${live ? 'bg-green-500' : 'bg-ink/20'}`} />
                  {live ? 'Live' : 'Connecting…'}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setExpanded((v) => !v)}
                className="rounded-xl border border-ink/15 bg-white px-4 py-2 text-sm font-semibold text-ink hover:bg-ink/5"
              >
                {expanded ? 'Close full map' : 'View full map'}
              </button>
            </div>
            <div className={expanded ? 'min-h-0 flex-1' : 'h-[380px] md:h-[520px]'}>
              <TrackingMap
                pickupCity={data.pickupCity}
                dropoffCity={data.dropoffCity}
                status={data.status}
                timeline={data.timeline}
                live={live}
                expanded={expanded}
              />
            </div>
            <p className="border-t border-ink/10 px-5 py-2 text-xs text-ink/50">
              The truck position is estimated from the parcel status.
            </p>
          </section>

          <section className="rounded-2xl border border-ink/10 bg-white p-5">
            <h2 className="mb-4 text-lg font-semibold text-ink">Shipment progress</h2>
            <StatusTimeline status={data.status} timeline={data.timeline} />
          </section>
        </div>
      )}
    </>
  );
}
