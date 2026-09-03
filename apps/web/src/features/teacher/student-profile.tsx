"use client";

import {
  ArrowLeft,
  ClipboardList,
  Clock3,
  GraduationCap,
  UserRound,
} from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";

import {
  Badge,
  Button,
  EmptyState,
  Panel,
  PanelContent,
  Skeleton,
  buttonStyles,
} from "@/components/ui";
import { messageFromError } from "@/lib/http";
import { useLiveEvents } from "@/lib/live-events";
import { classService, sessionService } from "@/lib/services";
import type {
  ClassResponse,
  TeacherAttemptSummary,
  TeacherStudentSummary,
} from "@/types/domain";

import { formatDateTime } from "./shared";
import { participantLabel } from "./student-answer-review";

const PROFILE_EVENTS = ["roster-changed", "results-changed"] as const;

interface ResolvedProfile {
  classes: ClassResponse[];
  name: string;
  summary: TeacherStudentSummary | null;
}

function timestamp(value: string) {
  const parsed = new Date(value).getTime();
  return Number.isNaN(parsed) ? 0 : parsed;
}

function percentage(value: number) {
  return Math.min(100, Math.max(0, Math.round(value)));
}

function displayName(
  studentId: string,
  preferredName: string | null | undefined,
) {
  const name = preferredName?.trim();
  return name && name !== studentId ? name : participantLabel(studentId);
}

function pendingLabel(count: number) {
  if (count === 1) return "1 odpověď ke kontrole";
  if (count >= 2 && count <= 4) return `${count} odpovědi ke kontrole`;
  return `${count} odpovědí ke kontrole`;
}

