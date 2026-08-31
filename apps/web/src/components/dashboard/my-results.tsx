"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { QuizResult } from "@/lib/types";
import { PageHeader } from "@/components/layout/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Trophy, Calendar, Percent } from "lucide-react";

function formatDate(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString("cs-CZ", {
    day: "numeric",
    month: "numeric",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function MyResults() {
  const [results, setResults] = useState<QuizResult[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.myResults()
      .then(setResults)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="space-y-8">
        <PageHeader
          eyebrow="Výsledky"
          title="Moje výsledky"
          description="Načítání..."
        />
      </div>
    );
  }

  if (results.length === 0) {
    return (
      <div className="space-y-8">
        <PageHeader
          eyebrow="Výsledky"
          title="Moje výsledky"
          description="Zatím nemáte žádné vyplněné kvízy."
        />
      </div>
    );
  }

  const totalScore = results.reduce((sum, r) => sum + r.score, 0);
  const totalMax = results.reduce((sum, r) => sum + r.maxScore, 0);
  const avgPercent = totalMax > 0 ? Math.round((totalScore / totalMax) * 100) : 0;

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Výsledky"
        title="Moje výsledky"
        description={`Celkem ${results.length} kvízů, průměrná úspěšnost ${avgPercent} %`}
      />

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Celkem kvízů</CardTitle>
            <Trophy className="size-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{results.length}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Celkové skóre</CardTitle>
            <Percent className="size-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {totalScore} / {totalMax}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Průměr</CardTitle>
            <Calendar className="size-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{avgPercent} %</div>
          </CardContent>
        </Card>
      </div>

      <div className="space-y-3">
        {results.map((result) => {
          const percent = result.maxScore > 0
            ? Math.round((result.score / result.maxScore) * 100)
            : 0;
          const variant =
            percent >= 90 ? "default"
            : percent >= 70 ? "secondary"
            : percent >= 50 ? "outline"
            : "destructive";

          return (
            <Card key={result.id} className="animate-rise">
              <CardContent className="flex items-center justify-between p-4">
                <div className="space-y-1">
                  <p className="text-sm font-medium">
                    Kvíz #{result.quizUuid.slice(0, 8)}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {formatDate(result.playedAt)}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-sm text-muted-foreground">
                    {result.score} / {result.maxScore}
                  </span>
                  <Badge variant={variant}>{percent} %</Badge>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
