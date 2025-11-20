---
status: "Proposed"
date: 2025-01-20
decision-makers: []
consulted: []
informed: []
---

# Version Tag-Based Event Subscription Management

## Context and Problem Statement

Organizations often use version tags to bundle process definitions and decision definitions together into logical groupings (e.g., "case definitions" in a specific business domain). When multiple versions of a process definition exist with different version tags, message correlation becomes problematic because Operaton currently only maintains event subscriptions for the latest version of each process definition key.

When a new process definition version is deployed, event subscriptions for older versions are automatically removed. This creates two critical issues:

1. **Version tag isolation is broken**: Messages cannot be correlated to specific versions identified by version tags, making it impossible to maintain separate bundles of process definitions that should operate independently.

2. **Accidental subscription removal**: If someone deploys a process definition with the same key but a different version tag (perhaps for a completely different use case), event subscriptions for the previous version are deleted, leaving existing "case definitions" in a broken state where they can no longer receive messages.

Related issue: [operaton/operaton#1559](https://github.com/operaton/operaton/issues/1559)

## Decision Drivers

- Need to support parallel deployment of multiple process definition versions with different version tags
- Maintain backward compatibility with existing message correlation behavior
- Prevent accidental breaking of message correlation when deploying new process versions
- Enable version tag-based message routing for business domain isolation
- Avoid requiring users to implement custom message correlation outside of Operaton (which increases upgrade risk and maintenance burden)
- Ensure database schema changes are backward compatible and don't impact existing deployments

## Considered Options

1. **Preserve all event subscriptions with version tag filtering** — Add version tag column to event subscription table, preserve subscriptions for all versions, and allow optional version tag filtering in message correlation
2. **Configurable subscription cleanup** — Add a configuration flag to toggle whether old subscriptions are removed, without version tag filtering
3. **Process definition key namespacing** — Require users to include version tag in process definition keys to ensure uniqueness
4. **Custom correlation service** — Provide extension points for users to implement custom message correlation logic
5. **Status quo** — Continue removing old subscriptions and only support correlation to latest version

## Decision Outcome

Chosen option: "**Preserve all event subscriptions with version tag filtering**", because it solves both the version tag isolation problem and the accidental subscription removal problem while maintaining backward compatibility. The solution provides explicit version tag filtering as an opt-in feature while preserving current behavior as the default.

### Consequences

- Good, because existing deployments continue to work with no configuration changes (latest version correlation remains default)
- Good, because version tag-based message routing becomes possible, enabling proper business domain isolation
- Good, because event subscriptions are no longer accidentally removed when new versions are deployed
- Good, because the solution aligns with how version tags are already used in process definitions
- Good, because users don't need to implement custom correlation logic outside Operaton
- Bad, because database schema requires migration to add `VERSION_TAG_` column to `ACT_RU_EVENT_SUBSCR` table
- Bad, because more event subscriptions will be retained in the database, increasing storage requirements
- Bad, because query complexity increases slightly when multiple subscriptions exist for the same event
- Neutral, because cleanup/retention policies for old subscriptions may need to be defined separately

### Confirmation

Implementation compliance will be confirmed through:

- Database migration scripts successfully add `VERSION_TAG_` column to `ACT_RU_EVENT_SUBSCR` for all supported databases (H2, PostgreSQL, MySQL, MariaDB, Oracle, DB2, SQL Server)
- Integration tests verify that event subscriptions are preserved when new process definition versions are deployed
- Integration tests verify that message correlation without version tag filter continues to correlate to latest version (backward compatibility)
- Integration tests verify that message correlation with version tag filter correctly routes to specified version
- Unit tests verify that `MessageCorrelationBuilder` API correctly accepts and applies version tag filter
- Code review confirms no breaking changes to existing message correlation APIs
- Documentation includes migration guide and examples of version tag-based correlation

## Pros and Cons of the Options

### Preserve all event subscriptions with version tag filtering

This option modifies the event subscription management to:
- Add `VERSION_TAG_` column to `ACT_RU_EVENT_SUBSCR` table
- Preserve event subscriptions when new process definition versions are deployed (instead of deleting them)
- Extend `MessageCorrelationBuilder` API to accept optional version tag parameter
- Default to correlating to latest version when no version tag is specified

- Good, because it solves both core problems (version tag isolation and accidental removal)
- Good, because backward compatibility is maintained (default behavior unchanged)
- Good, because it leverages existing version tag infrastructure
- Good, because API extension is natural and intuitive
- Neutral, because database migration is required but follows standard patterns
- Neutral, because cleanup policies for old subscriptions become a separate concern
- Bad, because storage requirements increase with retained subscriptions
- Bad, because queries need to handle multiple subscriptions per event name

### Configurable subscription cleanup

Add a process engine configuration flag (e.g., `removeOldEventSubscriptions`) that controls whether old subscriptions are removed when new versions are deployed.

- Good, because it prevents accidental subscription removal
- Good, because it requires minimal code changes
- Neutral, because it's simpler to implement than full version tag filtering
- Bad, because it doesn't solve the version tag routing problem
- Bad, because it's an all-or-nothing approach (either all versions or only latest)
- Bad, because users still need custom logic to route to specific versions

### Process definition key namespacing

Require or encourage users to include the version tag in the process definition key (e.g., `orderProcess-v1.0`, `orderProcess-v2.0`) to ensure uniqueness.

- Good, because it requires no engine changes
- Good, because it makes versioning explicit in the process definition key
- Neutral, because it follows a common naming pattern in some organizations
- Bad, because it doesn't align with how Operaton's version tag feature is designed to work
- Bad, because it forces a naming convention on users
- Bad, because it breaks existing deployments that use the same key across versions
- Bad, because it makes process definition management more cumbersome

### Custom correlation service

Provide extension points or a service provider interface (SPI) that allows users to implement custom message correlation logic.

- Good, because it provides maximum flexibility
- Good, because it doesn't require database schema changes
- Neutral, because it follows the SPI pattern used elsewhere in Operaton
- Bad, because it pushes complexity onto users
- Bad, because each user needs to implement and maintain correlation logic
- Bad, because custom implementations increase upgrade risk and testing burden
- Bad, because it doesn't solve the problem for the majority of users

### Status quo

Continue with current behavior where only the latest version's event subscriptions are maintained.

- Good, because it requires no changes and has no migration cost
- Good, because it keeps subscription table smaller
- Neutral, because it matches current documented behavior
- Bad, because it doesn't solve either problem raised in the issue
- Bad, because users must work around the limitation with custom solutions
- Bad, because version tag feature remains limited in usefulness for message-based processes

## More Information

- GitHub issue: [operaton/operaton#1559 - Version tag filtering for event subscriptions](https://github.com/operaton/operaton/issues/1559)
- Related tables:
  - `ACT_RU_EVENT_SUBSCR` — Runtime event subscriptions
  - `ACT_RE_PROCDEF` — Process definition repository (already has `VERSION_TAG_` column)
- Related APIs:
  - `MessageCorrelationBuilder` — API for correlating messages to processes
  - `RuntimeService.createMessageCorrelation()` — Entry point for message correlation
- Implementation considerations:
  - All database dialects must support the schema change
  - Migration scripts should be idempotent and safe to rerun
  - Consider index on `VERSION_TAG_` column for query performance
  - Document when to use version tag filtering vs. other correlation approaches
