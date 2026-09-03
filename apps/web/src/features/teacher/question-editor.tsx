"use client";

import { Plus, Save, Trash2, X } from "lucide-react";
import { useMemo, useState, type FormEvent } from "react";

import {
  Button,
  Checkbox,
  Field,
  Input,
  SelectField,
  Textarea,
} from "@/components/ui";
import type {
  Answer,
  MatchingPair,
  Question,
  QuestionInput,
  QuestionType,
} from "@/types/domain";
import { ErrorSummary, Notice, focusErrorSummary } from "./shared";

interface QuestionDraft {
  question: string;
  type: QuestionType;
  points: string;
  timeInSeconds: string;
  codeSnippet: string;
  imageRef: string;
  answers: Array<{ answer: string; isCorrect: boolean }>;
  pairs: MatchingPair[];
}

const typeLabels: Record<QuestionType, string> = {
  MULTIPLE_CHOICE: "Výběr z možností",
  MATCHING: "Přiřazování dvojic",
  FREE_TEXT: "Volná odpověď",
};

function emptyQuestion(): QuestionDraft {
  return {
    question: "",
    type: "MULTIPLE_CHOICE",
    points: "1",
    timeInSeconds: "30",
    codeSnippet: "",
    imageRef: "",
    answers: [
      { answer: "", isCorrect: true },
      { answer: "", isCorrect: false },
    ],
    pairs: [
      { left: "", right: "" },
      { left: "", right: "" },
    ],
  };
}

function questionToDraft(question: Question): QuestionDraft {
  const answers = question.answers.map((answer) => ({
    answer: answer.answer ?? "",
    isCorrect: Boolean(answer.isCorrect),
  }));
  return {
    question: question.question,
    type: question.type,
    points: String(question.points ?? 1),
    timeInSeconds: String(question.timeInSeconds ?? 30),
    codeSnippet: question.codeSnippet ?? "",
    imageRef: question.imageRef ?? "",
    answers:
      answers.length >= 2
        ? answers
        : [
            ...answers,
            ...Array.from({ length: 2 - answers.length }, () => ({
              answer: "",
              isCorrect: false,
            })),
          ],
    pairs:
      question.pairs.length >= 2
        ? question.pairs.map((pair) => ({ ...pair }))
        : [
            ...question.pairs.map((pair) => ({ ...pair })),
            ...Array.from({ length: 2 - question.pairs.length }, () => ({
              left: "",
              right: "",
            })),
          ],
  };
}

function validateQuestion(value: QuestionDraft) {
  const errors: Record<string, string> = {};
  const points = Number(value.points);
  const seconds = Number(value.timeInSeconds);

  if (!value.question.trim()) errors["question-text"] = "Zadejte znění otázky.";
  if (!Number.isInteger(points) || points < 1 || points > 100) {
    errors["question-points"] = "Body musí být celé číslo od 1 do 100.";
  }
  if (!Number.isInteger(seconds) || seconds < 1 || seconds > 3_600) {
    errors["question-time"] = "Čas musí být celé číslo od 1 do 3 600 sekund.";
  }
  if (value.codeSnippet.length > 20_000) {
    errors["question-code"] = "Ukázka kódu může mít nejvýše 20 000 znaků.";
  }
  if (value.imageRef.length > 255 || /[/\\]/.test(value.imageRef)) {
    errors["question-image"] =
      "Uveďte pouze název souboru bez cesty, nejvýše 255 znaků.";
  }

  if (value.type === "MULTIPLE_CHOICE") {
    value.answers.forEach((answer, index) => {
      if (!answer.answer.trim()) {
        errors[`question-answer-${index}`] = `Doplňte text možnosti ${index + 1}.`;
      }
    });
    if (!value.answers.some((answer) => answer.isCorrect)) {
      errors["question-correct-answer"] = "Označte alespoň jednu správnou možnost.";
    }
  }

  if (value.type === "MATCHING") {
    value.pairs.forEach((pair, index) => {
      if (!pair.left.trim()) {
        errors[`question-pair-left-${index}`] = `Doplňte levou část dvojice ${index + 1}.`;
      }
      if (!pair.right.trim()) {
        errors[`question-pair-right-${index}`] = `Doplňte pravou část dvojice ${index + 1}.`;
      }
    });
  }

  return errors;
}

