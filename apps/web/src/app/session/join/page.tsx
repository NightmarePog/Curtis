"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { RequireAuth } from "@/components/guards";
import { Button, Card, ErrorBanner, Input, Label } from "@/components/ui";

export default function JoinPage() {
  const router = useRouter();
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);

  function join() {
    const trimmed = code.trim();
    if (!trimmed) {
      setError("Zadejte kód kvízu.");
      return;
    }
    router.push(`/session/${trimmed}`);
  }

  return (
    <RequireAuth>
      <div className="mx-auto max-w-md space-y-6">
        <h1 className="text-2xl font-bold text-slate-900">Připojit se do kvízu</h1>
        {error && <ErrorBanner message={error} onDismiss={() => setError(null)} />}
        <Card>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              join();
            }}
            className="space-y-4"
          >
            <div>
              <Label htmlFor="code">Kód kvízu</Label>
              <Input
                id="code"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder="Např. 3f9a2c1e-…"
                autoFocus
              />
              <p className="mt-2 text-xs text-slate-500">
                Kód získáte od vyučujícího.
              </p>
            </div>
            <Button type="submit" className="w-full" disabled={!code.trim()}>
              Připojit se
            </Button>
          </form>
        </Card>
      </div>
    </RequireAuth>
  );
}
