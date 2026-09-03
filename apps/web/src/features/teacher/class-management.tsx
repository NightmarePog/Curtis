"use client";

import { Plus, Settings2, UsersRound, X } from "lucide-react";
import { useMemo, useState, type FormEvent } from "react";

import {
  Badge,
  Button,
  Checkbox,
  EmptyState,
  Field,
  Input,
  Panel,
  PanelContent,
  PanelDescription,
  PanelHeader,
  PanelTitle,
  SelectField,
} from "@/components/ui";
import { Notice } from "@/features/teacher/shared";
import { messageFromError } from "@/lib/http";
import { classService } from "@/lib/services";
import type { ClassResponse } from "@/types/domain";

export interface KnownStudent {
  studentId: string;
  studentName: string;
}

function memberLabel(name: string, id: string) {
  const trimmed = name.trim();
  return trimmed && trimmed !== id ? trimmed : id;
}

export function ClassManagement({
  classes,
  onChange,
  onClose,
}: {
  classes: ClassResponse[];
  knownStudents: KnownStudent[];
  onChange: (classes: ClassResponse[]) => void;
  onClose: () => void;
}) {
  const [selectedClassId, setSelectedClassId] = useState(classes[0]?.uuid ?? "");
  const [newGroupName, setNewGroupName] = useState("");
  const [creating, setCreating] = useState(false);
  const [changingMember, setChangingMember] = useState<string | null>(null);
  const [error, setError] = useState("");
  const selectedClass =
    classes.find((schoolClass) => schoolClass.uuid === selectedClassId) ??
    classes[0] ??
    null;
  const groups = selectedClass?.groups?.filter((group) => group.active) ?? [];
  const trimmedGroupName = newGroupName.trim();
  const duplicateGroup = groups.some(
    (group) => group.name.localeCompare(trimmedGroupName, "cs", { sensitivity: "accent" }) === 0,
  );
  const groupNameError = !trimmedGroupName
    ? "Zadejte název skupiny."
    : duplicateGroup
      ? "Skupina s tímto názvem už existuje."
      : "";

  const classOptions = useMemo(
    () => classes.map((schoolClass) => ({
      value: schoolClass.uuid,
      label: schoolClass.name,
    })),
    [classes],
  );

  function replaceClass(updated: ClassResponse) {
    onChange(classes.map((item) => item.uuid === updated.uuid ? updated : item));
  }

  async function createGroup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    if (!selectedClass || groupNameError || creating) return;
    setCreating(true);
    try {
      replaceClass(await classService.createGroup(selectedClass.uuid, trimmedGroupName));
      setNewGroupName("");
    } catch (caught) {
      setError(messageFromError(caught, "Skupinu se nepodařilo vytvořit."));
    } finally {
      setCreating(false);
    }
  }

  async function toggleMember(groupId: string, studentId: string, assigned: boolean) {
    if (!selectedClass || changingMember) return;
    const operation = `${groupId}:${studentId}`;
    setChangingMember(operation);
    setError("");
    try {
      replaceClass(await classService.setGroupStudent(
        selectedClass.uuid,
        groupId,
        studentId,
        assigned,
      ));
    } catch (caught) {
      setError(messageFromError(caught, "Členství ve skupině se nepodařilo uložit."));
    } finally {
      setChangingMember(null);
    }
  }

  return (
    <Panel id="class-management" className="overflow-hidden">
      <PanelHeader className="flex-row items-start justify-between gap-4 border-b border-border pb-5">
        <div>
          <div className="mb-2 flex items-center gap-2 text-sm font-semibold text-brand-text">
            <Settings2 aria-hidden="true" className="size-[1.125rem]" />
            Organizace žáků
          </div>
          <PanelTitle>Skupiny ve třídách</PanelTitle>
          <PanelDescription className="mt-1 max-w-2xl">
            Třídy a jejich seznamy žáků spravuje administrátor. Zde můžete žáky
            rozdělit do skupin pro cílené zadávání kvízů.
          </PanelDescription>
        </div>
        <Button
          variant="quiet"
          size="icon"
          aria-label="Zavřít správu skupin"
          onClick={onClose}
        >
          <X aria-hidden="true" className="size-5" />
        </Button>
      </PanelHeader>

      <PanelContent className="grid gap-6">
        {error ? <Notice tone="error">{error}</Notice> : null}

        {classes.length > 1 ? (
          <Field label="Třída" controlId="group-class-selector">
            <SelectField
              value={selectedClass?.uuid}
              onValueChange={setSelectedClassId}
              options={classOptions}
            />
          </Field>
        ) : null}

        {!selectedClass ? (
          <EmptyState
            compact
            icon={UsersRound}
            heading="Nemáte přiřazenou žádnou třídu"
            description="O přiřazení třídy požádejte administrátora."
          />
        ) : (
          <>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h3 className="text-lg font-semibold text-foreground">{selectedClass.name}</h3>
                <p className="mt-1 text-sm text-muted-foreground">
                  {selectedClass.studentCount} žáků · {groups.length} skupin
                </p>
              </div>
              <Badge variant="neutral">Přiřazená třída</Badge>
            </div>

            <form
              onSubmit={createGroup}
              className="grid items-end gap-3 border-t border-border pt-5 sm:grid-cols-[minmax(0,1fr)_auto]"
            >
              <Field
                label="Nová skupina"
                controlId="new-class-group"
                error={newGroupName && groupNameError ? groupNameError : undefined}
              >
                <Input
                  value={newGroupName}
                  maxLength={100}
                  disabled={creating}
                  onChange={(event) => setNewGroupName(event.target.value)}
                  placeholder="Např. Laboratorní skupina A"
                />
              </Field>
              <Button
                type="submit"
                isLoading={creating}
                disabled={Boolean(groupNameError)}
                leadingIcon={<Plus className="size-4" />}
              >
                Vytvořit skupinu
              </Button>
            </form>

            {groups.length === 0 ? (
              <EmptyState
                compact
                icon={UsersRound}
                heading="Třída zatím nemá skupiny"
                description="Vytvořte první skupinu a vyberte do ní žáky z třídního seznamu."
              />
            ) : (
              <div className="grid gap-3">
                {groups.map((group) => {
                  const assigned = new Set(group.members.map((member) => member.studentId));
                  return (
                    <details key={group.uuid} className="group rounded-lg border border-border bg-surface">
                      <summary className="flex min-h-14 list-none items-center justify-between gap-3 px-4 py-3 font-semibold text-foreground marker:content-none">
                        <span>{group.name}</span>
                        <Badge variant="outline">{group.members.length} žáků</Badge>
                      </summary>
                      <fieldset
                        disabled={changingMember !== null}
                        className="border-t border-border px-4 py-4"
                      >
                        <legend className="sr-only">Členové skupiny {group.name}</legend>
                        {selectedClass.members.length === 0 ? (
                          <p className="text-sm text-muted-foreground">
                            Ve třídě zatím nejsou přiřazení žáci.
                          </p>
                        ) : (
                          <ul className="grid gap-2 sm:grid-cols-2">
                            {selectedClass.members.map((member) => {
                              const checked = assigned.has(member.studentId);
                              const controlId = `group-${group.uuid}-student-${member.studentId}`;
                              return (
                                <li key={member.studentId}>
                                  <label
                                    htmlFor={controlId}
                                    className="flex min-h-11 items-center gap-3 rounded-md border border-border bg-panel px-3 py-2.5 text-sm font-medium text-foreground hover:border-brand/60"
                                  >
                                    <Checkbox
                                      id={controlId}
                                      checked={checked}
                                      onCheckedChange={() => void toggleMember(
                                        group.uuid,
                                        member.studentId,
                                        !checked,
                                      )}
                                    />
                                    <span className="min-w-0 truncate">
                                      {memberLabel(member.studentName, member.studentId)}
                                    </span>
                                  </label>
                                </li>
                              );
                            })}
                          </ul>
                        )}
                      </fieldset>
                    </details>
                  );
                })}
              </div>
            )}
          </>
        )}
      </PanelContent>
    </Panel>
  );
}
