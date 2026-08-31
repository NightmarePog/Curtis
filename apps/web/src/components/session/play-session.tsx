"use client";

import { useCallback, useEffect, useState } from "react";
import Image from "next/image";
import { Flag, Send } from "lucide-react";
import { ErrorBanner, LoadingScreen } from "@/components/common/feedback";
import { AnswerOption } from "@/components/session/answer-option";
import { CountdownRing } from "@/components/session/countdown-ring";
import { SessionResults, SessionUnavailable } from "@/components/session/session-results";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api";
import { useCountdown } from "@/lib/use-countdown";
import type { QuestionResponse, QuestionSubmission, ResultsResponse } from "@/lib/types";

type Phase = "joining" | "playing" | "finishing";

function mediaUrl(imageRef: string) {
  return imageRef.startsWith("http")
    ? imageRef
    : `/media/${encodeURIComponent(imageRef)}`;
}

export function PlaySession({ sessionUuid }: { sessionUuid: string }) {
  const [phase, setPhase] = useState<Phase>("joining");
  const [question, setQuestion] = useState<QuestionResponse | null>(null);
  const [questionNumber, setQuestionNumber] = useState(0);
  const [selected, setSelected] = useState<number[]>([]);
  const [matching, setMatching] = useState<(number | null)[]>([]);
  const [text, setText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [results, setResults] = useState<ResultsResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fatal, setFatal] = useState<string | null>(null);

  function resetAnswerState(next: QuestionResponse) {
    setSelected([]);
    setText("");
    setMatching(next.pairs.map(() => null));
  }

  useEffect(() => {
    let active = true;
    api
      .joinSession(sessionUuid)
      .then((first) => {
        if (!active) return;
        setQuestion(first);
        resetAnswerState(first);
        setQuestionNumber(1);
        setPhase("playing");
      })
      .catch((err) => {
        if (!active) return;
        setFatal(err instanceof Error ? err.message : "Kód je neplatný nebo kvíz už skončil.");
      });
    return () => {
      active = false;
    };
  }, [sessionUuid]);

  const finish = useCallback(async () => {
    setSubmitting(true);
    setPhase("finishing");
    setError(null);
    try {
      setResults(await api.finishSession(sessionUuid));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nepodařilo se dokončit kvíz");
      setSubmitting(false);
    }
  }, [sessionUuid]);

  const buildSubmission = useCallback((): QuestionSubmission => {
    if (!question || question.type === "MULTIPLE_CHOICE") {
      return { type: "MULTIPLE_CHOICE", selectedIndexes: selected };
    }
    if (question.type === "FREE_TEXT") {
      return { type: "FREE_TEXT", text };
    }
    return {
      type: "MATCHING",
      pairs: matching.map((rightIndex, leftIndex) => ({
        leftIndex,
        rightIndex: rightIndex ?? -1,
      })),
    };
  }, [matching, question, selected, text]);

  const canSubmit =
    question?.type !== "MATCHING" || matching.length > 0 && matching.every((index) => index != null);

  const submit = useCallback(
    async (answer: QuestionSubmission) => {
      if (submitting || phase !== "playing") return;
      setSubmitting(true);
      setError(null);
      try {
        const next = await api.nextQuestion(sessionUuid, answer);
        setQuestion(next);
        resetAnswerState(next);
        setQuestionNumber((number) => number + 1);
        setSubmitting(false);
      } catch (err) {
        if ((err as { status?: number })?.status === 400) {
          setSubmitting(false);
          await finish();
          return;
        }
        setError(err instanceof Error ? err.message : "Odpověď se nepodařilo odeslat");
        setSubmitting(false);
      }
    },
    [finish, phase, sessionUuid, submitting]
  );

  const total = question?.timeInSeconds ?? null;
  const remaining = useCountdown(
    total,
    () => {
      if (phase !== "playing" || submitting) return;
      if (question?.type === "MATCHING" && !canSubmit) {
        void finish();
      } else {
        void submit(buildSubmission());
      }
    },
    questionNumber
  );

  if (fatal) return <SessionUnavailable message={fatal} />;
  if (results) return <SessionResults results={results} />;
  if (phase === "finishing") return <LoadingScreen label="Vyhodnocuji odpovědi…" />;
  if (phase === "joining" || !question) return <LoadingScreen label="Připojuji se do kvízu…" />;

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      {error && <ErrorBanner message={error} onDismiss={() => setError(null)} />}
      <section key={questionNumber} className="surface surface-raised animate-rise" aria-live="polite">
        <div className="flex items-center justify-between gap-4 border-b border-border px-5 py-4 sm:px-6">
          <div className="min-w-0">
            <Badge variant="blue">Otázka {questionNumber}</Badge>
            <h1 className="mt-2.5 text-pretty text-xl font-semibold leading-snug tracking-tight text-foreground sm:text-2xl">
              {question.question}
            </h1>
          </div>
          {remaining != null && total != null && <CountdownRing remaining={remaining} total={total} />}
        </div>

        <div className="space-y-4 p-5 sm:p-6">
          {question.imageRef && (
            <div className="relative h-48 w-full overflow-hidden rounded-lg border border-border">
              <Image
                src={mediaUrl(question.imageRef)}
                alt="Doplňující obrázek k otázce"
                fill
                unoptimized
                className="object-contain"
              />
            </div>
          )}
          {question.codeSnippet && (
            <pre className="overflow-x-auto rounded-lg border border-border bg-muted/50 p-3 font-mono text-sm text-foreground">
              {question.codeSnippet}
            </pre>
          )}

          {question.type === "MULTIPLE_CHOICE" && (
            <div className="space-y-2.5">
              <p className="text-xs text-muted-foreground">Vyberte všechny správné odpovědi.</p>
              {question.answers.map((answer, index) => (
                <AnswerOption
                  key={index}
                  index={index}
                  text={answer.answer}
                  selected={selected.includes(index)}
                  disabled={submitting}
                  onToggle={() =>
                    setSelected((previous) =>
                      previous.includes(index)
                        ? previous.filter((item) => item !== index)
                        : [...previous, index]
                    )
                  }
                />
              ))}
            </div>
          )}

          {question.type === "MATCHING" && (
            <div className="space-y-3">
              <p className="text-xs text-muted-foreground">Přiřaďte každou levou část ke správné pravé části.</p>
              {question.pairs.map((pair, leftIndex) => {
                const usedByOther = new Set(
                  matching.filter((_, index) => index !== leftIndex).filter((index): index is number => index != null)
                );
                return (
                  <label key={leftIndex} className="grid gap-2 sm:grid-cols-[1fr_1fr] sm:items-center">
                    <span className="rounded-lg border border-border bg-muted/30 px-3 py-2 text-sm text-foreground">
                      {pair.left}
                    </span>
                    <select
                      value={matching[leftIndex] ?? ""}
                      onChange={(event) =>
                        setMatching((previous) =>
                          previous.map((value, index) =>
                            index === leftIndex
                              ? event.target.value === "" ? null : Number(event.target.value)
                              : value
                          )
                        )
                      }
                      disabled={submitting}
                      aria-label={`Přiřazení pro ${pair.left}`}
                      className="h-11 w-full rounded-lg border border-input bg-muted/40 px-3 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 md:h-10 md:text-sm"
                    >
                      <option value="">Vyberte pravou část…</option>
                      {question.pairs.map((rightPair, rightIndex) => (
                        <option key={rightIndex} value={rightIndex} disabled={usedByOther.has(rightIndex)}>
                          {rightPair.right}
                        </option>
                      ))}
                    </select>
                  </label>
                );
              })}
            </div>
          )}

          {question.type === "FREE_TEXT" && (
            <div className="space-y-2">
              <label htmlFor="free-text-answer" className="text-xs text-muted-foreground">
                Napište vlastní odpověď. Vyučující ji po odevzdání zkontroluje.
              </label>
              <textarea
                id="free-text-answer"
                value={text}
                onChange={(event) => setText(event.target.value)}
                disabled={submitting}
                rows={5}
                placeholder="Vaše odpověď…"
                className="flex min-h-20 w-full rounded-lg border border-input bg-muted/40 px-3 py-2.5 text-base outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 md:text-sm"
              />
            </div>
          )}
        </div>

        <div className="flex flex-col-reverse items-stretch gap-2 border-t border-border bg-muted/30 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <Button
            variant="ghost"
            onClick={() => void finish()}
            disabled={submitting}
            className="text-muted-foreground hover:text-destructive"
          >
            <Flag aria-hidden="true" data-icon="inline-start" />
            Ukončit kvíz
          </Button>
          <Button
            size="lg"
            loading={submitting}
            disabled={!canSubmit}
            onClick={() => void submit(buildSubmission())}
          >
            <Send aria-hidden="true" data-icon="inline-start" />
            {question.type === "MATCHING" && !canSubmit
              ? "Doplňte přiřazení"
              : question.type === "MULTIPLE_CHOICE" && selected.length === 0
                ? "Přeskočit"
                : "Odpovědět"}
          </Button>
        </div>
      </section>
    </div>
  );
}
