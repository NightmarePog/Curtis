"use client";

import { use } from "react";

import { RequireAuth } from "@/features/auth/auth-provider";
import { QuizWorkspace } from "@/features/teacher";

export default function QuizPage({ params }: { params: Promise<{ uuid: string }> }) {
  const { uuid } = use(params);
  return (
    <RequireAuth teacher>
      <QuizWorkspace uuid={uuid} />
    </RequireAuth>
  );
}
