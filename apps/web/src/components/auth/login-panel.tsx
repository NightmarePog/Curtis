"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { ExternalLink, LogIn } from "lucide-react";
import { useAuth } from "@/components/providers/auth-provider";
import { BrandMark } from "@/components/layout/brand";
import { LoadingScreen } from "@/components/common/feedback";
import { Button } from "@/components/ui/button";
import { LOGIN_URL } from "@/lib/api";

export function LoginPanel() {
  const auth = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (auth.status === "user") {
      router.replace("/dashboard");
    }
  }, [auth.status, router]);

  if (auth.status !== "guest") {
    return <LoadingScreen label="Ověřuji přihlášení…" />;
  }

  return (
    <div className="mx-auto w-full max-w-md">
      <div className="surface p-6 sm:p-7">
        <div className="flex items-center gap-3">
          <span className="flex size-10 items-center justify-center rounded-xl bg-primary p-2 text-primary-foreground">
            <BrandMark />
          </span>
          <div>
            <h1 className="text-xl font-semibold tracking-tight text-foreground">
              Curtis
            </h1>
            <p className="text-sm text-muted-foreground">
              Školní aplikace pro kvízy
            </p>
          </div>
        </div>

        <p className="mt-6 text-sm leading-relaxed text-muted-foreground">
          Pro vstup do aplikace se přihlaste školním účtem.
        </p>
        <p className="mt-2 text-xs leading-relaxed text-muted-foreground">
          Učitelský přístup se určuje podle oprávnění školního účtu.
        </p>

        <div className="mt-6 space-y-3">
          <Button
            size="lg"
            className="w-full"
            asChild
          >
            <a href={LOGIN_URL}>
              <LogIn aria-hidden="true" data-icon="inline-start" />
              Přihlásit se školním účtem
            </a>
          </Button>

          <a
            href="https://www.sosehl.cz"
            target="_blank"
            rel="noreferrer"
            className="inline-flex w-full items-center justify-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            Web školy
            <ExternalLink aria-hidden="true" className="size-3.5" />
          </a>
        </div>
      </div>
    </div>
  );
}
