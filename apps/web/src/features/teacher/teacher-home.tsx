"use client";

import {
  BookOpen,
  Check,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Clock3,
  FileCode2,
  Play,
  Plus,
  Radio,
  Users,
  X,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  useCallback,
  useDeferredValue,
  useEffect,
  useId,
  useMemo,
  useState,
  type FormEvent,
} from "react";

import {
  Badge,
  Button,
  EmptyState,
  Field,
  Input,
  Panel,
  PanelContent,
  PanelDescription,
  PanelHeader,
  PanelTitle,
  SelectField,
  Skeleton,
  buttonStyles,
} from "@/components/ui";
import { QuizLibrarySkeleton } from "@/components/page-skeletons";
import { messageFromError } from "@/lib/http";
import { useLiveEvents } from "@/lib/live-events";
import { classService, quizService, sessionService } from "@/lib/services";
import type {
  ActiveSession,
  ClassResponse,
  Quiz,
  QuizStatus,
} from "@/types/domain";
import {
  Notice,
  QuizStatusBadge,
  formatDateTime,
  quizStatusLabels,
} from "./shared";

const PAGE_SIZE = 8;
const DASHBOARD_EVENTS = ["sessions-changed", "roster-changed", "quizzes-changed"] as const;

type StatusFilter = "ALL" | QuizStatus;

function quizSearchText(quiz: Quiz) {
  return [quiz.title, quiz.description, quiz.subject, quiz.chapter]
    .filter(Boolean)
    .join(" ")
    .toLocaleLowerCase("cs");
}

function quizTimestamp(quiz: Quiz) {
  const value = quiz.editedAt ?? quiz.createdAt;
  const time = value ? new Date(value).getTime() : 0;
  return Number.isNaN(time) ? 0 : time;
}

function questionCount(quiz: Quiz) {
  return quiz.questions?.length ?? 0;
}

function questionCountLabel(count: number) {
  if (count === 1) return "1 otázka";
  if (count >= 2 && count <= 4) return `${count} otázky`;
  return `${count} otázek`;
}

function quizResultCountLabel(count: number) {
  if (count === 1) return "Nalezen 1 kvíz";
  if (count >= 2 && count <= 4) return `Nalezeny ${count} kvízy`;
  return `Nalezeno ${count} kvízů`;
}

interface QuizActionsProps {
  onStart: (quiz: Quiz) => void;
  quiz: Quiz;
}

function QuizActions({ onStart, quiz }: QuizActionsProps) {
  const hasQuestions = questionCount(quiz) > 0;
  const instanceId = useId().replaceAll(":", "");
  const helpId = `quiz-${quiz.uuid}-${instanceId}-start-help`;
  return (
    <div className="flex flex-wrap items-center gap-2">
      <Link
        href={`/quiz/${quiz.uuid}`}
        className={buttonStyles({ variant: "secondary", size: "sm" })}
      >
        Upravit
      </Link>
      <Button
        size="sm"
        leadingIcon={<Play className="size-4" />}
        disabled={!hasQuestions}
        aria-describedby={!hasQuestions ? helpId : undefined}
        onClick={() => onStart(quiz)}
      >
        Spustit
      </Button>
      {!hasQuestions ? (
        <span id={helpId} className="w-full text-xs text-muted-foreground">
          Nejdříve přidejte otázku.
        </span>
      ) : null}
    </div>
  );
}

function studentCountLabel(count: number) {
  if (count === 1) return "1 žák";
  if (count >= 2 && count <= 4) return `${count} žáci`;
  return `${count} žáků`;
}

interface ClassAudienceSelectorProps {
  classes: ClassResponse[];
  disabled: boolean;
  error: string | null;
  loading: boolean;
  onChange: (classIds: string[]) => void;
  onGroupsChange: (groupIds: string[]) => void;
  onRetry: () => void;
  selectedClassIds: string[];
  selectedGroupIds: string[];
}

