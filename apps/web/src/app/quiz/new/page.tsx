"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { TeacherOnly } from "@/components/guards";
import {
  Button,
  Card,
  Checkbox,
  ErrorBanner,
  Input,
  Label,
  Textarea,
} from "@/components/ui";
import { api } from "@/lib/api";

export default function NewQuizPage() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [maxQuestions, setMaxQuestions] = useState("");
  const [shuffle, setShuffle] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      const max = maxQuestions.trim()
        ? Number.parseInt(maxQuestions, 10)
        : undefined;
      const { quizUuid } = await api.createQuiz({
        title: title.trim(),
        description: description.trim() || null,
        maxQuestionsPerSession: max && Number.isFinite(max) ? max : 5,
        shuffle,
      });
      router.push(`/quiz/${quizUuid}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nepodařilo se vytvořit kvíz");
      setSaving(false);
    }
  }

  return (
    <TeacherOnly>
      <div className="mx-auto max-w-2xl space-y-6">
        <h1 className="text-2xl font-bold text-slate-900">Nový kvíz</h1>
        {error && <ErrorBanner message={error} onDismiss={() => setError(null)} />}
        <Card>
          <form onSubmit={submit} className="space-y-4">
            <div>
              <Label htmlFor="title">Název</Label>
              <Input
                id="title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Např. Matematika — 6. třída"
                required
                maxLength={200}
              />
            </div>
            <div>
              <Label htmlFor="description">Popis</Label>
              <Textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Krátký popis pro žáky"
                rows={3}
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <Label htmlFor="max">Otázek na jednu hru</Label>
                <Input
                  id="max"
                  type="number"
                  min={1}
                  value={maxQuestions}
                  onChange={(e) => setMaxQuestions(e.target.value)}
                  placeholder="5"
                />
              </div>
              <div className="flex items-end pb-1">
                <label className="flex items-center gap-2 text-sm text-slate-700">
                  <Checkbox
                    checked={shuffle}
                    onChange={(e) => setShuffle(e.target.checked)}
                  />
                  Náhodné pořadí otázek
                </label>
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button
                type="button"
                variant="secondary"
                onClick={() => router.push("/dashboard")}
              >
                Zrušit
              </Button>
              <Button type="submit" loading={saving} disabled={!title.trim()}>
                Vytvořit
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </TeacherOnly>
  );
}
