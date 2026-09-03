import {
  forwardRef,
  type HTMLAttributes,
  type ReactNode,
} from "react";

import { cn } from "@/lib/utils";

export interface PanelProps extends HTMLAttributes<HTMLDivElement> {
  tone?: "default" | "subtle";
}

export const Panel = forwardRef<HTMLDivElement, PanelProps>(
  ({ className, tone = "default", ...props }, ref) => (
    <div
      ref={ref}
      data-slot="panel"
      className={cn(
        "rounded-lg border border-border shadow-panel",
        tone === "default" ? "bg-panel text-panel-foreground" : "bg-surface",
        className,
      )}
      {...props}
    />
  ),
);

Panel.displayName = "Panel";

export const PanelHeader = forwardRef<
  HTMLDivElement,
  HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    data-slot="panel-header"
    className={cn("flex flex-col gap-1.5 px-5 pt-5 sm:px-6 sm:pt-6", className)}
    {...props}
  />
));

PanelHeader.displayName = "PanelHeader";

export interface PanelTitleProps extends HTMLAttributes<HTMLHeadingElement> {
  as?: "h2" | "h3" | "h4";
}

export function PanelTitle({
  as: Heading = "h2",
  className,
  ...props
}: PanelTitleProps) {
  return (
    <Heading
      data-slot="panel-title"
      className={cn(
        "text-lg leading-tight font-semibold tracking-[-0.01em] text-balance text-foreground",
        className,
      )}
      {...props}
    />
  );
}

export const PanelDescription = forwardRef<
  HTMLParagraphElement,
  HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <p
    ref={ref}
    data-slot="panel-description"
    className={cn("max-w-[70ch] text-sm leading-6 text-muted-foreground", className)}
    {...props}
  />
));

PanelDescription.displayName = "PanelDescription";

export const PanelContent = forwardRef<
  HTMLDivElement,
  HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    data-slot="panel-content"
    className={cn("px-5 py-5 sm:px-6 sm:py-6", className)}
    {...props}
  />
));

PanelContent.displayName = "PanelContent";

export interface PanelFooterProps extends HTMLAttributes<HTMLDivElement> {
  children?: ReactNode;
}

export const PanelFooter = forwardRef<HTMLDivElement, PanelFooterProps>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      data-slot="panel-footer"
      className={cn(
        "flex flex-wrap items-center justify-end gap-3 border-t border-border px-5 py-4 sm:px-6",
        className,
      )}
      {...props}
    />
  ),
);

PanelFooter.displayName = "PanelFooter";

// shadcn naming aliases keep registry-generated components interoperable while
// preserving the established Panel API used throughout the product.
export const Card = Panel;
export const CardHeader = PanelHeader;
export const CardTitle = PanelTitle;
export const CardDescription = PanelDescription;
export const CardContent = PanelContent;
export const CardFooter = PanelFooter;
