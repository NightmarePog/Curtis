"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { TeacherOnly } from "@/components/guards";
import {
  Badge,
  Button,
  Card,
  Checkbox,
  ErrorBanner,
  Input,
  Label,
  LoadingScreen,
  Textarea,
} from "@/components/ui";
import { api } from "@/lib/api";
import type { Question, QuestionAnswer, Quiz } from "@/lib/types";
import { cn } from "@/lib/cn";

interface AnswerDraft {
  answer: string;
  isCorrect: boolean;
}

const emptyAnswers = (): AnswerDraft[] => [
  { answer: "", isCorrect: true },
  { answer: "", isCorrect: false },
  { answer: "", isCorrect: false },
  { answer: "", isCorrect: false },
];

function QuestionForm({
  initial,
  onSubmit,
  onCancel,
  submitting,
}: {
  initial?: Question;
  onSubmit: (question: string, answers: QuestionAnswer[], time: number) => void;
  onCancel: () => void;
  submitting: boolean;
}) {
  const [text, setText] = useState(initial?.question ?? "");
  const [time, setTime] = useState(
    String(initial?.timeInSeconds ?? 30)
  );
  const [answers, setAnswers] = useState<AnswerDraft[]>(
    initial
      ? initial.answers.map((a) => ({ answer: a.answer, isCorrect: !!a.isCorrect }))
      : emptyAnswers()
  );

  function updateAnswer(index: number, patch: Partial<AnswerDraft>) {
    setAnswers((prev) =>
      prev.map((a, i) => (i === index ? { ...a, ...patch } : a))
    );
  }

  function addAnswer() {
    setAnswers((prev) => [...prev, { answer: "", isCorrect: false }]);
  }

  function removeAnswer(index: number) {
    setAnswers((prev) => prev.filter((_, i) => i !== index));
  }

  function submit(e: React.FormEvent) {
    e.preventDefault();
    const timeNum = Number.parseInt(time, 10);
    if (
      !text.trim() ||
      !Number.isFinite(timeNum) ||
      timeNum < 1 ||
      answers.length < 2 ||
      answers.some((a) => !a.answer.trim()) ||
      !answers.some((a) => a.isCorrect)
    ) {
      return;
    }
    onSubmit(
      text.trim(),
      answers.map((a) => ({ answer: a.answer.trim(), isCorrect: a.isCorrect })),
      timeNum
    );
  }

  return (
    <form onSubmit={submit} className="space-y-4 rounded-xl border border-slate-200 bg-slate-50 p-4">
      <div>
        <Label htmlFor={`question-${initial?.id ?? "new"}`}>Otázka</Label>
        <Textarea
          id={`question-${initial?.id ?? "new"}`}
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Znění otázky"
          rows={2}
          required
        />
      </div>

      <div>
        <Label htmlFor={`time-${initial?.id ?? "new"}`}>
          Čas na odpověď (vteřiny)
        </Label>
        <Input
          id={`time-${initial?.id ?? "new"}`}
          type="number"
          min={1}
          value={time}
          onChange={(e) => setTime(e.target.value)}
          className="max-w-40"
          required
        />
      </div>

      <div className="space-y-2">
        <Label>Odpovědi</Label>
        {answers.map((answer, index) => (
          <div key={index} className="flex items-center gap-2">
            <Checkbox
              checked={answer.isCorrect}
              onChange={(e) =>
                updateAnswer(index, { isCorrect: e.target.checked })
              }
              title="Správná odpověď"
              aria-label={`Správná odpověď ${index + 1}`}
            />
            <Input
              value={answer.answer}
              onChange={(e) => updateAnswer(index, { answer: e.target.value })}
              placeholder={`Odpověď ${index + 1}`}
              required
            />
            <button
              type="button"
              onClick={() => removeAnswer(index)}
              className="shrink-0 rounded px-2 text-slate-400 hover:text-red-600"
              aria-label="Odebrat odpověď"
              disabled={answers.length <= 2}
            >
              ✕
            </button>
          </div>
        ))}
        <div className="flex items-center gap-3 pt-1">
          <Button type="button" variant="secondary" size="sm" onClick={addAnswer}>
            + Odpověď
          </Button>
          <span className="text-xs text-slate-500">
            Zaškrtněte správnou odpověď (může být více).
          </span>
        </div>
      </div>

      <div className="flex justify-end gap-2">
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          Zrušit
        </Button>
        <Button type="submit" loading={submitting}>
          {initial ? "Uložit změny" : "Přidat otázku"}
        </Button>
      </div>
    </form>
  );
}

