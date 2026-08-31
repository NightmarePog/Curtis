import type { Metadata } from "next";
import { Dashboard } from "@/components/dashboard/dashboard";
import { RequireAuth } from "@/components/common/guards";

export const metadata: Metadata = { title: "Přehled" };

export default function DashboardPage() {
  return (
    <RequireAuth>
      <Dashboard />
    </RequireAuth>
  );
}
