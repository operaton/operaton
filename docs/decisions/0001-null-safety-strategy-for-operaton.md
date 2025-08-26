---
status: "Proposed"
date: 2025-08-25
decision-makers: ["@ungerts", "@kthoms", "@javahippie"]
consulted: []
informed: []
---

# Null Safety Strategy for Operaton

## Context and Problem Statement

Null safety has long been a challenge in Java. Operaton currently enforces non-null assumptions primarily at runtime using `EnsureUtil` and `Objects.requireNonNull`, but lacks a standardized, tool-friendly way to declare nullability intent across APIs, which causes ambiguity for contributors and users of the public API. JSpecify is emerging as the ecosystem's preferred approach to express nullability contracts (standardized `@Nullable`/non-null annotations, good IDE/static analysis support, ecosystem alignment such as Spring Framework 7). JSpecify is developed by consensus of major stakeholders in the Java ecosystem (e.g., Google, JetBrains, Microsoft, Oracle, Meta, Uber, Broadcom/Spring), indicating strong industry backing. Project Valhalla’s future null-related features are out of scope for the current baseline and do not remove the need for annotations in the near term. These capabilities are expected to arrive incrementally across multiple JDK releases. Operaton currently targets Java 17, so it will likely take several years until Operaton runs on a JDK that fully supports Valhalla, keeping annotation-based nullability necessary for the foreseeable future.

## Decision Drivers

- Improve clarity and safety of Operaton’s public APIs regarding nullability.
- Leverage static tooling to catch issues earlier.
- Keep migration cost reasonable and avoid accidental external behavior changes during refactors.
- Do not rely on preview/incubating language features.
- Align with major ecosystem dependencies (e.g., Spring 7) to reduce friction for users.

## Considered Options

1. Status Quo — Keep `EnsureUtil` and `Objects.requireNonNull`, no nullability annotations.
2. JSpecify for New Public APIs Only — Annotate only newly introduced public APIs going forward.
3. JSpecify for All Public APIs (Selected) — Annotate all public-facing APIs; internals optional/gradual.
4. JSpecify Project-Wide — Annotate public and internal codebase; highest migration cost.
5. Defer Adoption — Revisit when JSpecify/Valhalla matures further.
6. Use Another Annotation Framework (e.g., JetBrains) — Diverges from ecosystem direction; similar migration cost.

## Decision Outcome

Chosen option: "JSpecify for All Public APIs (Selected)", because it formalizes contracts at API boundaries, aligns with ecosystem direction and tooling, enables effective IDE/CI assistance, and allows gradual internal adoption with reasonable cost and risk.

### Consequences

- Good, because public API contracts become explicit (fewer “can this be null?” questions) and static analyzers/IDEs can detect contract violations earlier; alignment with ecosystem (e.g., Spring 7) reduces friction.
- Bad, because contributors need light training; build/CI configuration must be adjusted; care is required to avoid behavior changes during refactors; partial coverage may exist during transition.

### Confirmation

Compliance will be confirmed via code review and CI checks:
- 100% of public APIs annotated with JSpecify.
- CI flags nullability violations on public APIs.
- No externally visible behavior changes (e.g., exception types) in the engine module due to refactors.

## Pros and Cons of the Options

### Status Quo — Keep EnsureUtil/Objects.requireNonNull, no annotations

- Good, because it has no immediate migration cost and preserves current behavior and CI setup.
- Neutral, because runtime checks can still catch nulls at boundaries but provide no design-time guidance.
- Bad, because ambiguity about nullability persists; limited tool-assisted safety; diverges from ecosystem direction.
- Bad, because it adds runtime overhead.

### JSpecify for New Public APIs Only

- Good, because it minimizes churn and adds clarity for newly added surfaces.
- Good, because runtime null checks can be reduced on hot paths to cut overhead.
- Good, because null safety is part of the contract/API.
- Neutral, because mixed annotation coverage can be acceptable temporarily.
- Bad, because it leaves existing APIs ambiguous for a long time; inconsistent experience for users.
- Bad, because of inconsistency in null safety across public APIs.

### JSpecify for All Public APIs (Selected)

- Good, because it sets clear, consistent contracts at API boundaries where they matter most; strong tool support; ecosystem alignment.
- Good, because Spring 7 adopts JSpecify, so projects using Spring and Operaton do not have to deal with multiple null-safety approaches.
- Good, because it provides consistency in null safety across public APIs.
- Neutral, because internals can adopt annotations gradually.
- Bad, because it requires a focused sweep of public APIs and careful handling where exception semantics must be preserved.

### JSpecify Project-Wide

- Good, because it maximizes clarity and static analysis coverage across the codebase.
- Neutral, because it may be a long-term aspiration after initial adoption.
- Bad, because it has the highest migration cost and risk of noise/false positives initially.

### Defer Adoption

- Good, because it avoids immediate cost and waits for further maturation.
- Neutral, because current runtime checks remain in place.
- Bad, because it delays benefits to users; misses ecosystem alignment; increases future migration burden.
- Bad, because imitating null safety using `Objects.requireNonNull` creates runtime overhead.

### Use Another Annotation Framework (e.g., JetBrains)

- Good, because it provides similar expressiveness with existing tooling support.
- Neutral, because many teams are familiar with these annotations.
- Bad, because it diverges from emerging ecosystem consensus and Spring 7 alignment; offers no clear advantage over JSpecify.
- Bad, because it risks fragmentation and potentially reduced long-term support as the ecosystem consolidates around JSpecify.

## More Information

- [Operaton forum thread – Null Safety using JSpecify](https://forum.operaton.org/t/null-safety-using-jspecify/281/1)
- [Operaton issue #1127](https://github.com/operaton/operaton/issues/1127)
- [JSpecify homepage](https://jspecify.dev/)
- [JSpecify GitHub repository](https://github.com/jspecify/jspecify)
- [JSpecify Javadoc](https://javadoc.io/doc/org.jspecify/jspecify/latest/index.html)
- [JSpecify FAQ](https://jspecify.dev/faq/)
- [Java SE 17 Javadoc: Objects.requireNonNull](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Objects.html#requireNonNull%28T%29)
- [Spring Framework documentation – Null-safety](https://docs.spring.io/spring-framework/reference/overview/null-safety.html)
- [OpenJDK Project Valhalla](https://openjdk.org/projects/valhalla/)
