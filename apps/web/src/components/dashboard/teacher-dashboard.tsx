"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  BarChart3,
  HelpCircle,
  LibraryBig,
  ListChecks,
  Plus,
  Search,
} from "lucide-react";
import {
  CardSkeletonGrid,
  EmptyState,
  ErrorBanner,
  Stat,
} from "@/components/common/feedback";
import { PageHeader } from "@/components/layout/page-header";
import { QuizCard } from "@/components/quiz/quiz-card";
import { QuizImportCard } from "@/components/quiz/quiz-import-card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api } from "@/lib/api";
import type { Page } from "@/lib/api";
import type { Quiz } from "@/lib/types";

export function TeacherDashboard() {
  const [page, setPage] = useState<Page<Quiz> | null>(null);
  const [quizzes, setQuizzes] = useState<Quiz[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [pageNum, setPageNum] = useState(0);

  useEffect(() => {
    let active = true;
    api
      .listQuizzes(pageNum)
      .then((data) => {
        if (active) {
          setPage(data);
          setQuizzes(data.content);
        }
      })
      .catch((err) => {
        if (active) {
          setError(err instanceof Error ? err.message : "Načtení se nepodařilo");
          setQuizzes([]);
        }
      });
    return () => {
      active = false;
    };
  }, [pageNum]);

  const stats = useMemo(() => {
    if (!page || !quizzes) return null;
    const questions = quizzes.reduce((sum, q) => sum + q.questions.length, 0);
    const ready = quizzes.filter((q) => q.questions.length > 0).length;
    return {
      total: page.totalElements,
      questions,
      ready,
      averageQuestions: quizzes.length > 0 ? Math.round(questions / quizzes.length) : 0,
    };
  }, [page, quizzes]);

  const filtered = useMemo(() => {
    if (!quizzes) return null;
    const needle = query.trim().toLocaleLowerCase("cs");
    if (!needle) return quizzes;
    return quizzes.filter(
      (quiz) =>
        quiz.title.toLocaleLowerCase("cs").includes(needle) ||
        (quiz.description ?? "").toLocaleLowerCase("cs").includes(needle)
    );
  }, [quizzes, query]);

  const hasPagination = page ? page.totalPages > 1 : false;

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Vyučující"
        title="Přehled kvízů"
        description="Vytvářejte kvízy, sledujte jejich stav a spusťte hru, až bude připravená."
        actions={
          <Button size="lg" asChild>
            <Link href="/quiz/new">
              <Plus aria-hidden="true" data-icon="inline-start" />
              Nový kvíz
            </Link>
          </Button>
        }
      />

      <QuizImportCard />

      {stats && stats.total > 0 && (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <Stat label="Kvízů celkem" value={stats.total} icon={LibraryBig} />
          <Stat label="Otázek celkem" value={stats.questions} icon={ListChecks} />
          <Stat
            label="Připraveno ke spuštění"
            value={stats.ready}
            hint={
              stats.ready < stats.total
                ? `${stats.total - stats.ready} kvízů bez otázek`
                : "Všechny kvízy mají otázky"
            }
            icon={HelpCircle}
          />
          <Stat
            label="Otázek na kvíz"
            value={stats.averageQuestions}
            hint="Průměrná délka kvízu"
            icon={BarChart3}
          />
        </div>
      )}

      {error && <ErrorBanner message={error} onDismiss={() => setError(null)} />}

      {!quizzes ? (
        <CardSkeletonGrid />
      ) : quizzes.length === 0 ? (
        <EmptyState
          icon={LibraryBig}
          title="Zatím tu nejsou žádné kvízy"
          description="Vytvořte první kvíz, přidejte otázky a spusťte ho ve třídě."
          action={
            <Button asChild>
              <Link href="/quiz/new">
                <Plus aria-hidden="true" data-icon="inline-start" />
                Vytvořit první kvíz
              </Link>
            </Button>
          }
        />
      ) : (
        <div className="space-y-4">
          {quizzes.length > 4 && (
            <div className="relative max-w-sm">
              <Search
                aria-hidden="true"
                className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
              />
              <Input
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Hledat v kvízech…"
                aria-label="Hledat v kvízech"
                className="pl-9"
              />
            </div>
          )}

          {filtered && filtered.length === 0 ? (
            <EmptyState
              icon={Search}
              title="Nic nenalezeno"
              description={`Pro „${query}" neexistuje žádný kvíz.`}
              action={
                <Button variant="ghost" onClick={() => setQuery("")}>
                  Zrušit hledání
                </Button>
              }
            />
          ) : (
             <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
               {filtered?.map((quiz, index) => (
                 <QuizCard key={quiz.uuid} quiz={quiz} index={index} />
               ))}
            </div>
          )}

          {hasPagination && !query && (
            <div className="flex items-center justify-between pt-4">
              <p className="text-sm text-muted-foreground">
                Strana {page!.number + 1} z {page!.totalPages} ({page!.totalElements} celkem)
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page!.first}
                  onClick={() => setPageNum((p) => p - 1)}
                >
                  Předchozí
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page!.last}
                  onClick={() => setPageNum((p) => p + 1)}
                >
                  Dalši
                </Button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
