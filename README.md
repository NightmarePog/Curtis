# Curtis

Curtis is a classroom quiz platform for secondary schools. It uses Microsoft
Entra ID for sign-in and separates the product into three explicit roles:

- Students take assigned quizzes, review results, and view class or group
  leaderboards.
- Teachers create quizzes, import quiz content, target classes or groups,
  monitor live attempts, and grade free-text answers.
- Administrators manage users, classes, teacher assignments, groups, and
  subjects across the school.
- Quizzes support multiple-choice, free-text, and matching questions.
- Teachers can review student progress and past quiz sessions.
- Live session updates keep teachers informed as students participate.

## Production deployment

Requirements: a Linux server with Docker, a DNS record pointing your domain at
it, and ports 80 and 443 open. In the Entra app registration, add the redirect
URI `https://<DOMAIN>/api/login/oauth2/code/microsoft`.

```bash
cp .env.example .env        # set DOMAIN, POSTGRES_PASSWORD, MICROSOFT_*
just prod-up                # or: docker compose up -d --build
just prod-ps                # every service should become healthy
just prod-smoke https://<DOMAIN>
just prod-backup            # pg_dump into backups/
```

Caddy obtains and renews the TLS certificate automatically. To try the stack
locally, set `DOMAIN=localhost` and run `just prod-smoke https://localhost -k`.
