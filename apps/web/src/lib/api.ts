import type {
  Me,
  Question,
  QuestionCreateDto,
  QuestionPatchDto,
  QuestionResponse,
  Quiz,
  QuizCreateRequest,
  QuizPatchRequest,
  QuizResult,
  ResultsResponse,
} from "./types";

const BASE = "/api";

async function request<T>(
  path: string,
  init?: RequestInit
): Promise<T> {
  const response = await fetch(`${BASE}${path}`, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    ...init,
  });

  if (!response.ok) {
    let message = `Požadavek selhal (${response.status})`;
    try {
      const body = await response.json();
      if (typeof body.message === "string") {
        message = body.message;
      } else if (typeof body.message === "object" && body.message !== null) {
        message = Object.entries(body.message)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join(", ");
      }
    } catch {
      // non-JSON error body, keep generic message
    }
    const error = new Error(message) as Error & { status?: number };
    error.status = response.status;
    throw error;
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const api = {
  me: () => request<Me>("/v1/me"),

  listQuizzes: () => request<Quiz[]>("/v1/quiz"),
  getQuiz: (uuid: string) => request<Quiz>(`/v1/quiz/${uuid}`),
  createQuiz: (body: QuizCreateRequest) =>
    request<{ quizUuid: string }>("/v1/quiz", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  patchQuiz: (uuid: string, body: QuizPatchRequest) =>
    request<void>(`/v1/quiz/${uuid}`, {
      method: "PATCH",
      body: JSON.stringify(body),
    }),

  listQuestions: (quizUuid: string) =>
    request<Question[]>(`/v1/quiz/${quizUuid}/questions`),
  createQuestion: (quizUuid: string, body: QuestionCreateDto) =>
    request<void>(`/v1/quiz/${quizUuid}/questions`, {
      method: "POST",
      body: JSON.stringify(body),
    }),
  patchQuestion: (
    quizUuid: string,
    questionId: number,
    body: QuestionPatchDto
  ) =>
    request<void>(`/v1/quiz/${quizUuid}/questions/${questionId}`, {
      method: "PATCH",
      body: JSON.stringify(body),
    }),
  deleteQuestion: (quizUuid: string, questionId: number) =>
    request<void>(`/v1/quiz/${quizUuid}/questions/${questionId}`, {
      method: "DELETE",
    }),

  createSession: (quizUuid: string, expiresInMinutes?: number) => {
    const params = new URLSearchParams({ quizUuid });
    if (expiresInMinutes) {
      params.set("expiresInMinutes", String(expiresInMinutes));
    }
    return request<string>(`/v1/sessions?${params.toString()}`, {
      method: "POST",
    });
  },
  joinSession: (sessionUuid: string) =>
    request<QuestionResponse>(`/v1/sessions/${sessionUuid}/join`, {
      method: "POST",
    }),
  nextQuestion: (sessionUuid: string, answer: number[]) =>
    request<QuestionResponse>(`/v1/sessions/${sessionUuid}/next`, {
      method: "POST",
      body: JSON.stringify(answer),
    }),
  finishSession: (sessionUuid: string) =>
    request<ResultsResponse>(`/v1/sessions/${sessionUuid}/finish`, {
      method: "POST",
    }),
  sessionResults: (sessionUuid: string) =>
    request<QuizResult[]>(`/v1/sessions/${sessionUuid}/results`),
};

export const LOGIN_URL = `${BASE}/login`;
export const LOGOUT_URL = `${BASE}/logout`;
