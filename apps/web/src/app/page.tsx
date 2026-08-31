import { redirect } from "next/navigation";
import { LOGIN_URL } from "@/lib/api";

export default function HomePage() {
  redirect(LOGIN_URL);
}
