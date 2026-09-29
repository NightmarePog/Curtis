import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { Button } from "@/components/ui/button";
import {
  Panel,
  PanelContent,
  PanelDescription,
  PanelHeader,
  PanelTitle,
} from "@/components/ui/panel";
import { DemoRolePicker } from "@/features/auth/demo-role-picker";
import { DEMO_MODE, LOGIN_URL } from "@/lib/constants";

export const metadata: Metadata = { title: "Přihlášení" };

interface LoginPageProps {
  searchParams: Promise<{ error?: string }>;
}

export default async function LoginPage({ searchParams }: LoginPageProps) {
  if (DEMO_MODE) return <DemoRolePicker />;

  const { error } = await searchParams;

  if (!error) redirect(LOGIN_URL);

  return (
    <main className="mx-auto grid min-h-[70vh] w-full max-w-xl place-items-center px-4 py-12">
      <Panel className="w-full" role="alert">
        <PanelHeader>
          <PanelTitle>Přihlášení se nezdařilo</PanelTitle>
          <PanelDescription>
            Microsoft účet se nepodařilo ověřit. Zkuste přihlášení znovu. Pokud
            problém přetrvá, předejte správci čas chyby.
          </PanelDescription>
        </PanelHeader>
        <PanelContent>
          <Button asChild size="lg" className="w-full sm:w-auto">
            <a href={LOGIN_URL}>Zkusit znovu</a>
          </Button>
        </PanelContent>
      </Panel>
    </main>
  );
}
