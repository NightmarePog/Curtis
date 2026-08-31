"use client";

import { useParams } from "next/navigation";
import { isTeacher, useMe } from "@/components/providers/auth-provider";
import { RequireAuth } from "@/components/common/guards";
import { LiveSessionPanel } from "@/components/session/live-session-panel";
import { PlaySession } from "@/components/session/play-session";

function SessionRouter() {
  const { uuid } = useParams<{ uuid: string }>();
  const me = useMe();

  return isTeacher(me) ? (
    <LiveSessionPanel sessionUuid={uuid} />
  ) : (
    <PlaySession sessionUuid={uuid} />
  );
}

export default function SessionPage() {
  return (
    <RequireAuth>
      <SessionRouter />
    </RequireAuth>
  );
}
