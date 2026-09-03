"use client";

import {
  BookOpen,
  Building2,
  ChevronDown,
  Plus,
  Save,
  Search,
  ShieldCheck,
  Trash2,
  UserRoundCog,
  UsersRound,
} from "lucide-react";
import {
  useCallback,
  useEffect,
  useId,
  useMemo,
  useState,
  type FormEvent,
} from "react";

import {
  Button,
  Checkbox,
  EmptyState,
  Field,
  Input,
  Panel,
  PanelContent,
  Skeleton,
} from "@/components/ui";
import { Notice } from "@/features/teacher/shared";
import { messageFromError } from "@/lib/http";
import { adminService } from "@/lib/services";
import type {
  AdminClassResponse,
  AdminSubjectResponse,
  AdminTeacherResponse,
  AdminTeacherSummary,
  ClassMember,
} from "@/types/domain";

function teacherName(teacher: AdminTeacherSummary) {
  return (
    teacher.displayName?.trim() ||
    teacher.preferredUsername?.trim() ||
    "Vyučující bez jména"
  );
}

function memberName(member: ClassMember) {
  const name = member.studentName?.trim();
  return name && name !== member.studentId ? name : "Žák bez jména";
}

const dateTimeFormatter = new Intl.DateTimeFormat("cs-CZ", {
  dateStyle: "medium",
  timeStyle: "short",
});

function formatDateTime(value: string | null) {
  if (!value) return "Zatím bez aktivity";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "Čas není k dispozici"
    : dateTimeFormatter.format(date);
}

function countLabel(count: number, one: string, few: string, many: string) {
  if (count === 1) return `1 ${one}`;
  if (count >= 2 && count <= 4) return `${count} ${few}`;
  return `${count} ${many}`;
}

function sameIds(left: Set<string>, right: Set<string>) {
  return left.size === right.size && [...left].every((id) => right.has(id));
}

function TeacherSelection({
  disabled = false,
  error,
  onChange,
  selectedIds,
  teachers,
}: {
  disabled?: boolean;
  error?: string;
  onChange: (next: Set<string>) => void;
  selectedIds: Set<string>;
  teachers: AdminTeacherResponse[];
}) {
  const instanceId = useId().replaceAll(":", "");
  const helpId = `${instanceId}-teacher-help`;
  const errorId = error ? `${instanceId}-teacher-error` : undefined;

  return (
    <fieldset
      disabled={disabled}
      aria-describedby={[helpId, errorId].filter(Boolean).join(" ")}
      className="grid min-w-0 grid-cols-1 gap-3"
    >
      <legend className="text-sm font-semibold text-foreground">
        Vyučující s přístupem
        <span className="ml-1 text-danger-text" aria-hidden="true">*</span>
        <span className="sr-only"> (povinné)</span>
      </legend>
      <p id={helpId} className="text-sm leading-6 text-muted-foreground">
        Vyberte alespoň jednoho vyučujícího. Každý vybraný bude moci třídu
        spravovat a spouštět pro ni relace.
      </p>
      {teachers.length === 0 ? (
        <p className="rounded-md border border-dashed border-border-strong bg-surface px-4 py-3 text-sm text-muted-foreground">
          Zatím není koho přiřadit.
        </p>
      ) : (
        <ul className="grid min-w-0 grid-cols-1 gap-2 sm:grid-cols-2">
          {teachers.map((teacher) => {
            const controlId = `${instanceId}-teacher-${teacher.teacherId}`;
            const checked = selectedIds.has(teacher.teacherId);
            return (
              <li key={teacher.teacherId}>
                <label
                  htmlFor={controlId}
                  className="flex min-h-11 items-center gap-3 rounded-md border border-border bg-surface px-3 py-2.5 text-sm font-medium text-foreground hover:border-brand/60"
                >
                  <Checkbox
                    id={controlId}
                    checked={checked}
                    onCheckedChange={() => {
                      const next = new Set(selectedIds);
                      if (next.has(teacher.teacherId)) next.delete(teacher.teacherId);
                      else next.add(teacher.teacherId);
                      onChange(next);
                    }}
                  />
                  <span className="min-w-0 truncate">{teacherName(teacher)}</span>
                </label>
              </li>
            );
          })}
        </ul>
      )}
      {error ? (
        <p id={errorId} role="alert" className="text-sm text-danger-text">
          {error}
        </p>
      ) : null}
    </fieldset>
  );
}

