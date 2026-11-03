import { MAIN_STEPS, STATUS_LABEL } from '../statusMeta';
import type { ParcelStatus, TimelineEntry } from '../types';

interface Props {
  status: ParcelStatus;
  timeline: TimelineEntry[];
}

function formatTime(iso: string): string {
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleString();
}

/** Progress steps + history list. Reused by the customer parcel detail page. */
export default function StatusTimeline({ status, timeline }: Props) {
  const reached = new Set(timeline.map((e) => e.status));
  const failed = status === 'FAILED';
  const cancelled = status === 'CANCELLED';
  const history = [...timeline].reverse(); // newest first

  return (
    <div className="space-y-6">
      {failed && (
        <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-3 text-red-800">
          <p className="font-semibold">Delivery attempt failed</p>
          <p className="text-sm">The courier could not deliver this parcel. A new attempt may follow.</p>
        </div>
      )}
      {cancelled && (
        <div role="status" className="rounded-xl border border-ink/10 bg-ink/5 p-3 text-ink">
          <p className="font-semibold">This delivery was cancelled</p>
          <p className="text-sm text-ink/70">The parcel was not collected and no further updates will follow.</p>
        </div>
      )}

      <ol className="flex items-start" aria-label="Delivery progress">
        {MAIN_STEPS.map((step, i) => {
          const done = reached.has(step);
          const current = step === status;
          return (
            <li key={step} className="flex flex-1 flex-col items-center text-center">
              <div className="flex w-full items-center">
                <div className={`h-1 flex-1 ${i === 0 ? 'invisible' : done ? 'bg-ink' : 'bg-ink/15'}`} />
                <div
                  className={`h-4 w-4 shrink-0 rounded-full border-2 ${
                    done ? 'border-ink bg-ink' : 'border-ink/25 bg-white'
                  } ${current && !cancelled ? 'ring-4 ring-coral/30' : ''}`}
                />
                <div
                  className={`h-1 flex-1 ${
                    i === MAIN_STEPS.length - 1
                      ? 'invisible'
                      : reached.has(MAIN_STEPS[i + 1])
                        ? 'bg-ink'
                        : 'bg-ink/15'
                  }`}
                />
              </div>
              <span className={`mt-2 text-xs ${done ? 'font-semibold text-ink' : 'text-ink/40'}`}>
                {STATUS_LABEL[step]}
              </span>
            </li>
          );
        })}
      </ol>

      <section aria-label="History">
        <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-ink/50">History</h2>
        <ul className="divide-y divide-ink/10 rounded-xl border border-ink/10 bg-white">
          {history.map((entry) => (
            <li key={`${entry.status}-${entry.changedAt}`} className="p-3">
              <div className="flex items-baseline justify-between gap-3">
                <span className={`font-medium ${entry.status === 'FAILED' ? 'text-red-700' : ''}`}>
                  {STATUS_LABEL[entry.status]}
                </span>
                <time className="text-xs text-ink/50" dateTime={entry.changedAt}>
                  {formatTime(entry.changedAt)}
                </time>
              </div>
              {entry.note && <p className="mt-1 text-sm text-ink/60">{entry.note}</p>}
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
