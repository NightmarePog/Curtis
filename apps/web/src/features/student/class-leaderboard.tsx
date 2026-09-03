"use client";

import { ArrowLeft, RefreshCw, UsersRound } from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

import {
  Badge,
  Button,
  EmptyState,
  Panel,
  PanelContent,
  PanelDescription,
  PanelHeader,
  PanelTitle,
  Skeleton,
} from "@/components/ui";
import { messageFromError } from "@/lib/http";
import { useLiveEvents } from "@/lib/live-events";
import { sessionService } from "@/lib/services";
import { cn } from "@/lib/utils";
import type {
  ClassLeaderboard,
  ClassLeaderboardMember,
} from "@/types/domain";

const LEADERBOARD_EVENTS = ["roster-changed", "results-changed"] as const;

const scoreFormatter = new Intl.NumberFormat("cs-CZ", {
  maximumFractionDigits: 2,
});

const percentageFormatter = new Intl.NumberFormat("cs-CZ", {
  maximumFractionDigits: 1,
});

function memberCountLabel(count: number) {
  if (count === 1) return "1 žák";
  if (count >= 2 && count <= 4) return `${count} žáci`;
  return `${count} žáků`;
}

function attemptCountLabel(count: number) {
  if (count === 1) return "1 pokus";
  if (count >= 2 && count <= 4) return `${count} pokusy`;
  return `${count} pokusů`;
}

function scoreLabel(member: ClassLeaderboardMember) {
  return `${scoreFormatter.format(member.totalScore)} / ${scoreFormatter.format(member.totalMaxScore)} b.`;
}

function percentageLabel(value: number) {
  return `${percentageFormatter.format(value)} %`;
}

function ClassLeaderboardSkeleton() {
  const groups = [0, 1];
  const members = [0, 1, 2, 3];

  return (
    <div role="status" aria-busy="true" aria-live="polite">
      <span className="sr-only">Načítám pořadí ve třídách…</span>
      <div aria-hidden="true" className="grid gap-6">
        {groups.map((group) => (
          <Panel key={group} className="overflow-hidden">
            <PanelHeader className="gap-2 border-b border-border pb-5">
              <Skeleton className="h-7 w-44 max-w-[70%]" />
              <Skeleton className="h-4 w-24" />
            </PanelHeader>
            <PanelContent className="p-0 sm:p-0">
              <div className="hidden md:block">
                <div className="grid grid-cols-[12%_34%_22%_17%_15%] bg-surface px-5 py-3">
                  {members.concat(4).map((item) => (
                    <Skeleton key={item} className="h-4 w-14 max-w-[80%]" />
                  ))}
                </div>
                <div className="divide-y divide-border">
                  {members.map((member) => (
                    <div
                      key={member}
                      className="grid min-h-16 grid-cols-[12%_34%_22%_17%_15%] items-center px-5 py-3"
                    >
                      <Skeleton className="h-5 w-8" />
                      <Skeleton className="h-5 w-36 max-w-[80%]" />
                      <Skeleton className="h-5 w-24" />
                      <Skeleton className="h-5 w-14" />
                      <Skeleton className="h-5 w-10" />
                    </div>
                  ))}
                </div>
              </div>
              <div className="grid gap-3 p-4 md:hidden">
                {members.slice(0, 3).map((member) => (
                  <div
                    key={member}
                    className="grid gap-4 rounded-md border border-border bg-surface px-4 py-4"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex min-w-0 flex-1 items-center gap-3">
                        <Skeleton className="h-7 w-8 shrink-0" />
                        <Skeleton className="h-5 w-32 max-w-[70%]" />
                      </div>
                      <Skeleton className="h-6 w-14" />
                    </div>
                    <div className="grid grid-cols-2 gap-4 border-t border-border pt-3">
                      <Skeleton className="h-9 w-24" />
                      <Skeleton className="h-9 w-20" />
                    </div>
                  </div>
                ))}
              </div>
            </PanelContent>
          </Panel>
        ))}
      </div>
    </div>
  );
}