function StudentSelection({
  disabled = false,
  onChange,
  selectedIds,
  students,
}: {
  disabled?: boolean;
  onChange: (next: Set<string>) => void;
  selectedIds: Set<string>;
  students: ClassMember[];
}) {
  const instanceId = useId().replaceAll(":", "");
  const [query, setQuery] = useState("");
  const normalizedQuery = query.trim().toLocaleLowerCase("cs");
  const visibleStudents = students.filter((student) => {
    if (!normalizedQuery) return true;
    return [student.studentName, student.preferredUsername, student.studentId]
      .filter(Boolean)
      .some((value) => value?.toLocaleLowerCase("cs").includes(normalizedQuery));
  });

  return (
    <fieldset disabled={disabled} className="grid min-w-0 grid-cols-1 gap-3">
      <legend className="font-semibold text-foreground">Členové třídy</legend>
      <p className="text-sm leading-6 text-muted-foreground">
        Vyberte žáky z ověřeného školního adresáře. Jeden žák může být současně
        pouze v jedné hlavní třídě.
      </p>
      <Field label="Hledat žáka" optional>
        <div className="relative">
          <Search
            aria-hidden="true"
            className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
          />
          <Input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Jméno nebo školní účet"
            className="pl-9"
            autoComplete="off"
          />
        </div>
      </Field>
      {visibleStudents.length > 0 ? (
        <ul className="grid max-h-72 min-w-0 grid-cols-1 gap-2 overflow-y-auto pr-1 sm:grid-cols-2">
          {visibleStudents.map((student) => {
            const controlId = `${instanceId}-student-${student.studentId}`;
            const checked = selectedIds.has(student.studentId);
            return (
              <li key={student.studentId}>
                <label
                  htmlFor={controlId}
                  className="flex min-h-11 items-center gap-3 rounded-md border border-border bg-surface px-3 py-2.5 text-sm text-foreground hover:border-brand/60"
                >
                  <Checkbox
                    id={controlId}
                    checked={checked}
                    onCheckedChange={() => {
                      const next = new Set(selectedIds);
                      if (next.has(student.studentId)) next.delete(student.studentId);
                      else next.add(student.studentId);
                      onChange(next);
                    }}
                  />
                  <span className="min-w-0">
                    <span className="block truncate font-medium">
                      {memberName(student)}
                    </span>
                    {student.preferredUsername ? (
                      <span className="mt-0.5 block truncate text-xs text-muted-foreground">
                        {student.preferredUsername}
                      </span>
                    ) : null}
                  </span>
                </label>
              </li>
            );
          })}
        </ul>
      ) : (
        <p className="rounded-md border border-dashed border-border-strong bg-surface px-4 py-3 text-sm text-muted-foreground">
          {students.length === 0
            ? "Zatím nejsou evidováni žádní žáci."
            : "Hledání neodpovídá žádnému žákovi."}
        </p>
      )}
      <p className="text-sm text-muted-foreground" aria-live="polite">
        {countLabel(
          selectedIds.size,
          "vybraný člen",
          "vybraní členové",
          "vybraných členů",
        )}
      </p>
    </fieldset>
  );
}

function AdminWorkspaceSkeleton() {
  return (
    <div role="status" aria-busy="true" aria-live="polite" className="grid gap-8">
      <span className="sr-only">Načítám administraci školy…</span>
      <header aria-hidden="true" className="grid gap-3 border-b border-border pb-6">
        <Skeleton className="h-5 w-28" />
        <Skeleton className="h-10 w-72 max-w-[85vw]" />
        <Skeleton className="h-5 w-full max-w-2xl" />
      </header>
      <Panel aria-hidden="true">
        <PanelContent className="grid gap-px p-0 sm:grid-cols-3">
          {[0, 1, 2].map((item) => (
            <div key={item} className="grid gap-2 bg-panel px-5 py-4">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-8 w-12" />
            </div>
          ))}
        </PanelContent>
      </Panel>
      {[0, 1, 2].map((section) => (
        <section key={section} aria-hidden="true" className="grid gap-4">
          <div className="grid gap-2">
            <Skeleton className="h-8 w-52" />
            <Skeleton className="h-5 w-full max-w-xl" />
          </div>
          <Panel>
            <PanelContent className="grid gap-4">
              {[0, 1].map((row) => (
                <div key={row} className="grid gap-2 border-t border-border py-4 first:border-0">
                  <Skeleton className="h-5 w-48" />
                  <Skeleton className="h-4 w-72 max-w-full" />
                </div>
              ))}
            </PanelContent>
          </Panel>
        </section>
      ))}
    </div>
  );
}

