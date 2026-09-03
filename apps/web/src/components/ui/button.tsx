import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from "react";

import { cn } from "@/lib/utils";

export const buttonVariants = cva(
  [
    "inline-flex items-center justify-center gap-2 rounded-md border font-semibold leading-none select-none",
    "transition-colors duration-150 motion-reduce:transition-none",
    "focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/45 focus-visible:ring-offset-2 focus-visible:ring-offset-background",
    "disabled:pointer-events-none disabled:opacity-45 aria-disabled:pointer-events-none aria-disabled:opacity-45",
    "[&_svg]:pointer-events-none [&_svg]:shrink-0",
  ],
  {
    variants: {
      variant: {
        default:
          "border-brand bg-brand text-brand-foreground hover:border-brand-hover hover:bg-brand-hover active:border-brand-active active:bg-brand-active",
        primary:
          "border-brand bg-brand text-brand-foreground hover:border-brand-hover hover:bg-brand-hover active:border-brand-active active:bg-brand-active",
        secondary:
          "border-border-strong bg-surface-raised text-foreground hover:border-brand/60 hover:bg-surface-subtle active:bg-surface",
        outline:
          "border-border-strong bg-surface-raised text-foreground hover:border-brand/60 hover:bg-surface-subtle active:bg-surface",
        quiet:
          "border-transparent bg-transparent text-muted-foreground hover:bg-surface-subtle hover:text-foreground active:bg-surface",
        ghost:
          "border-transparent bg-transparent text-muted-foreground hover:bg-surface-subtle hover:text-foreground active:bg-surface",
        link: "border-transparent bg-transparent text-brand-text underline-offset-4 hover:underline",
        danger:
          "border-danger bg-danger text-danger-foreground hover:border-danger-hover hover:bg-danger-hover active:bg-danger",
        destructive:
          "border-danger bg-danger text-danger-foreground hover:border-danger-hover hover:bg-danger-hover active:bg-danger",
      },
      size: {
        sm: "min-h-11 px-3 text-sm",
        md: "min-h-11 px-4 text-[0.9375rem]",
        lg: "min-h-12 px-5 text-base",
        icon: "size-11 shrink-0 p-0",
      },
    },
    defaultVariants: {
      variant: "primary",
      size: "md",
    },
  },
);

export type ButtonVariant = NonNullable<
  VariantProps<typeof buttonVariants>["variant"]
>;
export type ButtonSize = NonNullable<
  VariantProps<typeof buttonVariants>["size"]
>;

export interface ButtonStyleOptions {
  variant?: ButtonVariant;
  size?: ButtonSize;
  className?: string;
}

export function buttonStyles({
  variant = "primary",
  size = "md",
  className,
}: ButtonStyleOptions = {}) {
  return cn(buttonVariants({ variant, size }), className);
}

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  asChild?: boolean;
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  leadingIcon?: ReactNode;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      children,
      className,
      disabled,
      asChild = false,
      isLoading = false,
      leadingIcon,
      size = "md",
      type = "button",
      variant = "primary",
      ...props
    },
    ref,
  ) => {
    const Comp = asChild ? Slot : "button";
    const pending = disabled || isLoading;

    return (
      <Comp
        ref={ref}
        data-slot="button"
        data-loading={isLoading || undefined}
        type={asChild ? undefined : type}
        className={buttonStyles({ variant, size, className })}
        disabled={asChild ? undefined : pending}
        aria-disabled={asChild && pending ? true : undefined}
        aria-busy={isLoading || undefined}
        {...props}
      >
        {asChild ? (
          children
        ) : (
          <>
            {leadingIcon ? (
              isLoading ? (
                <span
                  aria-hidden="true"
                  className="size-[1.125rem] shrink-0 animate-pulse rounded-sm bg-current opacity-30 motion-reduce:animate-none"
                />
              ) : (
                <span aria-hidden="true" className="flex shrink-0 items-center">
                  {leadingIcon}
                </span>
              )
            ) : null}
            {isLoading ? (
              <span className="relative whitespace-nowrap">
                <span className="opacity-0">{children}</span>
                <span
                  aria-hidden="true"
                  className="absolute inset-x-0 top-1/2 h-3 -translate-y-1/2 animate-pulse rounded-sm bg-current opacity-30 motion-reduce:animate-none"
                />
              </span>
            ) : (
              children
            )}
          </>
        )}
      </Comp>
    );
  },
);

Button.displayName = "Button";
