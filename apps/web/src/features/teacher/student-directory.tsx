"use client";

import { Settings2, UserRound, UsersRound } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";

import {
  Badge,
  Button,
  EmptyState,
  Panel,
  PanelContent,
  Skeleton,
} from "@/components/ui";
import { ClassManagement } from "@/features/teacher/class-management";
import { participantLabel } from "@/features/teacher/student-answer-review";
import { messageFromError } from "@/lib/http";
import { useLiveEvents } from "@/lib/live-events";
import { classService, sessionService } from "@/lib/services";
import type { ClassResponse, TeacherStudentSummary } from "@/types/domain";

const DIRECTORY_EVENTS = ["roster-changed", "results-changed"] as const;

interface RosterStudent {
  studentId: string;
  studentName: string;
  summary: TeacherStudentSummary | null;
}

interface StudentGroup {
  key: string;
  title: string;
  students: RosterStudent[];
}

const dateTimeFormatter = new Intl.DateTimeFormat("cs-CZ", {
  dateStyle: "medium",
  timeStyle: "short",
});

function formatDateTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "Datum není k dispozici"
    : dateTimeFormatter.format(date);
}

function clampedPercentage(value: number) {
  return Math.min(100, Math.max(0, Math.round(value)));
}

function resolvedStudentName(
  studentId: string,
  preferredName: string | null | undefined,
) {
  const name = preferredName?.trim();
  return name && name !== studentId ? name : participantLabel(studentId);
}

function studentCountLabel(count: number) {
  if (count === 1) return "1 žák";
  if (count >= 2 && count <= 4) return `${count} žáci`;
  return `${count} žáků`;
}

function sortRoster(students: RosterStudent[]) {
  return [...students].sort((left, right) =>
    left.studentName.localeCompare(right.studentName, "cs"),
  );
}

function buildGroups(
  students: TeacherStudentSummary[],
  classes: ClassResponse[],
): StudentGroup[] {
  const summaries = new Map(
    students.map((student) => [student.studentId, student] as const),
  );
  const assignedStudentIds = new Set<string>();

  const classGroups = [...classes]
    .sort((left, right) => left.name.localeCompare(right.name, "cs"))
    .map((teacherClass) => {
      const uniqueMembers = new Map(
        teacherClass.members.map((member) => [member.studentId, member] as const),
      );
      const roster = [...uniqueMembers.values()].map((member) => {
        assignedStudentIds.add(member.studentId);
        const summary = summaries.get(member.studentId) ?? null;
        return {
          studentId: member.studentId,
          studentName: resolvedStudentName(
            member.studentId,
            member.studentName || summary?.studentName,
          ),
          summary,
        };
      });
      return {
        key: teacherClass.uuid,
        title: teacherClass.name,
        students: sortRoster(roster),
      };
    });

  const withoutClass = students
    .filter((student) => !assignedStudentIds.has(student.studentId))
    .map((student) => ({
      studentId: student.studentId,
      studentName: resolvedStudentName(student.studentId, student.studentName),
      summary: student,
    }));

  return [
    ...classGroups,
    {
      key: "without-class",
      title: "Bez třídy",
      students: sortRoster(withoutClass),
    },
  ];
}

function ScoreValue({ student }: { student: RosterStudent }) {
  if (!student.summary) {
    return <span className="text-sm text-muted-foreground">Bez výsledku</span>;
  }
  return (
    <span className="font-mono text-sm font-semibold tabular-nums text-foreground">
      {student.summary.totalScore} / {student.summary.totalMaxScore} b.
    </span>
  );
}

function PercentageValue({ student }: { student: RosterStudent }) {
  if (!student.summary) {
    return <span className="text-sm text-muted-foreground">—</span>;
  }
  return (
    <span className="font-mono text-sm font-semibold tabular-nums text-foreground">
      {clampedPercentage(student.summary.percentage)} %
    </span>
  );
}

function ActivityValue({ student }: { student: RosterStudent }) {
  if (!student.summary) {
    return <span className="text-sm text-muted-foreground">Zatím bez aktivity</span>;
  }
  return (
    <time
      dateTime={student.summary.lastPlayedAt}
      className="text-sm text-muted-foreground"
    >
      {formatDateTime(student.summary.lastPlayedAt)}
    </time>
  );
}

