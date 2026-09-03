"use client";

import {
  ArrowLeft,
  ArrowLeftRight,
  CircleCheck,
  Clock3,
  FileImage,
  ListChecks,
  Pencil,
  Plus,
  Save,
  Settings2,
  Trash2,
} from "lucide-react";
import Link from "next/link";
import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type FormEvent,
} from "react";

import {
  Badge,
  Button,
  EmptyState,
  Panel,
  PanelContent,
  PanelDescription,
  PanelFooter,
  PanelHeader,
  PanelTitle,
  buttonStyles,
} from "@/components/ui";
import { QuizWorkspaceSkeleton } from "@/components/page-skeletons";
import { cn } from "@/lib/cn";
import { messageFromError } from "@/lib/http";
import { quizService } from "@/lib/services";
import type { Question, QuestionInput, Quiz } from "@/types/domain";
import {
  QuizFields,
  quizFormToInput,
  quizToForm,
  validateQuizForm,
  type QuizFormErrors,
  type QuizFormState,
} from "./quiz-form";
import { QuestionEditor, questionTypeLabels } from "./question-editor";
import {
  ErrorSummary,
  Notice,
  QuizStatusBadge,
  focusErrorSummary,
  formatDateTime,
} from "./shared";

interface QuizWorkspaceProps {
  uuid: string;
}

function questionCountLabel(count: number) {
  if (count === 1) return "1 otázka";
  if (count >= 2 && count <= 4) return `${count} otázky`;
  return `${count} otázek`;
}

function QuestionContent({ question }: { question: Question }) {
  return (
    <div className="grid gap-3 text-sm">
      {question.codeSnippet ? (
        <pre className="max-w-full overflow-x-auto rounded-md border border-border bg-field p-4 font-mono text-sm leading-6 text-foreground">
          <code>{question.codeSnippet}</code>
        </pre>
      ) : null}
      {question.imageRef ? (
        <p className="flex items-center gap-2 text-muted-foreground">
          <FileImage aria-hidden="true" className="size-4 shrink-0" />
          Obrázek: <span className="font-mono text-xs text-foreground">{question.imageRef}</span>
        </p>
      ) : null}
      {question.type === "MULTIPLE_CHOICE" ? (
        <ol className="grid gap-2 sm:grid-cols-2">
          {question.answers.map((answer, index) => (
            <li
              key={index}
              className={cn(
                "flex items-start gap-2 rounded-md border px-3 py-2.5",
                answer.isCorrect
                  ? "border-brand/40 bg-brand-subtle text-foreground"
                  : "border-border bg-surface text-muted-foreground",
              )}
            >
              {answer.isCorrect ? (
                <CircleCheck
                  aria-label="Správná odpověď"
                  className="mt-0.5 size-4 shrink-0 text-brand-text"
                />
              ) : (
                <span className="w-4 shrink-0 text-center tabular-nums" aria-hidden="true">
                  {index + 1}.
                </span>
              )}
              <span>{answer.answer}</span>
            </li>
          ))}
        </ol>
      ) : null}
      {question.type === "MATCHING" ? (
        <dl className="grid gap-2 sm:grid-cols-2">
          {question.pairs.map((pair, index) => (
            <div
              key={index}
              className="grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-2 rounded-md border border-border bg-surface px-3 py-2.5"
            >
              <dt className="font-medium text-foreground">{pair.left}</dt>
              <ArrowLeftRight
                aria-hidden="true"
                className="size-4 shrink-0 text-muted-foreground"
              />
              <dd className="text-foreground">{pair.right}</dd>
            </div>
          ))}
        </dl>
      ) : null}
      {question.type === "FREE_TEXT" ? (
        <p className="rounded-md border border-border bg-surface px-3 py-2.5 text-muted-foreground">
          Odpověď vyhodnotí vyučující po odevzdání.
        </p>
      ) : null}
    </div>
  );
}

