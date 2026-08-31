"use client";

import { useState } from "react";
import { Check, Image as ImageIcon, Pencil, Timer, Trash2 } from "lucide-react";
import { QuestionEditor } from "@/components/quiz/question-editor";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { Question, QuestionCreateDto } from "@/lib/types";

const label = (index: number) => String.fromCharCode(65 + index);

export function QuestionCard({
  question,
  position,
  onEdit,
  onDelete,
  onCancelEdit,
  saving,
}: {
  question: Question;
  position: number;
  onEdit: (payload: QuestionCreateDto) => void;
  onDelete: () => void;
  onCancelEdit: () => void;
  saving: boolean;
}) {
  const [editing, setEditing] = useState(false);

  if (editing) {
    return (
      <QuestionEditor
        initial={question}
        onSubmit={(payload) => {
          onEdit(payload);
          setEditing(false);
        }}
        onCancel={() => {
          setEditing(false);
          onCancelEdit();
        }}
        submitting={saving}
      />
    );
  }

  return (
    <article className="group rounded-xl border border-border bg-card p-4 transition-colors duration-200 hover:border-ring/30">
      <div className="flex items-start gap-3">
        <span
          data-numeric
          className="mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-lg bg-muted text-xs font-semibold text-muted-foreground"
        >
          {position}
        </span>

        <div className="min-w-0 flex-1">
          <div className="flex items-start justify-between gap-3">
            <p className="text-pretty font-medium leading-snug text-foreground">
              {question.question}
            </p>
            <div className="flex shrink-0 flex-wrap justify-end gap-1.5">
              <Badge variant="blue">{typeLabel(question.type)}</Badge>
              <Badge variant="neutral">
                <Timer aria-hidden="true" data-icon="inline-start" />
                {question.timeInSeconds ?? "∞"} s
              </Badge>
              <Badge variant="neutral">{question.points} b.</Badge>
            </div>
          </div>

          {question.type === "MULTIPLE_CHOICE" && (
            <ul className="mt-3 grid gap-1.5 sm:grid-cols-2">
              {question.answers.map((answer, index) => (
                <li
                  key={index}
                  className={cn(
                    "flex items-center gap-2 rounded-lg border px-2.5 py-2 text-sm",
                    answer.isCorrect
                      ? "border-success/30 bg-success-soft text-foreground"
                      : "border-border bg-muted/30 text-muted-foreground"
                  )}
                >
                  <span
                    className={cn(
                      "flex size-5 shrink-0 items-center justify-center rounded text-[0.65rem] font-semibold",
                      answer.isCorrect
                        ? "bg-success/20 text-success"
                        : "bg-muted text-muted-foreground"
                    )}
                  >
                    {answer.isCorrect ? <Check aria-hidden="true" className="size-3" /> : label(index)}
                  </span>
                  <span className="min-w-0 flex-1 break-words">{answer.answer}</span>
                </li>
              ))}
            </ul>
          )}

          {question.type === "MATCHING" && (
            <ul className="mt-3 grid gap-1.5 sm:grid-cols-2">
              {question.pairs.map((pair, index) => (
                <li key={index} className="rounded-lg border border-border bg-muted/30 px-2.5 py-2 text-sm">
                  <span className="font-medium text-foreground">{pair.left}</span>
                  <span className="mx-2 text-muted-foreground">-&gt;</span>
                  <span className="text-muted-foreground">{pair.right}</span>
                </li>
              ))}
            </ul>
          )}

          {question.type === "FREE_TEXT" && (
            <p className="mt-3 rounded-lg border border-border bg-muted/30 px-2.5 py-2 text-sm text-muted-foreground">
              Žák zadá vlastní textovou odpověď.
            </p>
          )}

          {(question.codeSnippet || question.imageRef) && (
            <div className="mt-3 space-y-2">
              {question.codeSnippet && (
                <pre className="overflow-x-auto rounded-lg border border-border bg-muted/50 p-3 font-mono text-xs text-foreground">
                  {question.codeSnippet}
                </pre>
              )}
              {question.imageRef && (
                <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
                  <ImageIcon aria-hidden="true" className="size-3.5" />
                  {question.imageRef}
                </p>
              )}
            </div>
          )}

          <div className="mt-3 flex justify-end gap-1.5 opacity-100 transition-opacity md:opacity-60 md:group-focus-within:opacity-100 md:group-hover:opacity-100">
            <Button variant="ghost" size="sm" onClick={() => setEditing(true)}>
              <Pencil aria-hidden="true" data-icon="inline-start" />
              Upravit
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={onDelete}
              className="text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
            >
              <Trash2 aria-hidden="true" data-icon="inline-start" />
              Smazat
            </Button>
          </div>
        </div>
      </div>
    </article>
  );
}

function typeLabel(type: Question["type"]) {
  return type === "MATCHING"
    ? "Přiřazování"
    : type === "FREE_TEXT"
      ? "Volný text"
      : "Výběr";
}
