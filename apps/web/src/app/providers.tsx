"use client";

import type { ReactNode } from "react";

import { ThemeProvider } from "@/components/theme-provider";
import { AuthProvider } from "@/features/auth/auth-provider";

export function Providers({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider>
      <AuthProvider>{children}</AuthProvider>
    </ThemeProvider>
  );
}
