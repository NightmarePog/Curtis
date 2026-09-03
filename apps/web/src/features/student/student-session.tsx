"use client";

import {
  ArrowLeft,
  ArrowRight,
  BookOpen,
  Check,
  CheckCircle2,
  CircleAlert,
  Clock3,
  History,
  ListChecks,
  RefreshCw,
  UserRound,
} from "lucide-react";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import { useRouter } from "next/navigation";
import {
  type FormEvent,
  type ReactNode,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
} from "react";

import {
  Badge,
  Button,
  EmptyState,
  Field,
  Panel,
  PanelContent,
  PanelFooter,
  PanelHeader,
  SelectField,
  Textarea,
} from "@/components/ui";
import { StudentSessionSkeleton } from "@/components/page-skeletons";
import { cn } from "@/lib/cn";
import { API_BASE } from "@/lib/constants";
import { isNoMoreQuestions, messageFromError } from "@/lib/http";
import { sessionService } from "@/lib/services";
import type {
  ActiveSession,
  FinishResult,
  MatchingPair,
  Question,
  QuestionSubmission,
} from "@/types/domain";

import { ScoreRing } from "./score-ring";

type SessionStage =
  | { status: "joining" }
  | { status: "question"; question: Question; index: number }
  | { status: "finishing" }
  | { status: "finish-error"; message: string }
  | { status: "result"; result: FinishResult }
  | { status: "error"; message: string };

interface TimerState {
  announcement: string;
  questionKey: string | null;
  remaining: number | null;
  total: number | null;
}

function secondsPhrase(seconds: number) {
  if (seconds === 1) return "1 sekundu";
  if (seconds >= 2 && seconds <= 4) return `${seconds} sekundy`;
  return `${seconds} sekund`;
}

function pointsLabel(points: number) {
  if (points === 1) return "1 bod";
  if (points >= 2 && points <= 4) return `${points} body`;
  return `${points} bodů`;
}

function questionImageSource(imageRef: string) {
  const source = imageRef.trim();
  if (
    source.startsWith("/") ||
    /^https?:\/\//i.test(source) ||
    /^data:image\//i.test(source) ||
    /^blob:/i.test(source)
  ) {
    return source;
  }

  return `${API_BASE}/v1/media/${encodeURIComponent(source)}`;
}

function useQuestionTimer(
  duration: number | null | undefined,
  questionKey: string | null,
  questionNumber: number,
): TimerState {
  const [timer, setTimer] = useState<TimerState>({
    announcement: "",
    questionKey: null,
    remaining: null,
    total: null,
  });

  useEffect(() => {
    if (!questionKey || !duration || duration <= 0) {
      const reset = window.setTimeout(
        () =>
          setTimer({
            announcement: "",
            questionKey,
            remaining: null,
            total: null,
          }),
        0,
      );
      return () => window.clearTimeout(reset);
    }

    const total = Math.max(1, Math.ceil(duration));
    const deadline = Date.now() + total * 1_000;
    const milestones = [60, 30, 10, 5, 0].filter(
      (milestone) => milestone < total,
    );
    let previous = total;

    const initialUpdate = window.setTimeout(
      () =>
        setTimer({
          announcement: `Otázka ${questionNumber}. Na odpověď máš ${secondsPhrase(total)}.`,
          questionKey,
          remaining: total,
          total,
        }),
      0,
    );

    const update = () => {
      const remaining = Math.max(0, Math.ceil((deadline - Date.now()) / 1_000));
      if (remaining === previous) return;

      const crossed = milestones.filter(
        (milestone) => previous > milestone && remaining <= milestone,
      );
      const milestone = crossed.at(-1);
      let announcement = "";
      if (milestone === 0) {
        announcement = "Čas na odpověď vypršel.";
      } else if (milestone !== undefined) {
        announcement = `Zbývá ${secondsPhrase(milestone)}.`;
      }

      previous = remaining;
      setTimer((current) => ({
        announcement: announcement || current.announcement,
        questionKey,
        remaining,
        total,
      }));

      if (remaining === 0) window.clearInterval(interval);
    };

    const interval = window.setInterval(update, 250);
    return () => {
      window.clearTimeout(initialUpdate);
      window.clearInterval(interval);
    };
  }, [duration, questionKey, questionNumber]);

  if (timer.questionKey === questionKey) return timer;
  if (!questionKey || !duration || duration <= 0) {
    return {
      announcement: "",
      questionKey,
      remaining: null,
      total: null,
    };
  }

  const total = Math.max(1, Math.ceil(duration));
  return {
    announcement: "",
    questionKey,
    remaining: total,
    total,
  };
}

