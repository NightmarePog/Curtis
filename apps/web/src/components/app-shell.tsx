"use client";

import {
  ClipboardCheck,
  History,
  LayoutDashboard,
  LogOut,
  Plus,
  ShieldCheck,
  UserRound,
  UsersRound,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useRef, type ReactNode } from "react";

import { BrandLogo } from "@/components/brand";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui";
import {
  isAdministrator,
  isStudent,
  isTeacher,
  useAuth,
} from "@/features/auth/auth-provider";
import { cn } from "@/lib/cn";

interface NavigationItem {
  href: string;
  label: string;
  icon: typeof LayoutDashboard;
}

const studentNavigation: NavigationItem[] = [
  { href: "/dashboard", label: "Dnes", icon: LayoutDashboard },
  { href: "/leaderboard", label: "Třída", icon: UsersRound },
  { href: "/results", label: "Výsledky", icon: ClipboardCheck },
];

const teacherNavigation: NavigationItem[] = [
  { href: "/dashboard", label: "Kvízy", icon: LayoutDashboard },
  { href: "/students", label: "Žáci", icon: UsersRound },
  { href: "/history", label: "Historie", icon: History },
  { href: "/quiz/new", label: "Nový kvíz", icon: Plus },
];

const administratorNavigation: NavigationItem[] = [
  { href: "/admin", label: "Administrace", icon: ShieldCheck },
];

function Navigation({ items, mobile = false }: { items: NavigationItem[]; mobile?: boolean }) {
  const pathname = usePathname();
  return (
    <nav aria-label={mobile ? "Mobilní navigace" : "Hlavní navigace"}>
      <ul
        className={mobile ? "grid" : "flex items-center gap-1"}
        style={
          mobile
            ? { gridTemplateColumns: `repeat(${items.length}, minmax(0, 1fr))` }
            : undefined
        }
      >
        {items.map((item) => {
          const active =
            pathname === item.href ||
            (item.href !== "/dashboard" && pathname.startsWith(`${item.href}/`));
          const Icon = item.icon;
          return (
            <li key={item.href}>
              <Link
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "group",
                  mobile
                    ? "flex min-h-14 flex-col items-center justify-center gap-1 px-1 text-[0.7rem] font-semibold"
                    : "inline-flex min-h-11 items-center gap-2 rounded-md px-3 text-sm font-semibold",
                  active
                    ? "bg-primary-soft text-primary-strong"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground",
                )}
              >
                <Icon
                  aria-hidden="true"
                  className="size-4 transition-transform duration-200 group-hover:-translate-y-0.5 motion-reduce:transition-none"
                />
                {item.label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}

function AccountMenu() {
  const { state, logout } = useAuth();
  const detailsRef = useRef<HTMLDetailsElement>(null);
  if (state.status !== "user") return null;

  return (
    <details ref={detailsRef} className="relative">
      <summary className="flex min-h-11 list-none items-center gap-2 rounded-md px-2 text-sm font-semibold text-foreground hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring [&::-webkit-details-marker]:hidden">
        <span className="grid size-8 place-items-center rounded-md border border-border bg-muted text-primary-strong">
          <UserRound aria-hidden="true" className="size-4" />
        </span>
        <span className="hidden max-w-40 truncate lg:block">{state.user.name}</span>
        <span className="sr-only">Otevřít nabídku účtu</span>
      </summary>
      <div className="absolute right-0 z-50 mt-2 w-64 rounded-lg border border-border bg-popover p-2 shadow-overlay">
        <div className="border-b border-border px-2 pb-3 pt-1">
          <p className="truncate text-sm font-semibold text-foreground">{state.user.name}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">
            {isAdministrator(state.user)
              ? "Administrátor"
              : isTeacher(state.user)
                ? "Vyučující"
                : isStudent(state.user)
                  ? "Žák"
                  : "Uživatel"}
          </p>
        </div>
        <Button
          variant="ghost"
          className="mt-1 w-full justify-start"
          onClick={() => {
            detailsRef.current?.removeAttribute("open");
            void logout();
          }}
        >
          <LogOut aria-hidden="true" className="size-4" />
          Odhlásit se
        </Button>
      </div>
    </details>
  );
}

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const { state } = useAuth();
  const user = state.status === "user" ? state.user : undefined;
  const administrator = isAdministrator(user);
  const teacher = isTeacher(user);
  const student = isStudent(user);
  const login = pathname === "/login";
  const focusedStudentSession =
    pathname.startsWith("/session/") && state.status === "user" && student;
  const showChrome = !login && !focusedStudentSession;
  const navigation = administrator
    ? administratorNavigation
    : teacher
      ? teacherNavigation
      : student
        ? studentNavigation
        : [];

  return (
    <>
      <a
        href="#main-content"
        className="fixed left-4 top-4 z-[100] -translate-y-24 rounded-md bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground transition-transform focus:translate-y-0"
      >
        Přeskočit na obsah
      </a>

      {showChrome && (
        <header className="sticky top-0 z-40 border-b border-border bg-background pt-[env(safe-area-inset-top)]">
          <div className="mx-auto flex h-16 max-w-[80rem] items-center gap-5 px-4 sm:px-6 lg:px-8">
            <BrandLogo
              href={administrator ? "/admin" : "/dashboard"}
              descriptor="SOŠE Hluboká"
            />
            {user && (
              <div className="hidden md:block">
                <Navigation items={navigation} />
              </div>
            )}
            <div className="ml-auto flex items-center gap-1.5">
              <ThemeToggle />
              <AccountMenu />
            </div>
          </div>
        </header>
      )}

      <main
        id="main-content"
        tabIndex={-1}
        className={cn(
          "mx-auto w-full focus:outline-none",
          focusedStudentSession
            ? "min-h-svh max-w-[54rem] px-4 py-5 sm:px-6 sm:py-8"
            : login
              ? "min-h-svh max-w-[80rem] px-4 py-4 sm:px-6 sm:py-8"
              : "max-w-[80rem] px-4 pb-24 pt-7 sm:px-6 lg:px-8 lg:pb-16 lg:pt-10",
        )}
      >
        {children}
      </main>

      {showChrome && user && (
        <div className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-card pb-[env(safe-area-inset-bottom)] md:hidden">
          <Navigation items={navigation} mobile />
        </div>
      )}
    </>
  );
}
