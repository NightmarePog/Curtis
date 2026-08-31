"use client";

import type { ComponentType, ReactNode } from "react";
import { Inbox, Loader2, TriangleAlert, X } from "lucide-react";
import {
  Alert,
  AlertAction,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export function Spinner({ className }: { className?: string }) {
  return (
    <Loader2
      aria-hidden="true"
      className={cn("size-5 animate-spin text-brand", className)}
    />
  );
}

export function LoadingScreen({ label = "Načítám…" }: { label?: string }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className="flex min-h-[45vh] flex-col items-center justify-center gap-4 animate-fade-in"
    >
      <span className="relative flex size-12 items-center justify-center">
        <span className="absolute inset-0 rounded-full bg-brand/25 animate-pulse-ring" />
        <span className="relative flex size-12 items-center justify-center rounded-full border border-border bg-card">
          <Spinner className="size-5" />
        </span>
      </span>
      <p className="text-sm text-muted-foreground">{label}</p>
    </div>
  );
}

/** Skeleton grid used while quiz cards load — keeps layout stable (no CLS). */
export function CardSkeletonGrid({ count = 4 }: { count?: number }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {Array.from({ length: count }).map((_, i) => (
        <div
          key={i}
          aria-hidden="true"
          className="surface h-44 animate-pulse p-5"
        >
          <div className="h-4 w-2/3 rounded bg-muted" />
          <div className="mt-3 h-3 w-full rounded bg-muted/70" />
          <div className="mt-2 h-3 w-4/5 rounded bg-muted/70" />
          <div className="mt-6 h-8 w-32 rounded-lg bg-muted/60" />
        </div>
      ))}
    </div>
  );
}

export function EmptyState({
  title,
  description,
  icon: Icon = Inbox,
  action,
}: {
  title: string;
  description?: string;
  icon?: ComponentType<{ className?: string; "aria-hidden"?: boolean }>;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-5 rounded-2xl border border-dashed border-border bg-card/40 px-6 py-14 text-center animate-rise">
      <span className="flex size-14 items-center justify-center rounded-2xl bg-brand-soft text-brand ring-1 ring-brand/15">
        <Icon aria-hidden className="size-6" />
      </span>
      <div className="space-y-1.5">
        <p className="text-base font-semibold text-foreground">{title}</p>
        {description && (
          <p className="mx-auto max-w-sm text-pretty text-sm text-muted-foreground">
            {description}
          </p>
        )}
      </div>
      {action}
    </div>
  );
}

export function ErrorBanner({
  message,
  onDismiss,
}: {
  message: string;
  onDismiss?: () => void;
}) {
  return (
    <Alert variant="destructive" className="animate-rise">
      <TriangleAlert />
      <AlertTitle>Něco se nepovedlo</AlertTitle>
      <AlertDescription>{message}</AlertDescription>
      {onDismiss && (
        <AlertAction>
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={onDismiss}
            aria-label="Zavřít upozornění"
            className="text-destructive hover:bg-destructive/10"
          >
            <X />
          </Button>
        </AlertAction>
      )}
    </Alert>
  );
}

/** Compact labelled metric used across dashboards and result screens. */
export function Stat({
  label,
  value,
  hint,
  icon: Icon,
  className,
}: {
  label: string;
  value: ReactNode;
  hint?: string;
  icon?: ComponentType<{ className?: string; "aria-hidden"?: boolean }>;
  className?: string;
}) {
  return (
    <div className={cn("surface surface-raised overflow-hidden p-4", className)}>
      <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground">
        {Icon && <Icon aria-hidden className="size-3.5" />}
        {label}
      </div>
      <p
        data-numeric
        className="mt-2 text-2xl font-semibold tracking-tight text-foreground"
      >
        {value}
      </p>
      {hint && <p className="mt-1 text-xs text-muted-foreground">{hint}</p>}
    </div>
  );
}
