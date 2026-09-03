# Frontend architecture

Curtis uses Next.js App Router. The browser always calls `/api`; local Next.js
rewrites forward those requests to Spring, and production Caddy handles them.
This keeps cookies and OAuth on one origin.

## Pages

- `/login` — sign-in or the demo role picker
- `/dashboard` — the role-specific home page
- `/quiz/new`, `/quiz/import`, `/quiz/[uuid]` — quiz authoring
- `/session/[uuid]` — student attempt or teacher monitoring
- `/results`, `/leaderboard` — student results and rankings
- `/students`, `/students/[studentId]`, `/history` — teacher views
- `/admin` — administrator tools

There is no anonymous join-code flow.

## Code ownership

```text
src/features/auth      login state and roles
src/features/student   student workflows
src/features/teacher   teacher workflows
src/features/admin     administrator workflows
src/components/ui      shared accessible controls
src/lib/services.ts    feature API surface
src/lib/http.ts        requests, cookies, and API errors
src/lib/live-events.ts SSE and refresh fallback
src/lib/demo-store.ts  browser-only demo data
```

Feature components own their local form and page state. There is no global
client store for production data.

The API types are generated from the backend contract. Demo mode uses the same
service methods with deterministic browser-local data, so the UI can be shown
without the backend.

Live events contain no data. They tell a page to refetch its authorized API
endpoint. If SSE is unavailable, the page refreshes periodically.
