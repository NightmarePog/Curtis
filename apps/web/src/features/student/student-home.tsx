"use client";

import {
  ArrowRight,
  BookOpen,
  Clock3,
  GraduationCap,
  History,
  ListChecks,
  RefreshCw,
  UserRound,
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
  PanelFooter,
  PanelHeader,
  PanelTitle,
} from "@/components/ui";
import { SessionListSkeleton } from "@/components/page-skeletons";
import { messageFromError } from "@/lib/http";
import { useLiveEvents } from "@/lib/live-events";
import { sessionService } from "@/lib/services";
import type { ActiveSession } from "@/types/domain";

const ACTIVE_SESSION_EVENTS = ["sessions-changed"] as const;

const dateTimeFormatter = new Intl.DateTimeFormat("cs-CZ", {
  dateStyle: "medium",
  timeStyle: "short",
});

const relativeTimeFormatter = new Intl.RelativeTimeFormat("cs-CZ", {
  numeric: "auto",
});

function sessionCountLabel(count: number) {
  if (count === 1) return "1 aktivní relace";
  if (count >= 2 && count <= 4) return `${count} aktivní relace`;
  return `${count} aktivních relací`;
}

function questionCountLabel(count: number) {
  if (count === 1) return "1 otázka";
  if (count >= 2 && count <= 4) return `${count} otázky`;
  return `${count} otázek`;
}

function validDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function formatDateTime(value: string) {
  const date = validDate(value);
  return date ? dateTimeFormatter.format(date) : "Čas není k dispozici";
}

function formatExpiry(value: string, now: number) {
  const date = validDate(value);
  if (!date) return "Čas ukončení není k dispozici";

  const difference = date.getTime() - now;
  if (difference <= 0) return "Relace skončila";
  if (difference < 60_000) return "Končí za méně než minutu";

  return `Končí ${relativeTimeFormatter.format(
    Math.ceil(difference / 60_000),
    "minute",
  )}`;
}

function SessionCard({
  now,
  onOpen,
  session,
}: {
  now: number;
  onOpen: (sessionUuid: string) => void;
  session: ActiveSession;
}) {
  const context = [session.subject, session.chapter].filter(Boolean).join(" · ");
  const expiryLabel = formatExpiry(session.expiresAt, now);

  return (
    <Panel className="flex h-full flex-col overflow-hidden">
      <PanelHeader className="gap-3">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0 flex-1">
            <PanelTitle as="h3" className="text-xl">
              {session.title}
            </PanelTitle>
            {session.description ? (
              <PanelDescription className="mt-1">
                {session.description}
              </PanelDescription>
            ) : null}
          </div>
          <Badge variant="brand" className="shrink-0">
            Aktivní
          </Badge>
        </div>
        {context ? (
          <div className="flex min-w-0 items-center gap-2 text-sm text-brand-text">
            <BookOpen aria-hidden="true" className="size-4 shrink-0" />
            <span className="font-medium">{context}</span>
          </div>
        ) : null}
      </PanelHeader>

      <PanelContent className="flex-1 pt-4">
        <dl className="grid gap-x-5 gap-y-3 text-sm sm:grid-cols-2">
          <div className="flex min-w-0 items-start gap-2.5">
            <UserRound
              aria-hidden="true"
              className="mt-0.5 size-4 shrink-0 text-muted-foreground"
            />
            <div className="min-w-0">
              <dt className="sr-only">Vyučující</dt>
              <dd className="text-foreground">{session.teacherName}</dd>
            </div>
          </div>
          <div className="flex items-start gap-2.5">
            <ListChecks
              aria-hidden="true"
              className="mt-0.5 size-4 shrink-0 text-muted-foreground"
            />
            <div>
              <dt className="sr-only">Počet otázek</dt>
              <dd className="text-foreground">{questionCountLabel(session.questionCount)}</dd>
            </div>
          </div>
          <div className="flex items-start gap-2.5 sm:col-span-2">
            <Clock3
              aria-hidden="true"
              className="mt-0.5 size-4 shrink-0 text-muted-foreground"
            />
            <div>
              <dt className="sr-only">Doba relace</dt>
              <dd>
                <span className="font-medium text-foreground">{expiryLabel}</span>
                <span className="text-muted-foreground">
                  {" "}· do{" "}
                  <time dateTime={session.expiresAt}>
                    {formatDateTime(session.expiresAt)}
                  </time>
                </span>
              </dd>
            </div>
          </div>
        </dl>
      </PanelContent>

      <PanelFooter>
        <Button
          className="w-full sm:w-auto"
          onClick={() => onOpen(session.sessionUuid)}
          leadingIcon={<ArrowRight className="size-[1.125rem]" />}
        >
          Připojit se
        </Button>
      </PanelFooter>
    </Panel>
  );
}

