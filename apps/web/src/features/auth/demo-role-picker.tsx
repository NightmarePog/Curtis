"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { BrandLogo } from "@/components/brand";
import { Button } from "@/components/ui";
import {
  useAuth,
  workspacePath,
} from "@/features/auth/auth-provider";
import { messageFromError } from "@/lib/http";

export function DemoRolePicker() {
  const { state, enterDemo } = useAuth();
  const router = useRouter();
  const [pending, setPending] = useState<
    "ADMINISTRATOR" | "STUDENT" | "TEACHER" | null
  >(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (state.status === "user") router.replace(workspacePath(state.user));
  }, [router, state]);

  async function selectRole(
    role: "ADMINISTRATOR" | "STUDENT" | "TEACHER",
  ) {
    setPending(role);
    setError(null);
    try {
      await enterDemo(role);
      router.replace(role === "ADMINISTRATOR" ? "/admin" : "/dashboard");
    } catch (caught) {
      setPending(null);
      setError(messageFromError(caught, "Demo účet se nepodařilo otevřít."));
    }
  }

  return (
    <section className="mx-auto flex min-h-[80svh] max-w-sm flex-col justify-center">
      <BrandLogo href="/" descriptor="Demo režim" />
      <h1 className="mt-8 text-2xl font-semibold tracking-tight text-foreground">
        Vyberte roli
      </h1>
      <div className="mt-5 grid gap-2">
        <Button
          size="lg"
          variant="secondary"
          isLoading={pending === "STUDENT"}
          disabled={pending !== null}
          onClick={() => void selectRole("STUDENT")}
        >
          Žák
        </Button>
        <Button
          size="lg"
          variant="secondary"
          isLoading={pending === "TEACHER"}
          disabled={pending !== null}
          onClick={() => void selectRole("TEACHER")}
        >
          Vyučující
        </Button>
        <Button
          size="lg"
          variant="secondary"
          isLoading={pending === "ADMINISTRATOR"}
          disabled={pending !== null}
          onClick={() => void selectRole("ADMINISTRATOR")}
        >
          Administrátor
        </Button>
      </div>
      {error ? (
        <p role="alert" className="mt-4 text-sm text-danger-text">
          {error}
        </p>
      ) : null}
    </section>
  );
}
