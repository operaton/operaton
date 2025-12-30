---
status: proposed
date: 2025-12-23
decision-makers: Operaton Core Maintainers
consulted: Operaton Engine Contributors, API Maintainers
informed: Operaton Community, Extension Authors
---

# Introduce Preview Features in Operaton

## Context and Problem Statement

Operaton evolves rapidly and follows a high-frequency release cadence with multiple releases per year. New functionality often benefits from early integration and real-world feedback, but committing prematurely to permanent compatibility guarantees increases long-term risk and integration effort.

At the same time, Operaton implements standards such as BPMN 2.0 and DMN. Changes in those areas must be introduced carefully to avoid inconsistent semantics, unexpected persistence effects, or partial standard coverage becoming de-facto permanent. This creates a natural tension with fast releases: we want rapid delivery but must avoid locking in brittle or underspecified interpretations of BPMN/DMN constructs.

The question to be answered is: **Should Operaton introduce a formal concept of Preview Features to allow early integration of new functionality without committing to permanent compatibility guarantees?**

In particular, we want to align the semantics of preview features with the principles defined in JEP 12 for Java language preview features: clearly scoped, explicitly opt-in, time-boxed, and without long-term compatibility guarantees.

### Definition and Scope

A **Preview Feature** is functionality that is available to users for early evaluation but:

* is **explicitly opt-in** (default is off),
* is **time-boxed** (must be promoted or removed), and
* carries **no compatibility guarantees** (signatures, behavior, configuration, or persistence details may change without deprecation; database state may not be migrated automatically on a new release or when switching the feature on/off).

The preview concept applies to public surfaces such as Java APIs, REST APIs, configuration properties, and end-user behavior. It must not be used as a substitute for incomplete bug fixes or as a way to ship breaking changes without communication.

## Decision Drivers

* Preserve long-term stability and compatibility guarantees
* Support a high-frequency release cadence
* Reduce integration effort and risk from long-lived feature branches
* Enable real-world validation of new functionality
* Clearly communicate feature maturity and intent
* Avoid fragmentation of the Operaton platform

## Considered Options

* Introduce Preview Features
* Preview via isolated modules/extensions only (new artifacts, no in-place changes)
* Label-only preview (documentation/release notes only, no opt-in)
* Do not introduce Preview Features

## Decision Outcome

Chosen option: "Introduce Preview Features", because it enables incremental integration aligned with Operaton’s release cadence while maintaining explicit compatibility expectations via an opt-in mechanism and a time box.

### Consequences

* Good, because features can be integrated earlier in smaller, well-defined increments.
* Good, because integration effort and merge risk are reduced.
* Good, because users can provide feedback before features become permanent.
* Bad, because users must consciously opt into impermanent functionality.
* Bad, because documentation and release notes require additional precision and maintenance.

### Confirmation

Compliance with this ADR can be confirmed by the following fitness functions:

* Every preview feature is clearly labeled as **Preview** in documentation and release notes.
* Every preview feature has an **explicit opt-in** that is **off by default** (for example via configuration).
* Each preview feature documents its **time box** and its **promotion/removal criteria**.
* Reviews verify that preview surfaces do not introduce permanent compatibility guarantees (for example: no promise of stable API/package names, stable REST contract, or stable persistence/schema behavior while in preview).
* A periodic review (at least once per release) decides whether each preview feature is promoted, extended explicitly, or removed.

## Pros and Cons of the Options

### Introduce Preview Features

Preview Features are implemented as specified by Operaton, with clearly defined semantics and documented scope. Full coverage of external standards (for example BPMN, DMN) is not required.

* Good, because it supports frequent releases without large late-stage integrations.
* Good, because partial or scoped standards support (for example selected DMN hit policies) can be validated early.
* Good, because feature intent and maturity are clearly communicated.
* Neutral, because not all features must be introduced as preview features.
* Bad, because preview features may change or be removed across releases.

### Preview via isolated modules/extensions only (new artifacts, no in-place changes)

Preview functionality is shipped only in isolated modules or extensions (new artifacts) so that existing engine and API surfaces remain unchanged.

* Good, because it minimizes risk of accidental adoption and protects the core engine surface.
* Good, because it can reduce the chance of partially implemented BPMN/DMN semantics leaking into stable behavior.
* Neutral, because it can still gather feedback from early adopters.
* Bad, because it increases packaging and dependency complexity.
* Bad, because it can delay integration into the real engine/runtime and create long-lived divergence.

### Label-only preview (documentation/release notes only, no opt-in)

Features are announced as “Preview” purely via communication, but are enabled by default and have no technical opt-in.

* Good, because it has minimal implementation overhead.
* Neutral, because it can still communicate maturity.
* Bad, because it weakens the stability/compatibility signal (users may adopt preview unintentionally).
* Bad, because it makes it harder to keep preview usage scoped and reversible.

### Do Not Introduce Preview Features

All features are introduced as permanent once merged.

* Good, because the platform surface remains simpler.
* Neutral, because early feedback can still be gathered via external discussions.
* Bad, because features must be complete before integration, increasing development and merge complexity.
* Bad, because delivery of new functionality is delayed.
* Bad, because late integration increases risk and maintenance cost.

## More Information

### Governance

* Owner: Operaton Core Maintainers.
* Preview features are reviewed at least once per release.
* When this ADR is accepted, the metadata `status` should be updated to `accepted` and `date` to the acceptance date.

### Time box and lifecycle

A preview feature should be time-boxed to a limited number of releases (for example: promotion or removal within two stable releases). Each preview feature must define:

* how to opt in,
* what “done” means for promotion to stable,
* what would cause removal, and
* which users/portable behaviors are expected to change while in preview.

This decision should be revisited if Operaton’s release cadence or compatibility guarantees change significantly.

## References

* JEP 12: <https://openjdk.org/jeps/12>
