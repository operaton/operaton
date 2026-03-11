# Architectural Decision Records (ADRs) for Operaton

This folder contains Architectural Decision Records (ADRs) for the Operaton project. ADRs document important architectural decisions, their context, and their consequences. They help ensure transparency, consistency, and long-term traceability.

We use the [MADR format](https://adr.github.io/madr/) for writing ADRs.

## Table of Contents

- [When to Write an ADR](#when-to-write-an-adr)
- [ADR Creation Process](#adr-creation-process)
- [File Naming Convention](#file-naming-convention)
- [ADR Status Lifecycle](#adr-status-lifecycle)
- [Style and Tone](#style-and-tone)
- [Links and References](#links-and-references)
- [Diagram Support](#diagram-support)
- [Folder Structure](#folder-structure)
- [Issue Linking and Implementation Tracking](#issue-linking-and-implementation-tracking)
- [Appendix A: What Constitutes a Significant Architectural Change?](#appendix-a-what-constitutes-a-significant-architectural-change)
- [Appendix B: How to Review an ADR](#appendix-b-how-to-review-an-adr)
- [References](#references)

## When to Write an ADR

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

## ADR Creation Process

**Note:** The process of creating an ADR is part of future discussions and will be defined later.

## File Naming Convention

- Use a 4-digit prefix followed by a short kebab-case title
- If multiple ADRs are in flight, rebase and renumber as needed to maintain sequential order

Examples:

- `0001-drop-mariadb-support.md`
- `0010-new-api-auth.md`
- `0123-unify-logging.md`

## ADR Status Lifecycle

ADRs can have different statuses that indicate their current state:

| Status | Meaning |
| ------------ | -------------------------------------------------------- |
| Proposed | Under discussion; not yet approved |
| Accepted | Decision finalized and agreed upon |
| Rejected | Considered but not adopted |
| Superseded | Replaced by a newer ADR (include reference) |
| Deprecated | No longer relevant or applicable |

Status changes are made by updating the `status` field in the ADR's metadata section. When superseding an ADR, update both the old ADR (mark as superseded) and reference it in the new one.

## Style and Tone

ADRs are formal documentation that may be referenced years later. Write clearly and professionally.

### Writing Guidelines

Well-written ADRs help teams understand and revisit decisions years later. When creating your ADR:

**✅ Do:**

- Use simple, clear language. Assume future readers have no current context.
- Document the problem first, then the decision.
- Describe trade-offs and rejected alternatives honestly.
- Include meaningful consequences (both positive and negative).
- State who was involved (`decision-makers`, `consulted`, `informed`).
- Keep each ADR focused — one decision per file.
- Write in a clear, professional, and generally understandable tone.
- Avoid internal shorthand, abbreviations, or casual phrasing.
- Keep ADRs concise and focused. Aim for clarity over comprehensiveness.

**❌ Don't:**

- Avoid vague statements like "industry standard" without justification.
- Don't overcomplicate with excessive detail or jargon.
- Avoid putting implementation details in the decision section.
- Avoid creating ADRs after implementation solely to justify a pre-made decision.

### Context for Readers

Assume the reader may not be part of current discussions but needs to understand the rationale years later. Long ADRs are harder to review and maintain, so focus on the essential information that will remain relevant over time.

For detailed guidance, see the [References](#references) section.

## Links and References

When linking to external resources in ADRs:

- Use absolute URLs for links targeting URLs outside the decisions directory
- For GitHub issues, use the full URL (e.g., `https://github.com/operaton/operaton/issues/847`) instead of relative references (don't use simply `#847`).

This ensures links remain functional when ADRs are viewed outside the repository context or in different formats

## Diagram Support

You can use [Mermaid diagrams](https://mermaid.js.org/) in ADRs. GitHub supports rendering them directly in Markdown. See [GitHub's Mermaid documentation](https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/creating-diagrams) for details.

## Folder Structure

- `docs/decisions/`: All ADR files
- `docs/decisions/adr-template.md`: The MADR template
- `docs/decisions/assets/`: Optional images or diagrams

## Issue Linking and Implementation Tracking

To maintain clear traceability between ADRs and their implementation, consider one of the following approaches:

### Commit Alongside Implementation

Include the ADR in the same commit/PR as the code changes that implement it. This creates a direct link between the decision documentation and its implementation.

### Create Implementation Issues

Create separate GitHub issues for implementing the ADR (if applicable). Reference the ADR in these issues and link back to the implementation issues in the ADR itself. This approach is useful for larger changes that require multiple implementation phases.

### ADR Created During Implementation

When an ADR is created as part of an existing implementation issue to discuss and decide on architectural aspects, reference the original implementation issue in the ADR. This maintains the connection between the implementation work and the architectural decision that emerged during development.

## Appendix A: What Constitutes a Significant Architectural Change?

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

### Examples of Significant Changes

- Changes to the number of supported databases (e.g., dropping support for MariaDB)
- Introducing a new communication protocol or message format
- Altering BPMN/DMN parsing, validation, or execution logic
- Changes to process instance lifecycle or state management
- Updates to REST API endpoints that affect client compatibility
- Changes to database migration strategy or schema evolution

### Examples of Changes That Typically Don't Need an ADR

- Bug fixes or small feature additions that don't change behavior
- Fixing SonarQube issues or code quality improvements
- Refactoring that doesn't change public interfaces
- Updating versions of existing dependencies (unless breaking changes)
- Local optimizations that don't affect other components
- Minor UI/UX improvements in webapps
- Documentation updates or test improvements

## Appendix B: How to Review an ADR

Reviewing ADRs is not about personal taste — it's about improving the clarity, completeness, and relevance of architectural decisions. Keep the following best practices in mind:

### ✅ Do

- Ask clarifying questions to improve context and rationale.
- Challenge assumptions with constructive, project-relevant objections.
- Ensure all decision drivers and considered options are clearly documented.
- Check that consequences (both good and bad) are realistic and complete.
- Verify traceability (linked issues, status, stakeholders).
- Offer alternative suggestions only when they are grounded in objective reasoning.

### ❌ Don't

- Reject decisions based on personal preferences without justification.
- Accept vague reasons like "everyone uses it" or "sounds cool" without data.
- Focus on superficial formatting if clarity and structure are sufficient.
- Use non-technical arguments (e.g., "I don't like it" or "We never did it this way").

For detailed guidance, see the [References](#references) section.

## References

📖 *MADR (Markdown Architectural Decision Records)*  
<https://adr.github.io/madr/>

📖 *GitHub's Mermaid documentation*  
<https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/creating-diagrams>

📖 *How to create Architectural Decision Records (ADRs) — and how not to*  
<https://www.ozimmer.ch/practices/2023/04/03/ADRCreation.html>

📖 *How to review Architectural Decision Records (ADRs) — and how not to*  
<https://www.ozimmer.ch/practices/2023/04/05/ADRReview.html>
