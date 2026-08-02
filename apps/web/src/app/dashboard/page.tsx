"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { isTeacher, useAuth } from "@/components/auth";
import { RequireAuth } from "@/components/guards";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorBanner,
  Input,
  LoadingScreen,
} from "@/components/ui";
import { api } from "@/lib/api";
import type { Quiz } from "@/lib/types";

function QuizCard({ quiz }: { quiz: Quiz }) {
  const router = useRouter();
  const auth = useAuth();
  const me = auth.status === "user" ? auth.me : undefined;
  const teacher = isTeacher(me);
  const [starting, setStarting] = useState(false);

  async function startSession() {
    setStarting(true);
    try {
      const sessionUuid = await api.createSession(quiz.uuid);
      router.push(`/session/${sessionUuid}`);
    } catch (error) {
      console.error(error);
      setStarting(false);
    }
  }

  return (
    <Card className="flex flex-col justify-between gap-4">
      <div>
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-semibold text-slate-900">{quiz.title}</h3>
          <Badge variant="neutral">{quiz.questions.length} otázek</Badge>
        </div>
        {quiz.description && (
          <p className="mt-1 line-clamp-2 text-sm text-slate-500">
            {quiz.description}
          </p>
        )}
        <div className="mt-3 flex flex-wrap gap-2">
          {quiz.shuffle && <Badge variant="amber">Náhodné pořadí</Badge>}
          {quiz.maxQuestionsPerSession != null && (
            <Badge variant="blue">
              Max {quiz.maxQuestionsPerSession} otázek
            </Badge>
          )}
        </div>
      </div>

      {teacher && (
        <div className="flex gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => router.push(`/quiz/${quiz.uuid}`)}
          >
            Spravovat
          </Button>
          <Button size="sm" loading={starting} onClick={startSession}>
            Spustit kvíz
          </Button>
        </div>
      )}
    </Card>
  );
}

function JoinPanel() {
  const router = useRouter();
  const [code, setCode] = useState("");

  function join() {
    const trimmed = code.trim();
    if (trimmed) {
      router.push(`/session/${trimmed}`);
    }
  }

  return (
    <Card className="bg-indigo-600 text-white">
      <h2 className="text-lg font-semibold">Připojit se do kvízu</h2>
      <p className="mt-1 text-sm text-indigo-100">
        Zadejte kód kvízu, který vám sdělil vyučující.
      </p>
      <div className="mt-4 flex gap-2">
        <Input
          value={code}
          onChange={(e) => setCode(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && join()}
          placeholder="Kód kvízu (UUID)"
          className="border-indigo-300 bg-white placeholder:text-indigo-300"
        />
        <Button
          variant="secondary"
          className="border-white/20 bg-white/10 text-white hover:bg-white/20"
          onClick={join}
        >
          Připojit se
        </Button>
      </div>
    </Card>
  );
}

export default function DashboardPage() {
  const router = useRouter();
  const [quizzes, setQuizzes] = useState<Quiz[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const auth = useAuth();
  const me = auth.status === "user" ? auth.me : undefined;
  const teacher = isTeacher(me);

  const load = useCallback(() => {
    api
      .listQuizzes()
      .then(setQuizzes)
      .catch((err) => setError(err.message));
  }, []);

  useEffect(() => {
    if (teacher) {
      load();
    }
  }, [load, teacher]);

  return (
    <RequireAuth>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-slate-900">
            {teacher ? "Kvízy" : "Curtis"}
          </h1>
          {teacher && (
            <Button onClick={() => router.push("/quiz/new")}>Nový kvíz</Button>
          )}
        </div>

        {!teacher && (
          <div className="space-y-4">
            <JoinPanel />
            <Card>
              <h2 className="text-lg font-semibold text-slate-900">
                Jak to funguje
              </h2>
              <ol className="mt-3 list-decimal space-y-1 pl-5 text-sm text-slate-600">
                <li>Vyučující spustí kvíz a sdělí vám kód.</li>
                <li>Připojíte se zadáním kódu.</li>
                <li>Odpovídáte na otázky, než vyprší čas.</li>
                <li>Na konci uvidíte výsledek.</li>
              </ol>
            </Card>
          </div>
        )}

        {teacher && error && (
          <ErrorBanner message={error} onDismiss={() => setError(null)} />
        )}

        {teacher &&
          (quizzes === null ? (
            <LoadingScreen />
          ) : quizzes.length === 0 ? (
            <EmptyState
              title="Zatím tu nejsou žádné kvízy"
              description="Vytvořte první kvíz a pusťte se do práce."
              action={
                <Button onClick={() => router.push("/quiz/new")}>
                  Vytvořit kvíz
                </Button>
              }
            />
          ) : (
            <div className="grid gap-4 sm:grid-cols-2">
              {quizzes.map((quiz) => (
                <QuizCard key={quiz.uuid} quiz={quiz} />
              ))}
            </div>
          ))}
      </div>
    </RequireAuth>
  );
}
