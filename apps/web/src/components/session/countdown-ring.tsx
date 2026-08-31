"use client";

import { cn } from "@/lib/utils";

const RADIUS = 20;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

/**
 * Time remaining shown as a depleting ring. Colour escalates as time runs out;
 * the number stays visible so the state is never conveyed by colour alone.
 */
export function CountdownRing({
  remaining,
  total,
  className,
}: {
  remaining: number;
  total: number;
  className?: string;
}) {
  const safeTotal = Math.max(total, 1);
  const ratio = Math.min(Math.max(remaining / safeTotal, 0), 1);
  const urgent = remaining <= 5;
  const warning = !urgent && remaining <= 10;

  return (
    <div
      role="timer"
      aria-live={urgent ? "assertive" : "off"}
      aria-label={`Zbývá ${remaining} vteřin`}
      className={cn("relative size-12 shrink-0", className)}
    >
      <svg viewBox="0 0 48 48" className="size-full -rotate-90">
        <circle
          cx="24"
          cy="24"
          r={RADIUS}
          fill="none"
          strokeWidth="3.5"
          className="stroke-muted"
        />
        <circle
          cx="24"
          cy="24"
          r={RADIUS}
          fill="none"
          strokeWidth="3.5"
          strokeLinecap="round"
          strokeDasharray={CIRCUMFERENCE}
          strokeDashoffset={CIRCUMFERENCE * (1 - ratio)}
          className={cn(
            "transition-[stroke-dashoffset,stroke] duration-1000 ease-linear",
            urgent
              ? "stroke-destructive"
              : warning
                ? "stroke-warning"
                : "stroke-brand"
          )}
        />
      </svg>
      <span
        data-numeric
        aria-hidden="true"
        className={cn(
          "absolute inset-0 flex items-center justify-center text-sm font-semibold transition-colors",
          urgent
            ? "text-destructive"
            : warning
              ? "text-warning"
              : "text-foreground"
        )}
      >
        {remaining}
      </span>
    </div>
  );
}
