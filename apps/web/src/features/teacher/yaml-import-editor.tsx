"use client";

import {
  CheckCircle2,
  Clipboard,
  FileCode2,
  FileUp,
  ImagePlus,
  Info,
  Upload,
} from "lucide-react";
import { load, YAMLException } from "js-yaml";
import { useRouter } from "next/navigation";
import {
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react";

import {
  Badge,
  Button,
  Field,
  Input,
  Panel,
  PanelContent,
  PanelDescription,
  PanelHeader,
  PanelTitle,
} from "@/components/ui";
import { messageFromError } from "@/lib/http";
import { quizService } from "@/lib/services";
import { Notice } from "./shared";
import { YamlCodeEditor, type YamlCodeEditorHandle } from "./yaml-code-editor";

const STARTER_YAML = `title: "Síťové protokoly"
description: "Krátké opakování před cvičením."
subject: "Počítačové sítě"
chapter: "TCP/IP"
maxQuestionsPerSession: 3
shuffle: false
questions:
  - question: "Který port běžně používá HTTPS?"
    type: MULTIPLE_CHOICE
    points: 2
    timeInSeconds: 30
    options:
      - "22"
      - "53"
      - "443"
    correctIndexes: [2]

  - question: "Přiřaď protokol k portu."
    type: MATCHING
    points: 3
    timeInSeconds: 45
    pairs:
      - left: "SSH"
        right: "22"
      - left: "DNS"
        right: "53"

  - question: "Vysvětli účel výchozí brány."
    type: FREE_TEXT
    points: 3
    timeInSeconds: 60
`;

type YamlRecord = Record<string, unknown>;

interface YamlAnalysis {
  issues: string[];
  questionCount: number;
}

function isRecord(value: unknown): value is YamlRecord {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function nonEmptyString(value: unknown) {
  return typeof value === "string" && value.trim().length > 0;
}

function positiveInteger(value: unknown) {
  return Number.isInteger(value) && Number(value) >= 1;
}

function analyzeQuestion(value: unknown, index: number) {
  const label = `Otázka ${index + 1}`;
  if (!isRecord(value)) return [`${label} musí být objekt.`];

  const issues: string[] = [];
  if (!nonEmptyString(value.question)) issues.push(`${label}: chybí text otázky.`);
  if (!positiveInteger(value.timeInSeconds)) {
    issues.push(`${label}: timeInSeconds musí být kladné celé číslo.`);
  }
  if (value.points !== undefined && !positiveInteger(value.points)) {
    issues.push(`${label}: points musí být kladné celé číslo.`);
  }
  if (
    value.imageRef !== undefined &&
    (!nonEmptyString(value.imageRef) || /[/\\]/.test(String(value.imageRef)))
  ) {
    issues.push(`${label}: imageRef smí obsahovat jen název souboru.`);
  }

  const type = value.type ?? "MULTIPLE_CHOICE";
  if (!(["MULTIPLE_CHOICE", "MATCHING", "FREE_TEXT"] as unknown[]).includes(type)) {
    issues.push(`${label}: neznámý typ otázky.`);
    return issues;
  }

  if (type === "MULTIPLE_CHOICE") {
    const options = Array.isArray(value.options) ? value.options : [];
    const correctIndexes = Array.isArray(value.correctIndexes)
      ? value.correctIndexes
      : [];
    if (options.length < 2 || options.some((option) => !nonEmptyString(option))) {
      issues.push(`${label}: výběrová otázka potřebuje alespoň dvě možnosti.`);
    }
    if (
      correctIndexes.length === 0 ||
      correctIndexes.some(
        (correctIndex) =>
          !Number.isInteger(correctIndex) ||
          Number(correctIndex) < 0 ||
          Number(correctIndex) >= options.length,
      )
    ) {
      issues.push(`${label}: correctIndexes musí odkazovat na platné možnosti.`);
    }
  }

  if (type === "MATCHING") {
    const pairs = Array.isArray(value.pairs) ? value.pairs : [];
    if (
      pairs.length === 0 ||
      pairs.some(
        (pair) =>
          !isRecord(pair) ||
          !nonEmptyString(pair.left) ||
          !nonEmptyString(pair.right),
      )
    ) {
      issues.push(`${label}: přiřazování potřebuje úplné dvojice left a right.`);
    }
  }

  if (
    type === "FREE_TEXT" &&
    (value.options !== undefined ||
      value.correctIndexes !== undefined ||
      value.pairs !== undefined)
  ) {
    issues.push(`${label}: volná odpověď nepoužívá options, correctIndexes ani pairs.`);
  }

  return issues;
}

function analyzeYaml(source: string): YamlAnalysis {
  if (!source.trim()) {
    return { issues: ["Editor je prázdný."], questionCount: 0 };
  }

  try {
    const parsed = load(source);
    if (!isRecord(parsed)) {
      return { issues: ["Kořen YAML musí být objekt kvízu."], questionCount: 0 };
    }

    const issues: string[] = [];
    if (!nonEmptyString(parsed.title)) issues.push("Vyplňte title.");
    if (!positiveInteger(parsed.maxQuestionsPerSession)) {
      issues.push("maxQuestionsPerSession musí být kladné celé číslo.");
    }
    if (parsed.subject !== undefined && !nonEmptyString(parsed.subject)) {
      issues.push("subject nesmí být prázdný.");
    }
    const questions = Array.isArray(parsed.questions) ? parsed.questions : [];
    if (questions.length === 0) issues.push("Přidejte alespoň jednu otázku.");
    questions.forEach((question, index) => {
      issues.push(...analyzeQuestion(question, index));
    });

    return { issues, questionCount: questions.length };
  } catch (error) {
    const message =
      error instanceof YAMLException
        ? error.reason || error.message
        : "YAML se nepodařilo přečíst.";
    return { issues: [message], questionCount: 0 };
  }
}

function questionCountLabel(count: number) {
  if (count === 1) return "1 otázka";
  if (count >= 2 && count <= 4) return `${count} otázky`;
  return `${count} otázek`;
}

export function YamlImportEditor() {
  const router = useRouter();
  const editorRef = useRef<YamlCodeEditorHandle>(null);
  const [source, setSource] = useState(STARTER_YAML);
  const [sourceName, setSourceName] = useState("novy-kviz.yaml");
  const [images, setImages] = useState<File[]>([]);
  const [busy, setBusy] = useState(false);
  const [touched, setTouched] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const analysis = useMemo(() => analyzeYaml(source), [source]);
  const valid = analysis.issues.length === 0;

  async function loadFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    setError(null);
    setNotice(null);
    try {
      setSource(await file.text());
      setSourceName(file.name);
      setTouched(true);
      window.requestAnimationFrame(() => editorRef.current?.focus());
    } catch {
      setError("Soubor se nepodařilo přečíst.");
    } finally {
      event.target.value = "";
    }
  }

  async function copyTemplate() {
    setError(null);
    try {
      await navigator.clipboard.writeText(STARTER_YAML);
      setNotice("Vzor YAML je zkopírovaný do schránky.");
    } catch {
      setError("Vzor se nepodařilo zkopírovat. Můžete ho označit přímo v editoru.");
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setTouched(true);
    setError(null);
    setNotice(null);
    if (!valid) return;

    setBusy(true);
    try {
      const filename = /\.ya?ml$/i.test(sourceName)
        ? sourceName
        : `${sourceName || "kviz"}.yaml`;
      const yaml = new File([source], filename, {
        type: "application/yaml;charset=utf-8",
      });
      const imported = await quizService.importYaml(yaml, images);
      router.push(`/quiz/${imported.quizUuid}`);
    } catch (caught) {
      setError(
        messageFromError(
          caught,
          "Import se nepodařil. Zkontrolujte YAML a názvy přiložených obrázků.",
        ),
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <Panel className="overflow-hidden">
      <PanelHeader className="gap-4 border-b border-border pb-5 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="mb-2 flex items-center gap-2 text-sm font-semibold text-brand-text">
            <FileCode2 aria-hidden="true" className="size-[1.125rem]" />
            YAML nástroj
          </div>
          <PanelTitle>Editor a import kvízu</PanelTitle>
          <PanelDescription className="mt-1 max-w-2xl">
            Upravte definici přímo zde, nebo načtěte existující soubor. Editor
            zkontroluje strukturu ještě před odesláním.
          </PanelDescription>
        </div>
        <Button
          type="button"
          variant="secondary"
          size="sm"
          onClick={() => void copyTemplate()}
          leadingIcon={<Clipboard className="size-4" />}
        >
          Kopírovat vzor
        </Button>
      </PanelHeader>

      <PanelContent>
        <form onSubmit={submit} className="grid gap-5">
          <div className="grid gap-5 xl:grid-cols-[minmax(0,1.7fr)_minmax(18rem,0.8fr)]">
            <div className="grid min-w-0 gap-3">
              <div className="flex justify-end">
                <Badge variant={valid ? "brand" : touched ? "danger" : "neutral"}>
                  {valid ? (
                    <CheckCircle2 aria-hidden="true" />
                  ) : (
                    <Info aria-hidden="true" />
                  )}
                  {valid
                    ? `Platné · ${questionCountLabel(analysis.questionCount)}`
                    : `${analysis.issues.length} ${analysis.issues.length === 1 ? "problém" : "problémy"}`}
                </Badge>
              </div>
              <YamlCodeEditor
                ref={editorRef}
                value={source}
                describedBy="yaml-validation"
                invalid={touched && !valid}
                onBlur={() => setTouched(true)}
                onChange={(value) => {
                  setSource(value);
                  setNotice(null);
                }}
              />
              <div id="yaml-validation" aria-live="polite">
                {touched && !valid ? (
                  <ul className="grid gap-1 text-sm leading-5 text-danger-text">
                    {analysis.issues.slice(0, 5).map((issue) => (
                      <li key={issue}>• {issue}</li>
                    ))}
                    {analysis.issues.length > 5 ? (
                      <li>• A dalších {analysis.issues.length - 5}.</li>
                    ) : null}
                  </ul>
                ) : (
                  <p className="text-sm text-muted-foreground">
                    Kontrolují se povinná pole, typy otázek, dvojice a indexy
                    správných možností. Server provede závěrečnou kontrolu.
                  </p>
                )}
              </div>
            </div>

            <aside className="grid content-start gap-4" aria-label="YAML documentation">
              <div className="rounded-lg border border-border bg-surface p-4">
                <div className="flex items-start gap-3">
                  <Info aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-brand-text" />
                  <div>
                    <h2 className="font-semibold text-foreground">Quick reference</h2>
                    <p className="mt-1 text-sm leading-5 text-muted-foreground">
                      Start typing a field name to open suggestions. Press Enter to insert one.
                    </p>
                  </div>
                </div>
              </div>

              <section className="overflow-hidden rounded-lg border border-border bg-surface" aria-labelledby="quiz-fields-title">
                <div className="border-b border-border px-4 py-3">
                  <h3 id="quiz-fields-title" className="font-semibold text-foreground">Quiz fields</h3>
                </div>
                <dl className="divide-y divide-border text-sm">
                  {[
                    ["title", "string", "Required · max 100 characters"],
                    ["subject", "string", "Must match one of your assigned subjects"],
                    ["chapter", "string", "Optional chapter or topic"],
                    ["maxQuestionsPerSession", "integer", "Maximum questions in one session"],
                    ["shuffle", "boolean", "true or false"],
                  ].map(([name, type, description]) => (
                    <div key={name} className="px-4 py-3">
                      <dt className="flex min-w-0 flex-wrap items-center gap-2">
                        <code className="overflow-wrap-anywhere font-medium text-foreground">{name}</code>
                        <span className="rounded-sm bg-surface-subtle px-1.5 py-0.5 font-mono text-[0.7rem] text-muted-foreground">{type}</span>
                      </dt>
                      <dd className="mt-1 leading-5 text-muted-foreground">{description}</dd>
                    </div>
                  ))}
                </dl>
              </section>

              <section className="overflow-hidden rounded-lg border border-border bg-surface" aria-labelledby="question-types-title">
                <div className="border-b border-border px-4 py-3">
                  <h3 id="question-types-title" className="font-semibold text-foreground">Question types</h3>
                </div>
                <div className="grid gap-3 p-4 text-sm">
                  <div><code className="font-medium text-brand-text">MULTIPLE_CHOICE</code><p className="mt-1 leading-5 text-muted-foreground">Add <code>options</code> and zero-based <code>correctIndexes</code>.</p></div>
                  <div><code className="font-medium text-brand-text">MATCHING</code><p className="mt-1 leading-5 text-muted-foreground">Add <code>pairs</code>, each with a <code>left</code> and <code>right</code> value.</p></div>
                  <div><code className="font-medium text-brand-text">FREE_TEXT</code><p className="mt-1 leading-5 text-muted-foreground">No answer options. The teacher grades the response.</p></div>
                </div>
              </section>

              <details className="group rounded-lg border border-border bg-surface">
                <summary className="flex min-h-11 list-none items-center gap-2 px-4 py-3 font-semibold text-foreground hover:bg-surface-subtle [&::-webkit-details-marker]:hidden">
                  <FileCode2 aria-hidden="true" className="size-4 text-brand-text" />
                  Media and optional fields
                </summary>
                <div className="grid gap-3 border-t border-border px-4 py-3 text-sm leading-5 text-muted-foreground">
                  <p><code className="text-foreground">points</code> defaults to <code>1</code>.</p>
                  <p><code className="text-foreground">codeSnippet: |</code> starts a multiline code block.</p>
                  <p><code className="text-foreground">imageRef</code> must exactly match an attached file name—do not include a path.</p>
                </div>
              </details>
            </aside>
          </div>

          <div className="grid gap-4 border-t border-border pt-5 md:grid-cols-2">
            <div className="rounded-lg border border-border bg-surface p-4">
              <div className="mb-3 flex items-start gap-3">
                <span className="grid size-9 shrink-0 place-items-center rounded-md bg-brand-subtle text-brand-text">
                  <FileUp aria-hidden="true" className="size-4" />
                </span>
                <div>
                  <h3 className="font-semibold text-foreground">Nahradit obsah souborem</h3>
                  <p className="mt-0.5 text-sm leading-5 text-muted-foreground">Načtený .yaml nebo .yml soubor přepíše obsah editoru.</p>
                </div>
              </div>
              <Field label="Soubor YAML" controlId="yaml-file" optional>
                <Input
                  type="file"
                  accept=".yaml,.yml,application/yaml,text/yaml,text/x-yaml"
                  className="p-0 text-sm file:mr-3 file:min-h-11 file:border-0 file:border-r file:border-border file:bg-surface-subtle file:px-3.5 file:font-semibold file:text-foreground hover:file:bg-brand-subtle"
                  onChange={(event) => void loadFile(event)}
                />
              </Field>
            </div>
            <div className="rounded-lg border border-border bg-surface p-4">
              <div className="mb-3 flex items-start gap-3">
                <span className="grid size-9 shrink-0 place-items-center rounded-md bg-brand-subtle text-brand-text">
                  <ImagePlus aria-hidden="true" className="size-4" />
                </span>
                <div>
                  <h3 className="font-semibold text-foreground">Připojit obrázky</h3>
                  <p className="mt-0.5 text-sm leading-5 text-muted-foreground">Vyberte soubory, jejichž názvy používáte v <code>imageRef</code>.</p>
                </div>
              </div>
              <Field label="Soubory obrázků" controlId="yaml-images" optional>
                <Input
                  type="file"
                  accept="image/*"
                  multiple
                  className="p-0 text-sm file:mr-3 file:min-h-11 file:border-0 file:border-r file:border-border file:bg-surface-subtle file:px-3.5 file:font-semibold file:text-foreground hover:file:bg-brand-subtle"
                  onChange={(event) => setImages(Array.from(event.target.files ?? []))}
                />
              </Field>
            </div>
          </div>

          {images.length > 0 ? (
            <p className="flex items-center gap-2 text-sm text-muted-foreground">
              <ImagePlus aria-hidden="true" className="size-4" />
              Přiloženo {images.length} {images.length === 1 ? "obrázek" : "obrázků"}.
            </p>
          ) : null}
          {notice ? <Notice>{notice}</Notice> : null}
          {error ? <Notice tone="error">{error}</Notice> : null}

          <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="flex items-center gap-2 text-sm text-muted-foreground">
              <FileUp aria-hidden="true" className="size-4" />
              YAML se zkontroluje, vytvoří se nový kvíz a otevře se jeho detail.
            </p>
            <Button
              type="submit"
              disabled={!valid}
              isLoading={busy}
              leadingIcon={<Upload className="size-4" />}
            >
              Importovat kvíz z YAML
            </Button>
          </div>
        </form>
      </PanelContent>
    </Panel>
  );
}
