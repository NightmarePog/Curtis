"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight, Radio } from "lucide-react";
import { ErrorBanner } from "@/components/common/feedback";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export function JoinCard({ autoFocus = false }: { autoFocus?: boolean }) {
  const router = useRouter();
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);

  function join(event: React.FormEvent) {
    event.preventDefault();
    const trimmed = code.trim();
    if (!trimmed) {
      setError("Zadejte kód kvízu, který vám sdělil vyučující.");
      return;
    }
    router.push(`/session/${encodeURIComponent(trimmed)}`);
  }

  return (
    <div className="surface surface-raised overflow-hidden animate-rise">
      <form onSubmit={join} className="space-y-5 p-5 sm:p-6">
        <div className="flex items-center gap-3">
          <span className="relative flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand-soft text-brand">
            <Radio aria-hidden="true" className="relative size-5" />
          </span>
          <div>
            <h2 className="text-base font-semibold tracking-tight text-foreground">
              Připojit se do kvízu
            </h2>
            <p className="text-sm text-muted-foreground">
              Kód dostanete od vyučujícího.
            </p>
          </div>
        </div>

        {error && (
          <ErrorBanner message={error} onDismiss={() => setError(null)} />
        )}

        <div className="space-y-2">
          <Label htmlFor="session-code">Kód kvízu</Label>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Input
              id="session-code"
              value={code}
              onChange={(e) => {
                setCode(e.target.value);
                if (error) setError(null);
              }}
              placeholder="3f9a2c1e-…"
              autoFocus={autoFocus}
              autoComplete="off"
              autoCapitalize="none"
              spellCheck={false}
              className="font-mono tracking-tight sm:h-11"
            />
            <Button
              type="submit"
              size="lg"
              disabled={!code.trim()}
              className="sm:shrink-0"
            >
              Připojit se
              <ArrowRight aria-hidden="true" data-icon="inline-end" />
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
}
