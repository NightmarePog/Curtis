"use client";

import { ArrowRight, CheckCircle2, ClipboardList } from "lucide-react";
import { useState, type FormEvent } from "react";

import {
  Badge,
  Button,
  EmptyState,
  Field,
  Input,
  Panel,
  PanelContent,
  PanelHeader,
  PanelTitle,
} from "@/components/ui";
import { messageFromError } from "@/lib/http";
import type {
  PendingTextAnswer,
  Question,
  Quiz,
  QuizResult,
  StoredQuestionResult,
} from "@/types/domain";

import { Notice, formatDateTime } from "./shared";

export function participantLabel(studentId: string) {
  let hash = 2_166_136_261;
  for (const character of studentId) {
    hash ^= character.charCodeAt(0);
    hash = Math.imul(hash, 16_777_619);
  }
  const code = (hash >>> 0)
    .toString(36)
    .toUpperCase()
    .slice(-4)
    .padStart(4, "0");
  return `Žák ${code}`;
}

export function StudentIdentity({ label }: { label: string }) {
  return <span className="font-semibold text-foreground">{label}</span>;
}

function questionTypeLabel(type: StoredQuestionResult["type"]) {
  switch (type) {
    case "FREE_TEXT":
      return "Vlastní odpověď";
    case "MATCHING":
      return "Přiřazování";
    case "MULTIPLE_CHOICE":
      return "Výběr možností";
  }
}

function resolveQuestion(
  quiz: Quiz | null,
  answer: StoredQuestionResult,
): Question | null {
  if (!quiz) return null;

  if (!quiz.shuffle) {
    const indexed = quiz.questions[answer.questionIndex];
    if (indexed?.question === answer.question) return indexed;
  }

  const exactMatches = quiz.questions.filter(
    (candidate) => candidate.question === answer.question,
  );
  return exactMatches.length === 1 ? exactMatches[0] : null;
}

function answerStatus(answer: StoredQuestionResult) {
  if (answer.status === "PENDING_REVIEW") {
    return <Badge variant="brand">Čeká na hodnocení</Badge>;
  }
  if (answer.awardedPoints === answer.points) {
    return <Badge variant="brand">Plný počet bodů</Badge>;
  }
  if ((answer.awardedPoints ?? 0) > 0) {
    return <Badge variant="neutral">Částečně</Badge>;
  }
  return <Badge variant="outline">Bez bodu</Badge>;
}

function AnswerValue({
  answer,
  definition,
}: {
  answer: StoredQuestionResult;
  definition: Question | null;
}) {
  if (answer.type === "FREE_TEXT") {
    return answer.text?.trim() ? (
      <blockquote className="rounded-md border-l-4 border-brand bg-surface px-4 py-3 text-base leading-7 text-foreground">
        {answer.text}
      </blockquote>
    ) : (
      <p className="text-sm italic text-muted-foreground">Bez odpovědi</p>
    );
  }

  if (answer.type === "MULTIPLE_CHOICE") {
    if (answer.selectedIndexes.length === 0) {
      return <p className="text-sm italic text-muted-foreground">Bez odpovědi</p>;
    }
    return (
      <ul className="grid gap-2" aria-label="Vybrané možnosti">
        {answer.selectedIndexes.map((index) => (
          <li
            key={index}
            className="rounded-md border border-border bg-surface px-3 py-2.5 text-sm leading-6 text-foreground"
          >
            {answer.optionLabels?.[index] ||
              definition?.answers[index]?.answer ||
              `Možnost č. ${index + 1}`}
          </li>
        ))}
      </ul>
    );
  }

  if (answer.pairs.length === 0) {
    return <p className="text-sm italic text-muted-foreground">Bez odpovědi</p>;
  }

  return (
    <dl className="grid gap-2" aria-label="Přiřazené dvojice">
      {[...answer.pairs]
        .sort((left, right) => left.leftIndex - right.leftIndex)
        .map((pair) => (
          <div
            key={`${pair.leftIndex}-${pair.rightIndex}`}
            className="grid items-center gap-2 rounded-md border border-border bg-surface px-3 py-2.5 text-sm sm:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]"
          >
            <dt className="font-medium text-foreground">
              {answer.matchingPairLabels?.[pair.leftIndex]?.left ||
                definition?.pairs[pair.leftIndex]?.left ||
                `Položka č. ${pair.leftIndex + 1}`}
            </dt>
            <ArrowRight
              aria-hidden="true"
              className="size-4 rotate-90 text-muted-foreground sm:rotate-0"
            />
            <dd className="text-foreground">
              {answer.matchingPairLabels?.[pair.rightIndex]?.right ||
                definition?.pairs[pair.rightIndex]?.right ||
                `Možnost č. ${pair.rightIndex + 1}`}
            </dd>
          </div>
        ))}
    </dl>
  );
}

