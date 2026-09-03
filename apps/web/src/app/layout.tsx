import type { Metadata, Viewport } from "next";
import type { ReactNode } from "react";

import { AppShell } from "@/components/app-shell";
import { Providers } from "@/app/providers";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Curtis · školní kvízy",
    template: "%s · Curtis",
  },
  description: "Kvízy pro výuku na SOŠE Hluboká.",
  applicationName: "Curtis",
  icons: { icon: "/icon.svg" },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
  themeColor: [
    { media: "(prefers-color-scheme: dark)", color: "#101216" },
    { media: "(prefers-color-scheme: light)", color: "#f4f5f7" },
  ],
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="cs" suppressHydrationWarning>
      <body>
        <Providers>
          <AppShell>{children}</AppShell>
        </Providers>
      </body>
    </html>
  );
}