function QuestionItem({
  question,
  onEdit,
  onDelete,
  onCancelEdit,
  saving,
}: {
  question: Question;
  onEdit: (q: string, a: QuestionAnswer[], t: number) => void;
  onDelete: () => void;
  onCancelEdit: () => void;
  saving: boolean;
}) {
  const [editing, setEditing] = useState(false);

  if (editing) {
    return (
      <QuestionForm
        initial={question}
        onSubmit={(q, a, t) => {
          onEdit(q, a, t);
          setEditing(false);
        }}
        onCancel={() => {
          setEditing(false);
          onCancelEdit();
        }}
        submitting={saving}
      />
    );
  }

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      <div className="flex items-start justify-between gap-2">
        <p className="font-medium text-slate-900">{question.question}</p>
        <Badge variant="neutral">
          {question.timeInSeconds ?? "∞"} s
        </Badge>
      </div>
      <ul className="mt-3 space-y-1.5">
        {question.answers.map((answer, index) => (
          <li
            key={index}
            className={cn(
              "flex items-center justify-between rounded-lg border px-3 py-1.5 text-sm",
              answer.isCorrect
                ? "border-emerald-200 bg-emerald-50 text-emerald-900"
                : "border-slate-200 bg-slate-50 text-slate-700"
            )}
          >
            <span>{answer.answer}</span>
            {answer.isCorrect && <Badge variant="green">Správně</Badge>}
          </li>
        ))}
      </ul>
      <div className="mt-3 flex justify-end gap-2">
        <Button
          variant="secondary"
          size="sm"
          onClick={() => setEditing(true)}
        >
          Upravit
        </Button>
        <Button variant="danger" size="sm" onClick={onDelete}>
          Smazat
        </Button>
      </div>
    </div>
  );
}

function MetaEditor({
  quiz,
  onSaved,
}: {
  quiz: Quiz;
  onSaved: (quiz: Quiz) => void;
}) {
  const [title, setTitle] = useState(quiz.title);
  const [description, setDescription] = useState(quiz.description ?? "");
  const [maxQuestions, setMaxQuestions] = useState(
    String(quiz.maxQuestionsPerSession ?? "")
  );
  const [shuffle, setShuffle] = useState(!!quiz.shuffle);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const max = Number.parseInt(maxQuestions, 10);
      await api.patchQuiz(quiz.uuid, {
        title: title.trim(),
        description: description.trim() || null,
        maxQuestionsPerSession: Number.isFinite(max) ? max : 5,
        shuffle,
      });
      onSaved({ ...quiz, title: title.trim(), description: description.trim() || null });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Uložení se nepodařilo");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Card>
      <form onSubmit={save} className="space-y-4">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-slate-900">Nastavení kvízu</h1>
          <Button type="submit" loading={saving}>
            Uložit
          </Button>
        </div>
        {error && <ErrorBanner message={error} onDismiss={() => setError(null)} />}
        <div>
          <Label htmlFor="meta-title">Název</Label>
          <Input
            id="meta-title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
        </div>
        <div>
          <Label htmlFor="meta-description">Popis</Label>
          <Textarea
            id="meta-description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={2}
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <Label htmlFor="meta-max">Otázek na jednu hru</Label>
            <Input
              id="meta-max"
              type="number"
              min={1}
              value={maxQuestions}
              onChange={(e) => setMaxQuestions(e.target.value)}
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
      </form>
    </Card>
  );
}

