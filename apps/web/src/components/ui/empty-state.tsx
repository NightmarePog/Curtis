import { Inbox, type LucideIcon } from "lucide-react";
import type { HTMLAttributes, ReactNode } from "react";

import { cn } from "@/lib/utils";

export interface EmptyStateProps extends HTMLAttributes<HTMLDivElement> {
  action?: ReactNode;
  description?: ReactNode;
  heading: ReactNode;
  icon?: LucideIcon;
  compact?: boolean;
}

export function EmptyState({
  action,
  className,
  compact = false,
  description,
  heading,
  icon: Icon = Inbox,
  ...props
}: EmptyStateProps) {
  return (
    <div
      data-slot="empty-state"
      className={cn(
        "flex flex-col items-center justify-center rounded-lg border border-dashed border-border-strong bg-surface text-center",
        compact ? "min-h-44 px-5 py-8" : "min-h-64 px-6 py-12",
        className,
      )}
      {...props}
    >
      <span
        aria-hidden="true"
        className="mb-4 grid size-10 place-items-center rounded-md border border-border bg-surface-raised text-muted-foreground"
      >
        <Icon className="size-5" strokeWidth={1.8} />
      </span>
      <h2 className="max-w-md text-lg leading-tight font-semibold text-balance text-foreground">
        {heading}
      </h2>
      {description ? (
        <p className="mt-2 max-w-lg text-sm leading-6 text-muted-foreground">
          {description}
        </p>
      ) : null}
      {action ? <div className="mt-5 flex flex-wrap justify-center gap-3">{action}</div> : null}
    </div>
  );
}
