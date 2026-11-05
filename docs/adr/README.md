# Architecture Decision Records

Short records of decisions that shape the system, with their trade-offs. Format: [Michael Nygard's ADR](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions). New ADRs: copy [template.md](template.md) or use the `/adr` skill.

`Accepted` = decided by the owner. `Proposed` = drafted, waiting for the owner's decision.

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions as ADRs | Accepted |
| [0002](0002-java-and-spring-boot-backend.md) | Java 21 and Spring Boot for the backend | Accepted |
| [0003](0003-feature-packages-with-layers.md) | Package by feature, with controller/service/repository layers inside each feature | Accepted |
| [0004](0004-mutable-ledger.md) | The transaction ledger is mutable; history is validated by replay | Accepted |
| [0005](0005-compute-derived-state-on-read.md) | Derived state is computed on every read, never stored | Accepted |
| [0006](0006-exact-decimal-arithmetic.md) | Exact decimal arithmetic end to end | Proposed |
| [0007](0007-server-side-sessions.md) | Server-side sessions with a same-origin cookie | Accepted |
| [0008](0008-spring-data-jdbc-and-flyway.md) | Spring Data JDBC and Flyway for persistence | Proposed |
| [0009](0009-acceptance-criteria-traceability.md) | Acceptance criteria traceability enforced in CI | Proposed |
