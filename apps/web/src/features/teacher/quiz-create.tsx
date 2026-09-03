"use client";

import { ArrowLeft, Save } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo, useState, type FormEvent } from "react";

import {
  Button,
  Panel,
  PanelContent,
  PanelDescription,
  PanelFooter,
  PanelHeader,
  PanelTitle,
  buttonStyles,
} from "@/components/ui";
import { messageFromError } from "@/lib/http";
import { quizService } from "@/lib/services";
import { cn } from "@/lib/cn";
import {
  QuizFields,
  emptyQuizForm,
  quizFormToInput,
  validateQuizForm,
  type QuizFormState,
} from "./quiz-form";
import { ErrorSummary, Notice, focusErrorSummary } from "./shared";

export function QuizCreate() {
  const router = useRouter();
  const [value, setValue] = useState<QuizFormState>(emptyQuizForm);
  const [touched, setTouched] = useState<Set<keyof QuizFormState>>(new Set());
  const [submitted, setSubmitted] = useState(false);
  const [saving, setSaving] = useState(false);
  const [requestError, setRequestError] = useState<string | null>(null);
  const errors = useMemo(() => validateQuizForm(value), [value]);
  const visibleErrors = useMemo(
    () =>
      Object.fromEntries(
        Object.entries(errors).filter(([field]) =>
          submitted || touched.has(field as keyof QuizFormState),
        ),
      ),
    [errors, submitted, touched],
  );

  function markTouched(field: keyof QuizFormState) {
    setTouched((current) => new Set(current).add(field));
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitted(true);
    setRequestError(null);
    if (Object.keys(errors).length > 0) {
      focusErrorSummary(event.currentTarget);
      return;
    }

    setSaving(true);
    try {
      const created = await quizService.create(quizFormToInput(value));
      router.push(`/quiz/${created.quizUuid}`);
    } catch (error) {
      setRequestError(
        messageFromError(
          error,
          "Kvíz se nepodařilo vytvořit. Zkontrolujte údaje a zkuste to znovu.",
        ),
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="mx-auto grid w-full max-w-3xl gap-6">
      <header className="grid gap-3">
        <Link
          href="/dashboard"
          className={cn(buttonStyles({ variant: "quiet", size: "sm" }), "w-fit px-2")}
        >
          <ArrowLeft aria-hidden="true" className="size-4" />
          Zpět do knihovny
        </Link>
        <div>
          <p className="text-sm font-semibold text-brand-text">Nový kvíz</p>
          <h1 className="mt-1 text-3xl font-semibold tracking-[-0.025em] text-balance text-foreground sm:text-4xl">
            Základní nastavení
          </h1>
          <p className="mt-2 max-w-2xl text-base leading-7 text-muted-foreground">
            Nejprve založte kvíz. Otázky doplníte v následujícím kroku.
          </p>
        </div>
      </header>

      <form noValidate onSubmit={submit}>
        <Panel>
          <PanelHeader>
            <PanelTitle>Údaje o kvízu</PanelTitle>
            <PanelDescription>
              Povinná pole jsou označená hvězdičkou. Kvíz se vytvoří jako koncept.
            </PanelDescription>
          </PanelHeader>
          <PanelContent className="grid gap-5">
            {submitted ? <ErrorSummary errors={errors as Record<string, string>} /> : null}
            {requestError ? <Notice tone="error">{requestError}</Notice> : null}
            <QuizFields
              value={value}
              errors={visibleErrors}
              onChange={setValue}
              onBlur={markTouched}
              showStatus={false}
            />
          </PanelContent>
          <PanelFooter className="justify-between">
            <Link href="/dashboard" className={buttonStyles({ variant: "quiet" })}>
              Zrušit
            </Link>
            <Button
              type="submit"
              isLoading={saving}
              leadingIcon={<Save className="size-4" />}
            >
              Vytvořit kvíz
            </Button>
          </PanelFooter>
        </Panel>
      </form>
    </div>
  );
}

