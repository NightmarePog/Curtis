# Backend architecture

Curtis is one Spring Boot application with clear internal feature boundaries.
The boundaries are checked by tests.

## Features

```text
com.sosehl.curtis/
  feature/
    classrooms/   classes, groups, and membership
    media/        private quiz images
    quizzes/      quiz authoring and versions
    sessions/     attempts, grading, and rankings
    subjects/     subject catalogue
    users/        users and roles
    yaml/         YAML import and export
  platform/       security, realtime events, persistence helpers
  config/          application setup
  shared/errors/   shared HTTP error responses
```

Each feature owns its controllers, services, data access, and rules. There is
no shared business “core” package.

## Rules

- Controllers call services.
- A feature may use another feature only through its published `core` types.
- Features must not use another feature’s entities, repositories, or services.
- `core` types contain business rules and do not depend on Spring, JPA, HTTP, or
  Jackson.
- `sessions/attempt`, `sessions/ranking`, and `sessions/students` are internal
  parts of the sessions feature, not separate modules.
- YAML depends on quiz, media, and user contracts. Quizzes do not depend on
  YAML.

## Enforcement

`ApplicationModulesTest` checks the feature list, dependency direction, and
framework-free core packages. It also generates module documentation under
`build/spring-modulith-docs`.

The dependency list lives in `build.gradle`. Gradle generates the Spring
Modulith metadata during compilation, so package metadata is not duplicated in
the source tree.

## Data and HTTP

- Flyway owns database changes.
- Hibernate validates the schema and does not change it.
- Quiz launches store a snapshot of the quiz and audience.
- Student timers and attempts survive a server restart.
- Browser writes use CSRF protection and role-based authorization.
- Live updates use authenticated SSE events; the browser refetches the data.
- Media is private and checked against quiz ownership or attempt access.

For endpoint details, see [the API reference](../../docs/api.md).
