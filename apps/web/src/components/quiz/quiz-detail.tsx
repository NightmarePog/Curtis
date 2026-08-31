"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { HelpCircle, ListPlus, Play } from "lucide-react";
import {
  EmptyState,
  ErrorBanner,
  LoadingScreen,
} from "@/components/common/feedback";
import { PageHeader } from "@/components/layout/page-header";
import { QuestionCard } from "@/components/quiz/question-card";
import { QuestionEditor } from "@/components/quiz/question-editor";
import { QuizSettingsForm } from "@/components/quiz/quiz-settings-form";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api";
import type { Question, QuestionCreateDto, Quiz } from "@/lib/types";

export function QuizDetail() {
  const { uuid } = useParams<{ uuid: string }>();
  const router = useRouter();

  const [quiz, setQuiz] = useState<Quiz | null>(null);
  const [questions, setQuestions] = useState<Question[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const [savingQuestion, setSavingQuestion] = useState(false);
  const [starting, setStarting] = useState(false);

  const load = useCallback(async () => {
    try {
      const [loadedQuiz, loadedQuestions] = await Promise.all([
        api.getQuiz(uuid),
        api.listQuestions(uuid),
      ]);
      setQuiz(loadedQuiz);
      setQuestions(loadedQuestions);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Načtení se nepodařilo");
    }
  }, [uuid]);

  useEffect(() => {
    load();
  }, [load]);

  async function createQuestion(payload: QuestionCreateDto) {
    setSavingQuestion(true);
    setError(null);
    try {
      await api.createQuestion(uuid, payload);
      setAdding(false);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Přidání se nepodařilo");
    } finally {
      setSavingQuestion(false);
    }
  }

  async function patchQuestion(id: number, payload: QuestionCreateDto) {
    setSavingQuestion(true);
    setError(null);
    try {
      await api.patchQuestion(uuid, id, payload);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Úprava se nepodařila");
    } finally {
      setSavingQuestion(false);
    }
  }

  async function deleteQuestion(id: number) {
    if (!window.confirm("Opravdu smazat tuto otázku? Akci nelze vrátit.")) {
      return;
    }
    setError(null);
    try {
      await api.deleteQuestion(uuid, id);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Smazání se nepodařilo");
    }
  }

  async function startSession() {
    setStarting(true);
    setError(null);
    try {
      const sessionUuid = await api.createSession(uuid);
      router.push(`/session/${sessionUuid}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Spuštění se nepodařilo");
      setStarting(false);
    }
  }

  if (error && !quiz) {
    return (
      <div className="space-y-4">
        <PageHeader title="Kvíz se nepodařilo načíst" backHref="/dashboard" />
        <ErrorBanner message={error} />
      </div>
    );
  }

  if (!quiz || !questions) {
    return <LoadingScreen label="Načítám kvíz…" />;
  }

  const empty = questions.length === 0;

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Úprava kvízu"
        title={quiz.title}
        description={quiz.description ?? undefined}
        backHref="/dashboard"
        backLabel="Zpět na přehled"
        actions={
          <Button
            size="lg"
            loading={starting}
            disabled={empty}
            title={empty ? "Nejdřív přidejte alespoň jednu otázku" : undefined}
            onClick={startSession}
          >
            <Play aria-hidden="true" data-icon="inline-start" />
            Spustit kvíz
          </Button>
        }
      />

      {error && <ErrorBanner message={error} onDismiss={() => setError(null)} />}

      <QuizSettingsForm quiz={quiz} onSaved={setQuiz} />

      <section className="surface surface-raised" aria-labelledby="otazky">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border px-5 py-4 sm:px-6">
          <h2
            id="otazky"
            className="flex items-center gap-2 text-base font-semibold tracking-tight text-foreground"
          >
            Otázky
            <Badge variant="neutral">{questions.length}</Badge>
          </h2>
          {!adding && (
            <Button size="sm" onClick={() => setAdding(true)}>
              <ListPlus aria-hidden="true" data-icon="inline-start" />
              Přidat otázku
            </Button>
          )}
        </div>

        <div className="space-y-3 p-5 sm:p-6">
          {adding && (
            <QuestionEditor
              onSubmit={createQuestion}
              onCancel={() => setAdding(false)}
              submitting={savingQuestion}
            />
          )}

          {empty && !adding ? (
            <EmptyState
              icon={HelpCircle}
              title="Kvíz zatím nemá otázky"
              description="Přidejte první otázku, ať můžete kvíz spustit ve třídě."
              action={
                <Button onClick={() => setAdding(true)}>
                  <ListPlus aria-hidden="true" data-icon="inline-start" />
                  Přidat otázku
                </Button>
              }
            />
          ) : (
            questions.map((question, index) => (
              <QuestionCard
                key={question.id}
                question={question}
                position={index + 1}
                onEdit={(payload) => patchQuestion(question.id, payload)}
                onDelete={() => deleteQuestion(question.id)}
                onCancelEdit={() => setError(null)}
                saving={savingQuestion}
              />
            ))
          )}
        </div>
      </section>
    </div>
  );
}