function TeacherDirectory({
  classes,
  subjects,
  teachers,
}: {
  classes: AdminClassResponse[];
  subjects: AdminSubjectResponse[];
  teachers: AdminTeacherResponse[];
}) {
  const classCounts = useMemo(() => {
    const counts = new Map<string, number>();
    for (const schoolClass of classes) {
      for (const teacher of schoolClass.teachers) {
        counts.set(
          teacher.teacherId,
          (counts.get(teacher.teacherId) ?? 0) + 1,
        );
      }
    }
    return counts;
  }, [classes]);
  const subjectCounts = useMemo(() => {
    const counts = new Map<string, number>();
    for (const subject of subjects) {
      for (const teacher of subject.teachers) {
        counts.set(
          teacher.teacherId,
          (counts.get(teacher.teacherId) ?? 0) + 1,
        );
      }
    }
    return counts;
  }, [subjects]);

  if (teachers.length === 0) {
    return (
      <EmptyState
        compact
        icon={UserRoundCog}
        heading="Zatím nejsou evidováni vyučující"
        description="Vyučující se objeví po prvním přihlášení školním účtem."
      />
    );
  }

  return (
    <>
      <div className="hidden overflow-hidden rounded-lg border border-border bg-panel md:block">
        <table className="w-full border-collapse text-left">
          <thead className="bg-surface text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            <tr>
              <th scope="col" className="px-5 py-3">Vyučující</th>
              <th scope="col" className="px-4 py-3">Třídy</th>
              <th scope="col" className="px-4 py-3">Předměty</th>
              <th scope="col" className="px-5 py-3">Poslední aktivita</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {teachers.map((teacher) => (
              <tr key={teacher.teacherId}>
                <th scope="row" className="px-5 py-4 font-normal">
                  <p className="font-semibold text-foreground">{teacherName(teacher)}</p>
                  {teacher.preferredUsername ? (
                    <p className="mt-0.5 text-sm text-muted-foreground">
                      {teacher.preferredUsername}
                    </p>
                  ) : null}
                </th>
                <td className="px-4 py-4 text-sm text-foreground">
                  {countLabel(classCounts.get(teacher.teacherId) ?? 0, "třída", "třídy", "tříd")}
                </td>
                <td className="px-4 py-4 text-sm text-foreground">
                  {countLabel(subjectCounts.get(teacher.teacherId) ?? 0, "předmět", "předměty", "předmětů")}
                </td>
                <td className="px-5 py-4 text-sm text-muted-foreground">
                  {formatDateTime(teacher.lastSeenAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ul className="grid min-w-0 grid-cols-1 gap-3 md:hidden">
        {teachers.map((teacher) => (
          <li key={teacher.teacherId}>
            <Panel>
              <PanelContent className="grid gap-3">
                <div>
                  <p className="font-semibold text-foreground">{teacherName(teacher)}</p>
                  {teacher.preferredUsername ? (
                    <p className="mt-0.5 text-sm text-muted-foreground">
                      {teacher.preferredUsername}
                    </p>
                  ) : null}
                </div>
                <dl className="grid grid-cols-2 gap-3 border-t border-border pt-3 text-sm">
                  <div>
                    <dt className="text-muted-foreground">Třídy</dt>
                    <dd className="mt-0.5 font-semibold text-foreground">
                      {classCounts.get(teacher.teacherId) ?? 0}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-muted-foreground">Předměty</dt>
                    <dd className="mt-0.5 font-semibold text-foreground">
                      {subjectCounts.get(teacher.teacherId) ?? 0}
                    </dd>
                  </div>
                </dl>
                <p className="text-xs text-muted-foreground">
                  Poslední aktivita: {formatDateTime(teacher.lastSeenAt)}
                </p>
              </PanelContent>
            </Panel>
          </li>
        ))}
      </ul>
    </>
  );
}

function CreateClassForm({
  onCreated,
  teachers,
}: {
  onCreated: (created: AdminClassResponse) => void;
  teachers: AdminTeacherResponse[];
}) {
  const [name, setName] = useState("");
  const [teacherIds, setTeacherIds] = useState(
    () => new Set(teachers[0] ? [teachers[0].teacherId] : []),
  );
  const [touched, setTouched] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const trimmedName = name.trim();
  const nameError = !trimmedName
    ? "Zadejte název třídy."
    : trimmedName.length > 100
      ? "Název může mít nejvýše 100 znaků."
      : "";
  const teacherError = teacherIds.size === 0
    ? "Vyberte alespoň jednoho vyučujícího."
    : "";

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setTouched(true);
    setError("");
    if (nameError || teacherError) return;
    setBusy(true);
    try {
      const created = await adminService.createClass({
        name: trimmedName,
        teacherIds: [...teacherIds],
      });
      onCreated(created);
      setName("");
      setTouched(false);
    } catch (createError) {
      setError(messageFromError(createError, "Třídu se nepodařilo vytvořit."));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={create} className="grid min-w-0 grid-cols-1 gap-5 rounded-lg border border-border bg-surface px-4 py-4">
      <Field label="Název nové třídy" required error={touched ? nameError : undefined}>
        <Input
          value={name}
          maxLength={100}
          disabled={busy || teachers.length === 0}
          onBlur={() => setTouched(true)}
          onChange={(event) => setName(event.target.value)}
          placeholder="Např. 3.B"
        />
      </Field>
      <TeacherSelection
        teachers={teachers}
        selectedIds={teacherIds}
        onChange={setTeacherIds}
        disabled={busy}
        error={touched ? teacherError : undefined}
      />
      <div className="flex justify-end border-t border-border pt-4">
        <Button
          type="submit"
          isLoading={busy}
          disabled={busy || teachers.length === 0 || Boolean(nameError || teacherError)}
          leadingIcon={<Plus className="size-4" />}
        >
          Vytvořit třídu
        </Button>
      </div>
      {error ? <Notice tone="error">{error}</Notice> : null}
    </form>
  );
}

function AdminClassEditor({
  onDelete,
  onReplace,
  schoolClass,
  students,
  teachers,
}: {
  onDelete: (classUuid: string) => void;
  onReplace: (updated: AdminClassResponse) => void;
  schoolClass: AdminClassResponse;
  students: ClassMember[];
  teachers: AdminTeacherResponse[];
}) {
  const [name, setName] = useState(schoolClass.name);
  const [teacherIds, setTeacherIds] = useState(
    () => new Set(schoolClass.teachers.map((teacher) => teacher.teacherId)),
  );
  const [studentIds, setStudentIds] = useState(
    () => new Set(schoolClass.members.map((member) => member.studentId)),
  );
  const [busy, setBusy] = useState<"save" | "delete" | null>(null);
  const [error, setError] = useState("");
  const [confirmDelete, setConfirmDelete] = useState(false);

  const trimmedName = name.trim();
  const originalIds = new Set(schoolClass.members.map((member) => member.studentId));
  const originalTeacherIds = new Set(
    schoolClass.teachers.map((teacher) => teacher.teacherId),
  );
  const changed =
    trimmedName !== schoolClass.name ||
    !sameIds(teacherIds, originalTeacherIds) ||
    !sameIds(studentIds, originalIds);
  const nameError = !trimmedName
    ? "Název třídy nesmí být prázdný."
    : trimmedName.length > 100
      ? "Název může mít nejvýše 100 znaků."
      : "";
  const teacherError = teacherIds.size === 0
    ? "Vyberte alespoň jednoho vyučujícího."
    : "";
  const availableStudents = useMemo(() => {
    const values = new Map(
      students.map((student) => [student.studentId, student]),
    );
    schoolClass.members.forEach((student) => {
      if (!values.has(student.studentId)) values.set(student.studentId, student);
    });
    return [...values.values()].sort((left, right) =>
      memberName(left).localeCompare(memberName(right), "cs"),
    );
  }, [schoolClass.members, students]);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    if (!changed || nameError || teacherError) return;
    setBusy("save");
    try {
      const updated = await adminService.updateClass(schoolClass.uuid, {
        name: trimmedName,
        teacherIds: [...teacherIds],
        studentIds: [...studentIds],
      });
      onReplace(updated);
    } catch (saveError) {
      setError(messageFromError(saveError, "Změny třídy se nepodařilo uložit."));
    } finally {
      setBusy(null);
    }
  }

  async function remove() {
    setBusy("delete");
    setError("");
    try {
      await adminService.removeClass(schoolClass.uuid);
      onDelete(schoolClass.uuid);
    } catch (deleteError) {
      setError(messageFromError(deleteError, "Třídu se nepodařilo smazat."));
      setBusy(null);
    }
  }

  return (
    <details className="group rounded-lg border border-border bg-panel">
      <summary className="flex min-h-14 list-none items-center gap-3 px-4 py-3 hover:bg-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring [&::-webkit-details-marker]:hidden sm:px-5">
        <span className="grid size-9 shrink-0 place-items-center rounded-md border border-border bg-surface text-brand-text">
          <UsersRound aria-hidden="true" className="size-4" />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate font-semibold text-foreground">{schoolClass.name}</span>
          <span className="mt-0.5 block truncate text-sm text-muted-foreground">
            {schoolClass.teachers.map(teacherName).join(", ")} · {countLabel(schoolClass.studentCount, "žák", "žáci", "žáků")}
          </span>
        </span>
        <ChevronDown aria-hidden="true" className="size-5 shrink-0 text-muted-foreground transition-transform group-open:rotate-180 motion-reduce:transition-none" />
      </summary>
      <form onSubmit={save} className="grid min-w-0 grid-cols-1 gap-6 border-t border-border px-4 py-5 sm:px-5">
        {error ? <Notice tone="error">{error}</Notice> : null}
        <div className="grid min-w-0 grid-cols-1 gap-4">
          <Field label="Název třídy" required error={nameError || undefined}>
            <Input value={name} maxLength={100} disabled={busy !== null} onChange={(event) => setName(event.target.value)} />
          </Field>
          <TeacherSelection
            teachers={teachers}
            selectedIds={teacherIds}
            onChange={setTeacherIds}
            disabled={busy !== null}
            error={teacherError || undefined}
          />
        </div>

        <div className="border-t border-border pt-5">
          <StudentSelection
            students={availableStudents}
            selectedIds={studentIds}
            onChange={setStudentIds}
            disabled={busy !== null}
          />
        </div>

        <div className="flex flex-col gap-3 border-t border-border pt-5 sm:flex-row sm:items-center sm:justify-between">
          {confirmDelete ? (
            <div role="alert" className="flex flex-1 flex-wrap items-center gap-2 rounded-md border border-danger/45 bg-danger-subtle px-3 py-2">
              <p className="mr-auto text-sm text-danger-text">Smazat tuto třídu?</p>
              <Button type="button" variant="quiet" size="sm" disabled={busy !== null} onClick={() => setConfirmDelete(false)}>Zrušit</Button>
              <Button type="button" variant="danger" size="sm" isLoading={busy === "delete"} onClick={() => void remove()}>Smazat</Button>
            </div>
          ) : (
            <Button type="button" variant="danger" disabled={busy !== null} onClick={() => setConfirmDelete(true)} leadingIcon={<Trash2 className="size-4" />}>Smazat třídu</Button>
          )}
          <Button type="submit" isLoading={busy === "save"} disabled={busy !== null || !changed || Boolean(nameError || teacherError)} leadingIcon={<Save className="size-4" />}>
            Uložit změny
          </Button>
        </div>
      </form>
    </details>
  );
}

function CreateSubjectForm({
  onCreated,
}: {
  onCreated: (created: AdminSubjectResponse) => void;
}) {
  const [name, setName] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const trimmedName = name.trim();

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    if (!trimmedName || trimmedName.length > 100) return;
    setBusy(true);
    try {
      const created = await adminService.createSubject({ name: trimmedName });
      onCreated(created);
      setName("");
    } catch (createError) {
      setError(messageFromError(createError, "Předmět se nepodařilo vytvořit."));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={create} className="grid gap-4 rounded-lg border border-border bg-surface px-4 py-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-end">
      <Field label="Název nového předmětu" required>
        <Input value={name} maxLength={100} disabled={busy} onChange={(event) => setName(event.target.value)} placeholder="Např. Počítačové sítě" />
      </Field>
      <Button type="submit" isLoading={busy} disabled={busy || !trimmedName || trimmedName.length > 100} leadingIcon={<Plus className="size-4" />}>Vytvořit předmět</Button>
      {error ? <Notice tone="error" className="sm:col-span-2">{error}</Notice> : null}
    </form>
  );
}

function AdminSubjectEditor({
  onDelete,
  onReplace,
  subject,
  teachers,
}: {
  onDelete: (subjectUuid: string) => void;
  onReplace: (updated: AdminSubjectResponse) => void;
  subject: AdminSubjectResponse;
  teachers: AdminTeacherResponse[];
}) {
  const instanceId = useId().replaceAll(":", "");
  const [name, setName] = useState(subject.name);
  const [teacherIds, setTeacherIds] = useState(
    () => new Set(subject.teachers.map((teacher) => teacher.teacherId)),
  );
  const [busy, setBusy] = useState<"save" | "delete" | null>(null);
  const [error, setError] = useState("");
  const [confirmDelete, setConfirmDelete] = useState(false);
  const originalTeacherIds = new Set(subject.teachers.map((teacher) => teacher.teacherId));
  const trimmedName = name.trim();
  const changed =
    trimmedName !== subject.name || !sameIds(teacherIds, originalTeacherIds);
  const invalidName = !trimmedName || trimmedName.length > 100;

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    if (!changed || invalidName) return;
    setBusy("save");
    try {
      onReplace(
        await adminService.updateSubject(subject.uuid, {
          name: trimmedName,
          teacherIds: [...teacherIds],
        }),
      );
    } catch (saveError) {
      setError(messageFromError(saveError, "Předmět se nepodařilo uložit."));
    } finally {
      setBusy(null);
    }
  }

  async function remove() {
    setBusy("delete");
    setError("");
    try {
      await adminService.removeSubject(subject.uuid);
      onDelete(subject.uuid);
    } catch (deleteError) {
      setError(messageFromError(deleteError, "Předmět se nepodařilo smazat."));
      setBusy(null);
    }
  }

  return (
    <details className="group rounded-lg border border-border bg-panel">
      <summary className="flex min-h-14 list-none items-center gap-3 px-4 py-3 hover:bg-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring [&::-webkit-details-marker]:hidden sm:px-5">
        <span className="grid size-9 shrink-0 place-items-center rounded-md border border-border bg-surface text-brand-text"><BookOpen aria-hidden="true" className="size-4" /></span>
        <span className="min-w-0 flex-1">
          <span className="block truncate font-semibold text-foreground">{subject.name}</span>
          <span className="mt-0.5 block text-sm text-muted-foreground">{countLabel(subject.teacherCount, "vyučující", "vyučující", "vyučujících")}</span>
        </span>
        <ChevronDown aria-hidden="true" className="size-5 shrink-0 text-muted-foreground transition-transform group-open:rotate-180 motion-reduce:transition-none" />
      </summary>
      <form onSubmit={save} className="grid min-w-0 grid-cols-1 gap-5 border-t border-border px-4 py-5 sm:px-5">
        {error ? <Notice tone="error">{error}</Notice> : null}
        <Field label="Název předmětu" required error={invalidName ? "Zadejte název do 100 znaků." : undefined}>
          <Input value={name} maxLength={100} disabled={busy !== null} onChange={(event) => setName(event.target.value)} />
        </Field>
        <fieldset disabled={busy !== null} className="grid min-w-0 grid-cols-1 gap-3 border-t border-border pt-5">
          <legend className="font-semibold text-foreground">Přiřazení vyučující</legend>
          <p className="text-sm text-muted-foreground">Vybraní vyučující mohou předmět použít u svých kvízů.</p>
          {teachers.length === 0 ? (
            <p className="rounded-md border border-dashed border-border-strong bg-surface px-4 py-3 text-sm text-muted-foreground">Zatím není komu předmět přiřadit.</p>
          ) : (
            <ul className="grid min-w-0 grid-cols-1 gap-2 sm:grid-cols-2">
              {teachers.map((teacher) => {
                const controlId = `${instanceId}-teacher-${teacher.teacherId}`;
                const checked = teacherIds.has(teacher.teacherId);
                return (
                  <li key={teacher.teacherId}>
                    <label htmlFor={controlId} className="flex min-h-11 items-center gap-3 rounded-md border border-border bg-surface px-3 py-2.5 text-sm font-medium text-foreground hover:border-brand/60">
                      <Checkbox
                        id={controlId}
                        checked={checked}
                        onCheckedChange={() => setTeacherIds((current) => {
                          const next = new Set(current);
                          if (next.has(teacher.teacherId)) next.delete(teacher.teacherId);
                          else next.add(teacher.teacherId);
                          return next;
                        })}
                      />
                      <span className="min-w-0 truncate">{teacherName(teacher)}</span>
                    </label>
                  </li>
                );
              })}
            </ul>
          )}
        </fieldset>
        <div className="flex flex-col gap-3 border-t border-border pt-5 sm:flex-row sm:items-center sm:justify-between">
          {confirmDelete ? (
            <div role="alert" className="flex flex-1 flex-wrap items-center gap-2 rounded-md border border-danger/45 bg-danger-subtle px-3 py-2">
              <p className="mr-auto text-sm text-danger-text">Smazat tento předmět?</p>
              <Button type="button" variant="quiet" size="sm" disabled={busy !== null} onClick={() => setConfirmDelete(false)}>Zrušit</Button>
              <Button type="button" variant="danger" size="sm" isLoading={busy === "delete"} onClick={() => void remove()}>Smazat</Button>
            </div>
          ) : (
            <Button type="button" variant="danger" disabled={busy !== null} onClick={() => setConfirmDelete(true)} leadingIcon={<Trash2 className="size-4" />}>Smazat předmět</Button>
          )}
          <Button type="submit" isLoading={busy === "save"} disabled={busy !== null || !changed || invalidName} leadingIcon={<Save className="size-4" />}>Uložit změny</Button>
        </div>
      </form>
    </details>
  );
}

export function AdminWorkspace() {
  const [teachers, setTeachers] = useState<AdminTeacherResponse[]>([]);
  const [students, setStudents] = useState<ClassMember[]>([]);
  const [classes, setClasses] = useState<AdminClassResponse[]>([]);
  const [subjects, setSubjects] = useState<AdminSubjectResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [loadedTeachers, loadedStudents, loadedClasses, loadedSubjects] = await Promise.all([
        adminService.teachers(),
        adminService.students(),
        adminService.classes(),
        adminService.subjects(),
      ]);
      setTeachers([...loadedTeachers].sort((left, right) => teacherName(left).localeCompare(teacherName(right), "cs")));
      setStudents([...loadedStudents].sort((left, right) => memberName(left).localeCompare(memberName(right), "cs")));
      setClasses([...loadedClasses].sort((left, right) => left.name.localeCompare(right.name, "cs")));
      setSubjects([...loadedSubjects].sort((left, right) => left.name.localeCompare(right.name, "cs")));
    } catch (loadError) {
      setError(messageFromError(loadError, "Administraci školy se nepodařilo načíst."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(initialLoad);
  }, [load]);

  function replaceClass(updated: AdminClassResponse) {
    setClasses((current) => current.map((item) => item.uuid === updated.uuid ? updated : item).sort((left, right) => left.name.localeCompare(right.name, "cs")));
  }

  function replaceSubject(updated: AdminSubjectResponse) {
    setSubjects((current) => current.map((item) => item.uuid === updated.uuid ? updated : item).sort((left, right) => left.name.localeCompare(right.name, "cs")));
  }

  if (loading) return <AdminWorkspaceSkeleton />;

  if (error) {
    return (
      <EmptyState
        icon={ShieldCheck}
        heading="Administraci nelze zobrazit"
        description={error}
        action={<Button onClick={() => void load()}>Zkusit znovu</Button>}
      />
    );
  }

  return (
    <div className="grid min-w-0 grid-cols-1 gap-8">
      <header className="border-b border-border pb-6">
        <p className="flex items-center gap-2 text-sm font-semibold text-brand-text"><ShieldCheck aria-hidden="true" className="size-4" />Správa školy</p>
        <h1 className="mt-1 text-3xl font-semibold tracking-[-0.025em] text-balance text-foreground sm:text-4xl">Administrace</h1>
        <p className="mt-2 max-w-2xl text-base leading-7 text-muted-foreground">Spravujte přiřazení tříd, jejich členy a nabídku předmětů pro vyučující.</p>
      </header>

      <Panel tone="subtle">
        <PanelContent className="grid gap-px overflow-hidden p-0 sm:grid-cols-3">
          {[
            { label: "Vyučující", value: teachers.length, icon: UserRoundCog },
            { label: "Třídy", value: classes.length, icon: UsersRound },
            { label: "Předměty", value: subjects.length, icon: BookOpen },
          ].map(({ label, value, icon: Icon }, index) => (
            <div key={label} className={`bg-panel px-5 py-4 ${index ? "border-t border-border sm:border-t-0 sm:border-l" : ""}`}>
              <p className="flex items-center gap-2 text-sm text-muted-foreground"><Icon aria-hidden="true" className="size-4" />{label}</p>
              <p className="mt-1 text-2xl font-semibold tabular-nums text-foreground">{value}</p>
            </div>
          ))}
        </PanelContent>
      </Panel>

      <nav aria-label="Sekce administrace" className="flex flex-wrap gap-2 rounded-lg border border-border bg-surface p-1.5">
        <a href="#teachers" className="inline-flex min-h-11 items-center gap-2 rounded-md px-3 text-sm font-semibold text-foreground hover:bg-muted"><UserRoundCog aria-hidden="true" className="size-4" />Vyučující</a>
        <a href="#classes" className="inline-flex min-h-11 items-center gap-2 rounded-md px-3 text-sm font-semibold text-foreground hover:bg-muted"><UsersRound aria-hidden="true" className="size-4" />Třídy</a>
        <a href="#subjects" className="inline-flex min-h-11 items-center gap-2 rounded-md px-3 text-sm font-semibold text-foreground hover:bg-muted"><BookOpen aria-hidden="true" className="size-4" />Předměty</a>
      </nav>

      <section id="teachers" aria-labelledby="teachers-title" className="grid min-w-0 grid-cols-1 gap-4 scroll-mt-24">
        <div>
          <h2 id="teachers-title" className="text-2xl font-semibold text-foreground">Vyučující</h2>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">Účty evidované po přihlášení. Přiřazení tříd a předmětů upravíte v navazujících sekcích.</p>
        </div>
        <TeacherDirectory teachers={teachers} classes={classes} subjects={subjects} />
      </section>

      <section id="classes" aria-labelledby="classes-title" className="grid min-w-0 grid-cols-1 gap-4 scroll-mt-24">
        <div>
          <h2 id="classes-title" className="text-2xl font-semibold text-foreground">Třídy</h2>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">Ke každé třídě musí mít přístup alespoň jeden vyučující. Změna přiřazení nezasahuje do už spuštěných relací.</p>
        </div>
        {teachers.length === 0 ? <Notice>Novou třídu lze vytvořit až po prvním přihlášení vyučujícího.</Notice> : <CreateClassForm teachers={teachers} onCreated={(created) => setClasses((current) => [...current, created].sort((left, right) => left.name.localeCompare(right.name, "cs")))} />}
        {classes.length === 0 ? (
          <EmptyState compact icon={Building2} heading="Zatím nejsou vytvořené žádné třídy" description="První třídu založte formulářem výše." />
        ) : (
          <div className="grid min-w-0 grid-cols-1 gap-3">
            {classes.map((schoolClass) => <AdminClassEditor key={schoolClass.uuid} schoolClass={schoolClass} students={students} teachers={teachers} onReplace={replaceClass} onDelete={(classUuid) => setClasses((current) => current.filter((item) => item.uuid !== classUuid))} />)}
          </div>
        )}
      </section>

      <section id="subjects" aria-labelledby="subjects-title" className="grid min-w-0 grid-cols-1 gap-4 scroll-mt-24">
        <div>
          <h2 id="subjects-title" className="text-2xl font-semibold text-foreground">Předměty</h2>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">Centrální katalog udržuje názvy jednotné. Vyučující u kvízu vybírají pouze své přiřazené předměty.</p>
        </div>
        <CreateSubjectForm onCreated={(created) => setSubjects((current) => [...current, created].sort((left, right) => left.name.localeCompare(right.name, "cs")))} />
        {subjects.length === 0 ? (
          <EmptyState compact icon={BookOpen} heading="Katalog předmětů je prázdný" description="Vytvořte první předmět a přiřaďte ho vyučujícímu." />
        ) : (
          <div className="grid min-w-0 grid-cols-1 gap-3">
            {subjects.map((subject) => <AdminSubjectEditor key={subject.uuid} subject={subject} teachers={teachers} onReplace={replaceSubject} onDelete={(subjectUuid) => setSubjects((current) => current.filter((item) => item.uuid !== subjectUuid))} />)}
          </div>
        )}
      </section>
    </div>
  );
}
