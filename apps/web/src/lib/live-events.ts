import { useEffect, useMemo, useRef } from "react";

import { API_BASE, DEMO_MODE } from "@/lib/constants";

export const DEMO_DATABASE_CHANGED_EVENT = "curtis:demo-database-changed";

const DEMO_DATABASE_STORAGE_PREFIX = "curtis.fresh-demo.database.";
const DEFAULT_FALLBACK_INTERVAL = 45_000;

export type LiveEventName =
  | "sessions-changed"
  | "results-changed"
  | "roster-changed"
  | "quizzes-changed";

export type LiveEventReason =
  | LiveEventName
  | "connected"
  | "demo-data-changed"
  | "fallback";

interface LiveEventsOptions {
  fallbackIntervalMs?: number;
}

/**
 * Subscribes to non-sensitive server invalidation events. The caller remains
 * responsible for refetching its authorized resource after each notification.
 */
export function useLiveEvents(
  eventNames: readonly LiveEventName[],
  onEvent: (reason: LiveEventReason) => void,
  options: LiveEventsOptions = {},
) {
  const callbackRef = useRef(onEvent);
  const eventKey = useMemo(
    () => [...new Set(eventNames)].sort().join(","),
    [eventNames],
  );
  const fallbackIntervalMs = Math.max(
    30_000,
    options.fallbackIntervalMs ?? DEFAULT_FALLBACK_INTERVAL,
  );

  useEffect(() => {
    callbackRef.current = onEvent;
  }, [onEvent]);

  useEffect(() => {
    const notify = (reason: LiveEventReason) => callbackRef.current(reason);
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") notify("fallback");
    };
    document.addEventListener("visibilitychange", handleVisibilityChange);

    if (DEMO_MODE) {
      const handleDemoChange = () => notify("demo-data-changed");
      const handleStorage = (event: StorageEvent) => {
        if (event.key?.startsWith(DEMO_DATABASE_STORAGE_PREFIX)) {
          notify("demo-data-changed");
        }
      };
      window.addEventListener(DEMO_DATABASE_CHANGED_EVENT, handleDemoChange);
      window.addEventListener("storage", handleStorage);
      return () => {
        document.removeEventListener("visibilitychange", handleVisibilityChange);
        window.removeEventListener(DEMO_DATABASE_CHANGED_EVENT, handleDemoChange);
        window.removeEventListener("storage", handleStorage);
      };
    }

    const subscribedEvents = eventKey
      .split(",")
      .filter((eventName): eventName is LiveEventName => Boolean(eventName));
    let fallbackTimer: number | null = null;
    const stopFallback = () => {
      if (fallbackTimer !== null) {
        window.clearInterval(fallbackTimer);
        fallbackTimer = null;
      }
    };
    const startFallback = () => {
      if (fallbackTimer === null) {
        fallbackTimer = window.setInterval(
          () => notify("fallback"),
          fallbackIntervalMs,
        );
      }
    };

    if (typeof EventSource === "undefined") {
      startFallback();
      return () => {
        stopFallback();
        document.removeEventListener("visibilitychange", handleVisibilityChange);
      };
    }

    const source = new EventSource(`${API_BASE}/v1/events`, {
      withCredentials: true,
    });
    const handleConnected = () => notify("connected");
    source.addEventListener("connected", handleConnected);
    const listeners = subscribedEvents.map((eventName) => {
      const listener = () => notify(eventName);
      source.addEventListener(eventName, listener);
      return { eventName, listener };
    });
    source.onopen = stopFallback;
    source.onerror = startFallback;

    return () => {
      stopFallback();
      for (const { eventName, listener } of listeners) {
        source.removeEventListener(eventName, listener);
      }
      source.removeEventListener("connected", handleConnected);
      source.close();
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [eventKey, fallbackIntervalMs]);
}
