"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { api, LOGOUT_URL } from "@/lib/api";
import type { Me } from "@/lib/types";

export type AuthState =
  | { status: "loading" }
  | { status: "guest" }
  | { status: "user"; me: Me };

interface AuthContextValue {
  state: AuthState;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue>({
  state: { status: "loading" },
  logout: async () => {},
  refresh: async () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ status: "loading" });

  const refresh = useCallback(async () => {
    try {
      const me = await api.me();
      setState({ status: "user", me });
    } catch {
      setState({ status: "guest" });
    }
  }, []);

  useEffect(() => {
    let active = true;
    api
      .me()
      .then((me) => {
        if (active) setState({ status: "user", me });
      })
      .catch(() => {
        if (active) setState({ status: "guest" });
      });
    return () => {
      active = false;
    };
  }, []);

  const logout = useCallback(async () => {
    try {
      await fetch(LOGOUT_URL, { method: "POST", credentials: "include" });
    } catch {
      // Server may be unreachable — still drop the local session.
    }
    setState({ status: "guest" });
  }, []);

  const value = useMemo(
    () => ({ state, logout, refresh }),
    [state, logout, refresh]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  return useContext(AuthContext).state;
}

export function useLogout(): () => Promise<void> {
  return useContext(AuthContext).logout;
}

export function useMe(): Me | undefined {
  const state = useAuth();
  return state.status === "user" ? state.me : undefined;
}

export function isTeacher(me: Me | undefined): boolean {
  return !!me?.roles.includes("TEACHER");
}

export function isStudent(me: Me | undefined): boolean {
  return !!me?.roles.includes("STUDENT");
}

/** Prefer the human-readable OIDC name; `sub` is an opaque Entra identifier. */
export function displayName(me: Me | undefined): string {
  if (!me) return "";
  if (me.name?.trim()) return me.name.trim();
  const sub = me.sub;
  if (sub.includes("@")) return sub.split("@")[0];
  return sub.length > 14 ? `${sub.slice(0, 8)}…${sub.slice(-4)}` : sub;
}

export function initials(me: Me | undefined): string {
  const name = displayName(me);
  if (!name) return "?";
  const parts = name.split(/[.\-_\s]+/).filter(Boolean);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
  return name.slice(0, 2).toUpperCase();
}