export default function QuizDetailPage() {
  const params = useParams<{ uuid: string }>();
  const router = useRouter();
  const uuid = params.uuid;

  const [quiz, setQuiz] = useState<Quiz | null>(null);
  const [questions, setQuestions] = useState<Question[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [addingQuestion, setAddingQuestion] = useState(false);
  const [savingQuestion, setSavingQuestion] = useState(false);
  const [starting, setStarting] = useState(false);

  const load = useCallback(() => {
    api
      .getQuiz(uuid)
      .then(setQuiz)
      .catch((err) => setError(err.message));
    api
      .listQuestions(uuid)
      .then(setQuestions)
      .catch((err) => setError(err.message));
  }, [uuid]);

  useEffect(() => {
    load();
  }, [load]);

  async function createQuestion(q: string, a: QuestionAnswer[], t: number) {
    setSavingQuestion(true);
    setError(null);
    try {
      await api.createQuestion(uuid, {
        question: q,
        answers: a,
        timeInSeconds: t,
      });
      setAddingQuestion(false);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Přidání se nepodařilo");
    } finally {
      setSavingQuestion(false);
    }
  }

  async function patchQuestion(id: number, q: string, a: QuestionAnswer[], t: number) {
    setSavingQuestion(true);
    setError(null);
    try {
      await api.patchQuestion(uuid, id, {
        question: q,
        answers: a,
        timeInSeconds: t,
      });
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Úprava se nepodařila");
    } finally {
      setSavingQuestion(false);
    }
  }

  async function deleteQuestion(id: number) {
    if (!window.confirm("Opravdu smazat tuto otázku?")) {
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
      <TeacherOnly>
        <ErrorBanner message={error} onDismiss={() => setError(null)} />
      </TeacherOnly>
    );
  }

  if (!quiz || !questions) {
    return (
      <TeacherOnly>
        <LoadingScreen />
      </TeacherOnly>
    );
  }

  return (
    <TeacherOnly>
      <div className="space-y-6">
        <div className="flex items-center justify-between gap-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => router.push("/dashboard")}
          >
            ← Zpět
          </Button>
          <div className="flex gap-2">
          <Button loading={starting} onClick={startSession}>
            Spustit kvíz
          </Button>
          </div>
        </div>

        {error && <ErrorBanner message={error} onDismiss={() => setError(null)} />}

        <MetaEditor
          quiz={quiz}
          onSaved={(updated) => setQuiz(updated)}
        />

        <Card>
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-900">
              Otázky{" "}
              <span className="text-sm font-normal text-slate-500">
                ({questions.length})
              </span>
            </h2>
            {!addingQuestion && (
              <Button size="sm" onClick={() => setAddingQuestion(true)}>
                + Přidat otázku
              </Button>
            )}
          </div>

          {addingQuestion && (
            <div className="mb-4">
              <QuestionForm
                onSubmit={createQuestion}
                onCancel={() => setAddingQuestion(false)}
                submitting={savingQuestion}
              />
            </div>
          )}

          {questions.length === 0 && !addingQuestion ? (
            <p className="py-8 text-center text-sm text-slate-500">
              Žádné otázky. Přidejte první otázku.
            </p>
          ) : (
            <div className="space-y-3">
              {questions.map((question) => (
                <QuestionItem
                  key={question.id}
                  question={question}
                  onEdit={(q, a, t) => patchQuestion(question.id, q, a, t)}
                  onDelete={() => deleteQuestion(question.id)}
                  onCancelEdit={() => setError(null)}
                  saving={savingQuestion}
                />
              ))}
            </div>
          )}
        </Card>
      </div>
    </TeacherOnly>
  );
}
