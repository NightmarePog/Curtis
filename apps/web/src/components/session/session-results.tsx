"use client";

import Link from "next/link";
import Image from "next/image";
import { Check, LayoutGrid, PartyPopper, Target, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { ResultsResponse } from "@/lib/types";

const label = (index: number) => String.fromCharCode(65 + index);

function mediaUrl(imageRef: string) {
  return imageRef.startsWith("http")
    ? imageRef
    : `/media/${encodeURIComponent(imageRef)}`;
}

function verdict(percent: number): { title: string; tone: string } {
  if (percent >= 90) return { title: "Výborně!", tone: "text-success" };
  if (percent >= 70) return { title: "Dobrá práce", tone: "text-success" };
  if (percent >= 50) return { title: "Solidní pokus", tone: "text-warning" };
  return { title: "Ještě to chce trénink", tone: "text-muted-foreground" };
}

export function SessionResults({ results }: { results: ResultsResponse }) {
  const percent = results.maxScore
    ? Math.round((results.score / results.maxScore) * 100)
    : 0;
  const { title, tone } = verdict(percent);

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      {/* Score hero */}
      <section className="surface surface-raised overflow-hidden animate-rise">
        <div className="flex flex-col items-center gap-4 px-6 py-9 text-center">
          <span className="flex size-12 items-center justify-center rounded-2xl bg-brand-soft text-brand">
            <PartyPopper aria-hidden="true" className="size-6" />
          </span>

          <div>
            <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
              Váš výsledek
            </p>
            <p
              data-numeric
              className="mt-2 text-6xl font-semibold tracking-tight text-foreground"
            >
              {results.score}
              <span className="text-3xl text-muted-foreground">
                /{results.maxScore}
              </span>
            </p>
            <p className={cn("mt-2 text-sm font-medium", tone)}>
              {title} — úspěšnost {percent} %
            </p>
          </div>

          <div
            role="progressbar"
            aria-valuenow={percent}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-label="Úspěšnost"
            className="h-2 w-full max-w-xs overflow-hidden rounded-full bg-muted"
          >
            <div
              className="h-full rounded-full bg-primary transition-[width] duration-700 ease-out-expo"
              style={{ width: `${percent}%` }}
            />
          </div>
        </div>
      </section>

      {/* Answer key */}
      <section className="surface" aria-labelledby="prehled">
        <div className="border-b border-border px-5 py-4 sm:px-6">
          <h2
            id="prehled"
            className="flex items-center gap-2 text-base font-semibold tracking-tight text-foreground"
          >
            <Target aria-hidden="true" className="size-4 text-brand" />
             Správné odpovědi
          </h2>
        </div>

        <ol className="divide-y divide-border">
          {results.questions.map((question, index) => (
            <li key={index} className="px-5 py-4 sm:px-6">
              <p className="text-pretty font-medium leading-snug text-foreground">
                <span data-numeric className="text-muted-foreground">
                  {index + 1}.
                </span>{" "}
                {question.question}
              </p>
              <div className="mt-2.5 flex flex-wrap items-center gap-1.5">
                <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                  {typeLabel(question.type)}
                </span>
                <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                  Maximum {question.points} b.
                </span>
                {question.type === "FREE_TEXT" && (
                  <span className="rounded-full bg-warning-soft px-2 py-0.5 text-xs font-medium text-warning">
                    Čeká na hodnocení
                  </span>
                )}
              </div>

              {question.codeSnippet && (
                <pre className="mt-2.5 overflow-x-auto rounded-lg border border-border bg-muted/50 p-3 font-mono text-xs text-foreground">
                  {question.codeSnippet}
                </pre>
              )}
              {question.imageRef && (
                <div className="relative mt-2.5 h-40 w-full overflow-hidden rounded-lg border border-border">
                  <Image
                    src={mediaUrl(question.imageRef)}
                    alt="Doplňující obrázek k otázce"
                    fill
                    unoptimized
                    className="object-contain"
                  />
                </div>
              )}

              {question.type === "MULTIPLE_CHOICE" && (
                <ul className="mt-2.5 grid gap-1.5 sm:grid-cols-2">
                  {question.answers.map((answer, i) => (
                    <li
                      key={i}
                      className={cn(
                        "flex items-center gap-2 rounded-lg border px-2.5 py-2 text-sm",
                        answer.isCorrect
                          ? "border-success/30 bg-success-soft text-foreground"
                          : "border-border bg-muted/30 text-muted-foreground"
                      )}
                    >
                      <span
                        aria-hidden="true"
                        className={cn(
                          "flex size-5 shrink-0 items-center justify-center rounded text-[0.65rem] font-semibold",
                          answer.isCorrect
                            ? "bg-success/20 text-success"
                            : "bg-muted text-muted-foreground"
                        )}
                      >
                        {answer.isCorrect ? <Check className="size-3" /> : label(i)}
                      </span>
                      <span className="min-w-0 flex-1 break-words">{answer.answer}</span>
                      {answer.isCorrect && (
                        <span className="shrink-0 text-xs font-medium text-success">Správně</span>
                      )}
                    </li>
                  ))}
                </ul>
              )}
              {question.type === "MATCHING" && (
                <ul className="mt-2.5 grid gap-1.5 sm:grid-cols-2">
                  {question.pairs.map((pair, i) => (
                    <li key={i} className="rounded-lg border border-border bg-muted/30 px-2.5 py-2 text-sm">
                      <span className="font-medium text-foreground">{pair.left}</span>
                      <span className="mx-2 text-muted-foreground">-&gt;</span>
                      <span className="text-muted-foreground">{pair.right}</span>
                    </li>
                  ))}
                </ul>
              )}
            </li>
          ))}
        </ol>
      </section>

      <div className="flex justify-center">
        <Button size="lg" asChild>
          <Link href="/dashboard">
            <LayoutGrid aria-hidden="true" data-icon="inline-start" />
            Zpět na přehled
          </Link>
        </Button>
      </div>
    </div>
  );
}

function typeLabel(type: ResultsResponse["questions"][number]["type"]) {
  return type === "MATCHING"
    ? "Přiřazování"
    : type === "FREE_TEXT"
      ? "Volný text"
      : "Výběr";
}

/** Shown when a session can't be joined or has already ended. */
export function SessionUnavailable({ message }: { message: string }) {
  return (
    <div className="mx-auto max-w-md">
      <div className="surface flex flex-col items-center gap-4 p-8 text-center animate-rise">
        <span className="flex size-12 items-center justify-center rounded-2xl bg-destructive-soft text-destructive">
          <X aria-hidden="true" className="size-6" />
        </span>
        <div className="space-y-1.5">
          <p className="text-base font-semibold text-foreground">
            Do kvízu se nelze připojit
          </p>
          <p className="text-pretty text-sm text-muted-foreground">{message}</p>
        </div>
        <div className="flex flex-wrap justify-center gap-2 pt-1">
          <Button variant="ghost" asChild>
            <Link href="/session/join">Zkusit jiný kód</Link>
          </Button>
          <Button asChild>
            <Link href="/dashboard">Zpět na přehled</Link>
          </Button>
        </div>
      </div>
    </div>
  );
}
