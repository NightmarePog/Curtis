# Curtis HTTP API

The canonical API prefix is `/v1`. All endpoints except OAuth entry points and
health probes require an authenticated Microsoft Entra session. Role prefixes
are authoritative: `/v1/student/**`, `/v1/teacher/**`, and `/v1/admin/**`.

## HTTP conventions

- JSON is UTF-8. Successful creation returns `201 Created` and a `Location`
  header; successful deletion or archival returns `204 No Content`.
- IDs are UUID strings. Timestamps are ISO-8601 instants in UTC.
- Mutating browser requests send `X-XSRF-TOKEN` with the value of the
  `XSRF-TOKEN` cookie. `GET /v1/me` initializes that cookie.
- Mutable administrative resources expose a `version`. Send the current version
  back on update; stale writes return `409 Conflict`.

### Errors

Errors use RFC 9457-style `application/problem+json` with English, stable
machine-readable codes:

```json
{
  "type": "urn:curtis:error:validation_failed",
  "title": "Bad Request",
  "status": 400,
  "detail": "The request contains invalid fields.",
  "code": "validation_failed",
  "traceId": "2ca7fd46-65f1-4cef-9bd3-20ea588ad9e9",
  "fieldErrors": { "title": "must not be blank" }
}
```

When a trace context is active, `traceId` matches the Micrometer trace recorded
in server logs. Requests use the standard W3C `traceparent` format rather than a
Curtis-specific correlation header. Authentication and authorization failures
use this same error format. Internal exception messages and stack traces are
never returned.

## Identity and live events

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/v1/me` | Current persisted user profile and all observed roles |
| `GET` | `/v1/events` | Authenticated `text/event-stream` invalidation feed |

`/v1/me` returns `id`, `subject`, `username`, `displayName`, and `roles`.
SSE events are named `sessions-changed`, `results-changed`, `roster-changed`,
and `quizzes-changed`. Their payload is always `{}`. The client refetches an
authorized endpoint. Result invalidations go only to the affected student, the
session owner, and active administrators. Logout or account deactivation closes
the user's existing streams. Heartbeats are sent about every 25 seconds and each
user may hold at most five streams.

## Teacher API

### Classes and groups

| Method | Route |
| --- | --- |
| `GET` | `/v1/teacher/classes` |
| `GET` | `/v1/teacher/classes/{classId}` |
| `POST` | `/v1/teacher/classes/{classId}/groups` |
| `PATCH` | `/v1/teacher/classes/{classId}/groups/{groupId}` |
| `DELETE` | `/v1/teacher/classes/{classId}/groups/{groupId}` |
| `PUT` / `DELETE` | `/v1/teacher/classes/{classId}/groups/{groupId}/students/{studentId}` |

Teachers can read only assigned classes. They may manage nested groups and
group membership inside those classes, but administrators own the main class
roster and teacher assignment.

`GET /v1/teacher/subjects` returns the teacher's assigned subject catalog.

### Quiz authoring

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` / `POST` | `/v1/teacher/quizzes` | List owned quizzes / create |
| `GET` / `PUT` | `/v1/teacher/quizzes/{quizId}` | Read / full versioned replacement |
| `DELETE` | `/v1/teacher/quizzes/{quizId}?expectedVersion=…` | Archive |
| `POST` | `/v1/teacher/quizzes/yaml` | Create from YAML body |
| `GET` / `PUT` | `/v1/teacher/quizzes/{quizId}/yaml` | Export / replace with YAML |
| `POST` | `/v1/teacher/media` | Upload PNG, JPEG, or WebP multipart `file` |
| `GET` | `/v1/media/{mediaId}` | Read media when authorized by ownership or attempt access |

A quiz is creator-owned. Question, option, and pair order is explicit. Valid
statuses are `DRAFT`, `PUBLISHED`, and `ARCHIVED`; only a published, currently
valid quiz can launch. See [quiz-yaml.md](quiz-yaml.md) for the complete YAML
schema and watched-package workflow.

### Sessions, attempts, and students

Create a self-paced session with a mandatory class/group audience:

```http
POST /v1/teacher/sessions
Content-Type: application/json

{
  "quizId": "e4313842-4774-4427-a435-3c49dc24f4a9",
  "classIds": ["c96be8dc-4d40-48f4-b940-e5674184d4d5"],
  "groupIds": [],
  "closesAt": "2026-09-02T19:00:00Z",
  "maxAttempts": 2,
  "scorePolicy": "BEST"
}
```

