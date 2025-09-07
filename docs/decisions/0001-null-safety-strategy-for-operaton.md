---
status: "Proposed"
date: 2025-08-25
decision-makers: ["@ungerts", "@kthoms", "@javahippie"]
consulted: []
informed: []
---

# Null Safety Strategy for Operaton

## Context and Problem Statement

Null safety is a long-standing concern in Java. Operaton currently relies primarily on runtime checks (e.g., `EnsureUtil`, `Objects.requireNonNull`) to enforce non-null assumptions. The project lacks a standardized, tool-friendly way to express nullability intent at API boundaries for design-time guidance.

This ADR evaluates whether to:

- Continue with runtime checks only (status quo),
- Adopt nullability annotations (e.g., JSpecify or other frameworks such as JetBrains annotations, Checker Framework, SpotBugs/FindBugs annotations, JSR 305, or Eclipse JDT), or
- Defer a decision and monitor potential language-level advancements (e.g., work in OpenJDK such as Project Valhalla and any future JVM/JDK nullability capabilities).

JSR 305 is included for completeness as a historical option; it is no longer actively maintained and has known interoperability ambiguities. Ecosystem signals (e.g., frameworks adopting nullability annotations) are considered, but this ADR aims to remain neutral in framing the alternatives.

## Decision Drivers

- Improve clarity and safety of Operaton’s public APIs regarding nullability.
- Leverage static tooling to catch issues earlier.
- Keep migration cost reasonable and avoid accidental external behavior changes during refactors.
- Do not rely on preview/incubating language features.
- Align with major ecosystem dependencies (e.g., Spring 7) to reduce friction for users.

## Considered Options

1. Status Quo — Runtime checks only, no nullability annotations
2. JSpecify for New Public APIs Only — Annotate only newly introduced public APIs going forward
3. JSpecify for All Public APIs — Annotate all public-facing APIs; internals optional/gradual
4. JSpecify Project-Wide — Annotate public and internal codebase
5. Defer Adoption — Revisit after a defined period/criteria
6. Use Another Annotation Framework — e.g., JetBrains, Checker Framework, SpotBugs/FindBugs, JSR 305, Eclipse JDT

### Scope separation note

The JSpecify variants (new APIs only, all public APIs, project-wide) represent scope choices of the same underlying approach. They are compared here for completeness. If needed, follow-up ADRs can focus on narrowing/expanding scope once the general approach is validated.

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
- Public API Javadocs explicitly describe nullability semantics (whether null is accepted/returned and what happens otherwise), leveraging annotations that render in Javadoc; include short examples where behavior could be ambiguous (e.g., empty vs. null returns).

## Pros and Cons of the Options

### General JSpecify considerations (applies to options 2–4)

#### Pros

- Public API contracts become explicit; IDEs/analyzers can detect violations earlier.
- Alignment with tooling and documentation; annotations surface in Javadoc and IDE tooltips.
- Can reduce redundant runtime null checks on hot paths once confidence is established.

#### Cons

- Requires migration effort and contributor familiarity with annotations and tool configuration.
- Potential annotation noise without sensible defaults (e.g., package-level `@NullMarked`).

#### Neutral

- Neutral, because a compile-only dependency means no runtime/transitive impact.
- Neutral, because adding annotations is binary-safe; changing contracts (nullable ↔ non-null) is a behavioral change.
- Neutral, because conventions for collections/Optional element nullability can be established.

### Status Quo — Runtime checks only, no nullability annotations

#### Pros

- No immediate migration cost; preserves current behavior and CI setup.

#### Cons

- Ambiguity about nullability persists; limited design-time guidance and tool-assisted safety.
- Runtime overhead from defensive checks remains.

#### Neutral

- Neutral, because runtime checks can still catch nulls at module boundaries.

### JSpecify for New Public APIs Only

#### Pros

- Minimizes churn while adding clarity for newly added surfaces.
- Allows gradual reduction of runtime checks on hot paths for new code.

#### Cons

- Existing APIs remain ambiguous for a long time; inconsistent experience for users.

#### Neutral

- Neutral, because mixed coverage can be acceptable temporarily during transition.

### JSpecify for All Public APIs

#### Pros

- Clear, consistent contracts at API boundaries where they matter most.
- Consistent experience for users across public APIs.

#### Cons

- Requires a focused sweep of public APIs and careful handling where exception semantics must be preserved.

#### Neutral

- Neutral, because internals can adopt annotations gradually.

### JSpecify Project-Wide

#### Pros

- Maximizes clarity and static analysis coverage across the codebase.

#### Cons

- Highest migration cost and risk of initial noise/false positives.

#### Neutral

- Neutral, because it may be a long-term aspiration after initial adoption on public APIs.

### Defer Adoption — Revisit after a defined period/criteria

#### Pros

- Avoids immediate migration cost while gathering more ecosystem/tooling data.

#### Cons

- Delays benefits to users; increases future migration burden.

#### Neutral

- Neutral, because this is distinct from Status Quo: an explicit, time-bound decision to postpone, with revisit triggers (date, tooling maturity, or dependency adoption milestones).

### Use Another Annotation Framework — JetBrains, Checker Framework, SpotBugs/FindBugs, JSR 305, Eclipse JDT

#### Pros

- Familiar options with existing tooling in some ecosystems; Checker Framework offers powerful type-checking.

#### Cons

- Potential divergence from emerging ecosystem consensus; mixed semantics across frameworks.
- JSR 305 is legacy/unmaintained and may cause ambiguity in tooling behavior.

#### Neutral

- Neutral, because per-module interop may require a particular framework.

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
