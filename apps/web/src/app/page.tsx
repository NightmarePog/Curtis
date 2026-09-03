import { redirect } from "next/navigation";
import { DEMO_MODE, LOGIN_URL } from "@/lib/constants";

export default function HomePage() {
  redirect(DEMO_MODE ? "/login" : LOGIN_URL);
}
