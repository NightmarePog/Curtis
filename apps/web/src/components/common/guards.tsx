"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { isTeacher, useAuth } from "@/components/providers/auth-provider";
import { LoadingScreen } from "@/components/common/feedback";

export function RequireAuth({ children }: { children: ReactNode }) {
  const auth = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (auth.status === "guest") {
      router.replace("/login");
    }
  }, [auth.status, router]);

  if (auth.status !== "user") {
    return <LoadingScreen label="Ověřuji přihlášení…" />;
  }

  return <>{children}</>;
}

export function TeacherOnly({ children }: { children: ReactNode }) {
  const auth = useAuth();
  const router = useRouter();
  const teacher = auth.status === "user" && isTeacher(auth.me);

  useEffect(() => {
    if (auth.status === "guest") {
      router.replace("/login");
      return;
    }
    if (auth.status === "user" && !isTeacher(auth.me)) {
      router.replace("/dashboard");
    }
  }, [auth, router]);

  if (auth.status !== "user") {
    return <LoadingScreen label="Ověřuji přihlášení…" />;
  }
  if (!teacher) {
    return <LoadingScreen label="Přesměrovávám…" />;
  }

  return <>{children}</>;
}