function LeaderboardTable({
  className,
  members,
}: {
  className: string;
  members: ClassLeaderboardMember[];
}) {
  return (
    <div className="hidden md:block">
      <table className="w-full border-collapse text-left">
        <caption className="sr-only">Pořadí ve třídě {className}</caption>
        <thead className="bg-surface text-xs font-semibold tracking-wide text-muted-foreground uppercase">
          <tr>
            <th scope="col" className="w-[12%] px-5 py-3">
              Pořadí
            </th>
            <th scope="col" className="w-[34%] px-4 py-3">
              Žák
            </th>
            <th scope="col" className="w-[22%] px-4 py-3">
              Body
            </th>
            <th scope="col" className="w-[17%] px-4 py-3">
              Úspěšnost
            </th>
            <th scope="col" className="w-[15%] px-5 py-3 text-right">
              Pokusy
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {members.map((member, index) => (
            <tr
              key={`${member.studentName}-${member.rank}-${index}`}
              className={cn(member.currentStudent && "bg-brand-subtle")}
            >
              <td className="px-5 py-4 font-mono text-sm font-semibold tabular-nums text-foreground">
                {member.rank}.
              </td>
              <th scope="row" className="px-4 py-4 font-normal">
                <span className="flex min-w-0 flex-wrap items-center gap-2">
                  <span className="font-semibold text-foreground [overflow-wrap:anywhere]">
                    {member.studentName}
                  </span>
                  {member.currentStudent ? <Badge variant="brand">Ty</Badge> : null}
                </span>
              </th>
              <td className="px-4 py-4 font-mono text-sm tabular-nums text-foreground">
                {scoreLabel(member)}
              </td>
              <td className="px-4 py-4 font-mono text-sm font-semibold tabular-nums text-foreground">
                {percentageLabel(member.percentage)}
              </td>
              <td className="px-5 py-4 text-right font-mono text-sm tabular-nums text-foreground">
                {member.attemptCount}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function LeaderboardCards({
  className,
  members,
}: {
  className: string;
  members: ClassLeaderboardMember[];
}) {
  return (
    <ul className="grid gap-3 p-4 md:hidden" aria-label={`Pořadí ve třídě ${className}`}>
      {members.map((member, index) => (
        <li
          key={`${member.studentName}-${member.rank}-${index}`}
          className={cn(
            "grid gap-4 rounded-md border border-border bg-surface px-4 py-4",
            member.currentStudent && "border-brand/35 bg-brand-subtle",
          )}
        >
          <div className="flex items-start justify-between gap-4">
            <div className="flex min-w-0 items-start gap-3">
              <p className="min-w-8 shrink-0 font-mono text-base font-semibold tabular-nums text-foreground">
                <span className="sr-only">Pořadí </span>
                {member.rank}.
              </p>
              <div className="min-w-0">
                <p className="flex flex-wrap items-center gap-2 font-semibold text-foreground [overflow-wrap:anywhere]">
                  <span>{member.studentName}</span>
                  {member.currentStudent ? <Badge variant="brand">Ty</Badge> : null}
                </p>
              </div>
            </div>
            <p className="shrink-0 font-mono text-sm font-semibold tabular-nums text-foreground">
              <span className="sr-only">Úspěšnost </span>
              {percentageLabel(member.percentage)}
            </p>
          </div>
          <dl className="grid grid-cols-2 gap-4 border-t border-border pt-3 text-sm">
            <div>
              <dt className="text-muted-foreground">Body</dt>
              <dd className="mt-1 font-mono font-semibold tabular-nums text-foreground">
                {scoreLabel(member)}
              </dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Pokusy</dt>
              <dd className="mt-1 font-semibold text-foreground">
                {attemptCountLabel(member.attemptCount)}
              </dd>
            </div>
          </dl>
        </li>
      ))}
    </ul>
  );
}

function LeaderboardGroup({
  leaderboard,
  position,
}: {
  leaderboard: ClassLeaderboard;
  position: number;
}) {
  const headingId = `class-leaderboard-${position}`;

  return (
    <section aria-labelledby={headingId}>
      <Panel className="overflow-hidden">
        <PanelHeader className="gap-1.5 border-b border-border pb-5">
          <PanelTitle id={headingId}>{leaderboard.className}</PanelTitle>
          <PanelDescription>
            {memberCountLabel(leaderboard.members.length)} v pořadí
          </PanelDescription>
        </PanelHeader>
        {leaderboard.members.length === 0 ? (
          <PanelContent>
            <EmptyState
              compact
              icon={UsersRound}
              heading="Ve třídě zatím není koho zobrazit"
              description="Pořadí se doplní, jakmile budou ve třídě dostupní žáci."
            />
          </PanelContent>
        ) : (
          <PanelContent className="p-0 sm:p-0">
            <LeaderboardTable
              className={leaderboard.className}
              members={leaderboard.members}
            />
            <LeaderboardCards
              className={leaderboard.className}
              members={leaderboard.members}
            />
          </PanelContent>
        )}
      </Panel>
    </section>
  );
}

export function StudentClassLeaderboard() {
  const router = useRouter();
  const [leaderboards, setLeaderboards] = useState<ClassLeaderboard[]>([]);
  const [status, setStatus] = useState<"error" | "loading" | "ready">(
    "loading",
  );
  const [error, setError] = useState("");
  const [refreshMessage, setRefreshMessage] = useState("");

  const loadLeaderboards = useCallback(async (background = false) => {
    if (!background) {
      setStatus("loading");
      setError("");
    }

    try {
      const loadedLeaderboards = await sessionService.myLeaderboards();
      setLeaderboards(
        [...loadedLeaderboards].sort((left, right) =>
          left.className.localeCompare(right.className, "cs"),
        ),
      );
      setStatus("ready");
      setError("");
      setRefreshMessage("");
    } catch (loadError) {
      const message = messageFromError(
        loadError,
        "Pořadí ve třídě se teď nepodařilo načíst.",
      );
      if (background) {
        setRefreshMessage(`${message} Zkusím to znovu automaticky.`);
      } else {
        setError(message);
        setStatus("error");
      }
    }
  }, []);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => void loadLeaderboards(), 0);
    return () => window.clearTimeout(initialLoad);
  }, [loadLeaderboards]);

  useLiveEvents(LEADERBOARD_EVENTS, () => void loadLeaderboards(true));

  return (
    <div className="mx-auto w-full max-w-[70rem]">
      <header className="border-b border-border pb-7">
        <Button
          variant="quiet"
          size="sm"
          className="mb-4 -ml-3"
          onClick={() => router.push("/dashboard")}
          leadingIcon={<ArrowLeft className="size-4" />}
        >
          Zpět na přehled
        </Button>
        <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-brand-text">
          <UsersRound aria-hidden="true" className="size-5" />
          Tvoje třída
        </div>
        <h1 className="text-3xl leading-tight font-semibold tracking-[-0.025em] text-balance text-foreground sm:text-4xl">
          Pořadí ve třídě
        </h1>
        <p className="mt-2 max-w-2xl text-base leading-7 text-muted-foreground">
          Souhrn bodů z dokončených kvízů v každé třídě, do které patříš.
        </p>
      </header>

      <section aria-label="Pořadí v mých třídách" className="mt-8">
        {refreshMessage ? (
          <p
            role="status"
            className="mb-4 rounded-md border border-border bg-surface px-4 py-3 text-sm leading-6 text-muted-foreground"
          >
            {refreshMessage}
          </p>
        ) : null}

        {status === "loading" ? (
          <ClassLeaderboardSkeleton />
        ) : status === "error" ? (
          <EmptyState
            icon={RefreshCw}
            heading="Pořadí se nepodařilo načíst"
            description={error}
            action={
              <Button onClick={() => void loadLeaderboards()}>
                Zkusit znovu
              </Button>
            }
          />
        ) : leaderboards.length === 0 ? (
          <EmptyState
            icon={UsersRound}
            heading="Zatím nejsi v žádné třídě"
            description="Až tě vyučující zařadí do třídy, uvidíš tady společné pořadí."
            action={<Button onClick={() => router.push("/dashboard")}>Na přehled</Button>}
          />
        ) : (
          <div className="grid gap-6">
            {leaderboards.map((leaderboard, index) => (
              <LeaderboardGroup
                key={leaderboard.classUuid}
                leaderboard={leaderboard}
                position={index}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
