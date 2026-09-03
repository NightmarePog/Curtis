import { ArrowLeft, Map } from "lucide-react";
import Link from "next/link";

import { buttonStyles } from "@/components/ui";

export default function NotFound() {
  return (
    <section className="mx-auto flex min-h-[65vh] max-w-lg flex-col items-center justify-center text-center">
      <span className="grid size-14 place-items-center rounded-lg border border-border bg-card text-primary-strong">
        <Map aria-hidden="true" className="size-6" />
      </span>
      <p className="mt-6 font-mono text-xs font-semibold text-primary-strong">404</p>
      <h1 className="mt-2 text-3xl font-semibold tracking-tight text-foreground">Tudy cesta nevede</h1>
      <p className="mt-3 text-pretty text-muted-foreground">Stránka neexistuje nebo byla přesunuta. Vraťte se do hlavního přehledu.</p>
      <Link href="/dashboard" className={buttonStyles({ className: "mt-7" })}>
        <ArrowLeft aria-hidden="true" className="size-4" />
        Zpět na přehled
      </Link>
    </section>
  );
}
