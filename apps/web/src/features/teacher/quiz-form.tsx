"use client";

import { useEffect, useState, type ChangeEvent } from "react";

import {
  Button,
  Checkbox,
  Field,
  Input,
  SelectField,
  Skeleton,
  Textarea,
} from "@/components/ui";
import { messageFromError } from "@/lib/http";
import { subjectService } from "@/lib/services";
import type {
  AssignedSubject,
  Quiz,
  QuizInput,
  QuizStatus,
} from "@/types/domain";
import { Notice, toDateTimeLocal } from "./shared";

const NO_SUBJECT_VALUE = "__no_subject__";
const LEGACY_SUBJECT_VALUE = "__legacy_subject__";

type SubjectLoadStatus = "error" | "loading" | "ready";

function useAssignedSubjects() {
  const [attempt, setAttempt] = useState(0);
  const [subjects, setSubjects] = useState<AssignedSubject[]>([]);
  const [status, setStatus] = useState<SubjectLoadStatus>("loading");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const initialLoad = window.setTimeout(() => {
      void subjectService
        .list()
        .then((loadedSubjects) => {
          if (cancelled) return;
          setSubjects(
            [...loadedSubjects].sort((left, right) =>
              left.name.localeCompare(right.name, "cs"),
            ),
          );
          setError(null);
          setStatus("ready");
        })
        .catch((loadError: unknown) => {
          if (cancelled) return;
          setError(
            messageFromError(
              loadError,
              "Seznam přiřazených předmětů se nepodařilo načíst.",
            ),
          );
          setStatus("error");
        });
    }, 0);

    return () => {
      cancelled = true;
      window.clearTimeout(initialLoad);
    };
  }, [attempt]);

  function retry() {
    setError(null);
    setStatus("loading");
    setAttempt((current) => current + 1);
  }

  return { error, retry, status, subjects };
}

export interface QuizFormState {
  title: string;
  description: string;
  subject: string;
  subjectUuid: string;
  chapter: string;
  maxQuestionsPerSession: string;
  shuffle: boolean;
  status: QuizStatus;
  validFrom: string;
  validTo: string;
}

export type QuizFormErrors = Partial<Record<keyof QuizFormState, string>>;

export const emptyQuizForm: QuizFormState = {
  title: "",
  description: "",
  subject: "",
  subjectUuid: "",
  chapter: "",
  maxQuestionsPerSession: "10",
  shuffle: false,
  status: "DRAFT",
  validFrom: "",
  validTo: "",
};

export function quizToForm(quiz: Quiz): QuizFormState {
  return {
    title: quiz.title,
    description: quiz.description ?? "",
    subject: quiz.subject ?? "",
    subjectUuid: quiz.subjectUuid ?? "",
    chapter: quiz.chapter ?? "",
    maxQuestionsPerSession: String(quiz.maxQuestionsPerSession ?? 10),
    shuffle: quiz.shuffle,
    status: quiz.status ?? "DRAFT",
    validFrom: toDateTimeLocal(quiz.validFrom),
    validTo: toDateTimeLocal(quiz.validTo),
  };
}

export function validateQuizForm(value: QuizFormState): QuizFormErrors {
  const errors: QuizFormErrors = {};
  const title = value.title.trim();
  const maxQuestions = Number(value.maxQuestionsPerSession);

  if (!title) errors.title = "Zadejte název kvízu.";
  else if (title.length > 100) {
    errors.title = "Název může mít nejvýše 100 znaků.";
  }

  if (!value.subjectUuid) {
    errors.subject = "Vyberte přiřazený předmět.";
  } else if (value.subject.trim().length > 100) {
    errors.subject = "Předmět může mít nejvýše 100 znaků.";
  }
  if (value.chapter.trim().length > 100) {
    errors.chapter = "Kapitola může mít nejvýše 100 znaků.";
  }
  if (
    !Number.isInteger(maxQuestions) ||
    maxQuestions < 1 ||
    maxQuestions > 100
  ) {
    errors.maxQuestionsPerSession =
      "Zadejte celé číslo od 1 do 100.";
  }

  const from = value.validFrom ? new Date(value.validFrom).getTime() : null;
  const to = value.validTo ? new Date(value.validTo).getTime() : null;
  if (value.validFrom && Number.isNaN(from)) {
    errors.validFrom = "Zadejte platné datum začátku.";
  }
  if (value.validTo && Number.isNaN(to)) {
    errors.validTo = "Zadejte platné datum konce.";
  }
  if (from !== null && to !== null && !Number.isNaN(from) && !Number.isNaN(to)) {
    if (to <= from) {
      errors.validTo = "Konec platnosti musí být později než začátek.";
    }
  }

  return errors;
}

