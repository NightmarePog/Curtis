"use client";

import { Moon, Sun } from "lucide-react";
import { useTheme } from "next-themes";
import { useSyncExternalStore } from "react";

import { Button, type ButtonProps } from "@/components/ui/button";
import { cn } from "@/lib/cn";

export type ThemeToggleProps = Omit<
  ButtonProps,
  "children" | "leadingIcon" | "size" | "variant"
>;

const subscribeToHydration = () => () => undefined;

export function ThemeToggle({ className, ...props }: ThemeToggleProps) {
  const { resolvedTheme, setTheme } = useTheme();
  const hydrated = useSyncExternalStore(
    subscribeToHydration,
    () => true,
    () => false,
  );
  const isDark = hydrated ? resolvedTheme !== "light" : true;
  const label = isDark
    ? "Přepnout na světlý motiv"
    : "Přepnout na tmavý motiv";

  return (
    <Button
      type="button"
      variant="quiet"
      size="icon"
      aria-label={label}
      title={label}
      className={cn("relative", className)}
      onClick={() => setTheme(isDark ? "light" : "dark")}
      {...props}
    >
      <Sun
        aria-hidden="true"
        className="absolute inset-0 m-auto size-5 -rotate-90 scale-75 opacity-0 transition-[opacity,transform] duration-200 ease-out motion-reduce:transition-none dark:rotate-0 dark:scale-100 dark:opacity-100"
        strokeWidth={1.8}
      />
      <Moon
        aria-hidden="true"
        className="absolute inset-0 m-auto size-5 rotate-0 scale-100 opacity-100 transition-[opacity,transform] duration-200 ease-out motion-reduce:transition-none dark:rotate-90 dark:scale-75 dark:opacity-0"
        strokeWidth={1.8}
      />
    </Button>
  );
}
