"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { isTeacher, useAuth } from "@/components/auth";
import { RequireAuth } from "@/components/guards";
import {
  Badge,
  Button,
  Card,
  ErrorBanner,
  LoadingScreen,
  Spinner,
} from "@/components/ui";
import { api } from "@/lib/api";
import type {
  QuestionAnswer,
  QuestionResponse,
  QuizResult,
  ResultsResponse,
} from "@/lib/types";
import { cn } from "@/lib/cn";

function useCountdown(seconds: number | null, onExpire: () => void) {
  const [remaining, setRemaining] = useState<number | null>(seconds);
  const [prevSeconds, setPrevSeconds] = useState<number | null>(seconds);
  const expireRef = useRef(onExpire);

  useEffect(() => {
    expireRef.current = onExpire;
  }, [onExpire]);

  if (prevSeconds !== seconds) {
    setPrevSeconds(seconds);
    setRemaining(seconds);
  }

  useEffect(() => {
    if (seconds == null) {
      return;
    }
    const id = window.setInterval(() => {
      setRemaining((current) => {
        if (current == null) {
          return current;
        }
        if (current <= 1) {
          window.clearInterval(id);
          expireRef.current();
          return 0;
        }
        return current - 1;
      });
    }, 1000);
    return () => window.clearInterval(id);
  }, [seconds]);

  return remaining;
}

