import type { Metadata } from "next";
import { TeacherOnly } from "@/components/common/guards";
import { PageHeader } from "@/components/layout/page-header";
import { QuizCreateForm } from "@/components/quiz/quiz-create-form";

export const metadata: Metadata = { title: "Nový kvíz" };

export default function NewQuizPage() {
  return (
    <TeacherOnly>
      <div className="mx-auto max-w-2xl space-y-6">
        <PageHeader
          eyebrow="Vyučující"
          title="Nový kvíz"
          description="Pojmenujte kvíz a nastavte průběh. Otázky přidáte hned v dalším kroku."
          backHref="/dashboard"
          backLabel="Zpět na přehled"
        />
        <QuizCreateForm />
      </div>
    </TeacherOnly>
  );
}
