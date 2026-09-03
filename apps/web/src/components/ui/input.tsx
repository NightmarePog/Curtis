import { forwardRef, type InputHTMLAttributes } from "react";

import { cn } from "@/lib/utils";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  invalid?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, invalid = false, ...props }, ref) => (
    <input
      ref={ref}
      data-slot="input"
      {...props}
      aria-invalid={invalid || props["aria-invalid"] || undefined}
      className={cn(
        "min-h-11 w-full rounded-md border border-border-strong bg-field px-3.5 py-2 text-base text-foreground",
        "transition-[border-color,box-shadow,background-color] duration-150 motion-reduce:transition-none",
        "focus-visible:border-brand focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/35",
        "disabled:bg-surface-subtle disabled:text-muted-foreground disabled:opacity-70",
        invalid && "border-danger focus-visible:border-danger focus-visible:ring-danger/30",
        className,
      )}
    />
  ),
);

Input.displayName = "Input";
