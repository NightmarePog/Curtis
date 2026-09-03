"use client";

import { RotateCcw, TriangleAlert } from "lucide-react";
import { useEffect } from "react";

import { Button } from "@/components/ui";

export default function ErrorPage({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <section className="mx-auto flex min-h-[55vh] max-w-lg flex-col items-center justify-center text-center">
      <span className="grid size-14 place-items-center rounded-lg border border-danger/35 bg-danger-subtle text-danger-text">
        <TriangleAlert aria-hidden="true" className="size-6" />
      </span>
      <h1 className="mt-6 text-3xl font-semibold tracking-tight text-foreground">Stránku se nepodařilo načíst</h1>
      <p className="mt-3 text-pretty text-muted-foreground">Zkontrolujte připojení a zkuste požadavek zopakovat.</p>
      <Button className="mt-7" onClick={reset}>
        <RotateCcw aria-hidden="true" className="size-4" />
        Zkusit znovu
      </Button>
    </section>
  );
}
