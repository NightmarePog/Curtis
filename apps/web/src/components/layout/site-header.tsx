"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { LayoutGrid, LogOut, Plus, UserRound, Trophy } from "lucide-react";
import {
  displayName,
  initials,
  isTeacher,
  useAuth,
  useLogout,
} from "@/components/providers/auth-provider";
import { BrandLogo } from "@/components/layout/brand";
import { ThemeToggle } from "@/components/layout/theme-toggle";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { LOGIN_URL } from "@/lib/api";
import { cn } from "@/lib/utils";

export interface NavItem {
  href: string;
  label: string;
  icon: typeof LayoutGrid;
}

export function useNavItems(): NavItem[] {
  const auth = useAuth();
  const me = auth.status === "user" ? auth.me : undefined;

  return [
    { href: "/dashboard", label: "Přehled", icon: LayoutGrid },
    ...(isTeacher(me)
      ? [{ href: "/quiz/new", label: "Nový kvíz", icon: Plus }]
      : [{ href: "/dashboard/my-results", label: "Moje výsledky", icon: Trophy }]),
  ];
}

export function isNavActive(pathname: string, href: string): boolean {
  return href === "/dashboard"
    ? pathname === href
    : pathname.startsWith(href);
}

export function SiteHeader() {
  const auth = useAuth();
  const logout = useLogout();
  const router = useRouter();
  const pathname = usePathname();
  const navItems = useNavItems();

  const me = auth.status === "user" ? auth.me : undefined;
  const teacher = isTeacher(me);

  return (
    <header className="sticky top-0 z-40 border-b border-border/70 glass">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4 sm:px-6">
        <BrandLogo href={me ? "/dashboard" : "/"} />

        {me && (
          <nav
            aria-label="Hlavní navigace"
            className="ml-4 hidden items-center gap-1 md:flex"
          >
            {navItems.map((item) => {
              const Icon = item.icon;
              const active = isNavActive(pathname, item.href);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "relative flex h-9 items-center gap-1.5 rounded-lg px-3 text-sm font-medium transition-colors duration-200",
                    active
                      ? "text-foreground"
                      : "text-muted-foreground hover:bg-muted hover:text-foreground"
                  )}
                >
                  <Icon aria-hidden="true" className="size-4" />
                  {item.label}
                  {active && (
                    <span className="absolute inset-x-2 -bottom-[13px] h-0.5 rounded-full bg-brand" />
                  )}
                </Link>
              );
            })}
          </nav>
        )}

        <div className="ml-auto flex items-center gap-1.5">
          <ThemeToggle />

          {auth.status === "loading" ? (
            <div
              aria-hidden="true"
              className="size-9 animate-pulse rounded-full bg-muted"
            />
          ) : me ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  type="button"
                  aria-label="Účet"
                  className="flex size-9 items-center justify-center rounded-full bg-muted text-xs font-semibold text-foreground ring-1 ring-border transition-colors outline-none hover:bg-accent focus-visible:ring-2 focus-visible:ring-ring"
                >
                  {initials(me)}
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuLabel className="flex items-center justify-between gap-3">
                  <span className="truncate">{displayName(me)}</span>
                  <Badge variant={teacher ? "blue" : "green"}>
                    {teacher ? "Vyučující" : "Žák"}
                  </Badge>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                {/* Navigation lives in the top bar (desktop) and bottom bar (mobile). */}
                <DropdownMenuItem
                  variant="destructive"
                  onSelect={async () => {
                    await logout();
                    router.replace("/");
                  }}
                >
                  <LogOut aria-hidden="true" />
                  Odhlásit se
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <Button size="sm" asChild>
              <a href={LOGIN_URL}>
                <UserRound aria-hidden="true" data-icon="inline-start" />
                Přihlásit se
              </a>
            </Button>
          )}
        </div>
      </div>
    </header>
  );
}

/** Thumb-reachable bottom bar on phones. Mirrors the desktop nav. */
export function MobileNav() {
  const auth = useAuth();
  const pathname = usePathname();
  const navItems = useNavItems();

  if (auth.status !== "user") return null;

  return (
    <nav
      aria-label="Mobilní navigace"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-border/70 glass pb-[env(safe-area-inset-bottom)] md:hidden"
    >
      <ul className="mx-auto flex max-w-md items-stretch">
        {navItems.map((item) => {
          const Icon = item.icon;
          const active = isNavActive(pathname, item.href);
          return (
            <li key={item.href} className="flex-1">
              <Link
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "flex h-14 flex-col items-center justify-center gap-1 text-[0.7rem] font-medium transition-colors",
                  active
                    ? "text-brand"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                <Icon aria-hidden="true" className="size-5" />
                {item.label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
