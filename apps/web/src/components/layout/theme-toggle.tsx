"use client";

import { useTheme } from "next-themes";
import { Moon, Sun } from "lucide-react";
import { Button } from "@/components/ui/button";

/**
 * Icon visibility is driven purely by the `.dark` class, not React state, so
 * there is nothing to hydrate and no flash on first paint.
 */
export function ThemeToggle() {
  const { resolvedTheme, setTheme } = useTheme();

  return (
    <Button
      variant="ghost"
      size="icon"
      aria-label="Přepnout mezi tmavým a světlým režimem"
      title="Přepnout motiv"
      onClick={() => setTheme(resolvedTheme === "light" ? "dark" : "light")}
      className="relative text-muted-foreground hover:text-foreground"
    >
      <Sun
        aria-hidden="true"
        className="size-4 rotate-0 scale-100 transition-transform duration-300 dark:absolute dark:-rotate-90 dark:scale-0"
      />
      <Moon
        aria-hidden="true"
        className="absolute rotate-90 scale-0 transition-transform duration-300 dark:static dark:size-4 dark:rotate-0 dark:scale-100"
      />
    </Button>
  );
}
