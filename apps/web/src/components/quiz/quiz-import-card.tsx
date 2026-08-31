"use client";

import { useState } from "react";
import {
  BookOpen,
  FileUp,
  Image as ImageIcon,
  Upload,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { ErrorBanner } from "@/components/common/feedback";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api } from "@/lib/api";

export function QuizImportCard() {
  const router = useRouter();
  const [view, setView] = useState<"import" | "docs">("import");
  const [yamlFile, setYamlFile] = useState<File | null>(null);
  const [images, setImages] = useState<File[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setStatus(null);

    if (!yamlFile) {
      setError("Vyberte YAML soubor.");
      return;
    }
    if (!/\.(ya?ml)$/i.test(yamlFile.name)) {
      setError("Import přijímá pouze soubor s příponou .yaml nebo .yml.");
      return;
    }

    const formData = new FormData();
    formData.append("file", yamlFile);
    images.forEach((image) => formData.append("images", image));

    setUploading(true);
    setStatus("Nahrávám YAML a přílohy…");
    try {
      const { quizUuid } = await api.importQuiz(formData);
      setStatus("Import proběhl úspěšně. Otevírám kvíz…");
      router.push(`/quiz/${quizUuid}`);
    } catch (err) {
      setUploading(false);
      setStatus(null);
      setError(err instanceof Error ? err.message : "YAML import selhal");
    }
  }

  return (
    <section className="surface surface-raised" aria-labelledby="import-kvizu">
      <div className="border-b border-border px-5 pt-4 sm:px-6">
        <div
          className="flex flex-wrap items-center gap-1"
          role="tablist"
          aria-label="YAML nástroje"
        >
          <TabButton
            active={view === "import"}
            icon={FileUp}
            onClick={() => setView("import")}
          >
            Importovat YAML
          </TabButton>
          <TabButton
            active={view === "docs"}
            icon={BookOpen}
            onClick={() => setView("docs")}
          >
            Jak psát YAML
          </TabButton>
        </div>
      </div>

      {view === "import" ? (
        <form onSubmit={submit} className="space-y-4 p-5 sm:p-6">
          {error && <ErrorBanner message={error} onDismiss={() => setError(null)} />}
          {status && (
            <p
              role="status"
              className="rounded-lg border border-brand/20 bg-brand-soft/40 px-3 py-2 text-sm text-brand"
            >
              {status}
            </p>
          )}
          <p className="text-sm text-muted-foreground">
            Nahrajte definici kvízu a volitelné obrázky uvedené v `imageRef`.
          </p>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="space-y-2 text-sm font-medium text-foreground">
              YAML soubor
              <Input
                type="file"
                accept=".yaml,.yml,application/yaml,text/yaml"
                onChange={(event) => setYamlFile(event.target.files?.[0] ?? null)}
                disabled={uploading}
              />
              <span className="block text-xs font-normal text-muted-foreground">
                Povinné: .yaml nebo .yml
              </span>
            </label>
            <label className="space-y-2 text-sm font-medium text-foreground">
              <span className="flex items-center gap-1.5">
                <ImageIcon aria-hidden="true" className="size-3.5" />
                Obrázky
              </span>
              <Input
                type="file"
                accept="image/*"
                multiple
                onChange={(event) => setImages(Array.from(event.target.files ?? []))}
                disabled={uploading}
              />
              <span className="block text-xs font-normal text-muted-foreground">
                Název souboru musí odpovídat `imageRef` v YAML.
              </span>
            </label>
          </div>
          <div className="flex justify-end">
            <Button type="submit" loading={uploading}>
              <Upload aria-hidden="true" data-icon="inline-start" />
              Importovat kvíz
            </Button>
          </div>
        </form>
      ) : (
        <YamlDocumentation />
      )}
    </section>
  );
}

function TabButton({
  active,
  children,
  icon: Icon,
  onClick,
}: {
  active: boolean;
  children: React.ReactNode;
  icon: typeof FileUp;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      onClick={onClick}
      className={`inline-flex min-h-10 items-center gap-2 border-b-2 px-2 text-sm font-medium transition-colors ${
        active
          ? "border-brand text-foreground"
          : "border-transparent text-muted-foreground hover:text-foreground"
      }`}
    >
      <Icon aria-hidden="true" className="size-4" />
      {children}
    </button>
  );
}

function YamlDocumentation() {
  const basicYaml = [
    'title: "Základy elektrotechniky"',
    'description: "Krátký opakovací kvíz"',
    "maxQuestionsPerSession: 10",
    "shuffle: true",
    "",
    "questions:",
    "  - type: MULTIPLE_CHOICE",
    '    question: "Co měří voltmetr?"',
    "    points: 2",
    "    timeInSeconds: 30",
    "    options:",
    '      - "Napětí"',
    '      - "Proud"',
    '      - "Odpor"',
    "    correctIndexes: [0]",
  ].join("\n");
  const advancedYaml = [
    "  - type: MATCHING",
    '    question: "Spojte pojem s vysvětlením"',
    "    points: 3",
    "    timeInSeconds: 45",
    "    pairs:",
    '      - left: "HTTP"',
    '        right: "Webový protokol"',
    '      - left: "TCP"',
    '        right: "Spolehlivý přenos"',
    "",
    "  - type: FREE_TEXT",
    '    question: "Vysvětlete Ohmův zákon."',
    "    points: 5",
    "    timeInSeconds: 120",
    "    codeSnippet: |",
    "      U = R * I",
    '    imageRef: "schema.png"',
  ].join("\n");

  return (
    <div className="space-y-5 p-5 sm:p-6">
      <div>
        <h3 className="text-sm font-semibold text-foreground">Základní struktura</h3>
        <p className="mt-1 text-sm text-muted-foreground">
          U starých YAML souborů lze `type` a `points` vynechat. Použije se výběr z možností a 1 bod.
        </p>
      </div>

      <YamlCode>{basicYaml}</YamlCode>

      <div className="grid gap-3 sm:grid-cols-3">
        <DocItem title="MULTIPLE_CHOICE" text="options + correctIndexes" />
        <DocItem title="MATCHING" text="pairs: left + right" />
        <DocItem title="FREE_TEXT" text="odpověď kontroluje učitel" />
      </div>

      <div>
        <h3 className="text-sm font-semibold text-foreground">
          Přiřazování a vlastní odpověď
        </h3>
        <YamlCode>{advancedYaml}</YamlCode>
        <p className="mt-2 text-xs text-muted-foreground">
          Obrázky vyberte při importu jako přílohy. Jejich název musí přesně odpovídat
          `imageRef`. Vlastní odpovědi se po odevzdání objeví učiteli ke kontrole.
        </p>
      </div>
    </div>
  );
}

function YamlCode({ children }: { children: string }) {
  return (
    <pre className="overflow-x-auto rounded-lg border border-border bg-muted/40 p-4 font-mono text-xs leading-relaxed text-foreground">
      <code>{children}</code>
    </pre>
  );
}

function DocItem({ title, text }: { title: string; text: string }) {
  return (
    <div className="rounded-lg border border-border bg-muted/20 p-3">
      <p className="font-mono text-xs font-semibold text-foreground">{title}</p>
      <p className="mt-1 text-xs text-muted-foreground">{text}</p>
    </div>
  );
}
