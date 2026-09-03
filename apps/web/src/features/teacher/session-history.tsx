"use client";

import {
  ArrowLeft,
  ClipboardList,
  Clock3,
  History,
  Users,
} from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";

import {
  Badge,
  Button,
  EmptyState,
  Panel,
  PanelContent,
  PanelHeader,
  PanelTitle,
  Skeleton,
  buttonStyles,
} from "@/components/ui";
import { ApiError, messageFromError } from "@/lib/http";
import { useLiveEvents } from "@/lib/live-events";
import { sessionService } from "@/lib/services";
import type { TeacherAttemptSummary } from "@/types/domain";

import { formatDateTime } from "./shared";
import { participantLabel } from "./student-answer-review";

const RESULT_EVENTS = ["results-changed"] as const;

interface SessionGroup {
  sessionUuid: string;
  quizTitle: string;
  latestPlayedAt: string;
  attempts: TeacherAttemptSummary[];
}

function timestamp(value: string) {
  const parsed = new Date(value).getTime();
  return Number.isNaN(parsed) ? 0 : parsed;
}

function groupAttempts(
  attempts: TeacherAttemptSummary[],
): SessionGroup[] {
  const groups = new Map<string, SessionGroup>();

  for (const attempt of attempts) {
    const existing = groups.get(attempt.sessionUuid);
    if (existing) {
      existing.attempts.push(attempt);
      if (timestamp(attempt.playedAt) > timestamp(existing.latestPlayedAt)) {
        existing.latestPlayedAt = attempt.playedAt;
      }
      continue;
    }

    groups.set(attempt.sessionUuid, {
      sessionUuid: attempt.sessionUuid,
      quizTitle: attempt.quizTitle || "Kvíz bez názvu",
      latestPlayedAt: attempt.playedAt,
      attempts: [attempt],
    });
  }

  return [...groups.values()]
    .map((group) => ({
      ...group,
      attempts: [...group.attempts].sort(
        (left, right) => timestamp(right.playedAt) - timestamp(left.playedAt),
      ),
    }))
    .sort(
      (left, right) =>
        timestamp(right.latestPlayedAt) - timestamp(left.latestPlayedAt),
    );
}

function attemptCountLabel(count: number) {
  if (count === 1) return "1 odevzdání";
  if (count >= 2 && count <= 4) return `${count} odevzdání`;
  return `${count} odevzdání`;
}

function pendingCountLabel(count: number) {
  if (count === 1) return "1 odpověď ke kontrole";
  if (count >= 2 && count <= 4) return `${count} odpovědi ke kontrole`;
  return `${count} odpovědí ke kontrole`;
}

function StudentName({ attempt }: { attempt: TeacherAttemptSummary }) {
  const availableName = attempt.studentName?.trim();
  return (
    <span className="font-semibold text-foreground">
      {availableName || participantLabel(attempt.studentId)}
    </span>
  );
}

function ReviewStatus({ count }: { count: number }) {
  return count > 0 ? (
    <Badge variant="brand">{pendingCountLabel(count)}</Badge>
  ) : (
    <Badge variant="neutral">Zkontrolováno</Badge>
  );
}

function SessionHistorySkeleton() {
  return (
    <div role="status" aria-busy="true" aria-live="polite" className="grid gap-7">
      <span className="sr-only">Načítám historii relací…</span>
      <header aria-hidden="true" className="grid gap-3 border-b border-border pb-6">
        <Skeleton className="h-11 w-32" />
        <Skeleton className="h-4 w-20" />
        <Skeleton className="h-10 w-64 max-w-[80vw]" />
        <Skeleton className="h-5 w-full max-w-2xl" />
      </header>

      <Panel aria-hidden="true">
        <PanelContent className="grid gap-px overflow-hidden p-0 sm:grid-cols-3">
          {[0, 1, 2].map((item) => (
            <div key={item} className="grid gap-2 bg-panel px-5 py-4">
              <Skeleton className="h-3 w-24" />
              <Skeleton className="h-7 w-12" />
            </div>
          ))}
        </PanelContent>
      </Panel>

      {[0, 1].map((session) => (
        <Panel key={session} aria-hidden="true" className="overflow-hidden">
          <PanelHeader className="gap-4 border-b border-border pb-5 sm:flex-row sm:items-start sm:justify-between">
            <div className="grid flex-1 gap-3">
              <Skeleton className="h-6 w-24" />
              <Skeleton className="h-7 w-2/3 max-w-md" />
              <Skeleton className="h-4 w-52" />
            </div>
            <Skeleton className="h-11 w-48" />
          </PanelHeader>
          <PanelContent className="grid gap-0 py-0">
            {[0, 1].map((attempt) => (
              <div
                key={attempt}
                className="grid grid-cols-[minmax(0,1fr)_8rem_10rem] gap-4 border-t border-border py-4 first:border-0"
              >
                <div className="grid gap-2">
                  <Skeleton className="h-5 w-40" />
                  <Skeleton className="h-3 w-56 max-w-full" />
                </div>
                <Skeleton className="h-5 w-20" />
                <Skeleton className="h-6 w-32" />
              </div>
            ))}
          </PanelContent>
        </Panel>
      ))}
    </div>
  );
}

