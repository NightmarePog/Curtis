"use client";

import { isTeacher, useMe } from "@/components/providers/auth-provider";
import { StudentDashboard } from "@/components/dashboard/student-dashboard";
import { TeacherDashboard } from "@/components/dashboard/teacher-dashboard";

export function Dashboard() {
  const me = useMe();
  return isTeacher(me) ? <TeacherDashboard /> : <StudentDashboard />;
}
