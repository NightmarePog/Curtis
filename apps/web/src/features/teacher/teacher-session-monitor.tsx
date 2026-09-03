"use client";

import {
  ArrowLeft,
  ClipboardList,
  Users,
} from "lucide-react";
import Link from "next/link";
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import {
  Badge,
  EmptyState,
  Panel,
  PanelContent,
  buttonStyles,
} from "@/components/ui";
import { SessionMonitorSkeleton } from "@/components/page-skeletons";
import { cn } from "@/lib/cn";
import { messageFromError } from "@/lib/http";
import { useLiveEvents } from "@/lib/live-events";
import { quizService, sessionService } from "@/lib/services";
import type {
  ActiveSession,
  PendingTextAnswer,
  Quiz,
  QuizResult,
} from "@/types/domain";
import { Notice, formatDateTime } from "./shared";
import {
  participantLabel,
  StudentAnswerReview,
  StudentIdentity,
} from "./student-answer-review";

interface TeacherSessionMonitorProps {
  sessionUuid: string;
}

const SESSION_MONITOR_EVENTS = [
  "sessions-changed",
  "results-changed",
] as const;

export function TeacherSessionMonitor({
  sessionUuid,
}: TeacherSessionMonitorProps) {
  const [results, setResults] = useState<QuizResult[]>([]);
  const [pending, setPending] = useState<PendingTextAnswer[]>([]);
  const [session, setSession] = useState<ActiveSession | null>(null);
  const [quiz, setQuiz] = useState<Quiz | null>(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [gradeNotice, setGradeNotice] = useState<string | null>(null);
  const refreshInProgress = useRef(false);
  const loadedQuizUuid = useRef<string | null>(null);
  const sessionIsActive = session?.status === undefined || session.status === "ACTIVE";

  const refresh = useCallback(
    async () => {
      if (refreshInProgress.current) return;
      refreshInProgress.current = true;
      try {
        const [loadedResults, loadedPending] = await Promise.all([
          sessionService.results(sessionUuid),
          sessionService.pending(sessionUuid),
        ]);
        setResults(loadedResults);
        setPending(loadedPending);
        setLastUpdated(new Date());
        setError(null);

        try {
          const activeSessions = await sessionService.teacherSessions();
          const activeSession =
            activeSessions.find((item) => item.sessionUuid === sessionUuid) ?? null;
          setSession(activeSession);

          const quizUuid = activeSession?.quizUuid ?? loadedResults[0]?.quizUuid;
          if (quizUuid && loadedQuizUuid.current !== quizUuid) {
            try {
              const loadedQuiz = await quizService.get(quizUuid);
              setQuiz(loadedQuiz);
              loadedQuizUuid.current = quizUuid;
            } catch {
              // Answer indexes remain reviewable if quiz details were deleted.
            }
          }
        } catch {
          // Results and grading stay usable even if lobby metadata is unavailable.
          const quizUuid = loadedResults[0]?.quizUuid;
          if (quizUuid && loadedQuizUuid.current !== quizUuid) {
            try {
              const loadedQuiz = await quizService.get(quizUuid);
              setQuiz(loadedQuiz);
              loadedQuizUuid.current = quizUuid;
            } catch {
              // Answer indexes remain reviewable if quiz details were deleted.
            }
          }
        }
      } catch (refreshError) {
        setError(
          messageFromError(
            refreshError,
            "Výsledky se nepodařilo aktualizovat. Zobrazená data mohou být zastaralá.",
          ),
        );
      } finally {
        setInitialLoading(false);
        refreshInProgress.current = false;
      }
    },
    [sessionUuid],
  );

  useEffect(() => {
    const initialRefresh = window.setTimeout(() => void refresh(), 0);
    return () => window.clearTimeout(initialRefresh);
  }, [refresh]);

  useLiveEvents(SESSION_MONITOR_EVENTS, () => void refresh());

  const studentLabels = useMemo(() => {
    const labels = new Map<string, string>();
    for (const participant of [...results, ...pending]) {
      const name = participant.studentName?.trim();
      const current = labels.get(participant.studentId);
      if (name && name !== participant.studentId) {
        labels.set(participant.studentId, name);
      } else if (!current) {
        labels.set(participant.studentId, participantLabel(participant.studentId));
      }
    }
    return labels;
  }, [pending, results]);

  const pendingStudents = useMemo(
    () => new Set(pending.map((answer) => answer.studentId)),
    [pending],
  );

  const leaderboard = useMemo(() => {
    const sorted = [...results].sort((left, right) => {
      const leftRatio = left.maxScore > 0 ? left.score / left.maxScore : 0;
      const rightRatio = right.maxScore > 0 ? right.score / right.maxScore : 0;
      if (rightRatio !== leftRatio) return rightRatio - leftRatio;
      if (right.score !== left.score) return right.score - left.score;
      return new Date(left.playedAt).getTime() - new Date(right.playedAt).getTime();
    });
    return sorted.map((result, index) => {
      const previous = sorted[index - 1];
      const sameAsPrevious =
        previous &&
        previous.score === result.score &&
        previous.maxScore === result.maxScore;
      const previousRank = index > 0 ? sorted.slice(0, index).findIndex((candidate) =>
        candidate.score === previous.score && candidate.maxScore === previous.maxScore,
      ) + 1 : 1;
      return {
        result,
        rank: sameAsPrevious ? previousRank : index + 1,
        percentage:
          result.maxScore > 0 ? Math.round((result.score / result.maxScore) * 100) : 0,
      };
    });
  }, [results]);

  const fullyReviewed = results.filter(
    (result) => !pendingStudents.has(result.studentId),
  ).length;

  async function grade(answer: PendingTextAnswer, points: number) {
    await sessionService.grade(sessionUuid, answer.resultId, points);
    setPending((current) =>
      current.filter((item) => item.resultId !== answer.resultId),
    );
    setGradeNotice(`Hodnocení pro ${studentLabels.get(answer.studentId) ?? "žáka"} bylo uloženo.`);
    await refresh();
  }

  if (initialLoading) {
    return <SessionMonitorSkeleton />;
  }

  return (
    <div className="grid gap-7">
      <header className="flex flex-col gap-5 border-b border-border pb-6 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <Link
            href="/dashboard"
            className={cn(buttonStyles({ variant: "quiet", size: "sm" }), "mb-3 w-fit px-2")}
          >
            <ArrowLeft aria-hidden="true" className="size-4" />
            Knihovna kvízů
          </Link>
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-sm font-semibold text-brand-text">
              {session && sessionIsActive ? "Probíhající relace" : "Výsledky relace"}
            </p>
            <Badge variant={session && sessionIsActive ? "brand" : "neutral"}>
              {session && sessionIsActive ? "Živá aktualizace" : "Ukončená relace"}
            </Badge>
          </div>
          <h1 className="mt-2 text-3xl font-semibold tracking-[-0.025em] text-balance text-foreground sm:text-4xl">
            {session?.title ?? "Výsledky relace"}
          </h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">
            {session && sessionIsActive
              ? `Dostupná do ${formatDateTime(session.expiresAt)}`
              : "Odevzdané odpovědi a hodnocení této relace."}
          </p>
        </div>
        <p className="text-sm text-muted-foreground" aria-live="polite">
          {lastUpdated
            ? `Aktualizováno ${lastUpdated.toLocaleTimeString("cs-CZ", {
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
              })}`
            : "Čekám na první aktualizaci"}
        </p>
      </header>

      {error ? <Notice tone="error">{error}</Notice> : null}
      {gradeNotice ? <Notice tone="success">{gradeNotice}</Notice> : null}

      <nav
        aria-label="Obsah relace"
        className="flex flex-wrap gap-2 rounded-lg border border-border bg-surface p-1.5"
      >
        <a
          href="#student-answers"
          className={buttonStyles({ variant: "secondary", size: "sm" })}
        >
          <ClipboardList aria-hidden="true" className="size-4" />
          Odpovědi žáků
          <Badge variant="brand">{results.length}</Badge>
        </a>
        <a
          href="#leaderboard"
          className={buttonStyles({ variant: "quiet", size: "sm" })}
        >
          <Users aria-hidden="true" className="size-4" />
          Pořadí
        </a>
      </nav>

      <section aria-labelledby="completion-title">
        <Panel tone="subtle">
          <PanelContent className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 id="completion-title" className="font-semibold text-foreground">
                Stav odevzdání
              </h2>
              <p className="mt-1 text-sm leading-6 text-muted-foreground">
                Počty vycházejí z uložených výsledků této relace.
              </p>
            </div>
            <dl className="grid grid-cols-3 divide-x divide-border rounded-md border border-border bg-panel">
              <div className="px-4 py-3 text-center">
                <dt className="text-xs text-muted-foreground">Odevzdáno</dt>
                <dd className="mt-1 text-xl font-semibold tabular-nums text-foreground">{results.length}</dd>
              </div>
              <div className="px-4 py-3 text-center">
                <dt className="text-xs text-muted-foreground">Zkontrolováno</dt>
                <dd className="mt-1 text-xl font-semibold tabular-nums text-foreground">{fullyReviewed}</dd>
              </div>
              <div className="px-4 py-3 text-center">
                <dt className="text-xs text-muted-foreground">Texty ke kontrole</dt>
                <dd className="mt-1 text-xl font-semibold tabular-nums text-foreground">{pending.length}</dd>
              </div>
            </dl>
          </PanelContent>
        </Panel>
      </section>

      <StudentAnswerReview
        labels={studentLabels}
        onGrade={grade}
        pending={pending}
        quiz={quiz}
        results={results}
      />

      <section
        id="leaderboard"
        aria-labelledby="leaderboard-title"
        className="scroll-mt-24 grid gap-4"
      >
        <div>
          <h2 id="leaderboard-title" className="text-2xl font-semibold tracking-[-0.015em] text-foreground">
            Pořadí podle výsledků
          </h2>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">
            U odpovědí čekajících na kontrolu je skóre zatím průběžné.
          </p>
        </div>
        {leaderboard.length === 0 ? (
          <EmptyState
            compact
            icon={Users}
            heading="Zatím nikdo neodevzdal"
            description="Po prvním dokončeném pokusu se zde zobrazí průběžné pořadí."
          />
        ) : (
          <>
            <div className="hidden overflow-hidden rounded-lg border border-border bg-panel sm:block">
              <table className="w-full border-collapse text-left">
                <thead className="bg-surface text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                  <tr>
                    <th scope="col" className="w-20 px-5 py-3">Pořadí</th>
                    <th scope="col" className="px-4 py-3">Žák</th>
                    <th scope="col" className="px-4 py-3">Skóre</th>
                    <th scope="col" className="px-4 py-3">Úspěšnost</th>
                    <th scope="col" className="px-5 py-3">Stav</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {leaderboard.map(({ result, rank, percentage }) => (
                    <tr key={result.id}>
                      <td className="px-5 py-4 font-semibold tabular-nums text-foreground">{rank}.</td>
                      <td className="px-4 py-4">
                        <StudentIdentity
                          label={studentLabels.get(result.studentId) ?? "Žák"}
                        />
                      </td>
                      <td className="px-4 py-4 font-semibold tabular-nums text-foreground">
                        {result.score} / {result.maxScore}
                      </td>
                      <td className="px-4 py-4 tabular-nums text-foreground">{percentage} %</td>
                      <td className="px-5 py-4">
                        <Badge variant={pendingStudents.has(result.studentId) ? "neutral" : "brand"}>
                          {pendingStudents.has(result.studentId) ? "Průběžné" : "Vyhodnoceno"}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <ol className="grid gap-3 sm:hidden">
              {leaderboard.map(({ result, rank, percentage }) => (
                <li key={result.id}>
                  <Panel>
                    <PanelContent className="grid gap-3">
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex items-start gap-3">
                          <span className="grid size-9 shrink-0 place-items-center rounded-md border border-border bg-surface font-semibold tabular-nums text-foreground">
                            {rank}.
                          </span>
                          <StudentIdentity
                            label={studentLabels.get(result.studentId) ?? "Žák"}
                          />
                        </div>
                        <Badge variant={pendingStudents.has(result.studentId) ? "neutral" : "brand"}>
                          {pendingStudents.has(result.studentId) ? "Průběžné" : "Hotovo"}
                        </Badge>
                      </div>
                      <dl className="grid grid-cols-2 border-t border-border pt-3 text-sm">
                        <div>
                          <dt className="text-muted-foreground">Skóre</dt>
                          <dd className="mt-0.5 font-semibold tabular-nums text-foreground">
                            {result.score} / {result.maxScore}
                          </dd>
                        </div>
                        <div>
                          <dt className="text-muted-foreground">Úspěšnost</dt>
                          <dd className="mt-0.5 font-semibold tabular-nums text-foreground">{percentage} %</dd>
                        </div>
                      </dl>
                    </PanelContent>
                  </Panel>
                </li>
              ))}
            </ol>
          </>
        )}
      </section>
    </div>
  );
}
