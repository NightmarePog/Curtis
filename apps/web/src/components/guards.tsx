"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { isTeacher, useAuth } from "@/components/auth";
import { LoadingScreen } from "@/components/ui";

export function RequireAuth({ children }: { children: ReactNode }) {
  const auth = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (auth.status === "guest") {
      router.replace("/login");
    }
  }, [auth, router]);

  if (auth.status === "loading" || auth.status === "guest") {
    return <LoadingScreen />;
  }

  return <>{children}</>;
}

export function TeacherOnly({ children }: { children: ReactNode }) {
  const auth = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (auth.status === "guest") {
      router.replace("/login");
      return;
    }
    if (auth.status === "user" && !isTeacher(auth.me)) {
      router.replace("/dashboard");
    }
  }, [auth, router]);

  if (auth.status === "loading" || auth.status === "guest") {
    return <LoadingScreen />;
  }
  if (!isTeacher(auth.me)) {
    return <LoadingScreen label="Přesměrovávám…" />;
  }

  return <>{children}</>;
}
