import type { ComponentProps } from "react";

import { cn } from "@/lib/utils";

export function Skeleton({
  className,
  ...props
}: ComponentProps<"div">) {
  return (
    <div
      data-slot="skeleton"
      aria-hidden="true"
      className={cn(
        "animate-pulse rounded-sm bg-surface-subtle motion-reduce:animate-none",
        className,
      )}
      {...props}
    />
  );
}
