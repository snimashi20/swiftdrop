import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { getApiErrorMessage } from "../../../lib/api";
import { notificationsApi } from "../api";
import { PageHeader } from "../components/PageHeader";
import type {
  NotificationChannel,
  NotificationDeliveryStatus,
  NotificationPage,
} from "../types";

const PAGE_SIZE = 20;

type Filters = { channel: "" | NotificationChannel; status: "" | NotificationDeliveryStatus; trackingNumber: string };
const NO_FILTERS: Filters = { channel: "", status: "", trackingNumber: "" };

const CHANNEL_LABELS: Record<NotificationChannel, string> = {
  IN_APP: "In-app",
  EMAIL: "E-mail",
  SMS: "SMS",
};

const STATUS_STYLE: Record<NotificationDeliveryStatus, string> = {
  SENT: "border-mint bg-mint/40 text-ink",
  PENDING: "border-ink/15 bg-mist text-ink/60",
  FAILED: "border-coral/30 bg-coral/10 text-coral",
};

const inputClass =
  "w-full rounded-xl border border-ink/15 bg-white px-3.5 py-2.5 text-sm outline-none transition placeholder:text-ink/30 focus:border-coral focus:ring-4 focus:ring-coral/10";

const formatTime = (iso: string | null) => (iso ? new Date(iso).toLocaleString() : "—");

// PICKED_UP -> "Picked up"
const humanise = (value: string | null) =>
  value ? value.toLowerCase().replace(/_/g, " ").replace(/^\w/, (c) => c.toUpperCase()) : "—";

const STATUS_LABEL: Record<NotificationDeliveryStatus, string> = {
  SENT: "Sent",
  PENDING: "Waiting to send",
  FAILED: "Not sent",
};

// The notification service stores only the technical error type; show a plain sentence instead.
const friendlyReason = (reason: string) => {
  if (/mail.?send|send.?failed/i.test(reason)) return "The e-mail could not be sent.";
  if (/connect|timeout|socket|unavailable/i.test(reason)) return "The mail service could not be reached.";
  if (/sms/i.test(reason)) return "The text message could not be sent.";
  return "The message could not be sent.";
};

