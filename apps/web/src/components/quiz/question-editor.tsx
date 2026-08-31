"use client";

import { useId, useState } from "react";
import { Check, Code2, Image as ImageIcon, Plus, Save, Timer, Trash2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";
import type {
  MatchingPair,
  Question,
  QuestionCreateDto,
  QuestionType,
} from "@/lib/types";

interface AnswerDraft {
  answer: string;
  isCorrect: boolean;
}

const TIME_PRESETS = [10, 15, 30, 60, 90];

const emptyAnswers = (): AnswerDraft[] => [
  { answer: "", isCorrect: true },
  { answer: "", isCorrect: false },
];

const emptyPairs = (): MatchingPair[] => [
  { left: "", right: "" },
  { left: "", right: "" },
];

const label = (index: number) => String.fromCharCode(65 + index);

const typeLabels: Record<QuestionType, string> = {
  MULTIPLE_CHOICE: "Výběr z možností",
  MATCHING: "Přiřazování dvojic",
  FREE_TEXT: "Volný text",
};

export function QuestionEditor({
  initial,
  onSubmit,
  onCancel,
  submitting,
}: {
  initial?: Question;
  onSubmit: (payload: QuestionCreateDto) => void;
  onCancel: () => void;
  submitting: boolean;
}) {
  const uid = useId();
  const [text, setText] = useState(initial?.question ?? "");
  const [type, setType] = useState<QuestionType>(
    initial?.type ?? "MULTIPLE_CHOICE"
  );
  const [points, setPoints] = useState(String(initial?.points ?? 1));
  const [time, setTime] = useState(String(initial?.timeInSeconds ?? 30));
  const [codeSnippet, setCodeSnippet] = useState(initial?.codeSnippet ?? "");
  const [imageRef, setImageRef] = useState(initial?.imageRef ?? "");
  const [answers, setAnswers] = useState<AnswerDraft[]>(
    initial?.answers?.length
      ? initial.answers.map((answer) => ({
          answer: answer.answer,
          isCorrect: !!answer.isCorrect,
        }))
      : emptyAnswers()
  );
  const [pairs, setPairs] = useState<MatchingPair[]>(
    initial?.pairs?.length ? initial.pairs : emptyPairs()
  );
  const [touched, setTouched] = useState(false);

  const timeNum = Number.parseInt(time, 10);
  const pointsNum = Number.parseInt(points, 10);
  const problems = {
    text: !text.trim(),
    points: !Number.isFinite(pointsNum) || pointsNum < 1,
    time: !Number.isFinite(timeNum) || timeNum < 1,
    answers:
      type === "MULTIPLE_CHOICE" &&
      (answers.length < 2 || answers.some((answer) => !answer.answer.trim())),
    noCorrect:
      type === "MULTIPLE_CHOICE" && !answers.some((answer) => answer.isCorrect),
    pairs:
      type === "MATCHING" &&
      (pairs.length < 2 ||
        pairs.some((pair) => !pair.left.trim() || !pair.right.trim())),
  };
  const invalid = Object.values(problems).some(Boolean);

  function updateAnswer(index: number, patch: Partial<AnswerDraft>) {
    setAnswers((prev) =>
      prev.map((answer, i) => (i === index ? { ...answer, ...patch } : answer))
    );
  }

  function updatePair(index: number, patch: Partial<MatchingPair>) {
    setPairs((prev) =>
      prev.map((pair, i) => (i === index ? { ...pair, ...patch } : pair))
    );
  }

  function submit(event: React.FormEvent) {
    event.preventDefault();
    setTouched(true);
    if (invalid) return;

    const payload: QuestionCreateDto = {
      question: text.trim(),
      type,
      points: pointsNum,
      codeSnippet: codeSnippet.trim() || null,
      imageRef: imageRef.trim() || null,
      answers:
        type === "MULTIPLE_CHOICE"
          ? answers.map((answer) => ({
              answer: answer.answer.trim(),
              isCorrect: answer.isCorrect,
            }))
          : [],
      pairs:
        type === "MATCHING"
          ? pairs.map((pair) => ({
              left: pair.left.trim(),
              right: pair.right.trim(),
            }))
          : [],
      timeInSeconds: timeNum,
    };
    onSubmit(payload);
  }

  const showErrors = touched;

  return (
    <form
      onSubmit={submit}
      className="space-y-5 rounded-xl border border-brand/25 bg-brand-soft/25 p-4 ring-1 ring-inset ring-brand/10 animate-rise sm:p-5"
    >
      <div className="grid gap-4 sm:grid-cols-[1fr_auto]">
        <div className="space-y-2">
          <Label htmlFor={`${uid}-type`}>Typ otázky</Label>
          <select
            id={`${uid}-type`}
            value={type}
            onChange={(event) => setType(event.target.value as QuestionType)}
            className="h-11 w-full rounded-lg border border-input bg-muted/40 px-3 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 md:h-10 md:text-sm"
          >
            {Object.entries(typeLabels).map(([value, labelText]) => (
              <option key={value} value={value}>
                {labelText}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-2 sm:w-28">
          <Label htmlFor={`${uid}-points`}>Body</Label>
          <Input
            id={`${uid}-points`}
            type="number"
            min={1}
            value={points}
            onChange={(event) => setPoints(event.target.value)}
            aria-invalid={showErrors && problems.points}
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor={`${uid}-text`}>Znění otázky</Label>
        <Textarea
          id={`${uid}-text`}
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder="Co měří ampérmetr?"
          rows={2}
          aria-invalid={showErrors && problems.text}
          autoFocus
        />
        {showErrors && problems.text && (
          <p className="text-xs text-destructive">Zadejte znění otázky.</p>
        )}
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor={`${uid}-code`}>
            <Code2 aria-hidden="true" className="mr-1 inline size-3.5" />
            Code snippet <span className="font-normal text-muted-foreground">(volitelné)</span>
          </Label>
          <Textarea
            id={`${uid}-code`}
            value={codeSnippet}
            onChange={(event) => setCodeSnippet(event.target.value)}
            placeholder="const answer = 42;"
            rows={4}
            className="font-mono text-sm"
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor={`${uid}-image`}>
            <ImageIcon aria-hidden="true" className="mr-1 inline size-3.5" />
            Název obrázku <span className="font-normal text-muted-foreground">(imageRef)</span>
          </Label>
          <Input
            id={`${uid}-image`}
            value={imageRef}
            onChange={(event) => setImageRef(event.target.value)}
            placeholder="schema.png"
          />
          <p className="text-xs text-muted-foreground">
            Použijte název souboru bez cesty. Soubory lze přiložit při YAML importu.
          </p>
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor={`${uid}-time`}>
          <Timer aria-hidden="true" className="mr-1 inline size-3.5" />
          Čas na odpověď
        </Label>
        <div className="flex flex-wrap items-center gap-2">
          {TIME_PRESETS.map((preset) => (
            <button
              key={preset}
              type="button"
              onClick={() => setTime(String(preset))}
              aria-pressed={timeNum === preset}
              className={cn(
                "press h-9 rounded-lg border px-3 text-sm font-medium tabular-nums",
                timeNum === preset
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-card text-muted-foreground hover:border-ring/40 hover:text-foreground"
              )}
            >
              {preset}s
            </button>
          ))}
          <Input
            id={`${uid}-time`}
            type="number"
            inputMode="numeric"
            min={1}
            value={time}
            onChange={(event) => setTime(event.target.value)}
            aria-label="Vlastní čas na odpověď ve vteřinách"
            aria-invalid={showErrors && problems.time}
            className="w-24"
          />
        </div>
        {showErrors && problems.time && (
          <p className="text-xs text-destructive">Čas musí být alespoň 1 vteřina.</p>
        )}
      </div>

      {type === "MULTIPLE_CHOICE" && (
        <div className="space-y-2.5">
          <div className="flex flex-wrap items-baseline justify-between gap-2">
            <Label>Odpovědi</Label>
            <span className="text-xs text-muted-foreground">
              Označte správné — může jich být víc
            </span>
          </div>
          <ul className="space-y-2">
            {answers.map((answer, index) => (
              <li key={index} className="flex items-center gap-2">
                <button
                  type="button"
                  role="checkbox"
                  aria-checked={answer.isCorrect}
                  aria-label={`Odpověď ${label(index)} je správná`}
                  onClick={() => updateAnswer(index, { isCorrect: !answer.isCorrect })}
                  className={cn(
                    "press flex size-10 shrink-0 items-center justify-center rounded-lg border text-sm font-semibold",
                    answer.isCorrect
                      ? "border-success/40 bg-success-soft text-success"
                      : "border-border bg-card text-muted-foreground hover:border-ring/40"
                  )}
                >
                  {answer.isCorrect ? <Check aria-hidden="true" className="size-4" /> : label(index)}
                </button>
                <Input
                  value={answer.answer}
                  onChange={(event) => updateAnswer(index, { answer: event.target.value })}
                  placeholder={`Odpověď ${label(index)}`}
                  aria-label={`Text odpovědi ${label(index)}`}
                  aria-invalid={showErrors && !answer.answer.trim()}
                  className={cn(answer.isCorrect && "border-success/40 bg-success-soft/40")}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => setAnswers((prev) => prev.filter((_, i) => i !== index))}
                  aria-label={`Odebrat odpověď ${label(index)}`}
                  disabled={answers.length <= 2}
                  className="shrink-0 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                >
                  <Trash2 />
                </Button>
              </li>
            ))}
          </ul>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => setAnswers((prev) => [...prev, { answer: "", isCorrect: false }])}
            className="text-brand hover:bg-brand-soft"
          >
            <Plus aria-hidden="true" data-icon="inline-start" />
            Přidat odpověď
          </Button>
          {showErrors && problems.answers && (
            <p className="text-xs text-destructive">Vyplňte alespoň dvě odpovědi.</p>
          )}
          {showErrors && problems.noCorrect && (
            <p className="text-xs text-destructive">Označte alespoň jednu správnou odpověď.</p>
          )}
        </div>
      )}

      {type === "MATCHING" && (
        <div className="space-y-2.5">
          <div className="flex items-baseline justify-between gap-2">
            <Label>Páry</Label>
            <span className="text-xs text-muted-foreground">Levá a pravá část se při hře promíchají</span>
          </div>
          <ul className="space-y-2">
            {pairs.map((pair, index) => (
              <li key={index} className="grid gap-2 sm:grid-cols-[1fr_1fr_auto]">
                <Input
                  value={pair.left}
                  onChange={(event) => updatePair(index, { left: event.target.value })}
                  placeholder={`Levá část ${index + 1}`}
                  aria-label={`Levá část páru ${index + 1}`}
                  aria-invalid={showErrors && !pair.left.trim()}
                />
                <Input
                  value={pair.right}
                  onChange={(event) => updatePair(index, { right: event.target.value })}
                  placeholder={`Pravá část ${index + 1}`}
                  aria-label={`Pravá část páru ${index + 1}`}
                  aria-invalid={showErrors && !pair.right.trim()}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => setPairs((prev) => prev.filter((_, i) => i !== index))}
                  aria-label={`Odebrat pár ${index + 1}`}
                  disabled={pairs.length <= 2}
                  className="text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                >
                  <Trash2 />
                </Button>
              </li>
            ))}
          </ul>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => setPairs((prev) => [...prev, { left: "", right: "" }])}
            className="text-brand hover:bg-brand-soft"
          >
            <Plus aria-hidden="true" data-icon="inline-start" />
            Přidat pár
          </Button>
          {showErrors && problems.pairs && (
            <p className="text-xs text-destructive">Vyplňte alespoň dva úplné páry.</p>
          )}
        </div>
      )}

      {type === "FREE_TEXT" && (
        <p className="rounded-lg border border-border bg-muted/30 px-3 py-2 text-sm text-muted-foreground">
          Žák odpoví textem. Odpověď se po odevzdání objeví vyučujícímu ke kontrole.
        </p>
      )}

      <div className="flex flex-col-reverse gap-2 border-t border-border/70 pt-4 sm:flex-row sm:justify-end">
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          <X aria-hidden="true" data-icon="inline-start" />
          Zrušit
        </Button>
        <Button type="submit" loading={submitting}>
          <Save aria-hidden="true" data-icon="inline-start" />
          {initial ? "Uložit změny" : "Přidat otázku"}
        </Button>
      </div>
    </form>
  );
}
