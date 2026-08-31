import type { Metadata } from "next";
import { LoginPanel } from "@/components/auth/login-panel";

export const metadata: Metadata = { title: "Přihlášení" };

export default function LoginPage() {
  return (
    <div className="flex min-h-[65vh] items-center justify-center">
      <LoginPanel />
    </div>
  );
}
