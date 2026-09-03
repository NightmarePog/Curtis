import Link from "next/link";
import {
  forwardRef,
  type ComponentPropsWithoutRef,
  type HTMLAttributes,
} from "react";

import { cn } from "@/lib/cn";

export type BrandMarkProps = HTMLAttributes<HTMLSpanElement>;

export const BrandMark = forwardRef<HTMLSpanElement, BrandMarkProps>(
  ({ className, ...props }, ref) => (
    <span
      ref={ref}
      aria-hidden="true"
      className={cn(
        "grid size-9 shrink-0 place-items-center rounded-md bg-brand text-lg leading-none font-bold text-brand-foreground",
        className,
      )}
      {...props}
    >
      <svg
        aria-hidden="true"
        viewBox="0 0 24 24"
        className="size-[1.375rem]"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M18 4.75H9L4.75 9v6L9 19.25h9v-3.5h-7.55l-2.2-2.2v-3.1l2.2-2.2H18v-3.5Z"
          fill="currentColor"
        />
      </svg>
    </span>
  ),
);

BrandMark.displayName = "BrandMark";

type NextLinkProps = ComponentPropsWithoutRef<typeof Link>;

export interface BrandLogoProps
  extends Omit<NextLinkProps, "children" | "href"> {
  compact?: boolean;
  descriptor?: string;
  href?: NextLinkProps["href"];
  markOnly?: boolean;
}

export function BrandLogo({
  "aria-label": ariaLabel,
  className,
  compact = false,
  descriptor = "Školní pracovní prostředí",
  href = "/",
  markOnly = false,
  ...props
}: BrandLogoProps) {
  return (
    <Link
      href={href}
      aria-label={ariaLabel ?? (markOnly ? "Curtis – domovská stránka" : undefined)}
      className={cn(
        "inline-flex min-h-11 w-fit items-center gap-3 rounded-md text-foreground",
        "transition-colors duration-150 hover:text-brand-text motion-reduce:transition-none",
        "focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/45 focus-visible:ring-offset-2 focus-visible:ring-offset-background",
        className,
      )}
      {...props}
    >
      <BrandMark />
      {!markOnly ? (
        <span className="flex min-w-0 flex-col">
          <span className="text-[0.9375rem] leading-4 font-bold tracking-[0.12em]">
            CURTIS
          </span>
          {!compact ? (
            <span className="mt-0.5 text-xs leading-4 font-medium text-muted-foreground">
              {descriptor}
            </span>
          ) : null}
        </span>
      ) : null}
    </Link>
  );
}