function formatTimer(seconds: number | null) {
  if (seconds === null) return "Bez limitu";
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `${minutes}:${rest.toString().padStart(2, "0")}`;
}

function stableRightOptions(pairs: MatchingPair[], seed: string) {
  const options = pairs.map((pair, index) => ({
    index,
    label: pair.right,
  }));
  if (options.length <= 1) return options;

  let hash = 0;
  for (let index = 0; index < seed.length; index += 1) {
    hash = (hash * 31 + seed.charCodeAt(index)) | 0;
  }
  const offset = (Math.abs(hash) % (options.length - 1)) + 1;
  return options.map((_, index) => options[(index + offset) % options.length]);
}

function SessionTransition({
  children,
  transitionKey,
}: {
  children: ReactNode;
  transitionKey: string;
}) {
  const reduceMotion = useReducedMotion();

  return (
    <AnimatePresence initial={false} mode="wait">
      <motion.div
        key={transitionKey}
        initial={reduceMotion ? false : { opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        exit={reduceMotion ? { opacity: 1 } : { opacity: 0, y: -5 }}
        transition={{ duration: reduceMotion ? 0 : 0.18, ease: "easeOut" }}
      >
        {children}
      </motion.div>
    </AnimatePresence>
  );
}

function QuestionTimer({ timer }: { timer: TimerState }) {
  const percentage =
    timer.remaining !== null && timer.total
      ? Math.max(0, Math.min(100, (timer.remaining / timer.total) * 100))
      : 100;
  const expired = timer.remaining === 0;

  return (
    <div className="min-w-32 rounded-md border border-border bg-surface px-3 py-2">
      <div
        role="timer"
        aria-label={
          timer.remaining === null
            ? "Otázka nemá časový limit"
            : expired
              ? "Čas vypršel"
              : `Zbývá ${secondsPhrase(timer.remaining)}`
        }
        className="flex items-center justify-between gap-3"
      >
        <Clock3 aria-hidden="true" className="size-4 text-muted-foreground" />
        <span
          aria-hidden="true"
          className={cn(
            "font-mono text-sm font-semibold tabular-nums",
            expired ? "text-muted-foreground" : "text-foreground",
          )}
        >
          {formatTimer(timer.remaining)}
        </span>
      </div>
      {timer.remaining !== null ? (
        <div aria-hidden="true" className="mt-2 h-1 overflow-hidden rounded-full bg-border">
          <div
            className={cn(
              "h-full w-full origin-left rounded-full transition-transform duration-200 motion-reduce:transition-none",
              expired ? "bg-border-strong" : "bg-brand",
            )}
            style={{ transform: `scaleX(${percentage / 100})` }}
          />
        </div>
      ) : null}
      <p className="sr-only" aria-live="polite" aria-atomic="true">
        {timer.announcement}
      </p>
    </div>
  );
}

function SessionContext({ session }: { session: ActiveSession }) {
  const context = [session.subject, session.chapter].filter(Boolean).join(" · ");

  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-muted-foreground">
      {context ? (
        <span className="inline-flex min-w-0 items-center gap-1.5">
          <BookOpen aria-hidden="true" className="size-4 shrink-0" />
          <span className="truncate">{context}</span>
        </span>
      ) : null}
      <span className="inline-flex items-center gap-1.5">
        <UserRound aria-hidden="true" className="size-4" />
        {session.teacherName}
      </span>
    </div>
  );
}

export interface StudentSessionProps {
  sessionUuid: string;
  questionCount?: number;
  onExit?: () => void;
  onOpenResults?: () => void;
  multipleChoiceMode?: "single" | "multiple";
}

export function StudentSession({
  multipleChoiceMode = "multiple",
  onExit,
  onOpenResults,
  questionCount,
  sessionUuid,
}: StudentSessionProps) {
  const router = useRouter();
  const choiceName = useId();
  const controlPrefix = useId().replaceAll(":", "");
  const answerHelpId = useId();
  const answerErrorId = useId();
  const errorRef = useRef<HTMLDivElement>(null);
  const questionHeadingRef = useRef<HTMLHeadingElement>(null);
  const runRef = useRef(0);
  const [stage, setStage] = useState<SessionStage>({ status: "joining" });
  const [session, setSession] = useState<ActiveSession | null>(null);
  const [resolvedQuestionCount, setResolvedQuestionCount] = useState(questionCount);
  const [selectedIndexes, setSelectedIndexes] = useState<number[]>([]);
  const [matchingSelections, setMatchingSelections] = useState<Array<number | null>>(
    [],
  );
  const [freeText, setFreeText] = useState("");
  const [answerError, setAnswerError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const prepareQuestion = useCallback((questionToShow: Question, index: number) => {
    setSelectedIndexes([]);
    setMatchingSelections(
      Array.from({ length: questionToShow.pairs.length }, () => null),
    );
    setFreeText("");
    setAnswerError("");
    setStage({ status: "question", question: questionToShow, index });
  }, []);

  const finishAttempt = useCallback(async () => {
    setStage({ status: "finishing" });
    try {
      const result = await sessionService.finish(sessionUuid);
      setStage({ status: "result", result });
    } catch (finishError) {
      setStage({
        status: "finish-error",
        message: messageFromError(
          finishError,
          "Odpovědi jsou uložené, ale výsledek se nepodařilo načíst.",
        ),
      });
    }
  }, [sessionUuid]);

  const startSession = useCallback(async () => {
    const run = runRef.current + 1;
    runRef.current = run;
    setStage({ status: "joining" });
    setSession(null);
    setResolvedQuestionCount(questionCount);

    const [joinResponse, activeResponse] = await Promise.allSettled([
      sessionService.join(sessionUuid),
      sessionService.active(),
    ]);
    if (run !== runRef.current) return;

    if (activeResponse.status === "fulfilled") {
      const matchingSession = activeResponse.value.find(
        (candidate) => candidate.sessionUuid === sessionUuid,
      );
      if (matchingSession) {
        setSession(matchingSession);
        setResolvedQuestionCount(questionCount ?? matchingSession.questionCount);
      }
    }

    if (joinResponse.status === "rejected") {
      setStage({
        status: "error",
        message: messageFromError(
          joinResponse.reason,
          "K této relaci se teď nepodařilo připojit.",
        ),
      });
      return;
    }

    if (joinResponse.value === null) {
      await finishAttempt();
      return;
    }

    prepareQuestion(joinResponse.value, 0);
  }, [finishAttempt, prepareQuestion, questionCount, sessionUuid]);

  useEffect(() => {
    const start = window.setTimeout(() => void startSession(), 0);
    return () => {
      window.clearTimeout(start);
      runRef.current += 1;
    };
  }, [startSession]);

  const activeQuestion = stage.status === "question" ? stage.question : null;
  const activeIndex = stage.status === "question" ? stage.index : 0;
  const questionFocusKey = stage.status === "question" ? stage.index : -1;
  const timer = useQuestionTimer(
    activeQuestion?.timeInSeconds,
    activeQuestion ? `${sessionUuid}:${activeIndex}` : null,
    activeIndex + 1,
  );
  const expired = timer.remaining === 0;

  useEffect(() => {
    if (questionFocusKey >= 0) questionHeadingRef.current?.focus();
  }, [questionFocusKey]);

  const rightOptions = useMemo(
    () =>
      activeQuestion?.type === "MATCHING"
        ? stableRightOptions(activeQuestion.pairs, activeQuestion.question)
        : [],
    [activeQuestion],
  );

  const focusError = (message: string) => {
    setAnswerError(message);
    window.requestAnimationFrame(() => errorRef.current?.focus());
  };

  const submitAnswer = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (stage.status !== "question" || submitting) return;

    const { index, question } = stage;
    let submission: QuestionSubmission;

    if (question.type === "MULTIPLE_CHOICE") {
      if (selectedIndexes.length === 0 && !expired) {
        focusError(
          multipleChoiceMode === "single"
            ? "Vyber jednu odpověď."
            : "Vyber alespoň jednu odpověď.",
        );
        return;
      }
      submission = { type: "MULTIPLE_CHOICE", selectedIndexes };
    } else if (question.type === "MATCHING") {
      if (matchingSelections.some((selection) => selection === null)) {
        focusError("Doplň všechna přiřazení, než budeš pokračovat.");
        return;
      }
      submission = {
        type: "MATCHING",
        pairs: matchingSelections.map((rightIndex, leftIndex) => ({
          leftIndex,
          rightIndex: rightIndex as number,
        })),
      };
    } else {
      if (!freeText.trim() && !expired) {
        focusError("Napiš odpověď, než budeš pokračovat.");
        return;
      }
      submission = { type: "FREE_TEXT", text: freeText.trim() };
    }

    setSubmitting(true);
    setAnswerError("");
    try {
      const nextQuestion = await sessionService.next(sessionUuid, submission);
      prepareQuestion(nextQuestion, index + 1);
    } catch (submitError) {
      if (isNoMoreQuestions(submitError)) {
        await finishAttempt();
      } else {
        focusError(
          messageFromError(
            submitError,
            "Odpověď se nepodařilo uložit. Zkus to ještě jednou.",
          ),
        );
      }
    } finally {
      setSubmitting(false);
    }
  };

  const toggleChoice = (answerIndex: number) => {
    setAnswerError("");
    setSelectedIndexes((current) => {
      if (multipleChoiceMode === "single") return [answerIndex];
      return current.includes(answerIndex)
        ? current.filter((index) => index !== answerIndex)
        : [...current, answerIndex];
    });
  };

  const updateMatch = (leftIndex: number, value: string) => {
    setAnswerError("");
    setMatchingSelections((current) => {
      const next = [...current];
      next[leftIndex] = value === "" ? null : Number(value);
      return next;
    });
  };

  const exitSession = () => {
    if (onExit) {
      onExit();
      return;
    }
    router.push("/dashboard");
  };

  const openResults = () => {
    if (onOpenResults) {
      onOpenResults();
      return;
    }
    router.push("/results");
  };

  const transitionKey =
    stage.status === "question" ? `question-${stage.index}` : stage.status;

  return (
    <div className="mx-auto w-full max-w-[52rem]">
      <SessionTransition transitionKey={transitionKey}>
        {stage.status === "joining" ? (
          <StudentSessionSkeleton />
        ) : stage.status === "error" ? (
          <>
            <Button
              variant="quiet"
              size="sm"
              className="mb-4 -ml-3"
              onClick={exitSession}
              leadingIcon={<ArrowLeft className="size-4" />}
            >
              Zpět na přehled
            </Button>
            <EmptyState
              heading="Ke kvízu se nejde připojit"
              description={stage.message}
              icon={CircleAlert}
              action={
                <Button
                  onClick={() => void startSession()}
                  leadingIcon={<RefreshCw className="size-[1.125rem]" />}
                >
                  Zkusit znovu
                </Button>
              }
            />
          </>
        ) : stage.status === "finishing" ? (
          <StudentSessionSkeleton phase="result" />
        ) : stage.status === "finish-error" ? (
          <EmptyState
            heading="Výsledek se nepodařilo načíst"
            description={stage.message}
            icon={CircleAlert}
            action={
              <div className="flex flex-wrap justify-center gap-3">
                <Button
                  onClick={() => void finishAttempt()}
                  leadingIcon={<RefreshCw className="size-[1.125rem]" />}
                >
                  Načíst výsledek
                </Button>
                <Button variant="secondary" onClick={exitSession}>
                  Zpět na přehled
                </Button>
              </div>
            }
          />
        ) : stage.status === "result" ? (
          <ResultView
            result={stage.result}
            title={session?.title}
            onExit={exitSession}
            onOpenResults={openResults}
          />
        ) : (
          <form noValidate onSubmit={submitAnswer}>
            <Panel className="overflow-hidden">
              <PanelHeader className="gap-5 border-b border-border pb-5 sm:pb-6">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="mb-2 flex flex-wrap items-center gap-2">
                      <Badge variant="brand">
                        Otázka {stage.index + 1}
                        {resolvedQuestionCount
                          ? ` z ${resolvedQuestionCount}`
                          : ""}
                      </Badge>
                      <Badge variant="outline">
                        {pointsLabel(stage.question.points)}
                      </Badge>
                    </div>
                    {resolvedQuestionCount ? (
                      <div
                        role="progressbar"
                        aria-label="Postup kvízem"
                        aria-valuemin={1}
                        aria-valuemax={resolvedQuestionCount}
                        aria-valuenow={Math.min(
                          stage.index + 1,
                          resolvedQuestionCount,
                        )}
                        className="h-1.5 max-w-sm overflow-hidden rounded-full bg-border"
                      >
                        <div
                          aria-hidden="true"
                          className="h-full w-full origin-left rounded-full bg-brand transition-transform duration-200 motion-reduce:transition-none"
                          style={{
                            transform: `scaleX(${Math.min(
                              1,
                              (stage.index + 1) / resolvedQuestionCount,
                            )})`,
                          }}
                        />
                      </div>
                    ) : null}
                  </div>
                  <QuestionTimer timer={timer} />
                </div>

                {session ? <SessionContext session={session} /> : null}

                <div>
                  <h1
                    ref={questionHeadingRef}
                    tabIndex={-1}
                    className="max-w-[28ch] scroll-mt-5 text-2xl leading-tight font-semibold tracking-[-0.02em] text-balance text-foreground outline-none sm:text-3xl"
                  >
                    {stage.question.question}
                  </h1>
                  <p id={answerHelpId} className="mt-3 text-sm leading-6 text-muted-foreground">
                    {stage.question.type === "MULTIPLE_CHOICE"
                      ? multipleChoiceMode === "single"
                        ? "Vyber jednu možnost a pokračuj tlačítkem dole."
                        : "Vyber jednu nebo více možností a pokračuj tlačítkem dole."
                      : stage.question.type === "MATCHING"
                        ? "Ke každému pojmu vyber odpovídající možnost. Každou lze použít jednou."
                        : "Odpověz vlastními slovy. Text po odevzdání ohodnotí vyučující."}
                  </p>
                </div>
              </PanelHeader>

              <PanelContent className="grid gap-5">
                {stage.question.codeSnippet ? (
                  <pre
                    tabIndex={0}
                    aria-label="Ukázka k otázce"
                    className="overflow-x-auto rounded-md border border-border bg-field p-4 text-sm leading-6 text-foreground"
                  >
                    <code>{stage.question.codeSnippet}</code>
                  </pre>
                ) : null}

                {stage.question.imageRef?.trim() ? (
                  <figure className="overflow-hidden rounded-md border border-border bg-surface">
                    {/* A question image can be served by the API or from a local asset path. */}
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img
                      src={questionImageSource(stage.question.imageRef)}
                      alt="Obrázek k otázce"
                      width={1200}
                      height={800}
                      decoding="async"
                      className="max-h-[28rem] w-full object-contain"
                    />
                  </figure>
                ) : null}

                {stage.question.type === "MULTIPLE_CHOICE" ? (
                  <fieldset
                    aria-describedby={`${answerHelpId}${answerError ? ` ${answerErrorId}` : ""}`}
                    disabled={submitting}
                    className="grid gap-3"
                  >
                    <legend className="sr-only">Možnosti odpovědi</legend>
                    {stage.question.answers.map((answer, answerIndex) => {
                      const selected = selectedIndexes.includes(answerIndex);
                      return (
                        <label
                          key={`${answer.answer ?? "odpověď"}-${answerIndex}`}
                          htmlFor={`${controlPrefix}-choice-${answerIndex}`}
                          className={cn(
                            "flex min-h-11 items-center gap-3 rounded-md border px-3.5 py-3 text-left",
                            "transition-colors duration-150 motion-reduce:transition-none",
                            selected
                              ? "border-brand bg-brand-subtle text-foreground"
                              : "border-border-strong bg-surface-raised text-foreground hover:border-brand/60 hover:bg-surface-subtle",
                            "cursor-pointer active:bg-surface-subtle has-[input:disabled]:cursor-not-allowed has-[input:disabled]:opacity-50",
                          )}
                        >
                          <input
                            id={`${controlPrefix}-choice-${answerIndex}`}
                            type={
                              multipleChoiceMode === "single" ? "radio" : "checkbox"
                            }
                            name={choiceName}
                            value={answerIndex}
                            checked={selected}
                            onChange={() => toggleChoice(answerIndex)}
                            aria-describedby={`${answerHelpId}${
                              answerError ? ` ${answerErrorId}` : ""
                            }`}
                            className="peer sr-only"
                          />
                          <span
                            aria-hidden="true"
                            className={cn(
                              "grid size-5 shrink-0 place-items-center border",
                              "peer-focus-visible:ring-[3px] peer-focus-visible:ring-ring/45 peer-focus-visible:ring-offset-2 peer-focus-visible:ring-offset-panel",
                              multipleChoiceMode === "single"
                                ? "rounded-full"
                                : "rounded-[0.3rem]",
                              selected
                                ? "border-brand bg-brand text-brand-foreground"
                                : "border-border-strong bg-field",
                            )}
                          >
                            {selected ? (
                              multipleChoiceMode === "single" ? (
                                <span className="size-2 rounded-full bg-current" />
                              ) : (
                                <Check className="size-3.5" strokeWidth={3} />
                              )
                            ) : null}
                          </span>
                          <span
                            className={cn(
                              "min-w-0 flex-1 leading-6",
                              selected && "font-semibold",
                            )}
                          >
                            {answer.answer || `Možnost ${answerIndex + 1}`}
                          </span>
                          {selected ? (
                            <span className="shrink-0 text-xs font-semibold text-brand-text">
                              Vybráno
                            </span>
                          ) : null}
                        </label>
                      );
                    })}
                  </fieldset>
                ) : stage.question.type === "MATCHING" ? (
                  <div className="grid gap-4" aria-describedby={answerHelpId}>
                    {stage.question.pairs.map((pair, leftIndex) => (
                      <Field
                        key={`${pair.left}-${leftIndex}`}
                        label={pair.left}
                        controlId={`${controlPrefix}-match-${leftIndex}`}
                      >
                        <SelectField
                          value={
                            matchingSelections[leftIndex] === null
                              ? undefined
                              : String(matchingSelections[leftIndex])
                          }
                          onValueChange={(value) => updateMatch(leftIndex, value)}
                          disabled={submitting}
                          aria-describedby={`${answerHelpId}${
                            answerError ? ` ${answerErrorId}` : ""
                          }`}
                          invalid={Boolean(answerError)}
                          placeholder="Vyber odpovídající možnost"
                          options={rightOptions.map((option) => {
                            const usedElsewhere = matchingSelections.some(
                              (selection, selectionIndex) =>
                                selectionIndex !== leftIndex && selection === option.index,
                            );
                            return {
                              value: String(option.index),
                              label: option.label,
                              disabled: usedElsewhere,
                            };
                          })}
                        />
                      </Field>
                    ))}
                  </div>
                ) : (
                  <Field
                    label="Tvoje odpověď"
                    description="Piš stručně a věcně. Odpověď po dokončení zkontroluje vyučující."
                    controlId={`${controlPrefix}-free-text-answer`}
                  >
                    <Textarea
                      value={freeText}
                      onChange={(event) => {
                        setFreeText(event.target.value);
                        setAnswerError("");
                      }}
                      disabled={submitting}
                      invalid={Boolean(answerError)}
                      aria-describedby={`${answerHelpId}${
                        answerError ? ` ${answerErrorId}` : ""
                      }`}
                      rows={6}
                      autoComplete="off"
                    />
                  </Field>
                )}

                {expired ? (
                  <p className="flex items-start gap-2 rounded-md border border-border bg-surface px-3.5 py-3 text-sm leading-5 text-muted-foreground">
                    <Clock3 aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
                    Čas vypršel. Odpověď můžeš dokončit, ale už se nemusí započítat.
                  </p>
                ) : null}

                {answerError ? (
                  <div
                    ref={errorRef}
                    id={answerErrorId}
                    role="alert"
                    tabIndex={-1}
                    className="flex items-start gap-2 rounded-md border border-danger/40 bg-danger-subtle px-3.5 py-3 text-sm leading-5 text-danger-text outline-none focus-visible:ring-[3px] focus-visible:ring-ring/45"
                  >
                    <CircleAlert
                      aria-hidden="true"
                      className="mt-0.5 size-4 shrink-0"
                    />
                    <span>{answerError}</span>
                  </div>
                ) : null}
              </PanelContent>

              <PanelFooter className="bg-panel pb-[max(1rem,env(safe-area-inset-bottom))] sm:pb-4">
                <div className="mr-auto hidden items-center gap-2 text-sm text-muted-foreground sm:flex">
                  <ListChecks aria-hidden="true" className="size-4" />
                  Odpověď odešleš až tlačítkem.
                </div>
                <Button
                  type="submit"
                  size="lg"
                  className="w-full sm:w-auto"
                  isLoading={submitting}
                  leadingIcon={<ArrowRight className="size-[1.125rem]" />}
                >
                  {resolvedQuestionCount &&
                  stage.index + 1 >= resolvedQuestionCount
                    ? "Dokončit kvíz"
                    : "Další otázka"}
                </Button>
              </PanelFooter>
            </Panel>
          </form>
        )}
      </SessionTransition>
    </div>
  );
}

function ResultView({
  onExit,
  onOpenResults,
  result,
  title,
}: {
  onExit: () => void;
  onOpenResults: () => void;
  result: FinishResult;
  title?: string;
}) {
  const headingRef = useRef<HTMLHeadingElement>(null);
  const score = result.score;
  const awaitingSessionClose = score === null;
  const provisional = result.pendingReviewCount > 0 || result.questions.some(
    (question) => question.type === "FREE_TEXT",
  );

  useEffect(() => {
    headingRef.current?.focus();
  }, []);

  return (
    <Panel className="overflow-hidden">
      <PanelHeader className="items-center border-b border-border pb-6 text-center sm:pb-7">
        <Badge variant={provisional ? "brand" : "neutral"} className="mb-2">
          {awaitingSessionClose
            ? "Odevzdáno"
            : provisional
              ? "Průběžný výsledek"
              : "Kvíz dokončen"}
        </Badge>
        {awaitingSessionClose ? (
          <div className="my-2 grid size-28 place-items-center rounded-full border border-brand/30 bg-brand-subtle text-brand-text">
            <Clock3 aria-hidden="true" className="size-9" />
          </div>
        ) : (
          <ScoreRing
            score={score}
            maxScore={result.maxScore}
            provisional={provisional}
            className="my-2"
          />
        )}
        <h1
          ref={headingRef}
          tabIndex={-1}
          className="text-3xl leading-tight font-semibold tracking-[-0.025em] text-balance text-foreground outline-none sm:text-4xl"
        >
          {title ?? (awaitingSessionClose ? "Odpovědi jsou uložené" : "Kvíz je dokončený")}
        </h1>
        {!awaitingSessionClose ? (
          <p className="mt-2 font-mono text-xl font-semibold tabular-nums text-foreground">
            {score} z {result.maxScore} bodů
          </p>
        ) : null}
      </PanelHeader>

      <PanelContent>
        <div className="mx-auto max-w-xl text-center">
          {awaitingSessionClose ? (
            <>
              <div className="mx-auto mb-3 grid size-10 place-items-center rounded-md border border-brand/35 bg-brand-subtle text-brand-text">
                <Clock3 aria-hidden="true" className="size-5" />
              </div>
              <h2 className="text-lg font-semibold text-foreground">
                Body se zobrazí po ukončení relace
              </h2>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Odpovědi jsou bezpečně odevzdané. Výsledek a správná řešení se
                zpřístupní, až relaci ukončí vyučující nebo vyprší její čas.
              </p>
            </>
          ) : provisional ? (
            <>
              <div className="mx-auto mb-3 grid size-10 place-items-center rounded-md border border-brand/35 bg-brand-subtle text-brand-text">
                <Clock3 aria-hidden="true" className="size-5" />
              </div>
              <h2 className="text-lg font-semibold text-foreground">
                Výsledek se ještě může změnit
              </h2>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Vyučující musí ohodnotit textovou odpověď. Aktuálně vidíš jen body,
                které šlo vyhodnotit automaticky.
              </p>
            </>
          ) : (
            <>
              <div className="mx-auto mb-3 grid size-10 place-items-center rounded-md border border-brand/35 bg-brand-subtle text-brand-text">
                <CheckCircle2 aria-hidden="true" className="size-5" />
              </div>
              <h2 className="text-lg font-semibold text-foreground">
                Výsledek je uložený
              </h2>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Podrobnosti najdeš kdykoli v historii svých výsledků.
              </p>
            </>
          )}
        </div>
      </PanelContent>

      <PanelFooter className="justify-center">
        <Button
          variant="secondary"
          onClick={onExit}
          leadingIcon={<ArrowLeft className="size-[1.125rem]" />}
        >
          Zpět na přehled
        </Button>
        <Button
          onClick={onOpenResults}
          leadingIcon={<History className="size-[1.125rem]" />}
        >
          Moje výsledky
        </Button>
      </PanelFooter>
    </Panel>
  );
}