export interface StudentHomeProps {
  onOpenResults?: () => void;
  onOpenSession?: (sessionUuid: string) => void;
}

export function StudentHome({
  onOpenResults,
  onOpenSession,
}: StudentHomeProps) {
  const router = useRouter();
  const [sessions, setSessions] = useState<ActiveSession[]>([]);
  const [status, setStatus] = useState<"loading" | "ready" | "error">(
    "loading",
  );
  const [error, setError] = useState("");
  const [refreshMessage, setRefreshMessage] = useState("");
  const [now, setNow] = useState(() => Date.now());

  const loadSessions = useCallback(async (background = false) => {
    if (!background) setStatus("loading");

    try {
      const activeSessions = await sessionService.active();
      setSessions(activeSessions);
      setStatus("ready");
      setError("");
      setRefreshMessage("");
      setNow(Date.now());
    } catch (loadError) {
      const message = messageFromError(
        loadError,
        "Aktivní relace se teď nepodařilo načíst.",
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
    const initialLoad = window.setTimeout(() => void loadSessions(), 0);
    return () => window.clearTimeout(initialLoad);
  }, [loadSessions]);

  useLiveEvents(ACTIVE_SESSION_EVENTS, () => void loadSessions(true));

  useEffect(() => {
    const interval = window.setInterval(() => setNow(Date.now()), 30_000);
    return () => window.clearInterval(interval);
  }, []);

  const visibleSessions = useMemo(
    () =>
      sessions
        .filter((session) => {
          const expiry = validDate(session.expiresAt);
          return !expiry || expiry.getTime() > now;
        })
        .sort((left, right) => {
          const leftStart = validDate(left.startedAt)?.getTime() ?? 0;
          const rightStart = validDate(right.startedAt)?.getTime() ?? 0;
          return rightStart - leftStart;
        }),
    [now, sessions],
  );

  const openSession = (sessionUuid: string) => {
    if (onOpenSession) {
      onOpenSession(sessionUuid);
      return;
    }
    router.push(`/session/${sessionUuid}`);
  };

  const openResults = () => {
    if (onOpenResults) {
      onOpenResults();
      return;
    }
    router.push("/results");
  };

  return (
    <div className="mx-auto w-full max-w-[74rem]">
      <header className="flex flex-col gap-5 border-b border-border pb-7 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-brand-text">
            <GraduationCap aria-hidden="true" className="size-5" />
            Studentský přehled
          </div>
          <h1 className="max-w-3xl text-3xl leading-tight font-semibold tracking-[-0.025em] text-balance text-foreground sm:text-4xl">
            Aktivní kvízy
          </h1>
          <p className="mt-2 max-w-2xl text-base leading-7 text-muted-foreground">
            Jakmile vyučující spustí relaci, objeví se tady automaticky.
          </p>
        </div>
        <Button
          variant="secondary"
          onClick={openResults}
          leadingIcon={<History className="size-[1.125rem]" />}
        >
          Moje výsledky
        </Button>
      </header>

      <section aria-labelledby="active-sessions-heading" className="mt-8">
        <div className="mb-4">
          <div>
            <h2 id="active-sessions-heading" className="text-xl font-semibold text-foreground">
              Relace, ke kterým se můžeš připojit
            </h2>
            {status === "ready" ? (
              <p className="mt-1 text-sm text-muted-foreground" aria-live="polite">
                {visibleSessions.length === 0
                  ? "Žádná relace právě neběží."
                  : sessionCountLabel(visibleSessions.length)}
              </p>
            ) : null}
          </div>
        </div>

        {refreshMessage ? (
          <p
            role="status"
            className="mb-4 rounded-md border border-border bg-surface px-4 py-3 text-sm text-muted-foreground"
          >
            {refreshMessage}
          </p>
        ) : null}

        {status === "loading" ? (
          <SessionListSkeleton />
        ) : status === "error" ? (
          <EmptyState
            heading="Relace se nepodařilo načíst"
            description={error}
            icon={RefreshCw}
            action={
              <Button onClick={() => void loadSessions()}>
                Zkusit znovu
              </Button>
            }
          />
        ) : visibleSessions.length === 0 ? (
          <EmptyState
            heading="Teď není spuštěný žádný kvíz"
            description="Stránku nemusíš obnovovat. Nová relace se tu ukáže automaticky."
            icon={Clock3}
          />
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {visibleSessions.map((session) => (
              <SessionCard
                key={session.sessionUuid}
                session={session}
                now={now}
                onOpen={openSession}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
