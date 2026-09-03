import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { DemoRolePicker } from "@/features/auth/demo-role-picker";
import { DEMO_MODE, LOGIN_URL } from "@/lib/constants";

export const metadata: Metadata = { title: "Přihlášení" };

export default function LoginPage() {
  if (!DEMO_MODE) redirect(LOGIN_URL);
  return <DemoRolePicker />;
}
