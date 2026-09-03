"use client";

import { RequireAuth } from "@/features/auth/auth-provider";
import { QuizCreate } from "@/features/teacher";

export default function NewQuizPage() {
  return (
    <RequireAuth teacher>
      <QuizCreate />
    </RequireAuth>
  );
}