export function NotificationLogPage() {
  const [form, setForm] = useState<Filters>(NO_FILTERS);
  const [filters, setFilters] = useState<Filters>(NO_FILTERS);
  const [page, setPage] = useState(0);
  const [data, setData] = useState<NotificationPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setData(
        await notificationsApi.list({
          page,
          size: PAGE_SIZE,
          channel: filters.channel || undefined,
          status: filters.status || undefined,
          trackingNumber: filters.trackingNumber.trim() || undefined,
        }),
      );
    } catch (err) {
      setError(getApiErrorMessage(err, "We couldn't load the messages. Please refresh the page."));
    } finally {
      setLoading(false);
    }
  }, [filters, page]);

  useEffect(() => {
    void load();
  }, [load]);

  const applyFilters = (event: FormEvent) => {
    event.preventDefault();
    setPage(0);
    setFilters(form);
  };

  const clearFilters = () => {
    setForm(NO_FILTERS);
    setFilters(NO_FILTERS);
    setPage(0);
  };

  const entries = data?.content ?? [];

  return (
    <div>
      <PageHeader
        eyebrow="Admin"
        title="Notification log"
        text="Every in-app, e-mail and text message sent to customers about their parcels, newest first. Contact details are partly hidden for privacy."
      />

      {error && <p className="mb-4 rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral">{error}</p>}

      <form onSubmit={applyFilters} className="mb-6 grid gap-3 rounded-2xl border border-ink/10 bg-white p-4 sm:grid-cols-4">
        <label className="block space-y-1">
          <span className="text-xs font-semibold text-ink/60">Channel</span>
          <select
            className={inputClass}
            value={form.channel}
            onChange={(e) => setForm({ ...form, channel: e.target.value as Filters["channel"] })}
          >
            <option value="">All</option>
            <option value="IN_APP">In-app</option>
            <option value="EMAIL">E-mail</option>
            <option value="SMS">Text message</option>
          </select>
        </label>
        <label className="block space-y-1">
          <span className="text-xs font-semibold text-ink/60">Message status</span>
          <select
            className={inputClass}
            value={form.status}
            onChange={(e) => setForm({ ...form, status: e.target.value as Filters["status"] })}
          >
            <option value="">All</option>
            <option value="SENT">Sent</option>
            <option value="PENDING">Pending</option>
            <option value="FAILED">Failed</option>
          </select>
        </label>
        <label className="block space-y-1">
          <span className="text-xs font-semibold text-ink/60">Tracking number</span>
          <input
            className={inputClass}
            placeholder="e.g. SD7K2M9QX4LP"
            value={form.trackingNumber}
            onChange={(e) => setForm({ ...form, trackingNumber: e.target.value })}
          />
        </label>
        <div className="flex items-end gap-2">
          <button className="rounded-xl bg-ink px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-coral">Apply</button>
          <button
            type="button"
            onClick={clearFilters}
            className="rounded-xl border border-ink/15 px-4 py-2.5 text-sm font-semibold"
          >
            Clear
          </button>
        </div>
      </form>

      <section className="rounded-2xl border border-ink/10 bg-white p-5">
        {loading && !data ? (
          <p className="text-sm text-ink/50">Loading notifications…</p>
        ) : entries.length === 0 ? (
          error ? null : (
            <p className="rounded-xl border border-dashed border-ink/20 p-6 text-sm text-ink/55">
              No notifications match these filters.
            </p>
          )
        ) : (
          <>
            <div className="overflow-x-auto rounded-xl border border-ink/10">
              <table className="w-full min-w-[48rem] text-left text-sm">
                <thead>
                  <tr className="border-b border-ink/10 bg-mist/60 text-xs uppercase tracking-wider text-ink/45">
                    <th className="px-4 py-3 font-semibold">Time</th>
                    <th className="px-4 py-3 font-semibold">Tracking no.</th>
                    <th className="px-4 py-3 font-semibold">Channel</th>
                    <th className="px-4 py-3 font-semibold">Recipient</th>
                    <th className="px-4 py-3 font-semibold">Parcel status</th>
                    <th className="px-4 py-3 font-semibold">Result</th>
                    <th className="px-4 py-3 font-semibold">Sent at</th>
                  </tr>
                </thead>
                <tbody>
                  {entries.map((entry) => (
                    <tr key={entry.id} className="border-b border-ink/5 align-top last:border-0">

                      <td className="px-4 py-3 text-ink/60">{formatTime(entry.createdAt)}</td>
                      <td className="px-4 py-3 font-semibold">{entry.trackingNumber}</td>
                      <td className="px-4 py-3">{CHANNEL_LABELS[entry.type] ?? entry.type}</td>
                      <td className="px-4 py-3 font-mono text-xs">{entry.recipient}</td>
                      <td className="px-4 py-3">
                        {entry.oldStatus ? humanise(entry.oldStatus) : "—"} → <span className="font-semibold">{humanise(entry.newStatus)}</span>

                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold ${STATUS_STYLE[entry.status]}`}
                        >
                          {STATUS_LABEL[entry.status]}
                        </span>
                        {entry.failureReason && (
                          <span className="mt-1 block text-xs text-coral">{friendlyReason(entry.failureReason)}</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-ink/60">{formatTime(entry.sentAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="mt-4 flex items-center justify-between text-sm text-ink/60">
              <span>
                Page {(data?.page ?? 0) + 1} of {Math.max(data?.totalPages ?? 1, 1)} · {data?.totalElements ?? 0}{" "}
                messages
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage(page - 1)}
                  disabled={page === 0 || loading}
                  className="rounded-lg border border-ink/15 px-3 py-1.5 font-semibold disabled:opacity-40"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage(page + 1)}
                  disabled={!data || page + 1 >= data.totalPages || loading}
                  className="rounded-lg border border-ink/15 px-3 py-1.5 font-semibold disabled:opacity-40"
                >
                  Next
                </button>
              </div>
            </div>
          </>
        )}
      </section>
    </div>
  );
}