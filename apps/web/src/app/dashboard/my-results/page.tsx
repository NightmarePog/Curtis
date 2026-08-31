import type { Metadata } from "next";
import { MyResults } from "@/components/dashboard/my-results";
import { RequireAuth } from "@/components/common/guards";

export const metadata: Metadata = { title: "Moje výsledky" };

export default function MyResultsPage() {
  return (
    <RequireAuth>
      <MyResults />
    </RequireAuth>
  );
}
