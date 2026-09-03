import type { Metadata } from "next";

import { RequireAuth } from "@/features/auth/auth-provider";
import { StudentProfile } from "@/features/teacher/student-profile";

export const metadata: Metadata = {
  title: "Profil žáka",
};

export default async function StudentProfilePage({
  params,
}: {
  params: Promise<{ studentId: string }>;
}) {
  const { studentId } = await params;
  return (
    <RequireAuth teacher>
      <StudentProfile studentId={studentId} />
    </RequireAuth>
  );
}
