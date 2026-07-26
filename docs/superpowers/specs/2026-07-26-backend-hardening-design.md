# Backend Hardening: Roles, Time-Limit Enforcement, YAML Import

**Date:** 2026-07-26
**Status:** Approved
**Scope:** `apps/server` only. Frontend (teacher dashboard, student quiz-taking UI) is a separate, later sub-project.

## Context

The backend already has working CRUD for `Quiz`/`Question` and a live in-memory `Session`/`StudentAttempt` flow for students playing a quiz (see `domain/v1/quiz`, `domain/v1/question`, `domain/v1/session`). Three gaps stand between this and the user's actual requirements ("teachers can create quizzes", "every question has a time limit so you can't just google it", "teacher wants to drop a YAML file to create a quiz"):

1. **No role concept.** Any authenticated Microsoft account can currently call `POST /v1/quiz`. There is no teacher/student distinction anywhere in the backend.
2. **Time limits are not enforced.** `Question.timeInSeconds` is stored and returned to the client, but the server never records when a question was served, so nothing stops a student taking unlimited time on a question before answering.
3. **No YAML import exists at all.**

This document specs all three as one backend-hardening pass.

## 1. Roles (Teacher / Student)

**Source of truth:** the Entra ID App Roles claim, named `roles`, containing values `"Teacher"` and/or `"Student"`. The Entra app registration side (defining the App Roles, assigning them to users/groups) is configured by the user separately and is out of scope for this spec.

**Mechanism:**
- A custom `OidcUserService` bean (in `common/components`) wraps the default Spring `OidcUserService`. After delegating to load the `OidcUser`, it reads the `roles` claim from the ID token claims and maps each value to a `SimpleGrantedAuthority` prefixed with `ROLE_` (i.e. `"Teacher"` → `ROLE_TEACHER`, `"Student"` → `ROLE_STUDENT`), merged with the authorities the default service already produced.
- If the `roles` claim is missing or empty, the user gets no extra authorities. They can still authenticate and hit read-only endpoints, but nothing that requires `ROLE_TEACHER`. This is the safe default — no accidental write access.

**Authorization changes in `SecurityConfig`:**
- Require `ROLE_TEACHER` for: `POST /v1/quiz`, `PATCH /v1/quiz/{uuid}`, all of `/v1/quiz/{quizUuid}/questions/**` (create/patch/delete), and `POST /v1/sessions` (starting a live session).
- Everything else that currently requires just `authenticated()` stays as `authenticated()`: `GET /v1/quiz`, `GET /v1/quiz/{uuid}`, and the session play endpoints (`join`, `next`, `finish`).
- Unauthenticated requests keep today's behavior (redirect to OAuth2 login, `302`). Authenticated-but-wrong-role requests get `403 Forbidden`.

**Test impact:** `QuizControllerTest` and any future `QuestionController`/`SessionController` tests that exercise mutating endpoints need `@WithMockUser(authorities = "ROLE_TEACHER")` instead of bare `@WithMockUser`. New tests are added asserting a plain `ROLE_STUDENT`/no-role user gets `403` on those same endpoints.

## 2. Server-side time-limit enforcement

**Problem:** `StudentAttempt.nextQuestion()` hands out a question but never records when. `addAnswer()` blindly stores whatever the client sends, whenever it sends it.

