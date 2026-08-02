"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/components/auth";
import { Button, Card, LoadingScreen } from "@/components/ui";
import { LOGIN_URL } from "@/lib/api";

export default function LoginPage() {
  const auth = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (auth.status === "user") {
      router.replace("/dashboard");
    }
  }, [auth.status, router]);

  if (auth.status === "loading") {
    return <LoadingScreen />;
  }

  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center">
      <Card className="w-full max-w-md text-center">
        <h1 className="text-2xl font-bold text-indigo-700">Curtis</h1>
        <p className="mt-2 text-sm text-slate-500">
          Kvízový nástroj pro školu
        </p>
        <div className="mt-6">
          <Button size="lg" onClick={() => router.push(LOGIN_URL)}>
            Přihlásit se školním účtem
          </Button>
        </div>
      </Card>
    </div>
  );
}
