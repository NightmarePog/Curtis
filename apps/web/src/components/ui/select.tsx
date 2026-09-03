"use client";

import * as SelectPrimitive from "@radix-ui/react-select";
import { Check, ChevronDown, ChevronUp } from "lucide-react";
import {
  forwardRef,
  type ComponentPropsWithoutRef,
  type ComponentRef,
  type ReactNode,
} from "react";

import { cn } from "@/lib/utils";

export const Select = SelectPrimitive.Root;
export const SelectGroup = SelectPrimitive.Group;
export const SelectValue = SelectPrimitive.Value;

export interface SelectTriggerProps
  extends ComponentPropsWithoutRef<typeof SelectPrimitive.Trigger> {
  invalid?: boolean;
}

export const SelectTrigger = forwardRef<
  ComponentRef<typeof SelectPrimitive.Trigger>,
  SelectTriggerProps
>(({ className, children, invalid = false, ...props }, ref) => (
  <SelectPrimitive.Trigger
    ref={ref}
    data-slot="select-trigger"
    aria-invalid={invalid || props["aria-invalid"] || undefined}
    className={cn(
      "flex min-h-11 w-full items-center justify-between gap-3 rounded-md border border-border-strong bg-field px-3.5 py-2 text-left text-base text-foreground",
      "transition-[border-color,box-shadow,background-color] duration-150 motion-reduce:transition-none",
      "focus-visible:border-brand focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/35",
      "disabled:cursor-not-allowed disabled:bg-surface-subtle disabled:text-muted-foreground disabled:opacity-70",
      "data-[placeholder]:text-muted-foreground [&>span]:min-w-0 [&>span]:truncate",
      invalid &&
        "border-danger focus-visible:border-danger focus-visible:ring-danger/30",
      className,
    )}
    {...props}
  >
    {children}
    <SelectPrimitive.Icon asChild>
      <ChevronDown
        aria-hidden="true"
        className="size-[1.125rem] shrink-0 text-muted-foreground"
        strokeWidth={1.8}
      />
    </SelectPrimitive.Icon>
  </SelectPrimitive.Trigger>
));

SelectTrigger.displayName = SelectPrimitive.Trigger.displayName;

export const SelectScrollUpButton = forwardRef<
  ComponentRef<typeof SelectPrimitive.ScrollUpButton>,
  ComponentPropsWithoutRef<typeof SelectPrimitive.ScrollUpButton>
>(({ className, ...props }, ref) => (
  <SelectPrimitive.ScrollUpButton
    ref={ref}
    data-slot="select-scroll-up-button"
    className={cn(
      "flex h-8 cursor-default items-center justify-center bg-popover text-muted-foreground",
      className,
    )}
    {...props}
  >
    <ChevronUp aria-hidden="true" className="size-4" />
  </SelectPrimitive.ScrollUpButton>
));

SelectScrollUpButton.displayName = SelectPrimitive.ScrollUpButton.displayName;

export const SelectScrollDownButton = forwardRef<
  ComponentRef<typeof SelectPrimitive.ScrollDownButton>,
  ComponentPropsWithoutRef<typeof SelectPrimitive.ScrollDownButton>
>(({ className, ...props }, ref) => (
  <SelectPrimitive.ScrollDownButton
    ref={ref}
    data-slot="select-scroll-down-button"
    className={cn(
      "flex h-8 cursor-default items-center justify-center bg-popover text-muted-foreground",
      className,
    )}
    {...props}
  >
    <ChevronDown aria-hidden="true" className="size-4" />
  </SelectPrimitive.ScrollDownButton>
));

SelectScrollDownButton.displayName =
  SelectPrimitive.ScrollDownButton.displayName;

export const SelectContent = forwardRef<
  ComponentRef<typeof SelectPrimitive.Content>,
  ComponentPropsWithoutRef<typeof SelectPrimitive.Content>
