"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

import {
  RequireAuth,
  isAdministrator,
  isStudent,
  isTeacher,
  useAuth,
} from "@/features/auth/auth-provider";
import { StudentHome } from "@/features/student";
import { TeacherHome } from "@/features/teacher";

export default function DashboardPage() {
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
          <TeacherHome />
        ) : isStudent(state.user) ? (
          <StudentHome />
        ) : null)}
    </RequireAuth>
  );
}
