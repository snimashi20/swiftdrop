import { Bell, CheckCheck } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getApiErrorMessage } from "../../lib/api";
import { notificationsApi } from "../dispatch/api";
import type { MyNotification, MyNotificationPage } from "../dispatch/types";

const PAGE_SIZE = 20;

function relativeTime(iso: string) {
  const minutes = Math.floor((Date.now() - new Date(iso).getTime()) / 60_000);
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}d ago`;
  return new Date(iso).toLocaleDateString();
}

export function MyNotificationsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [data, setData] = useState<MyNotificationPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setData(await notificationsApi.myFeed({ page, size: PAGE_SIZE, unreadOnly }));
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not load your notifications."));
    } finally {
      setLoading(false);
    }
  }, [page, unreadOnly]);

  useEffect(() => {
    void load();
  }, [load]);

  const markAllRead = async () => {
    try {
      await notificationsApi.markAllRead();
      setData((prev) =>
        prev ? { ...prev, content: prev.content.map((n) => ({ ...n, read: true })) } : prev,
      );
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not mark notifications as read."));
    }
  };

  const openNotification = async (item: MyNotification) => {
    if (!item.read) {
      try {
        await notificationsApi.markRead(item.id);
        setData((prev) =>
          prev
            ? { ...prev, content: prev.content.map((n) => (n.id === item.id ? { ...n, read: true } : n)) }
            : prev,
        );
      } catch {
        /* still navigate */
      }
    }
    navigate(`/parcels/${item.trackingNumber}`);
  };

  const toggleFilter = () => {
    setPage(0);
    setUnreadOnly((v) => !v);
  };

  const items = data?.content ?? [];
  const hasUnread = items.some((n) => !n.read);

  return (
    <div>
      <div className="mb-8">
        <p className="mb-2 text-xs font-bold uppercase tracking-[0.2em] text-coral">Updates</p>
        <h1 className="font-display text-4xl font-bold tracking-tight sm:text-5xl">Notifications</h1>
        <p className="mt-3 max-w-xl text-ink/55">
          Every status update for your parcels lands here. Click a notification to view the parcel.
        </p>
      </div>

      {error && <p className="mb-4 rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral">{error}</p>}

      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <button
          onClick={toggleFilter}
          className={`rounded-xl border px-4 py-2 text-sm font-semibold transition ${
            unreadOnly
              ? "border-coral bg-coral/10 text-coral"
              : "border-ink/15 text-ink/60 hover:bg-ink/5"
          }`}
        >
          {unreadOnly ? "Showing unread only" : "Show unread only"}
        </button>

        {hasUnread && (
          <button
            onClick={markAllRead}
            className="flex items-center gap-1.5 rounded-xl border border-ink/15 px-4 py-2 text-sm font-semibold text-ink/70 transition hover:bg-ink/5"
          >
            <CheckCheck size={15} />
            Mark all as read
          </button>
        )}
      </div>

      <section className="rounded-2xl border border-ink/10 bg-white">
        {loading && !data ? (
          <p className="px-5 py-8 text-sm text-ink/50">Loading notifications…</p>
        ) : items.length === 0 ? (
          error ? null : (
            <div className="flex flex-col items-center gap-3 px-5 py-14 text-center">
              <Bell size={32} className="text-ink/20" />
              <p className="text-sm text-ink/50">
                {unreadOnly ? "No unread notifications." : "Nothing yet. Updates will land here."}
              </p>
            </div>
          )
        ) : (
          <ul className="divide-y divide-ink/5">
            {items.map((item) => (
              <li key={item.id}>
                <button
                  onClick={() => void openNotification(item)}
                  className={`flex w-full items-start gap-4 px-5 py-4 text-left transition hover:bg-mist ${
                    item.read ? "" : "bg-coral/5"
                  }`}
                >
                  <span
                    className={`mt-2 h-2.5 w-2.5 shrink-0 rounded-full ${item.read ? "bg-transparent" : "bg-coral"}`}
                  />
                  <span className="min-w-0 flex-1">
                    <span className={`block text-sm ${item.read ? "text-ink/70" : "font-semibold text-ink"}`}>
                      {item.message}
                    </span>
                    <span className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-0.5 text-xs text-ink/45">
                      <span className="font-semibold">{item.trackingNumber}</span>
                      <span>
                        {item.oldStatus ?? "Created"} → {item.newStatus}
                      </span>
                      <span>{relativeTime(item.createdAt)}</span>
                    </span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}

        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-ink/10 px-5 py-3 text-sm text-ink/60">
            <span>
              Page {data.page + 1} of {data.totalPages} · {data.totalElements} notifications
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
                disabled={page + 1 >= data.totalPages || loading}
                className="rounded-lg border border-ink/15 px-3 py-1.5 font-semibold disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
