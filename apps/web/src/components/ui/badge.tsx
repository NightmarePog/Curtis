import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { forwardRef, type HTMLAttributes } from "react";

import { cn } from "@/lib/utils";

export const badgeVariants = cva(
  "inline-flex min-h-6 max-w-full items-center gap-1.5 rounded-sm border px-2 py-0.5 text-xs leading-4 font-semibold whitespace-nowrap [&>svg]:pointer-events-none [&>svg]:size-3",
  {
    variants: {
      variant: {
        default: "border-brand bg-brand text-brand-foreground",
        neutral: "border-border bg-surface-subtle text-foreground",
        secondary: "border-border bg-surface-subtle text-foreground",
        brand: "border-brand/35 bg-brand-subtle text-brand-text",
        outline: "border-border-strong bg-transparent text-muted-foreground",
        danger: "border-danger/40 bg-danger-subtle text-danger-text",
        destructive: "border-danger/40 bg-danger-subtle text-danger-text",
      },
    },
    defaultVariants: {
      variant: "neutral",
    },
  },
);

export type BadgeVariant = NonNullable<
  VariantProps<typeof badgeVariants>["variant"]
>;

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  asChild?: boolean;
  variant?: BadgeVariant;
}

export const Badge = forwardRef<HTMLSpanElement, BadgeProps>(
  ({ asChild = false, className, variant = "neutral", ...props }, ref) => {
    const Comp = asChild ? Slot : "span";

    return (
      <Comp
        ref={ref}
        data-slot="badge"
        className={cn(badgeVariants({ variant }), className)}
        {...props}
      />
    );
  },
);

Badge.displayName = "Badge";