function PlayView({ sessionUuid }: { sessionUuid: string }) {
  const router = useRouter();
  const [phase, setPhase] = useState<"joining" | "playing" | "finishing">(
    "joining"
  );
  const [question, setQuestion] = useState<QuestionResponse | null>(null);
  const [questionNumber, setQuestionNumber] = useState(0);
  const [selected, setSelected] = useState<number[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [results, setResults] = useState<ResultsResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    api
      .joinSession(sessionUuid)
      .then((first) => {
        if (!active) {
          return;
        }
        setQuestion(first);
        setQuestionNumber(1);
        setPhase("playing");
      })
      .catch((err) => {
        if (!active) {
          return;
        }
        setError(err instanceof Error ? err.message : "Nepodařilo se připojit");
        setPhase("finishing");
      });
    return () => {
      active = false;
    };
  }, [sessionUuid]);

  async function submit(answer: number[]) {
    if (submitting || phase !== "playing") {
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const next = await api.nextQuestion(sessionUuid, answer);
      setQuestion(next);
      setQuestionNumber((n) => n + 1);
      setSelected([]);
      setSubmitting(false);
    } catch (err) {
      if (
        err instanceof Error &&
        "status" in err &&
        (err as { status?: number }).status === 400
      ) {
        await finish();
        return;
      }
      setError(err instanceof Error ? err.message : "Nepodařilo se odeslat odpověď");
      setSubmitting(false);
    }
  }

  async function finish() {
    if (submitting) {
      return;
    }
    setSubmitting(true);
    setPhase("finishing");
    setError(null);
    try {
      const finalResults = await api.finishSession(sessionUuid);
      setResults(finalResults);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nepodařilo se dokončit kvíz");
      setSubmitting(false);
    }
  }

  function toggleAnswer(index: number) {
    setSelected((prev) =>
      prev.includes(index) ? prev.filter((i) => i !== index) : [...prev, index]
    );
  }

  const remaining = useCountdown(question?.timeInSeconds ?? null, () => {
    if (phase === "playing" && !submitting) {
      submit(selected);
    }
  });

  if (error && !question && !results) {
    return (
      <Card className="space-y-4 text-center">
        <p className="text-slate-700">{error}</p>
        <Button onClick={() => router.push("/dashboard")}>Zpět na kvízy</Button>
      </Card>
    );
  }

  if (results) {
    const percent = results.maxScore
      ? Math.round((results.score / results.maxScore) * 100)
      : 0;
    return (
      <div className="mx-auto max-w-2xl space-y-6">
        <Card className="text-center">
          <p className="text-sm font-medium text-slate-500">Výsledek</p>
          <p className="mt-2 text-5xl font-bold text-indigo-700">
            {results.score}
            <span className="text-2xl text-slate-400">/{results.maxScore}</span>
          </p>
          <p className="mt-2 text-sm text-slate-500">
            Úspěšnost {percent}%
          </p>
          <div className="mt-4">
            <Button onClick={() => router.push("/dashboard")}>
              Zpět na kvízy
            </Button>
          </div>
        </Card>

        <Card>
          <h2 className="mb-4 text-lg font-semibold text-slate-900">
            Přehled odpovědí
          </h2>
          <div className="space-y-4">
            {results.questions.map((qr, index) => (
              <div key={index} className="rounded-xl border border-slate-200 p-4">
                <p className="font-medium text-slate-900">
                  {index + 1}. {qr.question}
                </p>
                <ul className="mt-2 space-y-1.5">
                  {qr.answers.map((answer: QuestionAnswer, i) => (
                    <li
                      key={i}
                      className={cn(
                        "rounded-lg border px-3 py-1.5 text-sm",
                        answer.isCorrect
                          ? "border-emerald-200 bg-emerald-50 text-emerald-900"
                          : "border-slate-200 bg-slate-50 text-slate-700"
                      )}
                    >
                      {answer.answer}
                      {answer.isCorrect && (
                        <Badge variant="green" className="ml-2">
                          Správně
                        </Badge>
                      )}
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </Card>
      </div>
    );
  }

  if (phase === "joining" || !question) {
    return <LoadingScreen label="Připojuji se do kvízu…" />;
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      {error && <ErrorBanner message={error} onDismiss={() => setError(null)} />}
      <Card>
        <div className="mb-4 flex items-center justify-between">
          <Badge variant="blue">Otázka {questionNumber}</Badge>
          {remaining != null && (
            <Badge variant={remaining <= 10 ? "red" : "neutral"}>
              {remaining} s
            </Badge>
          )}
        </div>
        <h1 className="text-xl font-bold text-slate-900">{question.question}</h1>
        <div className="mt-6 space-y-2">
          {question.answers.map((answer: QuestionAnswer, index) => {
            const isSelected = selected.includes(index);
            return (
              <button
                key={index}
                type="button"
                onClick={() => toggleAnswer(index)}
                disabled={submitting}
                className={cn(
                  "w-full rounded-xl border px-4 py-3 text-left text-sm transition-colors disabled:opacity-50",
                  isSelected
                    ? "border-indigo-600 bg-indigo-50 text-indigo-900 ring-2 ring-indigo-600/20"
                    : "border-slate-200 bg-white text-slate-700 hover:border-indigo-300 hover:bg-slate-50"
                )}
              >
                {answer.answer}
              </button>
            );
          })}
        </div>
        <div className="mt-6 flex items-center justify-between">
          <Button variant="ghost" onClick={() => finish()} disabled={submitting}>
            Ukončit kvíz
          </Button>
          <Button onClick={() => submit(selected)} loading={submitting}>
            Odpovědět
          </Button>
        </div>
      </Card>
    </div>
  );
}

function TeacherView({ sessionUuid }: { sessionUuid: string }) {
  const [copied, setCopied] = useState(false);
  const [results, setResults] = useState<QuizResult[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api
      .sessionResults(sessionUuid)
      .then(setResults)
      .catch((err) => setError(err.message));
  }, [sessionUuid]);

  useEffect(() => {
    load();
    const id = window.setInterval(load, 3000);
    return () => window.clearInterval(id);
  }, [load]);

  async function copyText(text: string) {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // clipboard unavailable
    }
  }

  const joinUrl =
    typeof window !== "undefined"
      ? `${window.location.origin}/session/join`
      : "/session/join";

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <Card className="text-center">
        <p className="text-sm font-medium text-slate-500">Kód kvízu</p>
        <p className="mt-2 break-all font-mono text-2xl font-bold text-indigo-700">
          {sessionUuid}
        </p>
        <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
          <Button variant="secondary" size="sm" onClick={() => copyText(sessionUuid)}>
            {copied ? "Zkopírováno ✓" : "Kopírovat kód"}
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => copyText(joinUrl)}
          >
            Kopírovat odkaz pro žáky
          </Button>
        </div>
        <p className="mt-3 text-xs text-slate-500">
          Žáci se připojí na {joinUrl} zadáním kódu.
        </p>
      </Card>

      {error && <ErrorBanner message={error} onDismiss={() => setError(null)} />}

      <Card>
        <h2 className="mb-4 text-lg font-semibold text-slate-900">Výsledky</h2>
        {results === null ? (
          <div className="flex justify-center py-8 text-slate-400">
            <Spinner />
          </div>
        ) : results.length === 0 ? (
          <p className="py-8 text-center text-sm text-slate-500">
            Zatím se nikdo nepřipojil.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-xs uppercase text-slate-500">
                  <th className="py-2 pr-4">Žák</th>
                  <th className="py-2 pr-4">Skóre</th>
                  <th className="py-2">Čas</th>
                </tr>
              </thead>
              <tbody>
                {results.map((result) => {
                  const percent = result.maxScore
                    ? Math.round((result.score / result.maxScore) * 100)
                    : 0;
                  return (
                    <tr
                      key={result.id}
                      className="border-b border-slate-100 last:border-0"
                    >
                      <td className="py-2.5 pr-4 font-medium text-slate-800">
                        {result.studentId.slice(0, 8)}…
                      </td>
                      <td className="py-2.5 pr-4">
                        {result.score}/{result.maxScore}{" "}
                        <span className="text-xs text-slate-500">
                          ({percent}%)
                        </span>
                      </td>
                      <td className="py-2.5 text-slate-500">
                        {new Date(result.playedAt).toLocaleTimeString("cs-CZ", {
                          hour: "2-digit",
                          minute: "2-digit",
                        })}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}

export default function SessionPage() {
  const params = useParams<{ uuid: string }>();
  const sessionUuid = params.uuid;
  const auth = useAuth();

  const me = auth.status === "user" ? auth.me : undefined;

  return (
    <RequireAuth>
      {isTeacher(me) ? (
        <TeacherView sessionUuid={sessionUuid} />
      ) : (
        <PlayView sessionUuid={sessionUuid} />
      )}
    </RequireAuth>
  );
}