export function quizFormToInput(value: QuizFormState): QuizInput {
  return {
    title: value.title.trim(),
    description: value.description.trim(),
    subject: value.subject.trim(),
    subjectUuid: value.subjectUuid || null,
    chapter: value.chapter.trim(),
    maxQuestionsPerSession: Number(value.maxQuestionsPerSession),
    shuffle: value.shuffle,
    status: value.status,
    validFrom: value.validFrom || undefined,
    validTo: value.validTo || undefined,
  };
}

interface QuizFieldsProps {
  errors: QuizFormErrors;
  onBlur: (field: keyof QuizFormState) => void;
  onChange: (next: QuizFormState) => void;
  showStatus?: boolean;
  value: QuizFormState;
}

export function QuizFields({
  errors,
  onBlur,
  onChange,
  showStatus = true,
  value,
}: QuizFieldsProps) {
  const {
    error: subjectsError,
    retry: retrySubjects,
    status: subjectsStatus,
    subjects,
  } = useAssignedSubjects();
  const setText =
    (field: keyof QuizFormState) =>
    (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      onChange({ ...value, [field]: event.target.value });
    };

  const subjectName = value.subject.trim();
  const selectedAssignedSubject = subjects.find(
    (subject) => subject.uuid === value.subjectUuid,
  );
  const hasLegacySubject = Boolean(subjectName && !value.subjectUuid);
  const needsCurrentSubjectFallback = Boolean(
    value.subjectUuid && !selectedAssignedSubject,
  );
  const hasUnavailableSubject = Boolean(
    subjectsStatus === "ready" && needsCurrentSubjectFallback,
  );
  const selectedSubjectValue = value.subjectUuid
    ? value.subjectUuid
    : hasLegacySubject
      ? LEGACY_SUBJECT_VALUE
      : NO_SUBJECT_VALUE;
  const subjectOptions = [
    { value: NO_SUBJECT_VALUE, label: "Bez předmětu" },
    ...(hasLegacySubject
      ? [
          {
            value: LEGACY_SUBJECT_VALUE,
            label: `${subjectName} (původní hodnota)`,
          },
        ]
      : []),
    ...(needsCurrentSubjectFallback
      ? [
          {
            value: value.subjectUuid,
            label:
              subjectsStatus === "ready"
                ? `${subjectName || "Původně přiřazený předmět"} (již nepřiřazený)`
                : subjectName || "Aktuálně přiřazený předmět",
          },
        ]
      : []),
    ...subjects.map((subject) => ({
      value: subject.uuid,
      label: subject.name,
    })),
  ];

  function setSubject(subjectUuid: string) {
    if (subjectUuid === NO_SUBJECT_VALUE) {
      onChange({ ...value, subject: "", subjectUuid: "" });
      return;
    }

    if (
      subjectUuid === LEGACY_SUBJECT_VALUE ||
      (needsCurrentSubjectFallback && subjectUuid === value.subjectUuid)
    ) {
      return;
    }

    const selectedSubject = subjects.find(
      (subject) => subject.uuid === subjectUuid,
    );
    if (!selectedSubject) return;
    onChange({
      ...value,
      subject: selectedSubject.name,
      subjectUuid: selectedSubject.uuid,
    });
  }

  return (
    <div className="grid gap-5">
      <Field label="Název kvízu" required error={errors.title} controlId="title">
        <Input
          value={value.title}
          onChange={setText("title")}
          onBlur={() => onBlur("title")}
          maxLength={100}
          autoComplete="off"
        />
      </Field>

      <Field
        label="Popis"
        optional
        controlId="description"
        description="Stručně uveďte, co si žáci procvičí."
      >
        <Textarea
          value={value.description}
          onChange={setText("description")}
          rows={4}
        />
      </Field>

      <div className="grid gap-5 sm:grid-cols-2">
        <div className="grid content-start gap-2">
          <Field
            label="Předmět"
            required
            error={errors.subject}
            controlId="subject"
            description={
              hasLegacySubject
                ? "Původní hodnota není propojená s přiřazeným předmětem. Můžete ji ponechat nebo vybrat novou."
                : hasUnavailableSubject
                  ? "Původně přiřazený předmět už není ve vašem seznamu."
                  : subjectsStatus === "ready" && subjects.length === 0
                    ? "Nemáte přiřazený žádný předmět."
                    : undefined
            }
          >
            {subjectsStatus === "loading" ? (
              <div
                role="status"
                aria-live="polite"
                aria-busy="true"
                className="min-h-11"
              >
                <span className="sr-only">Načítám přiřazené předměty…</span>
                <Skeleton className="h-11 w-full" />
              </div>
            ) : (
              <SelectField
                value={selectedSubjectValue}
                onValueChange={setSubject}
                onBlur={() => onBlur("subject")}
                options={subjectOptions}
              />
            )}
          </Field>
          {subjectsStatus === "error" ? (
            <Notice tone="error" className="px-3 py-3">
              <div className="grid gap-3">
                <p>
                  {subjectsError} {subjectName || value.subjectUuid
                    ? "Aktuální hodnota zůstala zachovaná."
                    : "Bez přiřazeného předmětu nelze kvíz uložit."}
                </p>
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  className="w-fit"
                  onClick={retrySubjects}
                >
                  Načíst znovu
                </Button>
              </div>
            </Notice>
          ) : null}
        </div>
        <Field label="Kapitola" optional error={errors.chapter} controlId="chapter">
          <Input
            value={value.chapter}
            onChange={setText("chapter")}
            onBlur={() => onBlur("chapter")}
            maxLength={100}
            autoComplete="off"
          />
        </Field>
      </div>

      <div className="grid gap-5 sm:grid-cols-2">
        <Field
          label="Nejvýše otázek v jednom běhu"
          required
          error={errors.maxQuestionsPerSession}
          controlId="maxQuestionsPerSession"
          description="Použije se nejvýše dostupný počet otázek."
        >
          <Input
            type="number"
            inputMode="numeric"
            min={1}
            max={100}
            step={1}
            value={value.maxQuestionsPerSession}
            onChange={setText("maxQuestionsPerSession")}
            onBlur={() => onBlur("maxQuestionsPerSession")}
          />
        </Field>
        {showStatus ? (
          <Field label="Stav" required controlId="status">
            <SelectField
              value={value.status}
              onValueChange={(status) =>
                onChange({ ...value, status: status as QuizStatus })
              }
              onBlur={() => onBlur("status")}
              options={[
                { value: "DRAFT", label: "Koncept" },
                { value: "RUNNING", label: "Aktivní" },
                { value: "ARCHIVED", label: "Archivovaný" },
              ]}
            />
          </Field>
        ) : null}
      </div>

      <fieldset className="grid gap-3 rounded-md border border-border bg-surface px-4 py-4">
        <legend className="px-1 text-sm font-semibold text-foreground">
          Pořadí otázek
        </legend>
        <label
          htmlFor="quiz-shuffle"
          className="flex min-h-11 items-center gap-3 text-sm text-foreground"
        >
          <Checkbox
            id="quiz-shuffle"
            checked={value.shuffle}
            onCheckedChange={(checked) =>
              onChange({ ...value, shuffle: checked === true })
            }
          />
          Při každém spuštění otázky zamíchat
        </label>
      </fieldset>

      <fieldset className="grid gap-5 rounded-md border border-border bg-surface px-4 py-4 sm:grid-cols-2">
        <legend className="px-1 text-sm font-semibold text-foreground">
          Časová dostupnost
        </legend>
        <Field
          label="Dostupný od"
          optional
          error={errors.validFrom}
          controlId="validFrom"
        >
          <Input
            type="datetime-local"
            value={value.validFrom}
            onChange={setText("validFrom")}
            onBlur={() => onBlur("validFrom")}
          />
        </Field>
        <Field
          label="Dostupný do"
          optional
          error={errors.validTo}
          controlId="validTo"
        >
          <Input
            type="datetime-local"
            min={value.validFrom || undefined}
            value={value.validTo}
            onChange={setText("validTo")}
            onBlur={() => onBlur("validTo")}
          />
        </Field>
      </fieldset>
    </div>
  );
}
