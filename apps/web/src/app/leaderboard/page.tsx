"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

import {
  RequireAuth,
  isStudent,
  useAuth,
  workspacePath,
} from "@/features/auth/auth-provider";
import { StudentClassLeaderboard } from "@/features/student";

export default function LeaderboardPage() {
  const { state } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (state.status === "user" && !isStudent(state.user)) {
      router.replace(workspacePath(state.user));
    }
  }, [router, state]);

  return (
    <RequireAuth>
      {state.status === "user" && isStudent(state.user) ? (
        <StudentClassLeaderboard />
      ) : null}
    </RequireAuth>
  );
}
