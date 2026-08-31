"use client";

import { useEffect, useMemo, useState } from "react";
import {
  Check,
  Copy,
  Link2,
  Medal,
  Pencil,
  TrendingUp,
  Users,
} from "lucide-react";
import {
  EmptyState,
  ErrorBanner,
  LoadingScreen,
  Stat,
} from "@/components/common/feedback";
import { PageHeader } from "@/components/layout/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api } from "@/lib/api";
import { cn } from "@/lib/utils";
import type { PendingTextAnswer, QuizResult } from "@/lib/types";

const POLL_MS = 3000;

export function LiveSessionPanel({ sessionUuid }: { sessionUuid: string }) {
  const [copied, setCopied] = useState<"code" | "link" | null>(null);
  const [results, setResults] = useState<QuizResult[] | null>(null);
  const [pending, setPending] = useState<PendingTextAnswer[] | null>(null);
  const [draftPoints, setDraftPoints] = useState<Record<number, string>>({});
  const [grading, setGrading] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Poll for results while the game is live.
  useEffect(() => {
    let active = true;

    const poll = () =>
      Promise.all([
        api.sessionResults(sessionUuid),
        api.pendingTextAnswers(sessionUuid),
      ])
        .then(([resultData, pendingData]) => {
          if (!active) return;
          setResults(resultData);
          setPending(pendingData);
          setError(null);
        })
        .catch((err) => {
          if (!active) return;
          setError(err instanceof Error ? err.message : "Načtení se nepodařilo");
        });

    poll();
    const id = window.setInterval(poll, POLL_MS);
    return () => {
      active = false;
      window.clearInterval(id);
    };
  }, [sessionUuid]);

  async function grade(answer: PendingTextAnswer) {
    const raw = draftPoints[answer.resultId] ?? String(answer.points);
    const awardedPoints = Number.parseInt(raw, 10);
    if (!Number.isInteger(awardedPoints) || awardedPoints < 0 || awardedPoints > answer.points) {
      setError(`Body musí být celé číslo od 0 do ${answer.points}.`);
      return;
    }

    setGrading(answer.resultId);
    setError(null);
    try {
      await api.gradeTextAnswer(sessionUuid, answer.resultId, awardedPoints);
      setPending((previous) => previous?.filter((item) => item.resultId !== answer.resultId) ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Hodnocení se nepodařilo uložit");
    } finally {
      setGrading(null);
    }
  }

  const joinUrl =
    typeof window !== "undefined"
      ? `${window.location.origin}/session/join`
      : "/session/join";

  async function copyText(kind: "code" | "link", text: string) {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(kind);
      window.setTimeout(() => setCopied(null), 1800);
    } catch {
      setError("Schránka není dostupná — zkopírujte kód ručně.");
    }
  }

  const stats = useMemo(() => {
    if (!results?.length) return null;
    const percents = results.map((r) =>
      r.maxScore ? (r.score / r.maxScore) * 100 : 0
    );
    return {
      players: results.length,
      average: Math.round(percents.reduce((a, b) => a + b, 0) / results.length),
      best: Math.round(Math.max(...percents)),
    };
  }, [results]);

  // Leaderboard order: best percentage first, earliest finish breaks ties.
  const ranked = useMemo(() => {
    if (!results) return null;
    return [...results].sort((a, b) => {
      const pa = a.maxScore ? a.score / a.maxScore : 0;
      const pb = b.maxScore ? b.score / b.maxScore : 0;
      if (pb !== pa) return pb - pa;
      return Date.parse(a.playedAt) - Date.parse(b.playedAt);
    });
  }, [results]);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <PageHeader
        eyebrow="Živá hra"
        title="Kvíz běží"
        description="Sdílejte kód se třídou. Výsledky se aktualizují automaticky."
        backHref="/dashboard"
        backLabel="Zpět na přehled"
      />

      {/* Share code */}
      <section className="surface surface-raised overflow-hidden">
        <div className="space-y-4 px-5 py-7 text-center sm:px-6">
          <div className="flex items-center justify-center gap-2">
            <span className="relative flex size-2">
              <span className="absolute inset-0 rounded-full bg-success/70 animate-pulse-ring" />
              <span className="relative size-2 rounded-full bg-success" />
            </span>
            <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
              Kód kvízu
            </p>
          </div>

          <p className="break-all font-mono text-xl font-semibold tracking-tight text-foreground sm:text-2xl">
            {sessionUuid}
          </p>

          <div className="flex flex-wrap items-center justify-center gap-2">
            <Button
              variant="secondary"
              onClick={() => copyText("code", sessionUuid)}
            >
              {copied === "code" ? (
                <Check aria-hidden="true" data-icon="inline-start" />
              ) : (
                <Copy aria-hidden="true" data-icon="inline-start" />
              )}
              {copied === "code" ? "Zkopírováno" : "Kopírovat kód"}
            </Button>
            <Button variant="ghost" onClick={() => copyText("link", joinUrl)}>
              {copied === "link" ? (
                <Check aria-hidden="true" data-icon="inline-start" />
              ) : (
                <Link2 aria-hidden="true" data-icon="inline-start" />
              )}
              {copied === "link" ? "Zkopírováno" : "Kopírovat odkaz"}
            </Button>
          </div>

          <p className="text-xs text-muted-foreground">
            Žáci se připojí na{" "}
            <span className="font-mono text-foreground">{joinUrl}</span> zadáním
            kódu.
          </p>
        </div>
      </section>

      {error && <ErrorBanner message={error} onDismiss={() => setError(null)} />}

      {stats && (
        <div className="grid gap-3 sm:grid-cols-3">
          <Stat label="Dokončilo" value={stats.players} icon={Users} />
          <Stat
            label="Průměrná úspěšnost"
            value={`${stats.average} %`}
            icon={TrendingUp}
          />
          <Stat label="Nejlepší výsledek" value={`${stats.best} %`} icon={Medal} />
        </div>
      )}

      <section className="surface" aria-labelledby="textove-odpovedi">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border px-5 py-4 sm:px-6">
          <h2
            id="textove-odpovedi"
            className="flex items-center gap-2 text-base font-semibold tracking-tight text-foreground"
          >
            <Pencil aria-hidden="true" className="size-4 text-brand" />
            Textové odpovědi
          </h2>
          <Badge variant={pending?.length ? "amber" : "neutral"}>
            {pending?.length ?? 0} čeká na hodnocení
          </Badge>
        </div>
        <div className="space-y-3 p-5 sm:p-6">
          {pending === null ? (
            <LoadingScreen label="Načítám textové odpovědi…" />
          ) : pending.length === 0 ? (
            <p className="rounded-lg border border-border bg-muted/30 px-3 py-3 text-sm text-muted-foreground">
              Zatím nejsou k dispozici žádné textové odpovědi ke kontrole.
            </p>
          ) : (
            pending.map((answer) => (
              <article key={answer.resultId} className="rounded-xl border border-border bg-muted/20 p-4">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <p className="text-xs text-muted-foreground">
                      Žák <span className="font-mono text-foreground">{answer.studentId.slice(0, 8)}…</span>
                      <span className="mx-1.5">·</span> otázka {answer.questionIndex + 1}
                    </p>
                    <p className="mt-1 font-medium text-foreground">{answer.question}</p>
                  </div>
                  <Badge variant="amber">0–{answer.points} b.</Badge>
                </div>
                <p className="mt-3 whitespace-pre-wrap rounded-lg border border-border bg-card px-3 py-3 text-sm text-foreground">
                  {answer.text}
                </p>
                <div className="mt-3 flex items-center justify-end gap-2">
                  <Input
                    type="number"
                    min={0}
                    max={answer.points}
                    value={draftPoints[answer.resultId] ?? answer.points}
                    onChange={(event) =>
                      setDraftPoints((previous) => ({
                        ...previous,
                        [answer.resultId]: event.target.value,
                      }))
                    }
                    aria-label={`Body za odpověď žáka ${answer.studentId}`}
                    className="w-24"
                  />
                  <Button size="sm" loading={grading === answer.resultId} onClick={() => void grade(answer)}>
                    Uložit body
                  </Button>
                </div>
              </article>
            ))
          )}
        </div>
      </section>

      {/* Leaderboard */}
      <section className="surface" aria-labelledby="vysledky">
        <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-4 sm:px-6">
          <h2
            id="vysledky"
            className="text-base font-semibold tracking-tight text-foreground"
          >
            Výsledky
          </h2>
          <Badge variant="neutral">Aktualizuje se každé 3 s</Badge>
        </div>

        <div className="p-5 sm:p-6">
          {ranked === null ? (
            <LoadingScreen label="Načítám výsledky…" />
          ) : ranked.length === 0 ? (
            <EmptyState
              icon={Users}
              title="Zatím se nikdo nepřipojil"
              description="Jakmile žáci dokončí kvíz, objeví se tady. Kód zůstává aktivní."
            />
          ) : (
            <>
              <div className="mb-6 grid gap-2 sm:grid-cols-3">
                {ranked.slice(0, 3).map((result, index) => {
                  const percent = result.maxScore
                    ? Math.round((result.score / result.maxScore) * 100)
                    : 0;
                  const rankStyle =
                    index === 0
                      ? "border-warning/30 bg-warning-soft/40"
                      : index === 1
                        ? "border-border bg-muted/50"
                        : "border-brand/20 bg-brand-soft/30";

                  return (
                    <div
                      key={result.id}
                      className={cn("rounded-xl border p-3", rankStyle)}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-xs font-semibold text-muted-foreground">
                          {index + 1}. místo
                        </span>
                        <span data-numeric className="text-sm font-semibold text-foreground">
                          {percent} %
                        </span>
                      </div>
                      <p className="mt-2 truncate font-mono text-xs text-foreground">
                        {result.studentId.slice(0, 8)}…
                      </p>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {result.score}/{result.maxScore} bodů
                      </p>
                    </div>
                  );
                })}
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <caption className="sr-only">
                    Žebříček výsledků aktuální hry
                  </caption>
                  <thead>
                    <tr className="border-b border-border text-left text-xs font-medium tracking-wide text-muted-foreground uppercase">
                      <th scope="col" className="w-10 py-2.5 pr-2">
                        #
                      </th>
                      <th scope="col" className="py-2.5 pr-4">
                        Žák
                      </th>
                      <th scope="col" className="py-2.5 pr-4">
                        Skóre
                      </th>
                      <th scope="col" className="py-2.5">
                        Dokončeno
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/60">
                    {ranked.map((result, index) => {
                      const percent = result.maxScore
                        ? Math.round((result.score / result.maxScore) * 100)
                        : 0;
                      return (
                        <tr
                          key={result.id}
                          className="transition-colors hover:bg-muted/40"
                        >
                          <td className="py-3 pr-2">
                            <span
                              data-numeric
                              className={cn(
                                "flex size-6 items-center justify-center rounded-md text-xs font-semibold",
                                index === 0
                                  ? "bg-warning-soft text-warning"
                                  : "bg-muted text-muted-foreground"
                              )}
                            >
                              {index + 1}
                            </span>
                          </td>
                          <td className="py-3 pr-4 font-mono text-xs text-foreground">
                            {result.studentId.slice(0, 8)}…
                          </td>
                          <td className="py-3 pr-4">
                            <div className="flex items-center gap-2">
                              <span
                                data-numeric
                                className="font-medium text-foreground"
                              >
                                {result.score}/{result.maxScore}
                              </span>
                              <span
                                aria-hidden="true"
                                className="hidden h-1.5 w-20 overflow-hidden rounded-full bg-muted sm:block"
                              >
                                <span
                                  className={cn(
                                    "block h-full rounded-full",
                                    percent >= 70
                                      ? "bg-success"
                                      : percent >= 40
                                        ? "bg-warning"
                                        : "bg-destructive"
                                  )}
                                  style={{ width: `${percent}%` }}
                                />
                              </span>
                              <span
                                data-numeric
                                className="text-xs text-muted-foreground"
                              >
                                {percent} %
                              </span>
                            </div>
                          </td>
                          <td className="py-3 text-muted-foreground">
                            <time dateTime={result.playedAt}>
                              {new Date(result.playedAt).toLocaleTimeString(
                                "cs-CZ",
                                { hour: "2-digit", minute: "2-digit" }
                              )}
                            </time>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      </section>
    </div>
  );
}
