import type { Metadata, Viewport } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";
import { AuthProvider } from "@/components/providers/auth-provider";
import { ThemeProvider } from "@/components/providers/theme-provider";
import { Backdrop } from "@/components/layout/backdrop";
import { MobileNav, SiteHeader } from "@/components/layout/site-header";
import "./globals.css";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin", "latin-ext"],
  display: "swap",
});

const jetBrainsMono = JetBrains_Mono({
  variable: "--font-mono-code",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    default: "Curtis — kvízy SOŠE Hluboká",
    template: "%s · Curtis",
  },
  description:
    "Kvízový nástroj Střední odborné školy elektrotechnické v Hluboké nad Vltavou. Vyučující tvoří kvízy, žáci se připojí kódem a výsledky se vyhodnotí okamžitě.",
  applicationName: "Curtis",
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: dark)", color: "#07080b" },
    { media: "(prefers-color-scheme: light)", color: "#ffffff" },
  ],
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    // Font variables live on <html> because `font-sans` is applied there.
    <html
      lang="cs"
      suppressHydrationWarning
      className={`${inter.variable} ${jetBrainsMono.variable}`}
    >
      <body>
        <ThemeProvider>
          <AuthProvider>
            <a
              href="#obsah"
              className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-primary focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:text-primary-foreground"
            >
              Přeskočit na obsah
            </a>

            <Backdrop />
            <SiteHeader />

            <main
              id="obsah"
              className="mx-auto w-full max-w-6xl px-4 pb-24 pt-8 sm:px-6 sm:pb-16 md:pb-16 lg:pt-12"
            >
              {children}
            </main>

            <MobileNav />
          </AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