`scorePolicy` is `BEST` (default), `LATEST`, or `ALL`. Targeting a class and one
of its nested groups in the same session is rejected as redundant. The audience,
quiz, teacher name, ordering, and answer data are snapshotted at launch.

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` / `POST` | `/v1/teacher/sessions` | List own sessions / launch |
| `POST` | `/v1/teacher/sessions/{sessionId}/close` | Hard-stop session and attempts |
| `GET` | `/v1/teacher/sessions/{sessionId}/attempts` | Scores for own session |
| `GET` | `/v1/teacher/sessions/history?limit=100` | Own attempt history |
| `GET` | `/v1/teacher/attempts/{attemptId}` | Answers and answer key |
| `GET` | `/v1/teacher/reviews` | Pending free-text reviews |
| `PUT` | `/v1/teacher/reviews/{questionResultId}` | Grade with `{ "points": 3 }` |
| `GET` | `/v1/teacher/students` | Simple student tables grouped by class |
| `GET` | `/v1/teacher/students/{studentId}` | Student profile and own history |

Teachers see detailed answers only for sessions they created. Multiple teachers
may teach the same class, but that does not expose each other's answer details.

## Student API

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/v1/student/sessions` | Active sessions in the launch-time audience |
| `POST` | `/v1/student/sessions/{sessionId}/attempts` | Start or resume an attempt |
| `GET` | `/v1/student/attempts/{attemptId}` | Resume the persisted current question |
| `PUT` | `/v1/student/attempts/{attemptId}/questions/{questionId}/answer` | Lock one answer |
| `POST` | `/v1/student/attempts/{attemptId}/submit` | Submit the attempt |
| `GET` | `/v1/student/results?limit=100` | Own history from sessions that are no longer active |
| `GET` | `/v1/student/results/{attemptId}` | Own result detail |
| `GET` | `/v1/student/rankings` | Current class and group leaderboards |

Question timers are server-owned and persisted. Reopening a page does not reset
the current deadline. Sending the same semantic answer again is idempotent; a
different answer after it is locked returns `409 Conflict`. Answers use opaque
UUIDs, never display indexes:

```json
{ "type": "MULTIPLE_CHOICE", "optionIds": ["…"], "pairs": [], "text": null }
```

```json
{
  "type": "MATCHING",
  "optionIds": [],
  "pairs": [{ "leftId": "…", "rightId": "…" }],
  "text": null
}
```

```json
{ "type": "FREE_TEXT", "optionIds": [], "pairs": [], "text": "My answer" }
```

Non-empty free text is `PENDING_REVIEW` and is excluded from rankings until the
whole attempt is finalized. Class rankings aggregate only sessions assigned to
the whole class; group rankings aggregate only sessions assigned to that group.
Both use each participant's launch-time class/group snapshot, so later roster
moves never reattribute historical points. Active sessions are omitted from
history and rankings. Submission confirms that the attempt was stored, but its
score, question detail, and answer keys remain unavailable until the session is
no longer active. Result detail then includes snapshotted option and
matching-pair labels so teacher review never depends on a later quiz edit.

## Administrator API

Administrators have cross-school oversight but cannot launch a session unless
their verified account also has the teacher role.

| Area | Routes |
| --- | --- |
| Users | `GET /v1/admin/users`, `PATCH /v1/admin/users/{userId}` |
| Classes | `GET`/`POST /v1/admin/classes`, `GET`/`PATCH /v1/admin/classes/{classId}` |
| Main roster | `PUT`/`DELETE /v1/admin/classes/{classId}/students/{studentId}` |
| Teachers | `PUT`/`DELETE /v1/admin/classes/{classId}/teachers/{teacherId}` |
| Groups | Group routes under `/v1/admin/classes/{classId}/groups` |
| Subjects | `GET`/`POST /v1/admin/subjects`, `PATCH /v1/admin/subjects/{subjectId}` |
| Subject teachers | `PUT`/`DELETE /v1/admin/subjects/{subjectId}/teachers/{teacherId}` |
| Quizzes | `GET`/`POST /v1/admin/quizzes`, `GET`/`PUT`/`DELETE /v1/admin/quizzes/{quizId}` |
| Sessions | `GET /v1/admin/sessions`, `POST /v1/admin/sessions/{sessionId}/close` |
| Answers | `GET /v1/admin/sessions/{sessionId}/attempts`, `GET /v1/admin/attempts/{attemptId}` |
| Reviews | `GET /v1/admin/reviews`, `PUT /v1/admin/reviews/{questionResultId}` |

Creating an administrator-owned quiz is intentionally impossible. `POST
/v1/admin/quizzes` requires a verified active `creatorId` with the teacher role.

## Persistence guarantees

PostgreSQL stores users, roles, classes, groups, quiz versions, launch audiences,
session snapshots, attempts, question order, per-question deadlines, answers,
grades and Spring sessions. A backend restart does not lose a
live attempt. Flyway owns the schema and Hibernate runs with
`ddl-auto=validate`.
