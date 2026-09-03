"use client";

import { RequireAuth } from "@/features/auth/auth-provider";
import { AdminWorkspace } from "@/features/admin";

export default function AdminPage() {
  return (
    <RequireAuth administrator>
      <AdminWorkspace />
    </RequireAuth>
  );
}
