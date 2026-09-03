import { ArrowLeft } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";

import { buttonStyles } from "@/components/ui";
import { RequireAuth } from "@/features/auth/auth-provider";
import { YamlImportEditor } from "@/features/teacher/yaml-import-editor";
import { cn } from "@/lib/cn";

export const metadata: Metadata = { title: "YAML editor" };

export default function QuizImportPage() {
  return (
    <RequireAuth teacher>
      <div className="grid gap-7">
        <header className="border-b border-border pb-6">
          <Link
            href="/dashboard"
            className={cn(
              buttonStyles({ variant: "quiet", size: "sm" }),
              "mb-3 w-fit px-2",
            )}
          >
            <ArrowLeft aria-hidden="true" className="size-4" />
            Knihovna kvízů
          </Link>
          <h1 className="text-3xl font-semibold tracking-[-0.025em] text-balance text-foreground sm:text-4xl">
            YAML editor
          </h1>
          <p className="mt-2 max-w-2xl text-base leading-7 text-muted-foreground">
            Napište nebo načtěte YAML, ověřte jeho strukturu a importujte hotový
            kvíz včetně obrázků.
          </p>
        </header>
        <YamlImportEditor />
      </div>
    </RequireAuth>
  );
}
