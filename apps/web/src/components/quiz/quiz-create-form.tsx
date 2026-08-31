"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Sparkles } from "lucide-react";
import { ErrorBanner } from "@/components/common/feedback";
import {
  QuizFields,
  parseMaxQuestions,
  type QuizFieldsValue,
} from "@/components/quiz/quiz-fields";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api";

export function QuizCreateForm() {
  const router = useRouter();
  const [value, setValue] = useState<QuizFieldsValue>({
    title: "",
    description: "",
    maxQuestions: "",
    shuffle: false,
  });
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  function patch(next: Partial<QuizFieldsValue>) {
    setValue((prev) => ({ ...prev, ...next }));
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setSaving(true);
    try {
      const { quizUuid } = await api.createQuiz({
        title: value.title.trim(),
        description: value.description.trim() || null,
        maxQuestionsPerSession: parseMaxQuestions(value.maxQuestions),
        shuffle: value.shuffle,
      });
      router.push(`/quiz/${quizUuid}`);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Nepodařilo se vytvořit kvíz"
      );
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} className="surface surface-raised animate-rise">
      <div className="space-y-5 p-5 sm:p-6">
        {error && (
          <ErrorBanner message={error} onDismiss={() => setError(null)} />
        )}
        <QuizFields value={value} onChange={patch} idPrefix="create" />
      </div>

      <div className="flex flex-col-reverse gap-2 border-t border-border bg-muted/30 px-5 py-4 sm:flex-row sm:justify-end sm:px-6">
        <Button
          type="button"
          variant="ghost"
          onClick={() => router.push("/dashboard")}
        >
          Zrušit
        </Button>
        <Button type="submit" loading={saving} disabled={!value.title.trim()}>
          <Sparkles aria-hidden="true" data-icon="inline-start" />
          Vytvořit kvíz
        </Button>
      </div>
    </form>
  );
}