async function getHistory() {
  return sessionService.history(100);
}

export function SessionHistory() {
  const [attempts, setAttempts] = useState<TeacherAttemptSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadHistory = useCallback(async (background = false) => {
    if (!background) {
      setLoading(true);
      setError(null);
    }
    try {
      setAttempts(await getHistory());
      setError(null);
    } catch (loadError) {
      if (!background) {
        setError(
          loadError instanceof ApiError && loadError.status === 404
            ? "Historie relací zatím není na serveru dostupná."
            : messageFromError(
                loadError,
                "Historii relací se nepodařilo načíst.",
              ),
        );
      }
    } finally {
      if (!background) setLoading(false);
    }
  }, []);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => void loadHistory(), 0);
    return () => window.clearTimeout(initialLoad);
  }, [loadHistory]);

  useLiveEvents(RESULT_EVENTS, () => void loadHistory(true));

  const sessions = useMemo(() => groupAttempts(attempts), [attempts]);
  const pendingTotal = attempts.reduce(
    (sum, attempt) => sum + Math.max(0, attempt.pendingReviewCount),
    0,
  );

  if (loading) return <SessionHistorySkeleton />;

  return (
    <div className="grid gap-7">
      <header className="border-b border-border pb-6">
        <Link
          href="/dashboard"
          className={buttonStyles({
            variant: "quiet",
            size: "sm",
            className: "mb-3 w-fit px-2",
          })}
        >
          <ArrowLeft aria-hidden="true" className="size-4" />
          Knihovna kvízů
        </Link>
        <p className="text-sm font-semibold text-brand-text">Výuka</p>
        <h1 className="mt-1 text-3xl font-semibold tracking-[-0.025em] text-balance text-foreground sm:text-4xl">
          Historie relací
        </h1>
        <p className="mt-2 max-w-2xl text-base leading-7 text-muted-foreground">
          Odevzdané pokusy jsou seskupené podle relace. Odtud otevřete odpovědi
          žáků i nedokončené ruční hodnocení.
        </p>
      </header>

      {error ? (
        <EmptyState
          icon={History}
          heading="Historii nelze zobrazit"
          description={error}
          action={<Button onClick={() => void loadHistory()}>Zkusit znovu</Button>}
        />
      ) : attempts.length === 0 ? (
        <EmptyState
          icon={History}
          heading="Historie je zatím prázdná"
          description="Dokončená odevzdání se zde objeví po první relaci."
          action={
            <Link href="/dashboard" className={buttonStyles()}>
              Zpět ke kvízům
            </Link>
          }
        />
      ) : (
        <>
          <Panel tone="subtle">
            <PanelContent className="grid gap-px overflow-hidden p-0 sm:grid-cols-3">
              <div className="bg-panel px-5 py-4">
                <p className="flex items-center gap-2 text-sm text-muted-foreground">
                  <History aria-hidden="true" className="size-4" />
                  Relace
                </p>
                <p className="mt-1 text-2xl font-semibold tabular-nums text-foreground">
                  {sessions.length}
                </p>
              </div>
              <div className="border-t border-border bg-panel px-5 py-4 sm:border-t-0 sm:border-l">
                <p className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Users aria-hidden="true" className="size-4" />
                  Odevzdání
                </p>
                <p className="mt-1 text-2xl font-semibold tabular-nums text-foreground">
                  {attempts.length}
                </p>
              </div>
              <div className="border-t border-border bg-panel px-5 py-4 sm:border-t-0 sm:border-l">
                <p className="flex items-center gap-2 text-sm text-muted-foreground">
                  <ClipboardList aria-hidden="true" className="size-4" />
                  Odpovědi ke kontrole
                </p>
                <p className="mt-1 text-2xl font-semibold tabular-nums text-foreground">
                  {pendingTotal}
                </p>
              </div>
            </PanelContent>
          </Panel>

          <section aria-labelledby="history-list-title" className="grid gap-4">
            <div>
              <h2 id="history-list-title" className="text-xl font-semibold text-foreground">
                Odevzdání podle relace
              </h2>
              <p className="mt-1 text-sm text-muted-foreground" aria-live="polite">
                {sessions.length} {sessions.length === 1 ? "relace" : "relací"}
              </p>
            </div>

            <div className="grid gap-4">
              {sessions.map((session) => {
                const sessionPending = session.attempts.reduce(
                  (sum, attempt) => sum + Math.max(0, attempt.pendingReviewCount),
                  0,
                );
                return (
                  <Panel key={session.sessionUuid} className="overflow-hidden">
                    <PanelHeader className="gap-4 border-b border-border pb-5 lg:flex-row lg:items-start lg:justify-between">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <Badge variant={sessionPending > 0 ? "brand" : "neutral"}>
                            {sessionPending > 0
                              ? pendingCountLabel(sessionPending)
                              : "Hodnocení dokončeno"}
                          </Badge>
                          <span className="inline-flex items-center gap-1.5 text-sm text-muted-foreground">
                            <Clock3 aria-hidden="true" className="size-4" />
                            Naposledy {formatDateTime(session.latestPlayedAt)}
                          </span>
                        </div>
                        <PanelTitle as="h3" className="mt-3">
                          {session.quizTitle}
                        </PanelTitle>
                        <p className="mt-1 text-sm text-muted-foreground">
                          {attemptCountLabel(session.attempts.length)}
                        </p>
                      </div>
                      <Link
                        href={`/session/${session.sessionUuid}#student-answers`}
                        className={buttonStyles({
                          variant: "secondary",
                          size: "sm",
                          className: "w-full lg:w-auto",
                        })}
                      >
                        <ClipboardList aria-hidden="true" className="size-4" />
                        Odpovědi a hodnocení
                      </Link>
                    </PanelHeader>

                    <div className="hidden md:block">
                      <table className="w-full border-collapse text-left">
                        <thead className="bg-surface text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                          <tr>
                            <th scope="col" className="w-[38%] px-6 py-3">Žák</th>
                            <th scope="col" className="w-[20%] px-4 py-3">Skóre</th>
                            <th scope="col" className="w-[20%] px-4 py-3">Odevzdáno</th>
                            <th scope="col" className="w-[22%] px-6 py-3">Stav</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-border">
                          {session.attempts.map((attempt) => (
                            <tr key={attempt.resultId}>
                              <th scope="row" className="px-6 py-4 font-normal">
                                <StudentName attempt={attempt} />
                              </th>
                              <td className="px-4 py-4">
                                <p className="font-mono text-sm font-semibold tabular-nums text-foreground">
                                  {attempt.score} / {attempt.maxScore} b.
                                </p>
                                <p className="mt-0.5 text-xs tabular-nums text-muted-foreground">
                                  {attempt.percentage} %
                                </p>
                              </td>
                              <td className="px-4 py-4 text-sm text-muted-foreground">
                                <time dateTime={attempt.playedAt}>
                                  {formatDateTime(attempt.playedAt)}
                                </time>
                              </td>
                              <td className="px-6 py-4">
                                <ReviewStatus count={attempt.pendingReviewCount} />
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>

                    <ul className="divide-y divide-border md:hidden">
                      {session.attempts.map((attempt) => (
                        <li
                          key={attempt.resultId}
                          className="grid gap-3 px-5 py-4"
                        >
                          <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                            <StudentName attempt={attempt} />
                            <ReviewStatus count={attempt.pendingReviewCount} />
                          </div>
                          <dl className="grid grid-cols-2 gap-3 border-t border-border pt-3 text-sm">
                            <div>
                              <dt className="text-muted-foreground">Skóre</dt>
                              <dd className="mt-0.5 font-mono font-semibold tabular-nums text-foreground">
                                {attempt.score} / {attempt.maxScore} b. · {attempt.percentage} %
                              </dd>
                            </div>
                            <div>
                              <dt className="text-muted-foreground">Odevzdáno</dt>
                              <dd className="mt-0.5 text-foreground">
                                <time dateTime={attempt.playedAt}>
                                  {formatDateTime(attempt.playedAt)}
                                </time>
                              </dd>
                            </div>
                          </dl>
                        </li>
                      ))}
                    </ul>
                  </Panel>
                );
              })}
            </div>
          </section>
        </>
      )}
    </div>
  );
}
