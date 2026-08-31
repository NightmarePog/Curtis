import type { Metadata } from "next";
import { RequireAuth } from "@/components/common/guards";
import { PageHeader } from "@/components/layout/page-header";
import { JoinCard } from "@/components/session/join-card";

export const metadata: Metadata = { title: "Připojit se do kvízu" };

export default function JoinPage() {
  return (
    <RequireAuth>
      <div className="mx-auto max-w-lg space-y-6">
        <PageHeader
          eyebrow="Žák"
          title="Připojit se do kvízu"
          description="Zadejte kód, který vám sdělil vyučující."
        />
        <JoinCard autoFocus />
      </div>
    </RequireAuth>
  );
}
