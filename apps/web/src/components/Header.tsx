"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { isTeacher, useAuth, useLogout } from "@/components/auth";
import { Badge, Button } from "@/components/ui";
import { LOGIN_URL } from "@/lib/api";

export function Header() {
  const auth = useAuth();
  const logout = useLogout();
  const router = useRouter();

  const me = auth.status === "user" ? auth.me : undefined;
  const teacher = isTeacher(me);

  return (
    <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/80 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-5xl items-center justify-between px-4">
        <div className="flex items-center gap-6">
          <Link href="/dashboard" className="text-lg font-bold text-indigo-700">
            Curtis
          </Link>
          {me && (
            <nav className="hidden items-center gap-4 text-sm sm:flex">
              <Link
                href="/dashboard"
                className="font-medium text-slate-600 hover:text-slate-900"
              >
                Kvízy
              </Link>
              {teacher && (
                <Link
                  href="/quiz/new"
                  className="font-medium text-slate-600 hover:text-slate-900"
                >
                  Nový kvíz
                </Link>
              )}
              <Link
                href="/session/join"
                className="font-medium text-slate-600 hover:text-slate-900"
              >
                Připojit se
              </Link>
            </nav>
          )}
        </div>

        <div className="flex items-center gap-3">
          {auth.status === "loading" ? null : me ? (
            <>
              <Badge variant={teacher ? "blue" : "green"}>
                {teacher ? "Vyučující" : "Žák"}
              </Badge>
              <span className="hidden text-sm text-slate-600 md:inline">
                {me.sub.length > 24 ? `${me.sub.slice(0, 24)}…` : me.sub}
              </span>
              <Button
                variant="ghost"
                size="sm"
                onClick={async () => {
                  await logout();
                  router.replace("/");
                }}
              >
                Odhlásit se
              </Button>
            </>
          ) : (
            <Button size="sm" onClick={() => router.push(LOGIN_URL)}>
              Přihlásit se
            </Button>
          )}
        </div>
      </div>
    </header>
  );
}
