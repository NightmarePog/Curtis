import type { ReactNode } from "react";

import {
  Panel,
  PanelContent,
  PanelFooter,
  PanelHeader,
  Skeleton,
} from "@/components/ui";

function Status({ children, label }: { children: ReactNode; label: string }) {
  return (
    <div role="status" aria-busy="true" aria-live="polite" className="w-full">
      <span className="sr-only">{label}</span>
      <div aria-hidden="true">{children}</div>
    </div>
  );
}

function rows(count: number) {
  return Array.from({ length: Math.max(1, Math.min(8, count)) }, (_, index) => index);
}

export function RouteSkeleton() {
  return <span role="status" className="sr-only">Načítám stránku…</span>;
}

export function SessionListSkeleton({ count = 1 }: { count?: number }) {
  return (
    <Status label="Hledám aktivní relace…">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {rows(count).map((item) => (
          <Panel key={item} className="flex h-full flex-col overflow-hidden">
            <PanelHeader className="gap-3">
              <div className="flex items-start justify-between gap-3">
                <div className="grid min-w-0 flex-1 gap-2"><Skeleton className="h-7 w-4/5" /><Skeleton className="h-5 w-full" /></div>
                <Skeleton className="h-6 w-16" />
              </div>
              <Skeleton className="h-5 w-48 max-w-[80%]" />
            </PanelHeader>
            <PanelContent className="grid flex-1 gap-3 pt-4 sm:grid-cols-2">
              <Skeleton className="h-5 w-32" /><Skeleton className="h-5 w-20" /><Skeleton className="h-5 w-64 max-w-full sm:col-span-2" />
            </PanelContent>
            <PanelFooter><Skeleton className="h-11 w-full sm:w-32" /></PanelFooter>
          </Panel>
        ))}
      </div>
    </Status>
  );
}

export function ResultsListSkeleton({ count = 1 }: { count?: number }) {
  return (
    <Status label="Načítám tvoje výsledky…">
      <div className="grid gap-5">
        {rows(count).map((item) => (
          <Panel key={item} className="overflow-hidden">
            <PanelHeader className="sm:flex-row sm:items-start sm:justify-between sm:gap-6">
              <div className="grid min-w-0 flex-1 gap-2"><div className="flex gap-2"><Skeleton className="h-6 w-24" /><Skeleton className="h-5 w-40" /></div><Skeleton className="h-8 w-3/4" /><Skeleton className="h-5 w-48" /></div>
              <div className="flex items-center gap-3 rounded-md border border-border bg-surface px-3 py-2"><Skeleton className="size-20 rounded-full" /><div className="grid gap-2"><Skeleton className="h-3 w-12" /><Skeleton className="h-5 w-24" /></div></div>
            </PanelHeader>
            <PanelContent className="pt-4"><Skeleton className="h-11 w-full" /></PanelContent>
          </Panel>
        ))}
      </div>
    </Status>
  );
}

export function QuizLibrarySkeleton({ count = 3 }: { count?: number }) {
  const placeholders = rows(count);
  return (
    <Status label="Načítám knihovnu kvízů…">
      <div className="hidden overflow-hidden rounded-lg border border-border bg-panel md:block">
        <div className="grid grid-cols-[38%_18%_14%_30%] bg-surface px-5 py-3"><Skeleton className="h-4 w-16" /><Skeleton className="h-4 w-12" /><Skeleton className="h-4 w-14" /><Skeleton className="h-4 w-12" /></div>
        <div className="divide-y divide-border">{placeholders.map((item) => <div key={item} className="grid min-h-24 grid-cols-[38%_18%_14%_30%] items-center px-5 py-4"><div className="grid gap-2"><Skeleton className="h-5 w-3/4" /><Skeleton className="h-4 w-1/2" /><Skeleton className="h-4 w-2/3" /></div><Skeleton className="h-6 w-20" /><Skeleton className="h-5 w-10" /><div className="flex gap-2"><Skeleton className="h-11 w-20" /><Skeleton className="h-11 w-20" /></div></div>)}</div>
      </div>
      <ul className="grid gap-3 md:hidden">{placeholders.map((item) => <li key={item}><Panel><PanelContent className="grid gap-4"><div className="flex justify-between gap-3"><div className="grid flex-1 gap-2"><Skeleton className="h-6 w-4/5" /><Skeleton className="h-5 w-3/5" /></div><Skeleton className="h-6 w-20" /></div><div className="grid grid-cols-2 gap-3 border-y border-border py-3"><Skeleton className="h-10 w-20" /><Skeleton className="h-10 w-24" /></div><div className="flex gap-2"><Skeleton className="h-11 w-20" /><Skeleton className="h-11 w-20" /></div></PanelContent></Panel></li>)}</ul>
    </Status>
  );
}

function FormSkeleton() {
  return <Panel><PanelHeader className="gap-2"><Skeleton className="h-7 w-40" /><Skeleton className="h-5 w-72 max-w-full" /></PanelHeader><PanelContent className="grid gap-5"><Skeleton className="h-11 w-full" /><Skeleton className="h-28 w-full" /><div className="grid gap-5 sm:grid-cols-2"><Skeleton className="h-11 w-full" /><Skeleton className="h-11 w-full" /></div><div className="grid gap-5 sm:grid-cols-2"><Skeleton className="h-16 w-full" /><Skeleton className="h-11 w-full" /></div><Skeleton className="h-24 w-full" /><Skeleton className="h-32 w-full" /></PanelContent><PanelFooter><Skeleton className="h-11 w-36" /></PanelFooter></Panel>;
}

