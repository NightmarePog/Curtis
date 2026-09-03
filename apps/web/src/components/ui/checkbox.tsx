"use client";

import * as CheckboxPrimitive from "@radix-ui/react-checkbox";
import { Check, Minus } from "lucide-react";
import { forwardRef, type ComponentPropsWithoutRef, type ComponentRef } from "react";

import { cn } from "@/lib/utils";

export const Checkbox = forwardRef<
  ComponentRef<typeof CheckboxPrimitive.Root>,
  ComponentPropsWithoutRef<typeof CheckboxPrimitive.Root>
>(({ className, ...props }, ref) => (
  <CheckboxPrimitive.Root
    ref={ref}
    data-slot="checkbox"
    className={cn(
      "peer grid size-5 shrink-0 place-items-center rounded-sm border border-border-strong bg-field text-brand-foreground shadow-none outline-none",
      "transition-[border-color,background-color,box-shadow] duration-150 motion-reduce:transition-none",
      "hover:border-brand/70 focus-visible:border-brand focus-visible:ring-[3px] focus-visible:ring-ring/40",
      "data-[state=checked]:border-brand data-[state=checked]:bg-brand data-[state=indeterminate]:border-brand data-[state=indeterminate]:bg-brand",
      "disabled:cursor-not-allowed disabled:opacity-45",
      className,
    )}
    {...props}
  >
    <CheckboxPrimitive.Indicator className="grid place-items-center">
      {props.checked === "indeterminate" ? (
        <Minus aria-hidden="true" className="size-3.5" strokeWidth={3} />
      ) : (
        <Check aria-hidden="true" className="size-3.5" strokeWidth={3} />
      )}
    </CheckboxPrimitive.Indicator>
  </CheckboxPrimitive.Root>
));

Checkbox.displayName = CheckboxPrimitive.Root.displayName;
