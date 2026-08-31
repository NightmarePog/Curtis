"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ListChecks, Play, Settings2, Shuffle, Target, Trash2 } from "lucide-react";
import { ErrorBanner } from "@/components/common/feedback";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api";
import type { Quiz } from "@/lib/types";

export function QuizCard({ quiz, index = 0 }: { quiz: Quiz; index?: number }) {
  const router = useRouter();
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function startSession() {
    setStarting(true);
    setError(null);
    try {
      const sessionUuid = await api.createSession(quiz.uuid);
      router.push(`/session/${sessionUuid}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Spuštění se nepodařilo");
      setStarting(false);
    }
  }

  async function deleteQuiz() {
    if (!window.confirm(`Opravdu smazat kvíz "${quiz.title}"?`)) return;
    try {
      await api.deleteQuiz(quiz.uuid);
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Smazání se nepodařilo");
    }
  }

  const questionCount = quiz.questions.length;
  const empty = questionCount === 0;

  return (
    <article
      className="surface surface-raised group flex flex-col animate-rise"
      style={{ animationDelay: `${Math.min(index, 8) * 45}ms` }}
    >
      <div className="flex-1 space-y-3 p-5">
        <div className="flex items-start justify-between gap-3">
          <h3 className="text-pretty text-base font-semibold leading-snug tracking-tight text-foreground">
            <Link
              href={`/quiz/${quiz.uuid}`}
              className="outline-none after:absolute after:inset-0 after:rounded-xl focus-visible:underline"
            >
              {quiz.title}
            </Link>
          </h3>
          <Badge variant={empty ? "amber" : "neutral"} className="shrink-0">
            <ListChecks aria-hidden="true" data-icon="inline-start" />
            {questionCount}
          </Badge>
        </div>

        {quiz.description ? (
          <p className="line-clamp-2 text-sm text-muted-foreground">
            {quiz.description}
          </p>
        ) : (
          <p className="text-sm italic text-muted-foreground/70">Bez popisu</p>
        )}

        <div className="flex flex-wrap gap-1.5 pt-0.5">
          {quiz.shuffle && (
            <Badge variant="blue">
              <Shuffle aria-hidden="true" data-icon="inline-start" />
              Náhodné pořadí
            </Badge>
          )}
          {quiz.maxQuestionsPerSession != null && (
            <Badge variant="neutral">
              <Target aria-hidden="true" data-icon="inline-start" />
              Max {quiz.maxQuestionsPerSession} na hru
            </Badge>
          )}
          {empty && <Badge variant="amber">Zatím bez otázek</Badge>}
        </div>
      </div>

      {error && (
        <div className="px-5 pb-3">
          <ErrorBanner message={error} onDismiss={() => setError(null)} />
        </div>
      )}

      {/* z-10 keeps these above the title's stretched click target */}
      <div className="relative z-10 flex items-center justify-between border-t border-border bg-muted/30 px-5 py-3">
        <div className="flex items-center gap-1.5">
          <Button variant="ghost" size="sm" asChild>
            <Link href={`/quiz/${quiz.uuid}`}>
              <Settings2 aria-hidden="true" data-icon="inline-start" />
              Spravovat
            </Link>
          </Button>
          <Button variant="ghost" size="sm" onClick={deleteQuiz} className="text-destructive hover:text-destructive">
            <Trash2 aria-hidden="true" data-icon="inline-start" />
            Smazat
          </Button>
        </div>
        <Button
          size="sm"
          loading={starting}
          disabled={empty}
          title={empty ? "Nejdřív přidejte alespoň jednu otázku" : undefined}
          onClick={startSession}
        >
          <Play aria-hidden="true" data-icon="inline-start" />
          Spustit
        </Button>
      </div>
    </article>
  );
}
