import type { ReactNode } from 'react';
import StatusBadge from './StatusBadge';
import type { TrackingResult } from '../types';

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="min-w-0">
      <div className="text-xs text-ink/50">{label}</div>
      <div className="mt-1 truncate font-semibold text-ink">{children}</div>
    </div>
  );
}

function formatDate(iso?: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
}

export default function ShipmentInfoBar({ data }: { data: TrackingResult }) {
  const booked = data.timeline.find((e) => e.status === 'CREATED')?.changedAt ?? data.timeline[0]?.changedAt;
  return (
    <section
      aria-label="Shipment details"
      className="flex flex-col gap-4 rounded-2xl border border-ink/10 bg-white p-5 md:flex-row md:items-center"
    >
      <div className="grid h-12 w-12 shrink-0 place-items-center rounded-xl bg-mint text-ink">
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M21 8l-9-5-9 5v8l9 5 9-5z" />
          <path d="M3 8l9 5 9-5M12 13v8" />
        </svg>
      </div>
      <div className="grid flex-1 grid-cols-2 gap-x-6 gap-y-4 md:grid-cols-5">
        <Field label="Tracking number">
          <span className="tracking-wide">{data.trackingNumber}</span>
        </Field>
        <Field label="Status">
          <StatusBadge status={data.status} />
        </Field>
        <Field label="Booked on">{formatDate(booked)}</Field>
        <Field label="Route">
          {data.pickupCity} → {data.dropoffCity}
        </Field>
        <Field label="Receiver">{data.receiverName}</Field>
      </div>
    </section>
  );
}
