# ADR 0002: Spring Boot 3 / Java 17 pilot on entitlements-service

Status: Accepted
Owner: identity-platform

## Context

Boot 2.7 is out of OSS support (Nov 2023). The platform needs to move. Nobody wanted to pilot on a
compliance critical service; entitlements was new, small and owned by a team with capacity.

## Decision

Build entitlements-service on Java 17 and Boot 3.1.x from the start. Do not depend on
`common-starter` (javax). Copy the cross-cutting pieces, mark them `@Deprecated` with a pointer to
PLAT-1352, and replace them with a `common-starter-jakarta` when one exists.

## Consequences

* Two versions of the platform infrastructure in the tree. Known drift.
* The `maven-jdk17-rhel9` agent label exists for this one service (TOOL-1034).
* The migration report: Jakarta rename was mechanical, Hibernate 6 changed sequence allocation and broke a Flyway-seeded ID range, Log4j2 config was the only real surprise (it needed `log4j2-spring.xml` explicitly).
