import { Bell, CheckCheck } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { notificationsApi } from "../features/dispatch/api";
import type { MyNotification } from "../features/dispatch/types";
import { getApiErrorMessage } from "../lib/api";

const PANEL_SIZE = 8;
const POLL_MS = 60_000;

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

export function NotificationBell() {
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<MyNotification[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const wrapper = useRef<HTMLDivElement>(null);

  // The badge keeps its last value when a poll fails, so a blip never shows an error in the header.
  useEffect(() => {
    let cancelled = false;
    const refresh = async () => {
      try {
        const { unreadCount: count } = await notificationsApi.myUnreadCount();
        if (!cancelled) setUnreadCount(count);
      } catch {
        /* keep the previous count */
      }
    };
    void refresh();
    const timer = window.setInterval(refresh, POLL_MS);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, []);

  useEffect(() => {
    if (!open) return;
    const onPointerDown = (event: MouseEvent) => {
      if (!wrapper.current?.contains(event.target as Node)) setOpen(false);
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  const loadFeed = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await notificationsApi.myFeed({ page: 0, size: PANEL_SIZE });
      setItems(data.content);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not load your notifications."));
    } finally {
      setLoading(false);
    }
  }, []);

  const toggle = () => {
    if (open) {
      setOpen(false);
      return;
    }
    setOpen(true);
    void loadFeed();
  };

  const markAllRead = async () => {
    try {
      await notificationsApi.markAllRead();
      setItems((prev) => prev.map((item) => (item.read ? item : { ...item, read: true })));
      setUnreadCount(0);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not mark your notifications as read."));
    }
  };

  const openNotification = async (item: MyNotification) => {
    setOpen(false);
    if (!item.read) {
      try {
        await notificationsApi.markRead(item.id);
        setItems((prev) => prev.map((row) => (row.id === item.id ? { ...row, read: true } : row)));
        // The count tracks status changes, not log rows, so one item is always one off the badge.
        setUnreadCount((count) => Math.max(0, count - 1));
      } catch {
        /* still take them to the parcel */
      }
    }
    navigate(`/parcels/${item.trackingNumber}`);
  };

  const badge = unreadCount > 9 ? "9+" : String(unreadCount);

  return (
    <div className="relative" ref={wrapper}>
      <button
        onClick={toggle}
        className="relative rounded-lg p-2 text-ink/60 transition hover:bg-ink/5 hover:text-ink"
        aria-label={unreadCount > 0 ? `Notifications, ${unreadCount} unread` : "Notifications"}
        aria-haspopup="true"
        aria-expanded={open}
      >
        <Bell size={19} />
        {unreadCount > 0 && (
          <span className="absolute -right-0.5 -top-0.5 grid h-4 min-w-4 place-items-center rounded-full bg-coral px-1 text-[10px] font-bold leading-none text-white">
            {badge}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full z-40 mt-2 w-[min(22rem,calc(100vw-2rem))] overflow-hidden rounded-2xl border border-ink/10 bg-white shadow-xl shadow-ink/10">
          <div className="flex items-center justify-between border-b border-ink/10 px-4 py-3">
            <p className="font-display text-sm font-bold">Notifications</p>
            {unreadCount > 0 && (
              <button
                onClick={markAllRead}
                className="flex items-center gap-1.5 text-xs font-semibold text-coral hover:underline"
              >
                <CheckCheck size={14} />
                Mark all read
              </button>
            )}
          </div>

          {error && <p className="bg-coral/10 px-4 py-2 text-xs text-coral">{error}</p>}

          {loading ? (
            <p className="px-4 py-6 text-sm text-ink/50">Loading…</p>
          ) : items.length === 0 ? (
            error ? null : (
              <p className="px-4 py-8 text-center text-sm text-ink/50">Nothing yet. Updates land here.</p>
            )
          ) : (
            <ul className="max-h-80 divide-y divide-ink/5 overflow-y-auto">
              {items.map((item) => (
                <li key={item.id}>
                  <button
                    onClick={() => void openNotification(item)}
                    className={`flex w-full items-start gap-3 px-4 py-3 text-left transition hover:bg-mist ${
                      item.read ? "" : "bg-coral/5"
                    }`}
                  >
                    <span
                      className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${item.read ? "bg-transparent" : "bg-coral"}`}
                    />
                    <span className="min-w-0">
                      <span className={`block text-sm ${item.read ? "text-ink/70" : "font-semibold text-ink"}`}>
                        {item.message}
                      </span>
                      <span className="mt-0.5 block text-xs text-ink/45">
                        {item.trackingNumber} · {relativeTime(item.createdAt)}
                      </span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
