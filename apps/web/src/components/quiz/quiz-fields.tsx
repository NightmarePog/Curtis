"use client";

import { Shuffle } from "lucide-react";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

export interface QuizFieldsValue {
  title: string;
  description: string;
  maxQuestions: string;
  shuffle: boolean;
}

/**
 * Shared field set for creating and editing a quiz, so the two screens can
 * never drift apart. `idPrefix` keeps labels bound correctly if both render.
 */
export function QuizFields({
  value,
  onChange,
  idPrefix,
}: {
  value: QuizFieldsValue;
  onChange: (patch: Partial<QuizFieldsValue>) => void;
  idPrefix: string;
}) {
  const titleId = `${idPrefix}-title`;
  const descId = `${idPrefix}-description`;
  const maxId = `${idPrefix}-max`;

  return (
    <div className="space-y-5">
      <div className="space-y-2">
        <Label htmlFor={titleId}>Název kvízu</Label>
        <Input
          id={titleId}
          value={value.title}
          onChange={(e) => onChange({ title: e.target.value })}
          placeholder="Např. Elektrotechnika — základy obvodů"
          required
          maxLength={200}
          autoComplete="off"
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor={descId}>Popis</Label>
        <Textarea
          id={descId}
          value={value.description}
          onChange={(e) => onChange({ description: e.target.value })}
          placeholder="Krátké shrnutí pro žáky — nepovinné"
          rows={3}
        />
        <p className="text-xs text-muted-foreground">
          Zobrazí se žákům v přehledu kvízu.
        </p>
      </div>

      <div className="grid gap-5 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor={maxId}>Otázek na jednu hru</Label>
          <Input
            id={maxId}
            type="number"
            inputMode="numeric"
            min={1}
            value={value.maxQuestions}
            onChange={(e) => onChange({ maxQuestions: e.target.value })}
            placeholder="5"
          />
          <p className="text-xs text-muted-foreground">
            Kolik otázek dostane každý žák. Výchozí je 5.
          </p>
        </div>

        <div className="space-y-2">
          <span className="block text-sm font-medium text-foreground">
            Pořadí otázek
          </span>
          <label className="flex items-start gap-3 rounded-xl border border-border bg-muted/30 p-3.5 transition-colors hover:border-ring/40">
            {/* Radix renders a <button role=checkbox>, which a wrapping
                <label> does not name — so label it explicitly. */}
            <Checkbox
              checked={value.shuffle}
              onCheckedChange={(checked) =>
                onChange({ shuffle: checked === true })
              }
              aria-label="Zamíchat pořadí otázek"
              className="mt-0.5"
            />
            <span className="space-y-0.5">
              <span className="flex items-center gap-1.5 text-sm font-medium text-foreground">
                <Shuffle aria-hidden="true" className="size-3.5 text-brand" />
                Zamíchat otázky
              </span>
              <span className="block text-xs text-muted-foreground">
                Každý žák dostane jiné pořadí.
              </span>
            </span>
          </label>
        </div>
      </div>
    </div>
  );
}

/** Shared parsing so create and edit send identical payloads. */
export function parseMaxQuestions(raw: string): number {
  const parsed = Number.parseInt(raw.trim(), 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 5;
}