function ProfileSkeleton() {
  return (
    <div role="status" aria-live="polite" aria-busy="true" className="grid gap-7">
      <span className="sr-only">Načítám profil žáka…</span>
      <header aria-hidden="true" className="grid gap-3 border-b border-border pb-6">
        <Skeleton className="h-11 w-32" />
        <Skeleton className="h-5 w-24" />
        <Skeleton className="h-10 w-64 max-w-[80vw]" />
        <div className="flex gap-2">
          <Skeleton className="h-6 w-28" />
          <Skeleton className="h-6 w-20" />
        </div>
      </header>

      <Panel aria-hidden="true" className="overflow-hidden">
        <PanelContent className="grid gap-px bg-border p-0 sm:grid-cols-2 lg:grid-cols-4">
          {[0, 1, 2, 3].map((item) => (
            <div key={item} className="grid gap-2 bg-panel px-5 py-4">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-7 w-28" />
            </div>
          ))}
        </PanelContent>
      </Panel>

      <section aria-hidden="true" className="grid gap-4">
        <div className="flex items-center gap-2">
          <Skeleton className="h-8 w-36" />
          <Skeleton className="h-6 w-16" />
        </div>
        <Panel className="hidden overflow-hidden md:block">
          <table className="w-full table-fixed border-collapse">
            <thead className="bg-surface">
              <tr>
                <th className="w-[30%] px-5 py-3"><Skeleton className="h-4 w-14" /></th>
                <th className="w-[16%] px-4 py-3"><Skeleton className="h-4 w-14" /></th>
                <th className="w-[20%] px-4 py-3"><Skeleton className="h-4 w-20" /></th>
                <th className="w-[20%] px-4 py-3"><Skeleton className="h-4 w-12" /></th>
                <th className="w-[14%] px-5 py-3"><span className="sr-only">Detail</span></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {[0, 1, 2].map((row) => (
                <tr key={row}>
                  <td className="px-5 py-4"><Skeleton className="h-5 w-48 max-w-full" /></td>
                  <td className="px-4 py-4"><Skeleton className="h-5 w-20" /></td>
                  <td className="px-4 py-4"><Skeleton className="h-5 w-32" /></td>
                  <td className="px-4 py-4"><Skeleton className="h-6 w-28" /></td>
                  <td className="px-5 py-4"><Skeleton className="ml-auto h-11 w-24" /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>
        <div className="grid gap-3 md:hidden">
          {[0, 1].map((row) => (
            <Panel key={row}>
              <PanelContent className="grid gap-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="grid flex-1 gap-2">
                    <Skeleton className="h-6 w-48 max-w-full" />
                    <Skeleton className="h-5 w-32" />
                  </div>
                  <Skeleton className="h-6 w-24" />
                </div>
                <div className="grid grid-cols-2 gap-3 border-t border-border pt-3">
                  <Skeleton className="h-10 w-full" />
                  <Skeleton className="h-10 w-full" />
                </div>
                <Skeleton className="h-11 w-full" />
              </PanelContent>
            </Panel>
          ))}
        </div>
      </section>
    </div>
  );
}

function AggregateSummary({ summary }: { summary: TeacherStudentSummary | null }) {
  const items = [
    {
      label: "Body celkem",
      value: summary ? `${summary.totalScore} / ${summary.totalMaxScore} b.` : "Bez výsledku",
      numeric: Boolean(summary),
    },
    {
      label: "Úspěšnost",
      value: summary ? `${percentage(summary.percentage)} %` : "—",
      numeric: Boolean(summary),
    },
    {
      label: "Počet pokusů",
      value: String(summary?.attemptCount ?? 0),
      numeric: true,
    },
    {
      label: "Poslední aktivita",
      value: summary ? formatDateTime(summary.lastPlayedAt) : "Zatím bez aktivity",
      numeric: false,
    },
  ];

  return (
    <Panel className="overflow-hidden">
      <PanelContent className="grid gap-px bg-border p-0 sm:grid-cols-2 lg:grid-cols-4">
        {items.map((item) => (
          <dl key={item.label} className="bg-panel px-5 py-4">
            <dt className="text-sm text-muted-foreground">{item.label}</dt>
            <dd
              className={
                item.numeric
                  ? "mt-1 font-mono text-xl font-semibold tabular-nums text-foreground"
                  : "mt-1 text-base font-semibold text-foreground"
              }
            >
              {item.value}
            </dd>
          </dl>
        ))}
      </PanelContent>
    </Panel>
  );
}

function AttemptStatus({ attempt }: { attempt: TeacherAttemptSummary }) {
  return attempt.pendingReviewCount > 0 ? (
    <Badge variant="brand">{pendingLabel(attempt.pendingReviewCount)}</Badge>
  ) : (
    <Badge variant="neutral">Ohodnoceno</Badge>
  );
}

function Attempts({ attempts }: { attempts: TeacherAttemptSummary[] }) {
  if (attempts.length === 0) {
    return (
      <EmptyState
        compact
        icon={ClipboardList}
        heading="Zatím bez odevzdaného pokusu"
        description="První dokončený kvíz se zde objeví automaticky."
      />
    );
  }

  return (
    <>
      <Panel className="hidden overflow-hidden md:block">
        <table className="w-full table-fixed border-collapse text-left">
          <caption className="sr-only">Poslední odevzdané pokusy žáka</caption>
          <thead className="bg-surface text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            <tr>
              <th scope="col" className="w-[30%] px-5 py-3">Kvíz</th>
              <th scope="col" className="w-[16%] px-4 py-3">Skóre</th>
              <th scope="col" className="w-[20%] px-4 py-3">Odevzdáno</th>
              <th scope="col" className="w-[20%] px-4 py-3">Stav</th>
              <th scope="col" className="w-[14%] px-5 py-3 text-right">
                <span className="sr-only">Detail odpovědí</span>
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {attempts.map((attempt) => (
              <tr key={attempt.resultId}>
                <th scope="row" className="px-5 py-4 font-semibold text-foreground">
                  {attempt.quizTitle || "Kvíz bez názvu"}
                </th>
                <td className="px-4 py-4">
                  <p className="font-mono text-sm font-semibold tabular-nums text-foreground">
                    {attempt.score} / {attempt.maxScore} b.
                  </p>
                  <p className="mt-0.5 text-xs tabular-nums text-muted-foreground">
                    {percentage(attempt.percentage)} %
                  </p>
                </td>
                <td className="px-4 py-4 text-sm text-muted-foreground">
                  <time dateTime={attempt.playedAt}>{formatDateTime(attempt.playedAt)}</time>
                </td>
                <td className="px-4 py-4"><AttemptStatus attempt={attempt} /></td>
                <td className="px-5 py-4 text-right">
                  <Link
                    href={`/session/${encodeURIComponent(attempt.sessionUuid)}#student-answers`}
                    className={buttonStyles({ variant: "secondary", size: "sm" })}
                  >
                    Odpovědi
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>

      <ul className="grid gap-3 md:hidden">
        {attempts.map((attempt) => (
          <li key={attempt.resultId}>
            <Panel>
              <PanelContent className="grid gap-4">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <h3 className="text-lg font-semibold text-foreground">
                      {attempt.quizTitle || "Kvíz bez názvu"}
                    </h3>
                    <p className="mt-1 inline-flex items-center gap-1.5 text-sm text-muted-foreground">
                      <Clock3 aria-hidden="true" className="size-4" />
                      <time dateTime={attempt.playedAt}>{formatDateTime(attempt.playedAt)}</time>
                    </p>
                  </div>
                  <AttemptStatus attempt={attempt} />
                </div>
                <dl className="grid grid-cols-2 gap-3 border-t border-border pt-3 text-sm">
                  <div>
                    <dt className="text-muted-foreground">Skóre</dt>
                    <dd className="mt-0.5 font-mono font-semibold tabular-nums text-foreground">
                      {attempt.score} / {attempt.maxScore} b.
                    </dd>
                  </div>
                  <div>
                    <dt className="text-muted-foreground">Úspěšnost</dt>
                    <dd className="mt-0.5 font-mono font-semibold tabular-nums text-foreground">
                      {percentage(attempt.percentage)} %
                    </dd>
                  </div>
                </dl>
                <Link
                  href={`/session/${encodeURIComponent(attempt.sessionUuid)}#student-answers`}
                  className={buttonStyles({
                    variant: "secondary",
                    size: "sm",
                    className: "w-full",
                  })}
                >
                  <ClipboardList aria-hidden="true" className="size-4" />
                  Odpovědi
                </Link>
              </PanelContent>
            </Panel>
          </li>
        ))}
      </ul>
    </>
  );
}

export function StudentProfile({ studentId }: { studentId: string }) {
  const [students, setStudents] = useState<TeacherStudentSummary[]>([]);
  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [error, setError] = useState("");

  const refresh = useCallback(async (background = false) => {
    if (!background) {
      setStatus("loading");
      setError("");
    }
    try {
      const [studentResponse, classResponse] = await Promise.all([
        sessionService.students(100, 20),
        classService.list(),
      ]);
      setStudents(studentResponse);
      setClasses(classResponse);
      setError("");
      setStatus("ready");
    } catch (loadError) {
      if (!background) {
        setError(messageFromError(loadError, "Profil žáka se nepodařilo načíst."));
        setStatus("error");
      }
    }
  }, []);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => void refresh(), 0);
    return () => window.clearTimeout(initialLoad);
  }, [refresh]);

  useLiveEvents(PROFILE_EVENTS, () => void refresh(true));

  const profile = useMemo<ResolvedProfile | null>(() => {
    const summary = students.find((student) => student.studentId === studentId) ?? null;
    const memberships = classes.filter((teacherClass) =>
      teacherClass.members.some((member) => member.studentId === studentId),
    );
    const membershipRecord = memberships
      .flatMap((teacherClass) => teacherClass.members)
      .find((member) => member.studentId === studentId);
    if (!summary && !membershipRecord) return null;
    return {
      classes: memberships,
      name: displayName(studentId, summary?.studentName || membershipRecord?.studentName),
      summary,
    };
  }, [classes, studentId, students]);

  if (status === "loading") return <ProfileSkeleton />;

  if (status === "error" || !profile) {
    return (
      <div className="grid gap-6">
        <header className="border-b border-border pb-6">
          <Link
            href="/students"
            className={buttonStyles({
              variant: "quiet",
              size: "sm",
              className: "mb-3 w-fit px-2",
            })}
          >
            <ArrowLeft aria-hidden="true" className="size-4" />
            Zpět na žáky
          </Link>
          <h1 className="text-3xl font-semibold tracking-[-0.025em] text-foreground sm:text-4xl">
            {status === "error" ? "Profil nelze načíst" : "Žák nebyl nalezen"}
          </h1>
        </header>
        <EmptyState
          icon={UserRound}
          heading={status === "error" ? "Načtení profilu selhalo" : "Tento žák není v přehledu"}
          description={
            status === "error"
              ? error
              : "Žák nemá u tohoto vyučujícího výsledek ani členství ve třídě."
          }
          action={
            status === "error" ? (
              <Button onClick={() => void refresh()}>Zkusit znovu</Button>
            ) : (
              <Link href="/students" className={buttonStyles()}>
                Otevřít seznam žáků
              </Link>
            )
          }
        />
      </div>
    );
  }

  const attempts = [...(profile.summary?.attempts ?? [])].sort(
    (left, right) => timestamp(right.playedAt) - timestamp(left.playedAt),
  );

  return (
    <div className="grid gap-7">
      <header className="border-b border-border pb-6">
        <Link
          href="/students"
          className={buttonStyles({
            variant: "quiet",
            size: "sm",
            className: "mb-3 w-fit px-2",
          })}
        >
          <ArrowLeft aria-hidden="true" className="size-4" />
          Zpět na žáky
        </Link>
        <p className="inline-flex items-center gap-2 text-sm font-semibold text-brand-text">
          <UserRound aria-hidden="true" className="size-[1.125rem]" />
          Profil žáka
        </p>
        <h1 className="mt-1 text-3xl font-semibold tracking-[-0.025em] text-balance text-foreground sm:text-4xl">
          {profile.name}
        </h1>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center gap-1.5 text-sm text-muted-foreground">
            <GraduationCap aria-hidden="true" className="size-[1.125rem]" />
            Třídy
          </span>
          {profile.classes.length > 0 ? (
            profile.classes.map((teacherClass) => (
              <Badge key={teacherClass.uuid} variant="neutral">
                {teacherClass.name}
              </Badge>
            ))
          ) : (
            <Badge variant="outline">Bez třídy</Badge>
          )}
        </div>
      </header>

      <AggregateSummary summary={profile.summary} />

      <section aria-labelledby="recent-attempts-title" className="grid gap-4">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h2
              id="recent-attempts-title"
              className="text-2xl font-semibold tracking-[-0.015em] text-foreground"
            >
              Poslední pokusy
            </h2>
            <Badge variant="neutral">{profile.summary?.attemptCount ?? 0}</Badge>
          </div>
          {profile.summary && attempts.length < profile.summary.attemptCount ? (
            <p className="mt-1 text-sm text-muted-foreground">
              Zobrazeno {attempts.length} z {profile.summary.attemptCount} pokusů.
            </p>
          ) : null}
        </div>
        <Attempts attempts={attempts} />
      </section>
    </div>
  );
}
