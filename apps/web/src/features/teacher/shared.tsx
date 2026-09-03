import { CircleAlert, CircleCheck, Info } from "lucide-react";
import type { HTMLAttributes, ReactNode } from "react";

import { Badge } from "@/components/ui";
import { cn } from "@/lib/cn";
import type { QuizStatus } from "@/types/domain";

const czechDateFormatter = new Intl.DateTimeFormat("cs-CZ", {
  dateStyle: "medium",
  timeStyle: "short",
});

export const quizStatusLabels: Record<QuizStatus, string> = {
  DRAFT: "Koncept",
  RUNNING: "Aktivní",
  ARCHIVED: "Archivovaný",
};

export function QuizStatusBadge({ status }: { status: QuizStatus | null }) {
  const normalizedStatus = status ?? "DRAFT";
  return (
    <Badge
      variant={
        normalizedStatus === "RUNNING"
          ? "brand"
          : normalizedStatus === "ARCHIVED"
            ? "outline"
            : "neutral"
      }
    >
      {quizStatusLabels[normalizedStatus]}
    </Badge>
  );
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) return "Neuvedeno";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Neuvedeno";
  return czechDateFormatter.format(date);
}

export function toDateTimeLocal(value: string | null | undefined) {
  if (!value) return "";
  const match = value.match(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/);
  return match?.[0] ?? "";
}

type NoticeTone = "error" | "info" | "success";

interface NoticeProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  tone?: NoticeTone;
}

export function Notice({
  children,
  className,
  tone = "info",
  ...props
}: NoticeProps) {
  const Icon =
    tone === "error" ? CircleAlert : tone === "success" ? CircleCheck : Info;
  return (
    <div
      className={cn(
        "flex items-start gap-3 rounded-md border px-4 py-3 text-sm leading-6",
        tone === "error"
          ? "border-danger/45 bg-danger-subtle text-danger-text"
          : "border-brand/35 bg-brand-subtle text-foreground",
        className,
      )}
      role={tone === "error" ? "alert" : "status"}
      {...props}
    >
      <Icon
        aria-hidden="true"
        className={cn(
          "mt-0.5 size-[1.125rem] shrink-0",
          tone === "error" ? "text-danger-text" : "text-brand-text",
        )}
        strokeWidth={2}
      />
      <div className="min-w-0">{children}</div>
    </div>
  );
}

export function ErrorSummary({
  errors,
  title = "Formulář obsahuje chyby",
}: {
  errors: Record<string, string>;
  title?: string;
}) {
  const items = Object.entries(errors);
  if (items.length === 0) return null;
  return (
    <div
      className="rounded-md border border-danger/45 bg-danger-subtle px-4 py-3 text-danger-text"
      role="alert"
      tabIndex={-1}
      data-error-summary
    >
      <p className="font-semibold">{title}</p>
      <ul className="mt-1 list-disc space-y-0.5 pl-5 text-sm leading-5">
        {items.map(([field, message]) => (
          <li key={field}>
            <a className="underline underline-offset-2" href={`#${field}`}>
              {message}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function focusErrorSummary(form: HTMLFormElement) {
  window.requestAnimationFrame(() => {
    form.querySelector<HTMLElement>("[data-error-summary]")?.focus();
  });
}

