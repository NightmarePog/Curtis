import { forwardRef, type TextareaHTMLAttributes } from "react";

import { cn } from "@/lib/utils";

export interface TextareaProps
  extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  invalid?: boolean;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, invalid = false, ...props }, ref) => (
    <textarea
      ref={ref}
      data-slot="textarea"
      {...props}
      aria-invalid={invalid || props["aria-invalid"] || undefined}
      className={cn(
        "min-h-28 w-full resize-y rounded-md border border-border-strong bg-field px-3.5 py-2.5 text-base text-foreground",
        "transition-[border-color,box-shadow,background-color] duration-150 motion-reduce:transition-none",
        "focus-visible:border-brand focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/35",
        "disabled:resize-none disabled:bg-surface-subtle disabled:text-muted-foreground disabled:opacity-70",
        invalid && "border-danger focus-visible:border-danger focus-visible:ring-danger/30",
        className,
      )}
    />
  ),
);

Textarea.displayName = "Textarea";
