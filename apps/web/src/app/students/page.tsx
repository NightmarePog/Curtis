"use client";

import { RequireAuth } from "@/features/auth/auth-provider";
import { StudentDirectory } from "@/features/teacher/student-directory";

export default function StudentsPage() {
  return (
    <RequireAuth teacher>
      <StudentDirectory />
    </RequireAuth>
  );
}
