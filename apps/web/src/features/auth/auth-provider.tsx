"use client";

import { useRouter } from "next/navigation";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { DEMO_MODE } from "@/lib/constants";
import { RouteSkeleton } from "@/components/page-skeletons";
import { authService } from "@/lib/services";
import type { Me } from "@/types/domain";

type AuthState =
  | { status: "loading" }
  | { status: "guest" }
  | { status: "user"; user: Me };

interface AuthContextValue {
  state: AuthState;
  refresh: () => Promise<void>;
  enterDemo: (role: "ADMINISTRATOR" | "TEACHER" | "STUDENT") => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [state, setState] = useState<AuthState>({ status: "loading" });

  const refresh = useCallback(async () => {
    try {
      const user = await authService.me();
      setState({ status: "user", user });
    } catch {
      setState({ status: "guest" });
    }
  }, []);

  useEffect(() => {
    let active = true;
    authService
      .me()
      .then((user) => {
        if (active) setState({ status: "user", user });
      })
      .catch(() => {
        if (active) setState({ status: "guest" });
      });
    return () => {
      active = false;
    };
  }, []);

  const enterDemo = useCallback(async (
    role: "ADMINISTRATOR" | "TEACHER" | "STUDENT",
  ) => {
    if (!DEMO_MODE) return;
    await authService.demoLogin(role);
    const user = await authService.me();
    setState({ status: "user", user });
  }, []);

  const logout = useCallback(async () => {
    await authService.logoutLocal();
    if (DEMO_MODE) {
      setState({ status: "guest" });
      return;
    }
    setState({ status: "guest" });
    router.replace("/login");
    router.refresh();
  }, [router]);

  const value = useMemo(
    () => ({ state, refresh, enterDemo, logout }),
    [state, refresh, enterDemo, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}

export function isTeacher(user: Me | undefined) {
  return user?.roles.includes("TEACHER") ?? false;
}

export function isAdministrator(user: Me | undefined) {
  return user?.roles.includes("ADMINISTRATOR") ?? false;
}

export function isStudent(user: Me | undefined) {
  return user?.roles.includes("STUDENT") ?? false;
}

export function workspacePath(user: Me) {
  return isAdministrator(user) ? "/admin" : "/dashboard";
}

export function RequireAuth({
  administrator = false,
  children,
  teacher = false,
}: {
  administrator?: boolean;
  children: ReactNode;
  teacher?: boolean;
}) {
  const { state } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (state.status === "guest") router.replace("/login");
    if (
      state.status === "user" &&
      ((teacher && !isTeacher(state.user)) ||
        (administrator && !isAdministrator(state.user)))
    ) {
      router.replace(workspacePath(state.user));
    }
  }, [administrator, router, state, teacher]);

  if (state.status === "loading") {
    return <RouteSkeleton />;
  }

  if (
    state.status === "guest" ||
    (teacher && !isTeacher(state.user)) ||
    (administrator && !isAdministrator(state.user))
  ) return null;
  return children;
}