function toQuestionInput(value: QuestionDraft): QuestionInput {
  const answers: Answer[] =
    value.type === "MULTIPLE_CHOICE"
      ? value.answers.map((answer) => ({
          answer: answer.answer.trim(),
          isCorrect: answer.isCorrect,
        }))
      : [];
  const pairs =
    value.type === "MATCHING"
      ? value.pairs.map((pair) => ({
          left: pair.left.trim(),
          right: pair.right.trim(),
        }))
      : [];
  return {
    question: value.question.trim(),
    type: value.type,
    points: Number(value.points),
    timeInSeconds: Number(value.timeInSeconds),
    codeSnippet: value.codeSnippet,
    imageRef: value.imageRef.trim(),
    answers,
    pairs,
  };
}

interface QuestionEditorProps {
  busy?: boolean;
  initialQuestion?: Question;
  onCancel: () => void;
  onSubmit: (input: QuestionInput) => Promise<void>;
  requestError?: string | null;
}

export function QuestionEditor({
  busy = false,
  initialQuestion,
  onCancel,
  onSubmit,
  requestError,
}: QuestionEditorProps) {
  const [value, setValue] = useState<QuestionDraft>(() =>
    initialQuestion ? questionToDraft(initialQuestion) : emptyQuestion(),
  );
  const [touched, setTouched] = useState<Set<string>>(new Set());
  const [submitted, setSubmitted] = useState(false);
  const errors = useMemo(() => validateQuestion(value), [value]);
  const visibleError = (field: string) =>
    submitted || touched.has(field) ? errors[field] : undefined;

  function markTouched(field: string) {
    setTouched((current) => new Set(current).add(field));
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitted(true);
    if (Object.keys(errors).length > 0) {
      focusErrorSummary(event.currentTarget);
      return;
    }
    await onSubmit(toQuestionInput(value));
  }

  function updateAnswer(
    index: number,
    update: Partial<{ answer: string; isCorrect: boolean }>,
  ) {
    setValue((current) => ({
      ...current,
      answers: current.answers.map((answer, answerIndex) =>
        answerIndex === index ? { ...answer, ...update } : answer,
      ),
    }));
  }

  function updatePair(index: number, update: Partial<MatchingPair>) {
    setValue((current) => ({
      ...current,
      pairs: current.pairs.map((pair, pairIndex) =>
        pairIndex === index ? { ...pair, ...update } : pair,
      ),
    }));
  }

  return (
    <form noValidate onSubmit={submit} className="grid gap-5">
      {submitted ? <ErrorSummary errors={errors} title="Otázku je potřeba doplnit" /> : null}
      {requestError ? <Notice tone="error">{requestError}</Notice> : null}

      <Field
        label="Znění otázky"
        required
        controlId="question-text"
        error={visibleError("question-text")}
      >
        <Textarea
          value={value.question}
          onChange={(event) => setValue({ ...value, question: event.target.value })}
          onBlur={() => markTouched("question-text")}
          rows={3}
        />
      </Field>

      <div className="grid gap-5 sm:grid-cols-3">
        <Field label="Typ odpovědi" required controlId="question-type">
          <SelectField
            value={value.type}
            onValueChange={(type) =>
              setValue({ ...value, type: type as QuestionType })
            }
            options={(Object.keys(typeLabels) as QuestionType[]).map((type) => ({
              value: type,
              label: typeLabels[type],
            }))}
          />
        </Field>
        <Field
          label="Body"
          required
          controlId="question-points"
          error={visibleError("question-points")}
        >
          <Input
            type="number"
            inputMode="numeric"
            min={1}
            max={100}
            step={1}
            value={value.points}
            onChange={(event) => setValue({ ...value, points: event.target.value })}
            onBlur={() => markTouched("question-points")}
          />
        </Field>
        <Field
          label="Čas na odpověď"
          required
          controlId="question-time"
          error={visibleError("question-time")}
          description="V sekundách"
        >
          <Input
            type="number"
            inputMode="numeric"
            min={1}
            max={3600}
            step={1}
            value={value.timeInSeconds}
            onChange={(event) =>
              setValue({ ...value, timeInSeconds: event.target.value })
            }
            onBlur={() => markTouched("question-time")}
          />
        </Field>
      </div>

      {value.type === "MULTIPLE_CHOICE" ? (
        <fieldset className="grid gap-4 rounded-md border border-border bg-surface px-4 py-4 sm:px-5">
          <legend className="px-1 font-semibold text-foreground">Možnosti odpovědi</legend>
          <p className="text-sm leading-5 text-muted-foreground">
            Označte jednu nebo více správných možností.
          </p>
          <div className="grid gap-3">
            {value.answers.map((answer, index) => (
              <div
                key={index}
                className="grid gap-3 rounded-md border border-border bg-panel p-3 sm:grid-cols-[minmax(0,1fr)_auto_auto] sm:items-start"
              >
                <Field
                  label={`Možnost ${index + 1}`}
                  required
                  controlId={`question-answer-${index}`}
                  error={visibleError(`question-answer-${index}`)}
                >
                  <Input
                    value={answer.answer}
                    onChange={(event) => updateAnswer(index, { answer: event.target.value })}
                    onBlur={() => markTouched(`question-answer-${index}`)}
                  />
                </Field>
                <label
                  htmlFor={`question-answer-${index}-correct`}
                  className="flex min-h-11 items-center gap-2 pt-7 text-sm font-medium text-foreground sm:pt-6"
                >
                  <Checkbox
                    id={`question-answer-${index}-correct`}
                    checked={answer.isCorrect}
                    onCheckedChange={(checked) => {
                      updateAnswer(index, { isCorrect: checked === true });
                      markTouched("question-correct-answer");
                    }}
                  />
                  Správná
                </label>
                <Button
                  variant="quiet"
                  size="icon"
                  aria-label={`Odebrat možnost ${index + 1}`}
                  disabled={value.answers.length <= 2}
                  className="sm:mt-6"
                  onClick={() =>
                    setValue((current) => ({
                      ...current,
                      answers: current.answers.filter((_, answerIndex) => answerIndex !== index),
                    }))
                  }
                >
                  <Trash2 aria-hidden="true" className="size-4" />
                </Button>
              </div>
            ))}
          </div>
          {visibleError("question-correct-answer") ? (
            <p
              id="question-correct-answer"
              role="alert"
              tabIndex={-1}
              className="text-sm text-danger-text"
            >
              {visibleError("question-correct-answer")}
            </p>
          ) : null}
          <Button
            variant="secondary"
            size="sm"
            className="w-fit"
            leadingIcon={<Plus className="size-4" />}
            onClick={() =>
              setValue((current) => ({
                ...current,
                answers: [...current.answers, { answer: "", isCorrect: false }],
              }))
            }
          >
            Přidat možnost
          </Button>
        </fieldset>
      ) : null}

      {value.type === "MATCHING" ? (
        <fieldset className="grid gap-4 rounded-md border border-border bg-surface px-4 py-4 sm:px-5">
          <legend className="px-1 font-semibold text-foreground">Dvojice k přiřazení</legend>
          <p className="text-sm leading-5 text-muted-foreground">
            Žáci budou spojovat odpovídající položky z levého a pravého sloupce.
          </p>
          <div className="grid gap-3">
            {value.pairs.map((pair, index) => (
              <div
                key={index}
                className="grid gap-3 rounded-md border border-border bg-panel p-3 sm:grid-cols-[1fr_1fr_auto] sm:items-start"
              >
                <Field
                  label={`Levá část ${index + 1}`}
                  required
                  controlId={`question-pair-left-${index}`}
                  error={visibleError(`question-pair-left-${index}`)}
                >
                  <Input
                    value={pair.left}
                    maxLength={500}
                    onChange={(event) => updatePair(index, { left: event.target.value })}
                    onBlur={() => markTouched(`question-pair-left-${index}`)}
                  />
                </Field>
                <Field
                  label={`Pravá část ${index + 1}`}
                  required
                  controlId={`question-pair-right-${index}`}
                  error={visibleError(`question-pair-right-${index}`)}
                >
                  <Input
                    value={pair.right}
                    maxLength={500}
                    onChange={(event) => updatePair(index, { right: event.target.value })}
                    onBlur={() => markTouched(`question-pair-right-${index}`)}
                  />
                </Field>
                <Button
                  variant="quiet"
                  size="icon"
                  aria-label={`Odebrat dvojici ${index + 1}`}
                  disabled={value.pairs.length <= 2}
                  className="sm:mt-6"
                  onClick={() =>
                    setValue((current) => ({
                      ...current,
                      pairs: current.pairs.filter((_, pairIndex) => pairIndex !== index),
                    }))
                  }
                >
                  <Trash2 aria-hidden="true" className="size-4" />
                </Button>
              </div>
            ))}
          </div>
          <Button
            variant="secondary"
            size="sm"
            className="w-fit"
            leadingIcon={<Plus className="size-4" />}
            onClick={() =>
              setValue((current) => ({
                ...current,
                pairs: [...current.pairs, { left: "", right: "" }],
              }))
            }
          >
            Přidat dvojici
          </Button>
        </fieldset>
      ) : null}

      {value.type === "FREE_TEXT" ? (
        <Notice>
          Volnou odpověď po ukončení relace zkontroluje vyučující a přidělí jí body.
        </Notice>
      ) : null}

      <details className="group rounded-md border border-border bg-surface">
        <summary className="flex min-h-12 list-none items-center justify-between gap-3 px-4 py-3 text-sm font-semibold text-foreground hover:bg-surface-subtle [&::-webkit-details-marker]:hidden">
          Doplňující obsah
          <Plus
            aria-hidden="true"
            className="size-4 transition-transform group-open:rotate-45 motion-reduce:transition-none"
          />
        </summary>
        <div className="grid gap-5 border-t border-border px-4 py-4">
          <Field
            label="Ukázka kódu"
            optional
            controlId="question-code"
            error={visibleError("question-code")}
            description="Zobrazí se žákům monospace písmem."
          >
            <Textarea
              className="font-mono text-sm"
              value={value.codeSnippet}
              maxLength={20_000}
              onChange={(event) => setValue({ ...value, codeSnippet: event.target.value })}
              onBlur={() => markTouched("question-code")}
              rows={5}
            />
          </Field>
          <Field
            label="Název obrázku"
            optional
            controlId="question-image"
            error={visibleError("question-image")}
            description="Pouze název souboru, například schema.png."
          >
            <Input
              value={value.imageRef}
              maxLength={255}
              onChange={(event) => setValue({ ...value, imageRef: event.target.value })}
              onBlur={() => markTouched("question-image")}
              autoComplete="off"
            />
          </Field>
        </div>
      </details>

      <div className="flex flex-col-reverse gap-2 border-t border-border pt-4 sm:flex-row sm:justify-end">
        <Button variant="quiet" onClick={onCancel} disabled={busy}>
          <X aria-hidden="true" className="size-4" />
          Zrušit
        </Button>
        <Button
          type="submit"
          isLoading={busy}
          leadingIcon={<Save className="size-4" />}
        >
          {initialQuestion ? "Uložit změny" : "Přidat otázku"}
        </Button>
      </div>
    </form>
  );
}

export { typeLabels as questionTypeLabels };
