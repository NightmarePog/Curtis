"use client";

import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

const label = (index: number) => String.fromCharCode(65 + index);

/**
 * Large, thumb-friendly answer target (>=56px tall). Selection is signalled by
 * border, fill, a letter→check swap and aria-pressed — never by colour alone.
 */
export function AnswerOption({
  index,
  text,
  selected,
  disabled,
  onToggle,
}: {
  index: number;
  text: string;
  selected: boolean;
  disabled?: boolean;
  onToggle: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onToggle}
      disabled={disabled}
      aria-pressed={selected}
      className={cn(
        "press group flex w-full items-center gap-3 rounded-xl border p-4 text-left",
        "min-h-14 disabled:pointer-events-none disabled:opacity-55",
        selected
          ? "border-primary bg-primary/12 shadow-sm ring-1 ring-inset ring-primary/40"
          : "border-border bg-card hover:border-ring/50 hover:bg-muted/50"
      )}
    >
      <span
        aria-hidden="true"
        className={cn(
          "flex size-8 shrink-0 items-center justify-center rounded-lg text-sm font-semibold transition-colors",
          selected
            ? "bg-primary text-primary-foreground"
            : "bg-muted text-muted-foreground group-hover:bg-accent group-hover:text-accent-foreground"
        )}
      >
        {selected ? <Check className="size-4" /> : label(index)}
      </span>
      <span
        className={cn(
          "min-w-0 flex-1 text-pretty text-[0.95rem] leading-snug",
          selected ? "font-medium text-foreground" : "text-foreground/90"
        )}
      >
        {text}
      </span>
    </button>
  );
}
