import type { Metadata } from "next";
import type { ReactNode } from "react";

export const metadata: Metadata = { title: "Administrace" };

export default function AdminLayout({ children }: { children: ReactNode }) {
  return children;
}
