"use client";

import { useState } from "react";
import { Check, Save } from "lucide-react";
import { ErrorBanner } from "@/components/common/feedback";
import {
  QuizFields,
  parseMaxQuestions,
  type QuizFieldsValue,
} from "@/components/quiz/quiz-fields";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api";
import type { Quiz } from "@/lib/types";

export function QuizSettingsForm({
  quiz,
  onSaved,
}: {
  quiz: Quiz;
  onSaved: (quiz: Quiz) => void;
}) {
  const [value, setValue] = useState<QuizFieldsValue>({
    title: quiz.title,
    description: quiz.description ?? "",
    maxQuestions: String(quiz.maxQuestionsPerSession ?? ""),
    shuffle: !!quiz.shuffle,
  });
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function patch(next: Partial<QuizFieldsValue>) {
    setValue((prev) => ({ ...prev, ...next }));
    setSaved(false);
  }

  async function save(event: React.FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const maxQuestionsPerSession = parseMaxQuestions(value.maxQuestions);
      const title = value.title.trim();
      const description = value.description.trim() || null;

      await api.patchQuiz(quiz.uuid, {
        title,
        description,
        maxQuestionsPerSession,
        shuffle: value.shuffle,
      });

      onSaved({
        ...quiz,
        title,
        description,
        maxQuestionsPerSession,
        shuffle: value.shuffle,
      });
      setSaved(true);
      window.setTimeout(() => setSaved(false), 2500);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Uložení se nepodařilo");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={save} className="surface surface-raised">
      <div className="space-y-5 p-5 sm:p-6">
        <div>
          <h2 className="text-base font-semibold tracking-tight text-foreground">
            Nastavení kvízu
          </h2>
          <p className="mt-0.5 text-sm text-muted-foreground">
            Změny se projeví u nově spuštěných her.
          </p>
        </div>

        {error && (
          <ErrorBanner message={error} onDismiss={() => setError(null)} />
        )}

        <QuizFields value={value} onChange={patch} idPrefix="settings" />
      </div>

      <div className="flex items-center justify-end gap-3 border-t border-border bg-muted/30 px-5 py-4 sm:px-6">
        <span
          aria-live="polite"
          className={`flex items-center gap-1.5 text-sm text-success transition-opacity duration-300 ${
            saved ? "opacity-100" : "opacity-0"
          }`}
        >
          <Check aria-hidden="true" className="size-4" />
          Uloženo
        </span>
        <Button type="submit" loading={saving} disabled={!value.title.trim()}>
          <Save aria-hidden="true" data-icon="inline-start" />
          Uložit změny
        </Button>
      </div>
    </form>
  );
}
