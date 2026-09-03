"use client";

import {
  ArrowLeft,
  BookOpen,
  ChevronDown,
  ClipboardCheck,
  Clock3,
  RefreshCw,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";

import {
  Badge,
  Button,
  EmptyState,
  Panel,
  PanelContent,
  PanelDescription,
  PanelHeader,
  PanelTitle,
} from "@/components/ui";
import { ResultsListSkeleton } from "@/components/page-skeletons";
import { messageFromError } from "@/lib/http";
import { useLiveEvents } from "@/lib/live-events";
import { sessionService } from "@/lib/services";
import type {
  QuestionType,
  Quiz,
  QuizResult,
  StoredQuestionResult,
} from "@/types/domain";

import { ScoreRing } from "./score-ring";

const RESULT_EVENTS = ["results-changed"] as const;

const playedAtFormatter = new Intl.DateTimeFormat("cs-CZ", {
  dateStyle: "long",
  timeStyle: "short",
});

function playedAtLabel(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "Datum není k dispozici"
    : playedAtFormatter.format(date);
}

function questionTypeLabel(type: QuestionType) {
  switch (type) {
    case "FREE_TEXT":
      return "Vlastní odpověď";
    case "MATCHING":
      return "Přiřazování";
    case "MULTIPLE_CHOICE":
      return "Výběr možností";
  }
}

function questionCountLabel(count: number) {
  if (count === 1) return "1 otázka";
  if (count >= 2 && count <= 4) return `${count} otázky`;
  return `${count} otázek`;
}

function QuestionResultRow({ result }: { result: StoredQuestionResult }) {
  const pending = result.status === "PENDING_REVIEW";

  return (
    <li className="grid gap-2 border-t border-border py-4 first:border-t-0 first:pt-0 last:pb-0 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-start sm:gap-5">
      <div className="min-w-0">
        <div className="mb-1.5 flex flex-wrap items-center gap-2">
          <span className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Otázka {result.questionIndex + 1}
          </span>
          <Badge variant="outline">{questionTypeLabel(result.type)}</Badge>
        </div>
        <p className="leading-6 font-medium text-foreground">{result.question}</p>
        {result.type === "FREE_TEXT" && result.text ? (
          <blockquote className="mt-2 border-l-2 border-brand pl-3 text-sm leading-6 text-muted-foreground">
            {result.text}
          </blockquote>
        ) : null}
      </div>
      <div className="sm:text-right">
        {pending ? (
          <Badge variant="brand">Čeká na hodnocení</Badge>
        ) : (
          <p className="font-mono text-sm font-semibold tabular-nums text-foreground">
            {result.awardedPoints ?? 0} / {result.points} b.
          </p>
        )}
      </div>
    </li>
  );
}

function ResultCard({
  quiz,
  result,
}: {
  quiz?: Quiz;
  result: QuizResult;
}) {
  const provisional = result.questionResults.some(
    (question) => question.status === "PENDING_REVIEW",
  );
  const context = [quiz?.subject, quiz?.chapter].filter(Boolean).join(" · ");

  return (
    <Panel className="overflow-hidden">
      <PanelHeader className="sm:flex-row sm:items-start sm:justify-between sm:gap-6">
        <div className="min-w-0 flex-1">
          <div className="mb-2 flex flex-wrap items-center gap-2">
            <Badge variant={provisional ? "brand" : "neutral"}>
              {provisional ? "Průběžný výsledek" : "Ohodnoceno"}
            </Badge>
            <span className="inline-flex items-center gap-1.5 text-sm text-muted-foreground">
              <Clock3 aria-hidden="true" className="size-4" />
              <time dateTime={result.playedAt}>{playedAtLabel(result.playedAt)}</time>
            </span>
          </div>
          <PanelTitle as="h2" className="text-xl sm:text-2xl">
            {quiz?.title ?? result.quizTitle ?? "Výsledek kvízu"}
          </PanelTitle>
          {context ? (
            <PanelDescription className="mt-2 inline-flex items-center gap-2">
              <BookOpen aria-hidden="true" className="size-4 shrink-0" />
              <span>{context}</span>
            </PanelDescription>
          ) : null}
          {provisional ? (
            <p className="mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
              Textovou odpověď ještě projde vyučující. Počet bodů se proto může změnit.
            </p>
          ) : null}
        </div>

        <div className="flex shrink-0 items-center gap-3 self-start rounded-md border border-border bg-surface px-3 py-2">
          <ScoreRing
            score={result.score}
            maxScore={result.maxScore}
            provisional={provisional}
            size="sm"
          />
          <div className="pr-2">
            <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              {provisional ? "Zatím" : "Skóre"}
            </p>
            <p className="mt-1 font-mono text-base font-semibold tabular-nums text-foreground">
              {result.score} / {result.maxScore} b.
            </p>
          </div>
        </div>
      </PanelHeader>

      <PanelContent className="pt-4">
        {result.questionResults.length > 0 ? (
          <details className="group rounded-md border border-border bg-surface">
            <summary className="flex min-h-11 list-none items-center justify-between gap-3 px-4 py-2.5 font-semibold text-foreground marker:content-none">
              <span>
                Podrobnosti · {questionCountLabel(result.questionResults.length)}
              </span>
              <ChevronDown
                aria-hidden="true"
                className="size-5 shrink-0 text-muted-foreground transition-transform duration-150 group-open:rotate-180 motion-reduce:transition-none"
              />
            </summary>
            <ol className="border-t border-border px-4 py-4 sm:px-5">
              {[...result.questionResults]
                .sort((left, right) => left.questionIndex - right.questionIndex)
                .map((question) => (
                  <QuestionResultRow key={question.id} result={question} />
                ))}
            </ol>
          </details>
        ) : (
          <p className="text-sm text-muted-foreground">
            Podrobnosti jednotlivých odpovědí nejsou k dispozici.
          </p>
        )}
      </PanelContent>
    </Panel>
  );
}

export interface StudentResultsProps {
  onBack?: () => void;
}

export function StudentResults({ onBack }: StudentResultsProps) {
  const router = useRouter();
  const [results, setResults] = useState<QuizResult[]>([]);
  const [status, setStatus] = useState<"loading" | "ready" | "error">(
    "loading",
  );
  const [error, setError] = useState("");

  const loadResults = useCallback(async (background = false) => {
    if (!background) {
      setStatus("loading");
      setError("");
    }

    try {
      const ownResults = await sessionService.myResults();
      setResults(ownResults);
      setStatus("ready");
    } catch (loadError) {
      if (background) return;
      setError(
        messageFromError(loadError, "Historii výsledků se nepodařilo načíst."),
      );
      setStatus("error");
    }
  }, []);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => void loadResults(), 0);
    return () => window.clearTimeout(initialLoad);
  }, [loadResults]);

  useLiveEvents(RESULT_EVENTS, () => void loadResults(true));

  const sortedResults = useMemo(
    () =>
      [...results].sort(
        (left, right) =>
          new Date(right.playedAt).getTime() - new Date(left.playedAt).getTime(),
      ),
    [results],
  );

  const goBack = () => {
    if (onBack) {
      onBack();
      return;
    }
    router.push("/dashboard");
  };

  return (
    <div className="mx-auto w-full max-w-[64rem]">
      <header className="border-b border-border pb-7">
        <Button
          variant="quiet"
          size="sm"
          className="mb-4 -ml-3"
          onClick={goBack}
          leadingIcon={<ArrowLeft className="size-4" />}
        >
          Zpět na přehled
        </Button>
        <div>
          <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-brand-text">
            <ClipboardCheck aria-hidden="true" className="size-5" />
            Tvoje práce
          </div>
          <h1 className="text-3xl leading-tight font-semibold tracking-[-0.025em] text-foreground sm:text-4xl">
            Moje výsledky
          </h1>
          <p className="mt-2 max-w-2xl text-base leading-7 text-muted-foreground">
            Hotové kvízy a průběžné body u odpovědí, které ještě čekají na vyučujícího.
          </p>
        </div>
      </header>

      <section aria-label="Historie výsledků" className="mt-8">
        {status === "loading" ? (
          <ResultsListSkeleton />
        ) : status === "error" ? (
          <EmptyState
            heading="Výsledky se nepodařilo načíst"
            description={error}
            icon={RefreshCw}
            action={<Button onClick={() => void loadResults()}>Zkusit znovu</Button>}
          />
        ) : sortedResults.length === 0 ? (
          <EmptyState
            heading="Zatím tu nemáš žádný výsledek"
            description="Až dokončíš první kvíz, najdeš ho tady i s přehledem bodů."
            icon={ClipboardCheck}
            action={<Button onClick={goBack}>Najít aktivní kvíz</Button>}
          />
        ) : (
          <div className="grid gap-5">
            {sortedResults.map((result) => (
              <ResultCard
                key={result.id}
                result={result}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
