"use client";

import { useEffect, useRef, useState } from "react";

export function useCountdown(
  seconds: number | null,
  onExpire: () => void,
  resetKey?: string | number
) {
  const [state, setState] = useState({
    seconds,
    resetKey,
    value: seconds,
  });
  const expireRef = useRef(onExpire);

  useEffect(() => {
    expireRef.current = onExpire;
  }, [onExpire]);

  const isCurrent =
    state.seconds === seconds && Object.is(state.resetKey, resetKey);
  const remaining = isCurrent ? state.value : seconds;

  useEffect(() => {
    if (seconds == null) {
      return;
    }

    let expired = false;
    const id = window.setInterval(() => {
      setState((previous) => {
        const current =
          previous.seconds === seconds &&
          Object.is(previous.resetKey, resetKey)
            ? previous.value
            : seconds;

        if (current == null) {
          return { seconds, resetKey, value: current };
        }
        if (current <= 1) {
          window.clearInterval(id);
          if (!expired) {
            expired = true;
            expireRef.current();
          }
          return { seconds, resetKey, value: 0 };
        }
        return { seconds, resetKey, value: current - 1 };
      });
    }, 1000);
    return () => window.clearInterval(id);
  }, [resetKey, seconds]);

  return remaining;
}