export function QuizWorkspace({ uuid }: QuizWorkspaceProps) {
  const [quiz, setQuiz] = useState<Quiz | null>(null);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [settings, setSettings] = useState<QuizFormState | null>(null);
  const [settingsTouched, setSettingsTouched] = useState<Set<keyof QuizFormState>>(
    new Set(),
  );
  const [settingsSubmitted, setSettingsSubmitted] = useState(false);
  const [settingsSaving, setSettingsSaving] = useState(false);
  const [settingsError, setSettingsError] = useState<string | null>(null);
  const [settingsSaved, setSettingsSaved] = useState(false);

  const [creating, setCreating] = useState(false);
  const [editingId, setEditingId] = useState<string | number | null>(null);
  const [questionSaving, setQuestionSaving] = useState(false);
  const [questionError, setQuestionError] = useState<string | null>(null);
  const [questionNotice, setQuestionNotice] = useState<string | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | number | null>(null);
  const [deletingId, setDeletingId] = useState<string | number | null>(null);

  const loadWorkspace = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const [loadedQuiz, loadedQuestions] = await Promise.all([
        quizService.get(uuid),
        quizService.questions(uuid),
      ]);
      setQuiz({ ...loadedQuiz, questions: loadedQuestions });
      setQuestions(loadedQuestions);
      setSettings(quizToForm({ ...loadedQuiz, questions: loadedQuestions }));
    } catch (error) {
      setLoadError(
        messageFromError(error, "Kvíz se nepodařilo načíst."),
      );
    } finally {
      setLoading(false);
    }
  }, [uuid]);

  const reloadQuestions = useCallback(async () => {
    const loadedQuestions = await quizService.questions(uuid);
    setQuestions(loadedQuestions);
    setQuiz((current) =>
      current ? { ...current, questions: loadedQuestions } : current,
    );
  }, [uuid]);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => void loadWorkspace(), 0);
    return () => window.clearTimeout(initialLoad);
  }, [loadWorkspace]);

  const settingsErrors = useMemo(
    () => (settings ? validateQuizForm(settings) : {}),
    [settings],
  );
  const visibleSettingsErrors = useMemo<QuizFormErrors>(
    () =>
      Object.fromEntries(
        Object.entries(settingsErrors).filter(([field]) =>
          settingsSubmitted || settingsTouched.has(field as keyof QuizFormState),
        ),
      ) as QuizFormErrors,
    [settingsErrors, settingsSubmitted, settingsTouched],
  );

  async function saveSettings(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!settings) return;
    setSettingsSubmitted(true);
    setSettingsError(null);
    setSettingsSaved(false);
    if (Object.keys(settingsErrors).length > 0) {
      focusErrorSummary(event.currentTarget);
      return;
    }

    setSettingsSaving(true);
    try {
      await quizService.update(uuid, quizFormToInput(settings));
      const loadedQuiz = await quizService.get(uuid);
      const merged = { ...loadedQuiz, questions };
      setQuiz(merged);
      setSettings(quizToForm(merged));
      setSettingsTouched(new Set());
      setSettingsSubmitted(false);
      setSettingsSaved(true);
    } catch (error) {
      setSettingsError(
        messageFromError(
          error,
          "Nastavení se nepodařilo uložit. Zadané údaje zůstaly ve formuláři.",
        ),
      );
    } finally {
      setSettingsSaving(false);
    }
  }

  async function createQuestion(input: QuestionInput) {
    setQuestionSaving(true);
    setQuestionError(null);
    setQuestionNotice(null);
    try {
      await quizService.addQuestion(uuid, input);
      await reloadQuestions();
      setCreating(false);
      setQuestionNotice("Otázka byla přidána do kvízu.");
    } catch (error) {
      setQuestionError(
        messageFromError(
          error,
          "Otázku se nepodařilo přidat. Zadaný obsah zůstává ve formuláři.",
        ),
      );
    } finally {
      setQuestionSaving(false);
    }
  }

  async function updateQuestion(questionId: string | number, input: QuestionInput) {
    setQuestionSaving(true);
    setQuestionError(null);
    setQuestionNotice(null);
    try {
      await quizService.updateQuestion(uuid, questionId, input);
      await reloadQuestions();
      setEditingId(null);
      setQuestionNotice("Změny otázky byly uloženy.");
    } catch (error) {
      const detail = messageFromError(error, "Server změnu nepřijal.");
      setQuestionError(
        `Změnu otázky se nepodařilo uložit. Původní verze zůstává zachovaná. ${detail}`,
      );
    } finally {
      setQuestionSaving(false);
    }
  }

  async function deleteQuestion(questionId: string | number) {
    setDeletingId(questionId);
    setQuestionError(null);
    setQuestionNotice(null);
    try {
      await quizService.removeQuestion(uuid, questionId);
      await reloadQuestions();
      setConfirmDeleteId(null);
      setEditingId((current) => (current === questionId ? null : current));
      setQuestionNotice("Otázka byla smazána.");
    } catch (error) {
      setQuestionError(
        messageFromError(error, "Otázku se nepodařilo smazat."),
      );
    } finally {
      setDeletingId(null);
    }
  }

  if (loading) return <QuizWorkspaceSkeleton />;

  if (loadError || !quiz || !settings) {
    return (
      <EmptyState
        icon={ListChecks}
        heading="Kvíz není dostupný"
        description={loadError ?? "Požadovaný kvíz nebyl nalezen."}
        action={
          <div className="flex flex-wrap justify-center gap-2">
            <Button onClick={() => void loadWorkspace()}>Zkusit znovu</Button>
            <Link href="/dashboard" className={buttonStyles({ variant: "secondary" })}>
              Zpět do knihovny
            </Link>
          </div>
        }
      />
    );
  }

  return (
    <div className="grid gap-7">
      <header className="grid gap-4 border-b border-border pb-6">
        <Link
          href="/dashboard"
          className={cn(buttonStyles({ variant: "quiet", size: "sm" }), "w-fit px-2")}
        >
          <ArrowLeft aria-hidden="true" className="size-4" />
          Knihovna kvízů
        </Link>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-sm font-semibold text-brand-text">Pracovní prostor</p>
              <QuizStatusBadge status={quiz.status} />
            </div>
            <h1 className="mt-2 text-3xl font-semibold tracking-[-0.025em] text-balance text-foreground sm:text-4xl">
              {quiz.title}
            </h1>
            <p className="mt-2 max-w-3xl text-base leading-7 text-muted-foreground">
              {quiz.description || "Kvíz zatím nemá popis."}
            </p>
          </div>
          <a href="#questions" className={buttonStyles({ variant: "secondary" })}>
            <span className="tabular-nums">{questionCountLabel(questions.length)}</span>
          </a>
        </div>
      </header>

      <nav aria-label="Sekce kvízu" className="rounded-lg border border-border bg-surface p-1.5">
        <ul className="flex flex-wrap gap-1">
          <li>
            <a href="#overview" className={buttonStyles({ variant: "quiet", size: "sm" })}>
              Přehled
            </a>
          </li>
          <li>
            <a href="#settings" className={buttonStyles({ variant: "quiet", size: "sm" })}>
              <Settings2 aria-hidden="true" className="size-4" />
              Nastavení
            </a>
          </li>
          <li>
            <a href="#questions" className={buttonStyles({ variant: "quiet", size: "sm" })}>
              <ListChecks aria-hidden="true" className="size-4" />
              Otázky
            </a>
          </li>
        </ul>
      </nav>

      <section id="overview" aria-labelledby="overview-title" className="scroll-mt-24">
        <Panel tone="subtle">
          <PanelHeader>
            <PanelTitle id="overview-title">Přehled</PanelTitle>
          </PanelHeader>
          <PanelContent>
            <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-2 lg:grid-cols-4">
              <div>
                <dt className="text-sm text-muted-foreground">Předmět</dt>
                <dd className="mt-1 font-semibold text-foreground">{quiz.subject || "Bez zařazení"}</dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">Kapitola</dt>
                <dd className="mt-1 font-semibold text-foreground">{quiz.chapter || "Bez zařazení"}</dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">Otázky v relaci</dt>
                <dd className="mt-1 font-semibold tabular-nums text-foreground">
                  nejvýše {quiz.maxQuestionsPerSession}
                </dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">Poslední úprava</dt>
                <dd className="mt-1 font-semibold text-foreground">
                  {formatDateTime(quiz.editedAt ?? quiz.createdAt)}
                </dd>
              </div>
            </dl>
          </PanelContent>
        </Panel>
      </section>

      <section id="settings" aria-labelledby="settings-title" className="scroll-mt-24">
        <form noValidate onSubmit={saveSettings}>
          <Panel>
            <PanelHeader>
              <PanelTitle id="settings-title">Nastavení kvízu</PanelTitle>
              <PanelDescription>
                Změny se projeví při příštím spuštění relace.
              </PanelDescription>
            </PanelHeader>
            <PanelContent className="grid gap-5">
              {settingsSubmitted ? (
                <ErrorSummary errors={settingsErrors as Record<string, string>} />
              ) : null}
              {settingsError ? <Notice tone="error">{settingsError}</Notice> : null}
              {settingsSaved ? <Notice tone="success">Nastavení bylo uloženo.</Notice> : null}
              <QuizFields
                value={settings}
                errors={visibleSettingsErrors}
                onChange={(next) => {
                  setSettings(next);
                  setSettingsSaved(false);
                }}
                onBlur={(field) =>
                  setSettingsTouched((current) => new Set(current).add(field))
                }
              />
            </PanelContent>
            <PanelFooter>
              <Button
                type="submit"
                isLoading={settingsSaving}
                leadingIcon={<Save className="size-4" />}
              >
                Uložit nastavení
              </Button>
            </PanelFooter>
          </Panel>
        </form>
      </section>

      <section id="questions" aria-labelledby="questions-title" className="grid scroll-mt-24 gap-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h2 id="questions-title" className="text-2xl font-semibold tracking-[-0.015em] text-foreground">
              Otázky
            </h2>
            <p className="mt-1 text-sm leading-6 text-muted-foreground">
              Spravujte znění, typ odpovědi, bodování a časový limit.
            </p>
          </div>
          <Button
            leadingIcon={<Plus className="size-4" />}
            disabled={creating}
            onClick={() => {
              setCreating(true);
              setEditingId(null);
              setQuestionError(null);
              setQuestionNotice(null);
              window.requestAnimationFrame(() => {
                document.getElementById("new-question")?.scrollIntoView({ block: "start" });
              });
            }}
          >
            Přidat otázku
          </Button>
        </div>

        {questionError && !creating && editingId === null ? (
          <Notice tone="error">{questionError}</Notice>
        ) : null}
        {questionNotice ? <Notice tone="success">{questionNotice}</Notice> : null}

        {creating ? (
          <Panel id="new-question" className="scroll-mt-24 border-brand/45">
            <PanelHeader>
              <PanelTitle>Nová otázka</PanelTitle>
              <PanelDescription>
                Zvolte typ odpovědi a doplňte všechna povinná pole.
              </PanelDescription>
            </PanelHeader>
            <PanelContent>
              <QuestionEditor
                busy={questionSaving}
                requestError={questionError}
                onSubmit={createQuestion}
                onCancel={() => {
                  setCreating(false);
                  setQuestionError(null);
                }}
              />
            </PanelContent>
          </Panel>
        ) : null}

        {questions.length === 0 && !creating ? (
          <EmptyState
            icon={ListChecks}
            heading="Kvíz zatím nemá otázky"
            description="Přidejte alespoň jednu otázku, aby bylo možné spustit relaci pro žáky."
            action={
              <Button
                leadingIcon={<Plus className="size-4" />}
                onClick={() => setCreating(true)}
              >
                Přidat první otázku
              </Button>
            }
          />
        ) : (
          <ol className="grid gap-3">
            {questions.map((question, index) => {
              const questionId = question.id;
              const editing = questionId !== undefined && editingId === questionId;
              return (
                <li key={questionId ?? `${question.question}-${index}`}>
                  <Panel className={cn(editing && "border-brand/45")}>
                    <PanelHeader className="flex-row items-start justify-between gap-4">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <Badge variant="outline">Otázka {index + 1}</Badge>
                          <Badge variant="neutral">{questionTypeLabels[question.type]}</Badge>
                          <span className="inline-flex items-center gap-1 text-xs font-medium text-muted-foreground">
                            <Clock3 aria-hidden="true" className="size-3.5" />
                            <span className="tabular-nums">{question.timeInSeconds} s</span>
                          </span>
                          <span className="text-xs font-medium tabular-nums text-muted-foreground">
                            {question.points} b.
                          </span>
                        </div>
                        {!editing ? (
                          <PanelTitle as="h3" className="mt-3">
                            {question.question}
                          </PanelTitle>
                        ) : null}
                      </div>
                      {!editing ? (
                        <div className="flex shrink-0 gap-1">
                          <Button
                            variant="quiet"
                            size="icon"
                            aria-label={`Upravit otázku ${index + 1}`}
                            disabled={questionId === undefined || editingId !== null}
                            onClick={() => {
                              if (questionId === undefined) return;
                              setEditingId(questionId);
                              setCreating(false);
                              setQuestionError(null);
                              setQuestionNotice(null);
                            }}
                          >
                            <Pencil aria-hidden="true" className="size-4" />
                          </Button>
                          <Button
                            variant="quiet"
                            size="icon"
                            aria-label={`Smazat otázku ${index + 1}`}
                            disabled={questionId === undefined || editingId !== null}
                            onClick={() => {
                              if (questionId !== undefined) setConfirmDeleteId(questionId);
                            }}
                          >
                            <Trash2 aria-hidden="true" className="size-4" />
                          </Button>
                        </div>
                      ) : null}
                    </PanelHeader>
                    <PanelContent className="grid gap-4">
                      {editing && questionId !== undefined ? (
                        <QuestionEditor
                          key={questionId}
                          initialQuestion={question}
                          busy={questionSaving}
                          requestError={questionError}
                          onSubmit={(input) => updateQuestion(questionId, input)}
                          onCancel={() => {
                            setEditingId(null);
                            setQuestionError(null);
                          }}
                        />
                      ) : (
                        <QuestionContent question={question} />
                      )}
                      {confirmDeleteId === questionId && questionId !== undefined ? (
                        <div className="flex flex-col gap-3 rounded-md border border-danger/45 bg-danger-subtle px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
                          <p className="text-sm font-medium text-danger-text">
                            Smazání je trvalé. Opravdu chcete tuto otázku odstranit?
                          </p>
                          <div className="flex shrink-0 gap-2">
                            <Button
                              variant="quiet"
                              size="sm"
                              onClick={() => setConfirmDeleteId(null)}
                            >
                              Ponechat
                            </Button>
                            <Button
                              variant="danger"
                              size="sm"
                              isLoading={deletingId === questionId}
                              leadingIcon={<Trash2 className="size-4" />}
                              onClick={() => void deleteQuestion(questionId)}
                            >
                              Smazat otázku
                            </Button>
                          </div>
                        </div>
                      ) : null}
                    </PanelContent>
                  </Panel>
                </li>
              );
            })}
          </ol>
        )}
      </section>
    </div>
  );
}
