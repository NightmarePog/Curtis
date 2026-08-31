import type { Metadata } from "next";
import { TeacherOnly } from "@/components/common/guards";
import { QuizDetail } from "@/components/quiz/quiz-detail";

export const metadata: Metadata = { title: "Úprava kvízu" };

export default function QuizDetailPage() {
  return (
    <TeacherOnly>
      <QuizDetail />
    </TeacherOnly>
  );
}
