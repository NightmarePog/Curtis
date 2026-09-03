"use client";

import { RequireAuth } from "@/features/auth/auth-provider";
import { SessionHistory } from "@/features/teacher/session-history";

export default function HistoryPage() {
  return (
    <RequireAuth teacher>
      <SessionHistory />
    </RequireAuth>
  );
}