export function QuizWorkspaceSkeleton({ questionCount = 3 }: { questionCount?: number }) {
  return <Status label="Načítám pracovní prostor kvízu…"><div className="grid gap-7"><header className="grid gap-4 border-b border-border pb-6"><Skeleton className="h-11 w-40" /><div className="flex flex-col gap-4 sm:flex-row sm:justify-between"><div className="grid flex-1 gap-2"><div className="flex gap-2"><Skeleton className="h-5 w-28" /><Skeleton className="h-6 w-20" /></div><Skeleton className="h-10 w-2/3" /><Skeleton className="h-7 w-full max-w-3xl" /></div><Skeleton className="h-11 w-32" /></div></header><div className="flex flex-wrap gap-1 rounded-lg border border-border bg-surface p-1.5"><Skeleton className="h-11 w-24" /><Skeleton className="h-11 w-28" /><Skeleton className="h-11 w-24" /></div><Panel><PanelHeader><Skeleton className="h-6 w-24" /></PanelHeader><PanelContent className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{rows(4).map((item) => <div key={item} className="grid gap-1"><Skeleton className="h-5 w-20" /><Skeleton className="h-6 w-28" /></div>)}</PanelContent></Panel><FormSkeleton /><section className="grid gap-4"><div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between"><div className="grid gap-1"><Skeleton className="h-8 w-32" /><Skeleton className="h-6 w-80 max-w-full" /></div><Skeleton className="h-11 w-36" /></div><div className="grid gap-3">{rows(questionCount).map((item) => <Panel key={item}><PanelHeader className="flex-row justify-between gap-4"><div className="grid flex-1 gap-3"><div className="flex gap-2"><Skeleton className="h-6 w-20" /><Skeleton className="h-6 w-28" /></div><Skeleton className="h-6 w-4/5" /></div><div className="flex gap-1"><Skeleton className="size-11" /><Skeleton className="size-11" /></div></PanelHeader><PanelContent><div className="grid gap-2 sm:grid-cols-2">{rows(4).map((option) => <Skeleton key={option} className="h-11 w-full" />)}</div></PanelContent></Panel>)}</div></section></div></Status>;
}

export function SessionMonitorSkeleton() {
  return <Status label="Načítám průběžné výsledky relace…"><div className="grid gap-7"><header className="flex flex-col gap-5 border-b border-border pb-6 lg:flex-row lg:items-end lg:justify-between"><div className="grid gap-2"><Skeleton className="h-11 w-40" /><div className="flex gap-2"><Skeleton className="h-5 w-28" /><Skeleton className="h-6 w-28" /></div><Skeleton className="h-10 w-2/3" /><Skeleton className="h-6 w-64" /></div><div className="flex gap-3"><Skeleton className="h-5 w-44" /><Skeleton className="h-11 w-32" /></div></header><div className="flex gap-2 rounded-lg border border-border bg-surface p-1.5"><Skeleton className="h-11 w-44" /><Skeleton className="h-11 w-28" /></div><Panel><PanelContent className="flex flex-col gap-5 sm:flex-row sm:justify-between"><div className="grid gap-1"><Skeleton className="h-6 w-32" /><Skeleton className="h-6 w-72 max-w-full" /></div><div className="grid grid-cols-3 gap-px">{rows(3).map((item) => <Skeleton key={item} className="h-16 w-full min-w-20" />)}</div></PanelContent></Panel>{[0,1].map((section) => <section key={section} className="grid gap-4"><div className="grid gap-1"><Skeleton className="h-8 w-64 max-w-[80%]" /><Skeleton className="h-6 w-full max-w-xl" /></div><Panel><PanelHeader className="flex-row justify-between"><div className="grid gap-2"><Skeleton className="h-6 w-28" /><Skeleton className="h-4 w-48" /></div><Skeleton className="h-6 w-28" /></PanelHeader><PanelContent className="grid gap-3">{rows(3).map((row) => <Skeleton key={row} className="h-16 w-full" />)}</PanelContent></Panel></section>)}</div></Status>;
}

export function StudentSessionSkeleton({ phase = "question" }: { phase?: "question" | "result" }) {
  return <Status label={phase === "result" ? "Počítám výsledek…" : "Připojuji tě ke kvízu…"}><Panel className="overflow-hidden">{phase === "question" ? <><PanelHeader className="gap-5 border-b border-border pb-6"><div className="flex justify-between gap-4"><div className="grid flex-1 gap-2"><div className="flex gap-2"><Skeleton className="h-6 w-24" /><Skeleton className="h-6 w-16" /></div><Skeleton className="h-1.5 w-64 max-w-full" /></div><Skeleton className="h-14 w-32" /></div><div className="flex gap-4"><Skeleton className="h-5 w-44" /><Skeleton className="h-5 w-32" /></div><Skeleton className="h-9 w-4/5" /><Skeleton className="h-6 w-2/3" /></PanelHeader><PanelContent><div className="grid gap-3 sm:grid-cols-2">{rows(4).map((item) => <Skeleton key={item} className="h-14 w-full" />)}</div></PanelContent><PanelFooter><Skeleton className="mr-auto hidden h-5 w-56 sm:block" /><Skeleton className="h-12 w-full sm:w-40" /></PanelFooter></> : <><PanelHeader className="items-center border-b border-border pb-7"><Skeleton className="mb-2 h-6 w-32" /><Skeleton className="my-2 size-40 rounded-full sm:size-44" /><Skeleton className="h-10 w-4/5" /><Skeleton className="mt-2 h-7 w-40" /></PanelHeader><PanelContent className="grid justify-items-center gap-2"><Skeleton className="size-10" /><Skeleton className="h-6 w-64 max-w-full" /><Skeleton className="h-5 w-full max-w-md" /></PanelContent><PanelFooter className="justify-center"><Skeleton className="h-11 w-40" /><Skeleton className="h-11 w-36" /></PanelFooter></>}</Panel></Status>;
}