**Design:**
- `StudentAttempt` gains a `Clock` field (constructor parameter, defaulting to `Clock.systemDefaultZone()` so production code doesn't need to change call sites) and an `Instant servedAt`.
- Every time `nextQuestion()` successfully returns a question, `servedAt = clock.instant()` is set to the moment that question was served.
- `addAnswer(List<Integer> answer)` now needs to know which question it's answering (it already implicitly relies on call order matching `questionIndex`). Before appending, it computes `elapsed = Duration.between(servedAt, clock.instant())` and compares against `Duration.ofSeconds(question.getTimeInSeconds() + GRACE_PERIOD_SECONDS)`, where `GRACE_PERIOD_SECONDS = 2` (fixed constant, accounts for network latency — not user-configurable, YAGNI).
  - If within the allowed window: store the answer as given.
  - If late: store an empty `List<Integer>` instead, silently. No exception, no special response — the existing `nextQuestion()` call right after still proceeds normally (or throws `NoMoreQuestionsException` if that was the last question, exactly as today).
- `Session.submitAnswer` and `SessionService.next` are unchanged; the enforcement is fully internal to `StudentAttempt`.

**Why a `Clock` and not `Thread.sleep` in tests:** injecting `Clock` lets tests use `Clock.fixed(...)` and advance it deterministically, avoiding flaky sleep-based timing tests.

**Test additions (`StudentAttemptTest`):**
- Answering within the time limit records the real answer and it's scored normally.
- Answering after `timeInSeconds` + grace period records an empty answer (scored as wrong), verified via a fixed/advanceable `Clock`.
- Answering exactly at the boundary (`timeInSeconds` + grace, inclusive) is still accepted (grace period is inclusive).

## 3. YAML quiz import (drop-folder, no auth)

**Design decision:** this does not go through the web API or Spring Security at all. The teacher hosts the server themselves; filesystem access to the host machine is the trust boundary, not an HTTP credential. So: a scheduled background task on the already-running server polls a local directory for files and imports them directly via the same repositories the REST layer uses.

**Configuration** (`application.properties`, both with defaults so no config is required to get default behavior):
- `quiz.import.enabled` (default `true`)
- `quiz.import.path` (default `./import`) — created on startup if missing, along with `import/processed/` and `import/failed/` subdirectories.

**Polling, not filesystem watch:** a `@Scheduled(fixedDelay = ...)` task (e.g. every 5 seconds) scans the *top level only* of `quiz.import.path` for `*.yaml`/`*.yml` files. Polling is chosen over Java's `WatchService` because watch-service events are unreliable across Docker bind mounts (this app ships a `Dockerfile`), and the failure mode of a watch-service silently missing an event is worse than a few seconds of extra latency. Subdirectories (`processed/`, `failed/`) are excluded from the scan so moved files are never reprocessed.

**YAML shape** (same fields validated the same way as the manual create/question-create DTOs):

```yaml
uuid: "optional-existing-quiz-uuid"   # omit to create a new quiz
title: "Chapter 5 Quiz"
description: "Cell biology basics"
shuffle: true
maxQuestionsPerSession: 10
questions:
  - question: "What is the powerhouse of the cell?"
    timeInSeconds: 20
    options: ["Mitochondria", "Nucleus", "Ribosome"]
    correctIndexes: [0]
  - question: "Which of these are nucleic acids?"
    timeInSeconds: 30
    options: ["DNA", "RNA", "ATP"]
    correctIndexes: [0, 1]
```

**Per-file processing:**
1. Parse with Jackson's `YAMLMapper` (new dependency: `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml`) into a new `QuizYamlDto`/`QuestionYamlDto` pair (in `domain/v1/quiz/dto`).
2. Validate manually via `jakarta.validation.Validator` (bean validation isn't automatic outside a `@RequestBody` context): title non-blank ≤100 chars, `maxQuestionsPerSession` present, at least one question, each question has non-blank text, `timeInSeconds` ≥ 1, at least 2 options, at least one `correctIndexes` entry, and every index in range for its `options` list.
3. Map to `Quiz`/`Question`/`QuestionAnswer` entities (reusing/extending `QuizMapper`/`QuestionMapper` with a new mapping method rather than duplicating logic).
4. If `uuid` is present and matches an existing quiz: replace that quiz's title/description/shuffle/maxQuestionsPerSession and its full question list (`quiz.getQuestions().clear()` then re-populate — safe because of the existing `orphanRemoval = true`). If `uuid` is present but doesn't match anything, treat as failure (don't silently create a new quiz under a different uuid — that would hide a typo).
5. If `uuid` is absent: create a new quiz exactly like the manual create flow.
6. On success: move the source file to `import/processed/<yyyyMMdd-HHmmss>-<original filename>`, log an info line with the resulting quiz UUID.
7. On any parse or validation failure: move the source file to `import/failed/<yyyyMMdd-HHmmss>-<original filename>` and write a sibling `<same name>.error.txt` with a human-readable explanation of what failed; log a warning. The server keeps running — one bad file never affects the rest of the app.

**New class:** `QuizYamlImportService` in `domain/v1/quiz`, holding the scheduled method plus the parse/validate/map/move logic. Depends on `QuizRepository`, `QuizMapper` (extended), and a `Validator`.

**Test additions:** a new test class driving `QuizYamlImportService` directly (not through HTTP, since there's no HTTP surface) against a temp directory (`@TempDir`): valid create, valid replace-by-uuid, malformed YAML, missing-correct-answer validation failure, unknown-uuid failure — asserting the resulting quiz state and that files land in `processed/`/`failed/` as expected.

## Out of scope for this spec

- Frontend (teacher dashboard, YAML upload UI, student quiz-taking UI, countdown timer display) — separate future sub-project, per user's own priority ordering.
- Entra ID app registration / App Roles setup — user's responsibility, outside the codebase.
- Any web-based YAML upload path — explicitly rejected in favor of the drop-folder approach since the teacher hosts the server themselves.
