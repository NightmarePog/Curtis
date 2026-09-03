import { API_BASE } from "@/lib/constants";
import type { ApiErrorEnvelope } from "@/types/domain";

export class ApiError extends Error {
  readonly status: number;
  readonly details: ApiErrorEnvelope | null;
  readonly code: string | null;
  readonly traceId: string | null;
  readonly fieldErrors: ApiErrorEnvelope["fieldErrors"];

  constructor(status: number, message: string, details: ApiErrorEnvelope | null = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details
      ? {
          ...details,
          type: details.type ?? (details.code ? `urn:curtis:error:${details.code}` : undefined),
          title: details.title ?? statusTitle(status),
          status: details.status ?? status,
          detail: details.detail ?? message,
        }
      : null;
    this.code = this.details?.code ?? null;
    this.traceId = this.details?.traceId ?? null;
    this.fieldErrors = this.details?.fieldErrors;
  }
}

function statusTitle(status: number) {
  if (status === 400) return "Bad Request";
  if (status === 401) return "Unauthorized";
  if (status === 403) return "Forbidden";
  if (status === 404) return "Not Found";
  if (status === 405) return "Method Not Allowed";
  if (status === 409) return "Conflict";
  if (status === 410) return "Gone";
  return status >= 500 ? "Internal Server Error" : "Request Error";
}

function extractMessage(body: unknown, fallback: string) {
  if (!body || typeof body !== "object") return fallback;
  const envelope = body as ApiErrorEnvelope;
  if (typeof envelope.detail === "string" && envelope.detail.trim()) {
    return envelope.detail;
  }
  const message = envelope.message;
  if (typeof message === "string") return message;
  if (message && typeof message === "object") {
    return Object.values(message).join(" ");
  }
  return fallback;
}

function cookie(name: string) {
  if (typeof document === "undefined") return null;
  const encodedName = `${encodeURIComponent(name)}=`;
  for (const part of document.cookie.split(";")) {
    const value = part.trim();
    if (value.startsWith(encodedName)) {
      return decodeURIComponent(value.slice(encodedName.length));
    }
  }
  return null;
}

function isMutation(method: string | undefined) {
  const normalized = (method ?? "GET").toUpperCase();
  return !["GET", "HEAD", "OPTIONS"].includes(normalized);
}

export async function request<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  const isForm = init.body instanceof FormData;

  if (init.body && !isForm && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (isMutation(init.method) && !headers.has("X-XSRF-TOKEN")) {
    const csrfToken = cookie("XSRF-TOKEN");
    if (csrfToken) headers.set("X-XSRF-TOKEN", csrfToken);
  }
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json, application/problem+json");
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
    credentials: "include",
  });

  const contentType = response.headers.get("content-type") ?? "";
  const hasBody = response.status !== 204 && response.status !== 205;
  let body: unknown = null;

  if (hasBody) {
    if (contentType.includes("json")) {
      body = await response.json().catch(() => null);
    } else {
      body = await response.text().catch(() => "");
    }
  }

  if (!response.ok) {
    const fallback = `The request failed (${response.status}).`;
    const details = body && typeof body === "object" ? (body as ApiErrorEnvelope) : null;
    throw new ApiError(response.status, extractMessage(body, fallback), details);
  }

  return body as T;
}

export function isNoMoreQuestions(error: unknown) {
  return (
    error instanceof ApiError &&
    (error.code === "attempt_ready_to_submit" ||
      error.code === "no_more_questions" ||
      error.details?.error === "No more questions")
  );
}

export function messageFromError(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}
