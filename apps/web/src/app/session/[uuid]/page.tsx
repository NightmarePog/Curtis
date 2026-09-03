"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

import {
  RequireAuth,
  isAdministrator,
  isStudent,
  isTeacher,
  useAuth,
} from "@/features/auth/auth-provider";
import { StudentSession } from "@/features/student";
import { TeacherSessionMonitor } from "@/features/teacher";

export default function SessionPage({ params }: { params: Promise<{ uuid: string }> }) {
  const { uuid } = use(params);
  const { state } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (state.status === "user" && isAdministrator(state.user)) {
      router.replace("/admin");
    }
  }, [router, state]);

  return (
    <RequireAuth>
      {state.status === "user" &&
        (isTeacher(state.user) ? (
          <TeacherSessionMonitor sessionUuid={uuid} />
        ) : isStudent(state.user) ? (
          <StudentSession sessionUuid={uuid} />
        ) : null)}
    </RequireAuth>
  );
}
