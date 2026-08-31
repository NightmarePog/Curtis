import Link from "next/link";
import { cn } from "@/lib/utils";

/**
 * Curtis mark — a circuit trace bending into a "C" with a live node,
 * a nod to the school's electrotechnical focus.
 */
export function BrandMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 32 32"
      fill="none"
      aria-hidden="true"
      className={cn("size-full", className)}
    >
      <path
        d="M23 9.5A9 9 0 1 0 23 22.5"
        stroke="currentColor"
        strokeWidth="2.75"
        strokeLinecap="round"
      />
      <path
        d="M23 16h6"
        stroke="currentColor"
        strokeWidth="2.75"
        strokeLinecap="round"
        opacity="0.55"
      />
      <circle cx="23" cy="16" r="3" fill="currentColor" />
    </svg>
  );
}

export function BrandLogo({
  href = "/dashboard",
  className,
  showWordmark = true,
}: {
  href?: string;
  className?: string;
  showWordmark?: boolean;
}) {
  return (
    <Link
      href={href}
      className={cn(
        "group flex items-center gap-2.5 rounded-lg outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
        className
      )}
    >
      <span className="relative flex size-9 items-center justify-center rounded-xl bg-primary p-1.5 text-primary-foreground shadow-sm transition-transform duration-300 ease-out-expo group-hover:scale-105">
        <BrandMark />
      </span>
      {showWordmark && (
        <span className="flex flex-col leading-none">
          <span className="text-[0.95rem] font-semibold tracking-tight text-foreground">
            Curtis
          </span>
          <span className="mt-0.5 text-[0.65rem] font-medium tracking-wide text-muted-foreground">
            SOŠE Hluboká
          </span>
        </span>
      )}
    </Link>
  );
}
