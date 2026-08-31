"use client";

import { KeyRound, Send, Timer, Trophy } from "lucide-react";
import { displayName, useMe } from "@/components/providers/auth-provider";
import { PageHeader } from "@/components/layout/page-header";
import { JoinCard } from "@/components/session/join-card";

const steps = [
  {
    icon: KeyRound,
    title: "Získáte kód",
    text: "Vyučující spustí kvíz a sdělí vám kód.",
  },
  {
    icon: Send,
    title: "Připojíte se",
    text: "Kód vložíte do pole výše a hra začne.",
  },
  {
    icon: Timer,
    title: "Odpovídáte",
    text: "Každá otázka má vlastní časový limit.",
  },
  {
    icon: Trophy,
    title: "Vidíte výsledek",
    text: "Vyhodnocení proběhne hned po dokončení.",
  },
];

export function StudentDashboard() {
  const me = useMe();

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Žák"
        title={`Vítejte zpět, ${displayName(me)}`}
        description="Pro vstup do kvízu zadejte kód, který vám sdělil vyučující."
      />

      <JoinCard />

      <section aria-labelledby="jak-to-funguje" className="space-y-4">
        <h2
          id="jak-to-funguje"
          className="text-base font-semibold tracking-tight text-foreground"
        >
          Jak to funguje
        </h2>

        <ol className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {steps.map((step, index) => {
            const Icon = step.icon;
            return (
              <li
                key={step.title}
                className="surface relative overflow-hidden p-4 animate-rise"
                style={{ animationDelay: `${index * 60}ms` }}
              >
                <span
                  data-numeric
                  aria-hidden="true"
                  className="absolute -right-1 -top-3 text-5xl font-bold text-foreground/[0.045]"
                >
                  {index + 1}
                </span>
                <span className="flex size-9 items-center justify-center rounded-xl bg-brand-soft text-brand">
                  <Icon aria-hidden="true" className="size-4.5" />
                </span>
                <p className="mt-3 text-sm font-medium text-foreground">
                  {step.title}
                </p>
                <p className="mt-1 text-sm text-muted-foreground">{step.text}</p>
              </li>
            );
          })}
        </ol>
      </section>
    </div>
  );
}
