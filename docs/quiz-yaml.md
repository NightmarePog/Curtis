# Quiz YAML format

Curtis uses YAML schema version `1`. The web editor infers the owner from the
authenticated teacher. A watched package must also provide `ownerUsername`.
Unknown fields are rejected so spelling mistakes cannot silently change quiz
behavior.

```yaml
schemaVersion: 1
title: Network protocols
description: Revision quiz
subjectId: 2e30cbfd-9a37-431d-9445-45abcc16e945
chapter: HTTPS
status: DRAFT
maxQuestionsPerSession: 2
shuffle: true
questions:
  - type: MULTIPLE_CHOICE
    prompt: Which protocol encrypts HTTP traffic?
    points: 2
    timeSeconds: 30
    options:
      - text: HTTPS
        correct: true
      - text: FTP
        correct: false
  - type: MATCHING
    prompt: Match each protocol to its port
    points: 3
    timeSeconds: 45
    pairs:
      - left: HTTPS
        right: "443"
      - left: SSH
        right: "22"
  - type: FREE_TEXT
    prompt: Explain what TLS provides.
    points: 4
    timeSeconds: 90
```

## Editor schema

The editor contract is generated from `YamlQuizDocument` and its Jakarta
Validation annotations; it is not maintained by hand. From `apps/server`, run:

```bash
./gradlew generateQuizSchema
```

The resulting Draft 2020-12 schema is written to
`build/schema/quiz.schema.json`. It describes field names, required values,
lengths, ranges, and nested structures. Rules that depend on multiple fields,
such as the valid option layout for each question type, are still enforced by
the quiz service.

Compatibility aliases such as `timeInSeconds` are accepted when importing,
while exports and the generated schema use the canonical `timeSeconds` name.

Exports include `quizId`, `version`, and the UUIDs of questions, options, and
pairs. Keep those fields when replacing a quiz. A replacement without the
current `version` is rejected with a conflict instead of overwriting another
editor's changes.

Teacher endpoints:

- `POST /v1/teacher/quizzes/yaml` creates from a YAML request body.
- `GET /v1/teacher/quizzes/{quizId}/yaml` exports the owned quiz.
- `PUT /v1/teacher/quizzes/{quizId}/yaml` replaces it using the exported
  `version`.
- `POST /v1/teacher/media` uploads a PNG, JPEG, or WebP image. Use the returned
  media UUID as the question's `image` value in web-authored YAML.

## Watched packages

Place one package at `/data/import/incoming/<job>/` and create `.ready` only
after every file has been written:

```text
<job>/
  quiz.yaml
  assets/
    diagram.png
  .ready
```

The YAML must include the verified active teacher's `ownerUsername`. A question
may use `image: diagram.png`; the name must match a file directly inside
`assets/`. Watched packages never replace a quiz. Successful packages move to
`processed`, failed packages move to `failed` with `error.json`, and identical
package contents never create a second quiz.