interface GradeControlProps {
  answer: PendingTextAnswer;
  onGrade: (answer: PendingTextAnswer, points: number) => Promise<void>;
}

function GradeControl({ answer, onGrade }: GradeControlProps) {
  const [points, setPoints] = useState("");
  const [touched, setTouched] = useState(false);
  const [busy, setBusy] = useState(false);
  const [requestError, setRequestError] = useState<string | null>(null);
  const parsed = Number(points);
  const validationError =
    points.trim() === ""
      ? "Zadejte počet bodů."
      : !Number.isInteger(parsed) || parsed < 0 || parsed > answer.points
        ? `Zadejte celé číslo od 0 do ${answer.points}.`
        : null;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setTouched(true);
    setRequestError(null);
    if (validationError) return;

    setBusy(true);
    try {
      await onGrade(answer, parsed);
    } catch (error) {
      setRequestError(
        messageFromError(error, "Hodnocení se nepodařilo uložit."),
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <form
      onSubmit={submit}
      className="mt-4 grid items-end gap-3 rounded-md border border-border bg-surface-subtle p-3 sm:grid-cols-[minmax(10rem,14rem)_auto]"
    >
      <Field
        label={`Přidělené body (max. ${answer.points})`}
        required
        controlId={`grade-${answer.resultId}`}
        error={touched ? validationError : undefined}
      >
        <Input
          type="number"
          inputMode="numeric"
          min={0}
          max={answer.points}
          step={1}
          value={points}
          onChange={(event) => setPoints(event.target.value)}
          onBlur={() => setTouched(true)}
        />
      </Field>
      <Button
        type="submit"
        isLoading={busy}
        leadingIcon={<CheckCircle2 className="size-4" />}
      >
        Uložit hodnocení
      </Button>
      {requestError ? (
        <Notice tone="error" className="sm:col-span-2">
          {requestError}
        </Notice>
      ) : null}
    </form>
  );
}

function pendingAnswerFor(
  result: QuizResult,
  answer: StoredQuestionResult,
  pending: Map<string | number, PendingTextAnswer>,
): PendingTextAnswer {
  return (
    pending.get(answer.id) ?? {
      resultId: answer.id,
      studentId: result.studentId,
      studentName: result.studentName,
      questionIndex: answer.questionIndex,
      question: answer.question,
      text: answer.text ?? "",
      points: answer.points,
      awardedPoints: answer.awardedPoints,
      status: answer.status,
    }
  );
}

interface StudentAnswerReviewProps {
  labels: Map<string, string>;
  onGrade: (answer: PendingTextAnswer, points: number) => Promise<void>;
  pending: PendingTextAnswer[];
  quiz: Quiz | null;
  results: QuizResult[];
}

export function StudentAnswerReview({
  labels,
  onGrade,
  pending,
  quiz,
  results,
}: StudentAnswerReviewProps) {
  const pendingById = new Map(pending.map((answer) => [answer.resultId, answer]));
  const needsOptionLabels = results.some((result) =>
    result.questionResults.some(
      (answer) =>
        answer.type !== "FREE_TEXT" &&
        !(answer.type === "MULTIPLE_CHOICE"
          ? answer.optionLabels?.length
          : answer.matchingPairLabels?.length) &&
        !resolveQuestion(quiz, answer),
    ),
  );

  return (
    <section id="student-answers" aria-labelledby="student-answers-title" className="scroll-mt-24 grid gap-4">
      <div>
        <div className="flex flex-wrap items-center gap-2">
          <h2
            id="student-answers-title"
            className="text-2xl font-semibold tracking-[-0.015em] text-foreground"
          >
            Odpovědi žáků
          </h2>
          <Badge variant="neutral">{results.length} odevzdání</Badge>
        </div>
        <p className="mt-1 max-w-3xl text-sm leading-6 text-muted-foreground">
          Projděte každou odevzdanou odpověď. Volné texty ohodnotíte přímo u
          otázky.
        </p>
      </div>

      {needsOptionLabels ? (
        <Notice>
          U některých odpovědí nejsou dostupné texty možností. Tyto volby jsou
          proto uvedené pořadovým číslem přesně podle uloženého výsledku.
        </Notice>
      ) : null}

      {results.length === 0 ? (
        <EmptyState
          compact
          icon={ClipboardList}
          heading="Zatím nejsou žádné odevzdané odpovědi"
          description="První dokončený pokus se zde objeví automaticky."
        />
      ) : (
        <div className="grid gap-4">
          {results.map((result) => {
            const hasPending = result.questionResults.some(
              (answer) => answer.status === "PENDING_REVIEW",
            );
            return (
              <Panel key={result.id} className="overflow-hidden">
                <PanelHeader className="gap-4 border-b border-border pb-5 sm:flex-row sm:items-start sm:justify-between">
                  <div className="min-w-0">
                    <PanelTitle as="h3">
                      {labels.get(result.studentId) ?? "Žák"}
                    </PanelTitle>
                    <p className="mt-2 text-sm text-muted-foreground">
                      Odevzdáno {formatDateTime(result.playedAt)}
                    </p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2 sm:justify-end">
                    <Badge variant={hasPending ? "brand" : "neutral"}>
                      {hasPending ? "Čeká na kontrolu" : "Zkontrolováno"}
                    </Badge>
                    <span className="font-mono text-sm font-semibold tabular-nums text-foreground">
                      {result.score} / {result.maxScore} b.
                    </span>
                  </div>
                </PanelHeader>
                <PanelContent className="py-0">
                  {result.questionResults.length === 0 ? (
                    <p className="py-5 text-sm text-muted-foreground">
                      Detail jednotlivých odpovědí tento uložený výsledek neobsahuje.
                    </p>
                  ) : (
                    <ol className="divide-y divide-border">
                      {result.questionResults.map((answer) => {
                        const definition = resolveQuestion(quiz, answer);
                        return (
                          <li key={answer.id} className="grid gap-4 py-5 first:pt-5 last:pb-5">
                            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                              <div className="min-w-0">
                                <div className="flex flex-wrap items-center gap-2">
                                  <span className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                                    Otázka {answer.questionIndex + 1}
                                  </span>
                                  <Badge variant="outline">
                                    {questionTypeLabel(answer.type)}
                                  </Badge>
                                </div>
                                <h4 className="mt-2 text-base leading-6 font-semibold text-foreground">
                                  {answer.question}
                                </h4>
                              </div>
                              <div className="flex shrink-0 items-center gap-2">
                                {answerStatus(answer)}
                                {answer.status !== "PENDING_REVIEW" ? (
                                  <span className="font-mono text-xs font-semibold tabular-nums text-foreground">
                                    {answer.awardedPoints ?? 0} / {answer.points} b.
                                  </span>
                                ) : null}
                              </div>
                            </div>

                            <div>
                              <p className="mb-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                                Odpověď žáka
                              </p>
                              <AnswerValue answer={answer} definition={definition} />
                            </div>

                            {answer.type === "FREE_TEXT" &&
                            answer.status === "PENDING_REVIEW" ? (
                              <GradeControl
                                answer={pendingAnswerFor(result, answer, pendingById)}
                                onGrade={onGrade}
                              />
                            ) : null}
                          </li>
                        );
                      })}
                    </ol>
                  )}
                </PanelContent>
              </Panel>
            );
          })}
        </div>
      )}
    </section>
  );
}