>(({ className, children, position = "popper", sideOffset = 5, ...props }, ref) => (
  <SelectPrimitive.Portal>
    <SelectPrimitive.Content
      ref={ref}
      data-slot="select-content"
      position={position}
      sideOffset={sideOffset}
      className={cn(
        "relative z-50 max-h-[min(24rem,var(--radix-select-content-available-height))] min-w-[10rem] overflow-hidden rounded-md border border-border bg-popover text-popover-foreground shadow-overlay",
        "data-[state=open]:animate-[select-in_140ms_ease-out] motion-reduce:animate-none",
        position === "popper" &&
          "w-[var(--radix-select-trigger-width)] min-w-[var(--radix-select-trigger-width)]",
        className,
      )}
      {...props}
    >
      <SelectScrollUpButton />
      <SelectPrimitive.Viewport className="p-1.5">
        {children}
      </SelectPrimitive.Viewport>
      <SelectScrollDownButton />
    </SelectPrimitive.Content>
  </SelectPrimitive.Portal>
));

SelectContent.displayName = SelectPrimitive.Content.displayName;

export const SelectLabel = forwardRef<
  ComponentRef<typeof SelectPrimitive.Label>,
  ComponentPropsWithoutRef<typeof SelectPrimitive.Label>
>(({ className, ...props }, ref) => (
  <SelectPrimitive.Label
    ref={ref}
    data-slot="select-label"
    className={cn(
      "px-3 py-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase",
      className,
    )}
    {...props}
  />
));

SelectLabel.displayName = SelectPrimitive.Label.displayName;

export const SelectItem = forwardRef<
  ComponentRef<typeof SelectPrimitive.Item>,
  ComponentPropsWithoutRef<typeof SelectPrimitive.Item>
>(({ className, children, ...props }, ref) => (
  <SelectPrimitive.Item
    ref={ref}
    data-slot="select-item"
    className={cn(
      "relative flex min-h-11 w-full cursor-default items-center rounded-sm py-2 pr-3 pl-9 text-sm text-foreground outline-none select-none",
      "focus:bg-primary-soft focus:text-primary-strong data-[disabled]:pointer-events-none data-[disabled]:opacity-45",
      className,
    )}
    {...props}
  >
    <span className="absolute left-3 grid size-4 place-items-center">
      <SelectPrimitive.ItemIndicator>
        <Check aria-hidden="true" className="size-4" strokeWidth={2.5} />
      </SelectPrimitive.ItemIndicator>
    </span>
    <SelectPrimitive.ItemText>{children}</SelectPrimitive.ItemText>
  </SelectPrimitive.Item>
));

SelectItem.displayName = SelectPrimitive.Item.displayName;

export const SelectSeparator = forwardRef<
  ComponentRef<typeof SelectPrimitive.Separator>,
  ComponentPropsWithoutRef<typeof SelectPrimitive.Separator>
>(({ className, ...props }, ref) => (
  <SelectPrimitive.Separator
    ref={ref}
    data-slot="select-separator"
    className={cn("-mx-1 my-1 h-px bg-border", className)}
    {...props}
  />
));

SelectSeparator.displayName = SelectPrimitive.Separator.displayName;

export interface SelectFieldOption {
  disabled?: boolean;
  label: ReactNode;
  value: string;
}

export interface SelectFieldProps
  extends Omit<SelectTriggerProps, "children" | "defaultValue" | "value"> {
  defaultValue?: string;
  name?: string;
  onValueChange?: (value: string) => void;
  options: SelectFieldOption[];
  placeholder?: string;
  required?: boolean;
  value?: string;
}

/** Field-friendly composition of the standard shadcn Select primitives. */
export const SelectField = forwardRef<
  ComponentRef<typeof SelectPrimitive.Trigger>,
  SelectFieldProps
>(
  (
    {
      defaultValue,
      disabled,
      name,
      onValueChange,
      options,
      placeholder = "Vyberte možnost",
      required,
      value,
      ...triggerProps
    },
    ref,
  ) => (
    <Select
      defaultValue={defaultValue}
      disabled={disabled}
      name={name}
      onValueChange={onValueChange}
      required={required}
      value={value || undefined}
    >
      <SelectTrigger ref={ref} {...triggerProps}>
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {options.map((option) => (
          <SelectItem
            key={option.value}
            value={option.value}
            disabled={option.disabled}
          >
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  ),
);

SelectField.displayName = "SelectField";