function StudentTable({ group }: { group: StudentGroup }) {
  return (
    <>
      <Panel className="hidden overflow-hidden md:block">
        <table className="w-full table-fixed border-collapse text-left">
          <caption className="sr-only">
            Body a aktivita žáků ve skupině {group.title}
          </caption>
          <thead className="bg-surface">
            <tr className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              <th scope="col" className="w-[34%] px-5 py-3">Žák</th>
              <th scope="col" className="w-[22%] px-4 py-3">Body celkem</th>
              <th scope="col" className="w-[18%] px-4 py-3">Úspěšnost</th>
              <th scope="col" className="w-[26%] px-4 py-3">Poslední aktivita</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {group.students.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-5 py-6 text-sm text-muted-foreground">
                  V této skupině zatím nejsou žádní známí žáci.
                </td>
              </tr>
            ) : (
              group.students.map((student) => (
                <tr key={student.studentId}>
                  <th scope="row" className="px-5 py-4 font-normal">
                    <Link
                      href={`/students/${encodeURIComponent(student.studentId)}`}
                      aria-label={`Otevřít profil žáka ${student.studentName}`}
                      className="group flex min-h-11 w-fit max-w-full min-w-0 items-center gap-3 rounded-sm"
                    >
                      <span
                        aria-hidden="true"
                        className="grid size-9 shrink-0 place-items-center rounded-md border border-border bg-surface text-brand-text"
                      >
                        <UserRound className="size-[1.125rem]" />
                      </span>
                      <span className="truncate font-semibold text-foreground underline-offset-4 group-hover:text-brand-text group-hover:underline">
                        {student.studentName}
                      </span>
                    </Link>
                  </th>
                  <td className="px-4 py-4"><ScoreValue student={student} /></td>
                  <td className="px-4 py-4"><PercentageValue student={student} /></td>
                  <td className="px-4 py-4"><ActivityValue student={student} /></td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </Panel>

      <ul className="grid gap-3 md:hidden">
        {group.students.length === 0 ? (
          <li>
            <Panel>
              <PanelContent className="text-sm text-muted-foreground">
                V této skupině zatím nejsou žádní známí žáci.
              </PanelContent>
            </Panel>
          </li>
        ) : (
          group.students.map((student) => (
            <li key={student.studentId}>
              <Panel>
                <PanelContent className="grid gap-4">
                  <h3 className="min-w-0 text-lg font-semibold text-foreground">
                    <Link
                      href={`/students/${encodeURIComponent(student.studentId)}`}
                      aria-label={`Otevřít profil žáka ${student.studentName}`}
                      className="group flex min-h-11 min-w-0 items-center gap-3 rounded-sm"
                    >
                      <span
                        aria-hidden="true"
                        className="grid size-10 shrink-0 place-items-center rounded-md border border-border bg-surface text-brand-text"
                      >
                        <UserRound className="size-5" />
                      </span>
                      <span className="truncate underline-offset-4 group-hover:text-brand-text group-hover:underline">
                        {student.studentName}
                      </span>
                    </Link>
                  </h3>
                  <dl className="grid grid-cols-2 gap-px overflow-hidden rounded-md border border-border bg-border">
                    <div className="bg-surface px-3 py-3">
                      <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">Body celkem</dt>
                      <dd className="mt-1"><ScoreValue student={student} /></dd>
                    </div>
                    <div className="bg-surface px-3 py-3">
                      <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">Úspěšnost</dt>
                      <dd className="mt-1"><PercentageValue student={student} /></dd>
                    </div>
                  </dl>
                  <p className="text-sm text-muted-foreground">
                    Poslední aktivita · <ActivityValue student={student} />
                  </p>
                </PanelContent>
              </Panel>
            </li>
          ))
        )}
      </ul>
    </>
  );
}

function GroupTableSkeleton() {
  return (
    <section className="grid gap-3">
      <div className="flex items-center gap-2">
        <Skeleton className="h-8 w-28" />
        <Skeleton className="h-6 w-16" />
      </div>
      <Panel className="hidden overflow-hidden md:block">
        <table className="w-full table-fixed border-collapse text-left">
          <thead className="bg-surface">
            <tr className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              <th className="w-[34%] px-5 py-3">Žák</th>
              <th className="w-[22%] px-4 py-3">Body celkem</th>
              <th className="w-[18%] px-4 py-3">Úspěšnost</th>
              <th className="w-[26%] px-4 py-3">Poslední aktivita</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {[0, 1].map((row) => (
              <tr key={row}>
                <td className="px-5 py-4">
                  <div className="flex min-h-11 items-center gap-3">
                    <Skeleton className="size-9 shrink-0" />
                    <Skeleton className="h-5 w-32" />
                  </div>
                </td>
                <td className="px-4 py-4"><Skeleton className="h-5 w-24" /></td>
                <td className="px-4 py-4"><Skeleton className="h-5 w-12" /></td>
                <td className="px-4 py-4"><Skeleton className="h-5 w-36" /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>
      <div className="grid gap-3 md:hidden">
        {[0, 1].map((row) => (
          <Panel key={row}>
            <PanelContent className="grid gap-4">
              <div className="flex min-h-11 items-center gap-3">
                <Skeleton className="size-10 shrink-0" />
                <Skeleton className="h-6 w-36" />
              </div>
              <div className="grid grid-cols-2 gap-px overflow-hidden rounded-md border border-border bg-border">
                <div className="grid gap-2 bg-surface px-3 py-3">
                  <Skeleton className="h-4 w-20" />
                  <Skeleton className="h-5 w-24" />
                </div>
                <div className="grid gap-2 bg-surface px-3 py-3">
                  <Skeleton className="h-4 w-20" />
                  <Skeleton className="h-5 w-12" />
                </div>
              </div>
              <Skeleton className="h-5 w-64 max-w-full" />
            </PanelContent>
          </Panel>
        ))}
      </div>
    </section>
  );
}

function StudentGroupsSkeleton() {
  return (
    <div role="status" aria-live="polite" aria-busy="true">
      <span className="sr-only">Načítám přehled žáků podle tříd…</span>
      <div aria-hidden="true" className="grid gap-7">
        <GroupTableSkeleton />
        <GroupTableSkeleton />
      </div>
    </div>
  );
}

export function StudentDirectory() {
  const [students, setStudents] = useState<TeacherStudentSummary[]>([]);
  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [error, setError] = useState("");
  const [managingClasses, setManagingClasses] = useState(false);

  const refresh = useCallback(async (showSkeleton = false) => {
    if (showSkeleton) setStatus("loading");
    setError("");
    try {
      const [studentResponse, classResponse] = await Promise.all([
        sessionService.students(),
        classService.list(),
      ]);
      setStudents(studentResponse);
      setClasses(classResponse);
      setStatus("ready");
    } catch (loadError) {
      setError(messageFromError(loadError, "Přehled žáků se nepodařilo načíst."));
      setStatus("error");
    }
  }, []);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => void refresh(true), 0);
    return () => window.clearTimeout(initialLoad);
  }, [refresh]);

  useLiveEvents(DIRECTORY_EVENTS, () => void refresh(false));

  const groups = useMemo(() => buildGroups(students, classes), [classes, students]);

  const knownStudents = useMemo(() => {
    const roster = new Map<string, string>();
    students.forEach((student) => {
      roster.set(
        student.studentId,
        resolvedStudentName(student.studentId, student.studentName),
      );
    });
    classes.forEach((teacherClass) => {
      teacherClass.members.forEach((member) => {
        if (!roster.has(member.studentId)) {
          roster.set(
            member.studentId,
            resolvedStudentName(member.studentId, member.studentName),
          );
        }
      });
    });
    return [...roster].map(([studentId, studentName]) => ({ studentId, studentName }));
  }, [classes, students]);

  return (
    <div className="grid gap-7">
      <header className="flex flex-col gap-5 border-b border-border pb-6 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="inline-flex items-center gap-2 text-sm font-semibold text-brand-text">
            <UsersRound aria-hidden="true" className="size-5" />
            Výsledky výuky
          </p>
          <h1 className="mt-1 text-3xl font-semibold tracking-[-0.025em] text-balance text-foreground sm:text-4xl">Žáci</h1>
          <p className="mt-2 max-w-2xl text-base leading-7 text-muted-foreground">
            Rychlý přehled bodů podle tříd. Jednotlivé pokusy a odpovědi najdete v historii.
          </p>
        </div>
        <Button
          variant="secondary"
          disabled={status !== "ready"}
          aria-controls="class-management"
          aria-expanded={managingClasses}
          onClick={() => setManagingClasses((current) => !current)}
          leadingIcon={<Settings2 className="size-[1.125rem]" />}
        >
          Spravovat třídy
        </Button>
      </header>

      {managingClasses && status === "ready" ? (
        <ClassManagement
          classes={classes}
          knownStudents={knownStudents}
          onChange={setClasses}
          onClose={() => setManagingClasses(false)}
        />
      ) : null}

      {status === "loading" ? (
        <StudentGroupsSkeleton />
      ) : status === "error" ? (
        <EmptyState
          icon={UsersRound}
          heading="Přehled žáků se nepodařilo načíst"
          description={error}
          action={<Button onClick={() => void refresh(true)}>Zkusit znovu</Button>}
        />
      ) : groups.length === 1 && groups[0].students.length === 0 ? (
        <EmptyState
          icon={UsersRound}
          heading="Zatím tu nejsou žádní známí žáci"
          description="Žáci se objeví po prvním odevzdaném kvízu nebo po přiřazení do třídy."
          action={
            <Button variant="secondary" onClick={() => setManagingClasses(true)}>
              Spravovat třídy
            </Button>
          }
        />
      ) : (
        <div className="grid gap-7">
          {groups.map((group) => (
            <section
              key={group.key}
              aria-labelledby={`student-group-${group.key}`}
              className="grid gap-3"
            >
              <div className="flex flex-wrap items-center gap-2">
                <h2
                  id={`student-group-${group.key}`}
                  className="text-2xl font-semibold tracking-[-0.015em] text-foreground"
                >
                  {group.title}
                </h2>
                <Badge variant="neutral">{studentCountLabel(group.students.length)}</Badge>
              </div>
              <StudentTable group={group} />
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
