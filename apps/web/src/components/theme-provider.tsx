"use client";

import {
  ThemeProvider as NextThemesProvider,
  type ThemeProviderProps as NextThemesProviderProps,
} from "next-themes";

export type ThemeProviderProps = NextThemesProviderProps;

export function ThemeProvider({
  attribute = "class",
  children,
  defaultTheme = "dark",
  disableTransitionOnChange = true,
  enableSystem = true,
  storageKey = "curtis-theme",
  ...props
}: ThemeProviderProps) {
  return (
    <NextThemesProvider
      attribute={attribute}
      defaultTheme={defaultTheme}
      disableTransitionOnChange={disableTransitionOnChange}
      enableSystem={enableSystem}
      storageKey={storageKey}
      {...props}
    >
      {children}
    </NextThemesProvider>
  );
}
