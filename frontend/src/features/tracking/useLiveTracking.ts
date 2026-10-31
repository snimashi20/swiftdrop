import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { getTracking, isNotFound } from './api';
import type { TrackingResult, TrackingUpdateMessage } from './types';

export type TrackingError = 'not-found' | 'failed' | null;

function applyUpdate(current: TrackingResult, msg: TrackingUpdateMessage): TrackingResult {
  const alreadyThere = current.timeline.some(
    (e) => e.status === msg.status && e.changedAt === msg.changedAt,
  );
  if (alreadyThere) return current;
  return {
    ...current,
    status: msg.status,
    timeline: [
      ...current.timeline,
      { status: msg.status, changedAt: msg.changedAt, note: msg.note ?? null },
    ],
  };
}

/**
 * Loads the parcel timeline once through REST, then keeps it live through
 * STOMP over WebSocket (endpoint /ws, topic /topic/tracking/{trackingNumber}).
 * Reusable by the customer parcel detail page.
 */
export function useLiveTracking(trackingNumber: string) {
  const [data, setData] = useState<TrackingResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<TrackingError>(null);
  const [live, setLive] = useState(false);

  // 1. Initial load
  useEffect(() => {
    if (!trackingNumber) {
      setLoading(false); // nothing to load (invalid tracking number)
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    setData(null);

    getTracking(trackingNumber)
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(isNotFound(e) ? 'not-found' : 'failed');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [trackingNumber]);

  // 2. Live updates
  useEffect(() => {
    if (!trackingNumber) return;
    if (import.meta.env.DEV && new URLSearchParams(window.location.search).has('mock')) {
      return; // no WebSocket in mock mode
    }
    const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const topic = `/topic/tracking/${trackingNumber}`;

    const client = new Client({
      brokerURL: `${scheme}://${window.location.host}/ws`,
      reconnectDelay: 5_000,
      onConnect: () => {
        setLive(true);
        client.subscribe(topic, (frame) => {
          try {
            const msg = JSON.parse(frame.body) as TrackingUpdateMessage;
            setData((prev) => (prev ? applyUpdate(prev, msg) : prev));
          } catch {
            // ignore malformed message
          }
        });
        // Re-sync after (re)connecting so no update is missed.
        getTracking(trackingNumber)
          .then((result) => setData(result))
          .catch(() => undefined);
      },
      onWebSocketClose: () => setLive(false),
      onStompError: () => setLive(false),
    });

    client.activate();
    return () => {
      setLive(false);
      void client.deactivate();
    };
  }, [trackingNumber]);

  return { data, loading, error, live };
}