function ClassAudienceSelector({
  classes,
  disabled,
  error,
  loading,
  onChange,
  onGroupsChange,
  onRetry,
  selectedClassIds,
  selectedGroupIds,
}: ClassAudienceSelectorProps) {
  const selected = new Set(selectedClassIds);
  const selectedGroups = new Set(selectedGroupIds);
  const selectedNames = classes
    .filter((schoolClass) => selected.has(schoolClass.uuid))
    .map((schoolClass) => schoolClass.name)
    .concat(
      classes.flatMap((schoolClass) =>
        (schoolClass.groups ?? [])
          .filter((group) => selectedGroups.has(group.uuid))
          .map((group) => `${schoolClass.name} · ${group.name}`),
      ),
    );

  function toggle(classUuid: string) {
    const schoolClass = classes.find((candidate) => candidate.uuid === classUuid);
    if (!selected.has(classUuid) && schoolClass?.groups?.length) {
      const nestedIds = new Set(schoolClass.groups.map((group) => group.uuid));
      onGroupsChange(selectedGroupIds.filter((groupId) => !nestedIds.has(groupId)));
    }
    onChange(
      selected.has(classUuid)
        ? selectedClassIds.filter((candidate) => candidate !== classUuid)
        : [...selectedClassIds, classUuid],
    );
  }

  function toggleGroup(classUuid: string, groupUuid: string) {
    if (!selectedGroups.has(groupUuid)) {
      onChange(selectedClassIds.filter((candidate) => candidate !== classUuid));
    }
    onGroupsChange(
      selectedGroups.has(groupUuid)
        ? selectedGroupIds.filter((candidate) => candidate !== groupUuid)
        : [...selectedGroupIds, groupUuid],
    );
  }

  return (
    <fieldset
      disabled={disabled}
      aria-describedby="session-audience-help session-audience-status"
      className="grid gap-3"
    >
      <legend className="text-sm leading-5 font-semibold text-foreground">
        Kdo se může připojit
      </legend>
      <p
        id="session-audience-help"
        className="text-sm leading-6 text-muted-foreground"
      >
        Vyberte celou třídu, nebo jednu či více jejích skupin.
      </p>

      {loading ? (
        <div role="status" aria-busy="true" aria-live="polite">
          <span className="sr-only">Načítám třídy…</span>
          <div aria-hidden="true" className="grid gap-2 sm:grid-cols-2">
            {[0, 1].map((item) => (
              <div
                key={item}
                className="flex min-h-16 items-center gap-3 rounded-md border border-border bg-surface px-3 py-2.5"
              >
                <Skeleton className="size-5 shrink-0" />
                <div className="grid flex-1 gap-2">
                  <Skeleton className="h-4 w-2/3" />
                  <Skeleton className="h-3 w-20" />
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : error ? (
        <Notice tone="error">
          <div className="flex flex-col items-start gap-3 sm:flex-row sm:items-center sm:justify-between">
            <span>
              {error} Bez načteného cíle nelze relaci spustit.
            </span>
            <Button type="button" variant="secondary" size="sm" onClick={onRetry}>
              Načíst znovu
            </Button>
          </div>
        </Notice>
      ) : classes.length === 0 ? (
        <p className="rounded-md border border-border bg-surface px-3.5 py-3 text-sm leading-6 text-muted-foreground">
          Nemáte přiřazenou žádnou aktivní třídu. O přiřazení požádejte správce.
        </p>
      ) : (
        <div className="grid gap-2 sm:grid-cols-2">
          {classes.map((schoolClass) => {
            const checked = selected.has(schoolClass.uuid);
            return (
              <div key={schoolClass.uuid} className="overflow-hidden rounded-md border border-border bg-surface">
                <label className="flex min-h-16 items-center gap-3 px-3 py-2.5 text-left transition-colors duration-150 focus-within:ring-[3px] focus-within:ring-ring/35 hover:bg-surface-subtle motion-reduce:transition-none">
                  <input
                    type="checkbox"
                    className="peer sr-only"
                    checked={checked}
                    aria-describedby="session-audience-help"
                    onChange={() => toggle(schoolClass.uuid)}
                  />
                  <span
                    aria-hidden="true"
                    className={`grid size-5 shrink-0 place-items-center rounded-sm border ${
                      checked
                        ? "border-brand bg-brand text-brand-foreground"
                        : "border-border-strong bg-field"
                    }`}
                  >
                    {checked ? <Check className="size-3.5" strokeWidth={3} /> : null}
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block font-semibold text-foreground">{schoolClass.name}</span>
                    <span className="mt-0.5 block text-xs font-normal text-muted-foreground">
                      Celá třída · {studentCountLabel(schoolClass.studentCount)}
                    </span>
                  </span>
                </label>
                {(schoolClass.groups ?? []).filter((group) => group.active).length > 0 ? (
                  <div className="grid gap-1 border-t border-border px-3 py-2">
                    {(schoolClass.groups ?? []).filter((group) => group.active).map((group) => {
                      const groupChecked = selectedGroups.has(group.uuid);
                      return (
                        <label key={group.uuid} className="flex min-h-11 items-center gap-3 rounded-md px-2 py-2 text-sm hover:bg-surface-subtle">
                          <input
                            type="checkbox"
                            className="peer sr-only"
                            checked={groupChecked}
                            aria-describedby="session-audience-help"
                            onChange={() => toggleGroup(schoolClass.uuid, group.uuid)}
                          />
                          <span aria-hidden="true" className={`grid size-4 shrink-0 place-items-center rounded-sm border ${groupChecked ? "border-brand bg-brand text-brand-foreground" : "border-border-strong bg-field"}`}>
                            {groupChecked ? <Check className="size-3" strokeWidth={3} /> : null}
                          </span>
                          <span className="min-w-0 flex-1 truncate text-foreground">{group.name}</span>
                          <span className="text-xs text-muted-foreground">{studentCountLabel(group.members.length)}</span>
                        </label>
                      );
                    })}
                  </div>
                ) : null}
              </div>
            );
          })}
        </div>
      )}

      <p
        id="session-audience-status"
        className="text-sm leading-6 text-muted-foreground"
        aria-live="polite"
      >
        {selectedClassIds.length === 0 && selectedGroupIds.length === 0
          ? "Zatím není vybrána žádná třída ani skupina."
          : `Vybráno: ${selectedNames.join(", ")}.`}
      </p>
    </fieldset>
  );
}

export function TeacherHome() {
  const router = useRouter();
  const [quizzes, setQuizzes] = useState<Quiz[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [activeSessions, setActiveSessions] = useState<ActiveSession[]>([]);
  const [sessionsLoading, setSessionsLoading] = useState(true);
  const [sessionsError, setSessionsError] = useState<string | null>(null);
  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [classesLoading, setClassesLoading] = useState(true);
  const [classesError, setClassesError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const deferredSearch = useDeferredValue(search);
  const [status, setStatus] = useState<StatusFilter>("ALL");
  const [page, setPage] = useState(0);

  const [launchQuiz, setLaunchQuiz] = useState<Quiz | null>(null);
  const [duration, setDuration] = useState("45");
  const [durationTouched, setDurationTouched] = useState(false);
  const [selectedClassIds, setSelectedClassIds] = useState<string[]>([]);
  const [selectedGroupIds, setSelectedGroupIds] = useState<string[]>([]);
  const [launching, setLaunching] = useState(false);
  const [launchError, setLaunchError] = useState<string | null>(null);

  const loadQuizzes = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const firstPage = await quizService.list(0, 100);
      const remainingPages =
        firstPage.totalPages > 1
          ? await Promise.all(
              Array.from({ length: firstPage.totalPages - 1 }, (_, index) =>
                quizService.list(index + 1, 100),
              ),
            )
          : [];
      setQuizzes([
        ...firstPage.content,
        ...remainingPages.flatMap((response) => response.content),
      ]);
    } catch (error) {
      setLoadError(
        messageFromError(error, "Knihovnu kvízů se nepodařilo načíst."),
      );
    } finally {
      setLoading(false);
    }
  }, []);

  const loadActiveSessions = useCallback(async () => {
    try {
      const sessions = await sessionService.teacherActive();
      setActiveSessions(
        [...sessions].sort(
          (left, right) =>
            new Date(right.startedAt).getTime() -
            new Date(left.startedAt).getTime(),
        ),
      );
      setSessionsError(null);
    } catch (error) {
      setSessionsError(
        messageFromError(error, "Probíhající relace se nepodařilo načíst."),
      );
    } finally {
      setSessionsLoading(false);
    }
  }, []);

  const loadClasses = useCallback(async () => {
    setClassesLoading(true);
    setClassesError(null);
    try {
      const loadedClasses = await classService.list();
      setClasses(
        [...loadedClasses].sort((left, right) =>
          left.name.localeCompare(right.name, "cs"),
        ),
      );
      const availableIds = new Set(loadedClasses.map((item) => item.uuid));
      setSelectedClassIds((current) =>
        current.filter((classUuid) => availableIds.has(classUuid)),
      );
      const availableGroupIds = new Set(
        loadedClasses.flatMap((item) => (item.groups ?? []).map((group) => group.uuid)),
      );
      setSelectedGroupIds((current) =>
        current.filter((groupUuid) => availableGroupIds.has(groupUuid)),
      );
    } catch (error) {
      setClassesError(
        messageFromError(error, "Třídy se nepodařilo načíst."),
      );
    } finally {
      setClassesLoading(false);
    }
  }, []);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => void loadQuizzes(), 0);
    return () => window.clearTimeout(initialLoad);
  }, [loadQuizzes]);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => void loadClasses(), 0);
    return () => window.clearTimeout(initialLoad);
  }, [loadClasses]);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => void loadActiveSessions(), 0);
    return () => window.clearTimeout(initialLoad);
  }, [loadActiveSessions]);

  useLiveEvents(DASHBOARD_EVENTS, (reason) => {
    if (reason === "sessions-changed") {
      void loadActiveSessions();
      return;
    }
    if (reason === "quizzes-changed") {
      void loadQuizzes();
      return;
    }
    void Promise.all([loadActiveSessions(), loadClasses()]);
  });

  const filtered = useMemo(() => {
    const query = deferredSearch.trim().toLocaleLowerCase("cs");
    return quizzes
      .filter((quiz) => status === "ALL" || (quiz.status ?? "DRAFT") === status)
      .filter((quiz) => !query || quizSearchText(quiz).includes(query))
      .sort((left, right) => quizTimestamp(right) - quizTimestamp(left));
  }, [deferredSearch, quizzes, status]);

  const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, pageCount - 1);
  const visibleQuizzes = filtered.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

  const parsedDuration = Number(duration);
  const durationError =
    !Number.isInteger(parsedDuration) || parsedDuration < 1
      ? "Zadejte kladný počet celých minut."
      : parsedDuration > 1_440
        ? "Doba relace může být nejvýše 1 440 minut."
        : null;

  function selectLaunchQuiz(quiz: Quiz) {
    setLaunchQuiz(quiz);
    setDuration("45");
    setDurationTouched(false);
    setSelectedClassIds([]);
    setSelectedGroupIds([]);
    setLaunchError(null);
    window.requestAnimationFrame(() => {
      document.getElementById("session-launch")?.scrollIntoView({
        behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches
          ? "auto"
          : "smooth",
        block: "center",
      });
    });
  }

  async function startSession(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setDurationTouched(true);
    setLaunchError(null);
    if (
      !launchQuiz ||
      durationError ||
      questionCount(launchQuiz) === 0 ||
      classesLoading ||
      selectedClassIds.length === 0 &&
      selectedGroupIds.length === 0
    ) return;

    setLaunching(true);
    try {
      const sessionUuid = await sessionService.create(
        launchQuiz.uuid,
        parsedDuration,
        selectedClassIds,
        selectedGroupIds,
      );
      router.push(`/session/${sessionUuid}`);
    } catch (error) {
      setLaunchError(
        messageFromError(error, "Relaci se nepodařilo spustit. Zkuste to znovu."),
      );
    } finally {
      setLaunching(false);
    }
  }

  return (
    <div className="grid gap-7">
      <header className="flex flex-col gap-5 border-b border-border pb-6 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-brand-text">Výuka</p>
          <h1 className="mt-1 text-3xl font-semibold tracking-[-0.025em] text-balance text-foreground sm:text-4xl">
            Knihovna kvízů
          </h1>
          <p className="mt-2 max-w-2xl text-base leading-7 text-muted-foreground">
            Připravujte otázky, spravujte dostupnost a spouštějte relace pro třídu.
          </p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row">
          <Link
            href="/quiz/import"
            className={buttonStyles({ variant: "secondary", size: "lg" })}
          >
            <FileCode2 aria-hidden="true" className="size-5" />
            Vytvořit kvíz přes YAML
          </Link>
          <Link
            href="/quiz/new"
            className={buttonStyles({ variant: "primary", size: "lg" })}
          >
            <Plus aria-hidden="true" className="size-5" />
            Vytvořit kvíz
          </Link>
        </div>
      </header>

      <section aria-labelledby="active-sessions-title" className="grid gap-4">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h2
                id="active-sessions-title"
                className="text-xl font-semibold text-foreground"
              >
                Probíhající relace
              </h2>
              {!sessionsLoading ? (
                <Badge variant={activeSessions.length > 0 ? "brand" : "neutral"}>
                  {activeSessions.length}
                </Badge>
              ) : null}
            </div>
            <p className="mt-1 text-sm leading-6 text-muted-foreground">
              Otevřete odevzdané odpovědi, průběžné výsledky a ruční hodnocení.
            </p>
          </div>
        </div>

        {sessionsError ? <Notice tone="error">{sessionsError}</Notice> : null}

        {sessionsLoading ? (
          <div role="status" aria-busy="true" aria-live="polite">
            <span className="sr-only">Načítám probíhající relace…</span>
            <div aria-hidden="true" className="grid gap-3 lg:grid-cols-2">
              {[0, 1].map((item) => (
                <Panel key={item}>
                  <PanelContent className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
                    <div className="grid gap-2">
                      <Skeleton className="h-6 w-24" />
                      <Skeleton className="h-6 w-2/3" />
                      <Skeleton className="h-4 w-1/2" />
                      <Skeleton className="h-4 w-3/4" />
                    </div>
                    <Skeleton className="h-11 w-48" />
                  </PanelContent>
                </Panel>
              ))}
            </div>
          </div>
        ) : activeSessions.length === 0 ? (
          <Panel tone="subtle">
            <PanelContent className="flex items-start gap-3 py-4">
              <span className="grid size-9 shrink-0 place-items-center rounded-md border border-border bg-surface-raised text-muted-foreground">
                <Radio aria-hidden="true" className="size-[1.125rem]" />
              </span>
              <div>
                <p className="font-semibold text-foreground">
                  Žádná relace právě neběží
                </p>
                <p className="mt-1 text-sm leading-6 text-muted-foreground">
                  Novou relaci spustíte u některého z kvízů níže.
                </p>
              </div>
            </PanelContent>
          </Panel>
        ) : (
          <ul className="grid gap-3 lg:grid-cols-2">
            {activeSessions.map((session) => (
              <li key={session.sessionUuid}>
                <Panel className="h-full">
                  <PanelContent className="flex h-full flex-col gap-4">
                    <div className="min-w-0">
                      <Badge variant="brand">
                        <Radio aria-hidden="true" className="size-3" />
                        Probíhá
                      </Badge>
                      <h3 className="mt-3 text-lg leading-6 font-semibold text-foreground">
                        {session.title}
                      </h3>
                      <p className="mt-1 text-sm text-muted-foreground">
                        {[session.subject, session.chapter]
                          .filter(Boolean)
                          .join(" · ") || "Bez zařazení"}
                      </p>
                      <p className="mt-2 flex items-start gap-2 text-sm leading-6 text-muted-foreground">
                        <Users
                          aria-hidden="true"
                          className="mt-1 size-4 shrink-0"
                        />
                        <span>
                          {session.openToAllStudents !== false ||
                          !session.assignedClasses?.length
                            ? "Všichni žáci"
                            : session.assignedClasses
                                .map((schoolClass) => schoolClass.name)
                                .join(", ")}
                        </span>
                      </p>
                    </div>

                    <dl className="border-t border-border pt-3 text-sm">
                      <div className="flex items-center gap-2">
                        <Clock3
                          aria-hidden="true"
                          className="size-4 shrink-0 text-muted-foreground"
                        />
                        <div>
                          <dt className="sr-only">Dostupná do</dt>
                          <dd className="text-muted-foreground">
                            Do {formatDateTime(session.expiresAt)}
                          </dd>
                        </div>
                      </div>
                    </dl>

                    <Link
                      href={`/session/${session.sessionUuid}#student-answers`}
                      className={buttonStyles({
                        variant: "primary",
                        size: "sm",
                        className: "mt-auto w-full sm:w-fit",
                      })}
                    >
                      <ClipboardList aria-hidden="true" className="size-4" />
                      Odpovědi a hodnocení
                    </Link>
                  </PanelContent>
                </Panel>
              </li>
            ))}
          </ul>
        )}
      </section>

      {launchQuiz ? (
        <Panel id="session-launch" className="border-brand/45">
          <PanelHeader className="flex-row items-start justify-between gap-4">
            <div>
              <PanelTitle>Spustit relaci</PanelTitle>
              <PanelDescription>
                {launchQuiz.title} · {questionCountLabel(questionCount(launchQuiz))}
              </PanelDescription>
            </div>
            <Button
              variant="quiet"
              size="icon"
              aria-label="Zavřít nastavení relace"
              onClick={() => setLaunchQuiz(null)}
            >
              <X aria-hidden="true" className="size-5" />
            </Button>
          </PanelHeader>
          <PanelContent>
            <form
              onSubmit={startSession}
              className="grid items-end gap-4 sm:grid-cols-[minmax(12rem,18rem)_auto]"
            >
              <div className="sm:col-span-2">
                <ClassAudienceSelector
                  classes={classes}
                  disabled={launching}
                  error={classesError}
                  loading={classesLoading}
                  onChange={setSelectedClassIds}
                  onGroupsChange={setSelectedGroupIds}
                  onRetry={() => void loadClasses()}
                  selectedClassIds={selectedClassIds}
                  selectedGroupIds={selectedGroupIds}
                />
              </div>
              <Field
                label="Doba relace v minutách"
                required
                controlId="session-duration"
                error={durationTouched ? durationError : undefined}
                description="Po uplynutí této doby se žáci už nepřipojí."
              >
                <Input
                  type="number"
                  inputMode="numeric"
                  min={1}
                  max={1440}
                  step={1}
                  value={duration}
                  onChange={(event) => setDuration(event.target.value)}
                  onBlur={() => setDurationTouched(true)}
                />
              </Field>
              <Button
                type="submit"
                isLoading={launching}
                disabled={
                  questionCount(launchQuiz) === 0 ||
                  classesLoading ||
                  (selectedClassIds.length === 0 && selectedGroupIds.length === 0)
                }
                leadingIcon={<Play className="size-4" />}
              >
                Spustit relaci
              </Button>
              {launchError ? (
                <Notice tone="error" className="sm:col-span-2">
                  {launchError}
                </Notice>
              ) : null}
            </form>
          </PanelContent>
        </Panel>
      ) : null}

      <section aria-labelledby="quiz-list-title" className="grid gap-4">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 id="quiz-list-title" className="text-xl font-semibold text-foreground">
              Vaše kvízy
            </h2>
            <p className="mt-1 text-sm text-muted-foreground" aria-live="polite">
              {quizResultCountLabel(filtered.length)}
            </p>
          </div>
          <div className="grid gap-3 sm:grid-cols-[minmax(16rem,24rem)_12rem]">
            <Field label="Hledat v knihovně" controlId="quiz-search">
              <Input
                type="search"
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(0);
                }}
                placeholder="Název, předmět, kapitola…"
              />
            </Field>
            <Field label="Stav" controlId="quiz-status-filter">
              <SelectField
                value={status}
                onValueChange={(value) => {
                  setStatus(value as StatusFilter);
                  setPage(0);
                }}
                options={[
                  { value: "ALL", label: "Všechny stavy" },
                  ...(Object.keys(quizStatusLabels) as QuizStatus[]).map(
                    (item) => ({
                      value: item,
                      label: quizStatusLabels[item],
                    }),
                  ),
                ]}
              />
            </Field>
          </div>
        </div>

        {loading ? (
          <QuizLibrarySkeleton />
        ) : loadError ? (
          <EmptyState
            icon={BookOpen}
            heading="Knihovnu se nepodařilo načíst"
            description={loadError}
            action={<Button onClick={() => void loadQuizzes()}>Zkusit znovu</Button>}
          />
        ) : visibleQuizzes.length === 0 ? (
          <EmptyState
            icon={BookOpen}
            heading={quizzes.length === 0 ? "Zatím tu nejsou žádné kvízy" : "Žádný kvíz neodpovídá filtru"}
            description={
              quizzes.length === 0
                ? "Vytvořte první kvíz ručně nebo importujte připravený soubor YAML."
                : "Upravte hledaný výraz nebo zvolte jiný stav."
            }
            action={
              quizzes.length === 0 ? (
                <Link href="/quiz/new" className={buttonStyles()}>
                  <Plus aria-hidden="true" className="size-4" />
                  Vytvořit kvíz
                </Link>
              ) : (
                <Button
                  variant="secondary"
                  onClick={() => {
                    setSearch("");
                    setStatus("ALL");
                    setPage(0);
                  }}
                >
                  Zrušit filtry
                </Button>
              )
            }
          />
        ) : (
          <>
            <div className="hidden overflow-hidden rounded-lg border border-border bg-panel md:block">
              <table className="w-full table-fixed border-collapse text-left">
                <thead className="bg-surface text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                  <tr>
                    <th scope="col" className="w-[38%] px-5 py-3">Kvíz</th>
                    <th scope="col" className="w-[18%] px-4 py-3">Stav</th>
                    <th scope="col" className="w-[14%] px-4 py-3">Otázky</th>
                    <th scope="col" className="w-[30%] px-5 py-3">Akce</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {visibleQuizzes.map((quiz) => (
                    <tr key={quiz.uuid} className="align-top">
                      <th scope="row" className="px-5 py-4 font-normal">
                        <Link
                          href={`/quiz/${quiz.uuid}`}
                          className="font-semibold text-foreground underline-offset-4 hover:text-brand-text hover:underline"
                        >
                          {quiz.title}
                        </Link>
                        <p className="mt-1 line-clamp-1 text-sm text-muted-foreground">
                          {[quiz.subject, quiz.chapter].filter(Boolean).join(" · ") || "Bez zařazení"}
                        </p>
                        <p className="mt-1 text-xs text-muted-foreground">
                          Upraveno {formatDateTime(quiz.editedAt ?? quiz.createdAt)}
                        </p>
                      </th>
                      <td className="px-4 py-4"><QuizStatusBadge status={quiz.status} /></td>
                      <td className="px-4 py-4 tabular-nums text-sm text-foreground">
                        {questionCount(quiz)}
                      </td>
                      <td className="px-5 py-4"><QuizActions quiz={quiz} onStart={selectLaunchQuiz} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <ul className="grid gap-3 md:hidden">
              {visibleQuizzes.map((quiz) => (
                <li key={quiz.uuid}>
                  <Panel>
                    <PanelContent className="grid gap-4">
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <Link
                            href={`/quiz/${quiz.uuid}`}
                            className="text-lg font-semibold text-foreground underline-offset-4 hover:text-brand-text hover:underline"
                          >
                            {quiz.title}
                          </Link>
                          <p className="mt-1 text-sm text-muted-foreground">
                            {[quiz.subject, quiz.chapter].filter(Boolean).join(" · ") || "Bez zařazení"}
                          </p>
                        </div>
                        <QuizStatusBadge status={quiz.status} />
                      </div>
                      <dl className="grid grid-cols-2 gap-3 border-y border-border py-3 text-sm">
                        <div>
                          <dt className="text-muted-foreground">Otázky</dt>
                          <dd className="mt-0.5 font-semibold tabular-nums text-foreground">{questionCount(quiz)}</dd>
                        </div>
                        <div>
                          <dt className="text-muted-foreground">Upraveno</dt>
                          <dd className="mt-0.5 text-foreground">{formatDateTime(quiz.editedAt ?? quiz.createdAt)}</dd>
                        </div>
                      </dl>
                      <QuizActions quiz={quiz} onStart={selectLaunchQuiz} />
                    </PanelContent>
                  </Panel>
                </li>
              ))}
            </ul>

            {pageCount > 1 ? (
              <nav
                aria-label="Stránkování knihovny"
                className="flex flex-wrap items-center justify-between gap-3 border-t border-border pt-4"
              >
                <p className="text-sm text-muted-foreground">
                  Strana <span className="tabular-nums">{safePage + 1}</span> z{" "}
                  <span className="tabular-nums">{pageCount}</span>
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={safePage === 0}
                    leadingIcon={<ChevronLeft className="size-4" />}
                    onClick={() => setPage(Math.max(0, safePage - 1))}
                  >
                    Předchozí
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={safePage >= pageCount - 1}
                    onClick={() => setPage(Math.min(pageCount - 1, safePage + 1))}
                  >
                    Další
                    <ChevronRight aria-hidden="true" className="size-4" />
                  </Button>
                </div>
              </nav>
            ) : null}
          </>
        )}
      </section>
    </div>
  );
}
