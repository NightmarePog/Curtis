"use client";

import {
  createContext,
  useContext,
  useEffect,
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
}

const AuthContext = createContext<AuthContextValue>({
  state: { status: "loading" },
  logout: async () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ status: "loading" });

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

  async function logout() {
    try {
      await fetch(LOGOUT_URL, { method: "POST", credentials: "include" });
    } catch {
      // server may be unreachable; still clear local state
    }
    setState({ status: "guest" });
  }

  return (
    <AuthContext.Provider value={{ state, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  return useContext(AuthContext).state;
}

export function useLogout(): () => Promise<void> {
  return useContext(AuthContext).logout;
}

export function isTeacher(me: Me | undefined): boolean {
  return !!me?.roles.includes("TEACHER");
}

export function isStudent(me: Me | undefined): boolean {
  return !!me?.roles.includes("STUDENT");
}
