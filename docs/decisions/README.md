# Architectural Decision Records (ADRs) for Operaton

This folder contains Architectural Decision Records (ADRs) for the Operaton project. ADRs document important architectural decisions, their context, and their consequences. They help ensure transparency, consistency, and long-term traceability.

We use the [MADR format](https://adr.github.io/madr/) for writing ADRs.

## When to write an ADR

Create an ADR when you are:

- Proposing a significant architectural change (see [Appendix A](#appendix-a-what-constitutes-a-significant-architectural-change) for detailed criteria)
- Making a decision that will impact multiple contributors or components
- Removing or replacing core dependencies or services
- Introducing new patterns or major technical approaches

Common examples include:

- Changes to supported databases or core dependencies
- Alterations to BPMN/DMN execution logic or APIs
- Modifications to process engine architecture or data models
- Introduction of breaking changes

## How to create a new ADR

1. Create or identify a GitHub issue: If no relevant issue exists, create a new GitHub issue to track the ADR discussion and decision-making process. If an existing issue covers the topic, you can use that issue instead. This provides a central place for initial discussion and helps maintain traceability.

2. Copy `adr-template.md` and rename it to the next available number and a short title, for example:

    `cp adr-template.md 0003-replace-event-bus.md`

3. Edit the new file and fill in the sections with your proposal, context, considered alternatives, and decision.

4. **Important**: In the metadata section at the top of the ADR, use GitHub IDs (e.g., `@username`) to identify:
   - `decision-makers`: Everyone involved in making the decision
   - `consulted`: Subject-matter experts whose opinions were sought
   - `informed`: People who are kept up-to-date on progress

   This ensures clear accountability and traceability of who was involved in the decision-making process. Use individual GitHub usernames rather than generic team aliases (e.g., `@johndoe` instead of `@team-backend`) for better accountability.

5. Submit a draft Pull Request with your ADR. Use the PR to discuss and refine the proposal with others. Reference the GitHub issue in your PR description.

6. Once consensus is reached, the PR can be approved and merged. The ADR is then considered accepted.

## File naming convention

- Use a 4-digit prefix followed by a short kebab-case title
- Examples
  - `0001-drop-mariadb-support.md`
  - `0010-new-api-auth.md`
  - `0123-unify-logging.md`

## ADR Status Lifecycle

ADRs can have different statuses that indicate their current state:

- **Proposed**: The ADR is drafted and under discussion (typically during PR review)
- **Accepted**: The decision has been made and the ADR is approved
- **Rejected**: The proposal was considered but ultimately not accepted
- **Superseded**: A newer ADR has replaced this decision (reference the superseding ADR)
- **Deprecated**: The decision is no longer relevant or has been phased out

Status changes are made by updating the `status` field in the ADR's metadata section. When superseding an ADR, update both the old ADR (mark as superseded) and reference it in the new one.

## Style and tone

ADRs are published as part of Operaton releases. The state of all ADRs at the time of a release (e.g. accepted, superseded) is considered frozen and becomes part of that version's reference documentation.

While an individual ADR can evolve after being merged (for example, its status can change to "superseded" or "amended"), its content and state at each release are treated as historical artifacts.

For this reason:

- Write ADRs in a clear, professional, and generally understandable tone.
- Avoid internal shorthand, abbreviations, or casual phrasing.
- Assume the reader may not be part of current discussions but needs to understand the rationale years later.
- Keep ADRs concise and focused. Aim for clarity over comprehensiveness. Long ADRs are harder to review and maintain.

## Links and references

When linking to external resources in ADRs:

- Use absolute URLs for links targeting URLs outside the decisions directory
- For GitHub issues, use the full URL (e.g., `https://github.com/operaton/operaton/issues/847`) instead of relative references (don't use simply `#847`).

This ensures links remain functional when ADRs are viewed outside the repository context or in different formats

## Diagram support

You can use [Mermaid diagrams](https://mermaid.js.org/) in ADRs. GitHub supports rendering them directly in Markdown. See [GitHub's Mermaid documentation](https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/creating-diagrams) for details.

## Folder structure

- `docs/decisions/`: All ADR files
- `docs/decisions/adr-template.md`: The MADR template
- `docs/decisions/assets/`: Optional images or diagrams

## Issue linking and implementation tracking

To maintain clear traceability between ADRs and their implementation, consider one of the following approaches:

### Commit alongside implementation

Include the ADR in the same commit/PR as the code changes that implement it. This creates a direct link between the decision documentation and its implementation.

### Create implementation issues

Create separate GitHub issues for implementing the ADR (if applicable). Reference the ADR in these issues and link back to the implementation issues in the ADR itself. This approach is useful for larger changes that require multiple implementation phases.

### ADR created during implementation

When an ADR is created as part of an existing implementation issue to discuss and decide on architectural aspects, reference the original implementation issue in the ADR. This maintains the connection between the implementation work and the architectural decision that emerged during development.

## Appendix A: What constitutes a significant architectural change?

A change is considered significant if it:

- Affects the overall system structure or component interactions
- Introduces or removes major dependencies (databases, frameworks, libraries)
- Changes core APIs or interfaces used by multiple components
- Modifies data flow, security models, or deployment patterns
- Has long-term implications that are difficult to reverse
- Requires coordination across multiple teams or components
- Affects performance, scalability, or operational characteristics significantly
- Introduces breaking changes to existing APIs, interfaces, or behavior
- Impacts BPMN/DMN execution behavior or compatibility
- Changes process engine core functionality or lifecycle management
- Affects database schema or persistence layer architecture
- Modifies job execution, task handling, or event processing mechanisms

### Examples of significant changes

- Changes to the number of supported databases (e.g., dropping support for MariaDB)
- Introducing a new communication protocol or message format
- Altering BPMN/DMN parsing, validation, or execution logic
- Changes to process instance lifecycle or state management
- Updates to REST API endpoints that affect client compatibility
- Changes to database migration strategy or schema evolution

### Examples of changes that typically don't need an ADR

- Bug fixes or small feature additions that don't change behavior
- Fixing SonarQube issues or code quality improvements
- Refactoring that doesn't change public interfaces
- Updating versions of existing dependencies (unless breaking changes)
- Local optimizations that don't affect other components
- Minor UI/UX improvements in webapps
- Documentation updates or test improvements
