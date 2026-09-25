---
name: operaton:adr
description: Use when working with an Architectural Decision Record in the Operaton repository - drafting or proposing an ADR, opening an ADR discussion pad or issue, promoting an agreed draft into docs/decisions/, or reviewing an existing ADR or ADR pull request.
---

# Managing Operaton ADRs

Operaton records architectural decisions as MADR files in `docs/decisions/`.
The process (`docs/decisions/README.md`) is **discussion first, decision last**:
a pad and an issue come first, the numbered file comes only after agreement.

Read `docs/decisions/README.md` and `docs/decisions/0001-null-safety-strategy-for-operaton.md`
before drafting. 0001 is the quality bar for tone and depth.

## Pick the workflow

| Situation | Workflow |
|---|---|
| New decision to discuss | **Create a draft** |
| Pad discussion reached agreement | **Promote to ADR** |
| Reviewing an ADR or ADR PR | **Review** |

## Workflow: Create a draft

### 1. Interview before writing

Do not draft from a one-line request. Ask the user, and read the code, until you can answer:

- What is the actual problem, in terms a reader will understand in three years?
- What are the decision drivers (forces, constraints, quality goals)?
- What are the real options — including the status quo and at least one the team might reject?
- Who are the `decision-makers`, `consulted`, `informed`?
- What evidence exists (issues, forum threads, benchmarks, code)?

Ground every claim in something you verified. 0001 cites forum threads, issues and specs — match that.

### 2. Write the draft body

Follow `docs/decisions/adr-template.md` section order. Two differences from a finished ADR:

**Omit YAML frontmatter.** The pad's markdown renderer turns `---` into a heading. Put the
metadata as a plain list under the title instead; real frontmatter is added at promote time.

**The Decision Outcome section is this text, verbatim:**

```markdown
## Decision Outcome

> **This ADR is a draft. No decision has been made yet.**
> The decision is taken when this draft is proposed, after the discussion in the
> pad and issue linked below has converged. Until then this section stays as is.
```

Everything else — Context and Problem Statement, Decision Drivers, Considered Options,
Pros and Cons of the Options, More Information — is written in full. `Consequences` and
`Confirmation` are subsections of Decision Outcome, so they are also left out of the draft.

Write the draft to a scratchpad file. **Do not create a file in `docs/decisions/`** — the
4-digit number is assigned at PR time, not now.

### 3. Create the pad

```bash
python3 .claude/skills/managing-adrs/etherpad.py create <shortdesc> --file <draft.md>
```

Prints the URL and deletion token and records both in `~/.operaton/etherpad-docs.md`.
`<shortdesc>` is kebab-case with no number prefix; the script adds the `Operaton-ADR-` prefix
and the `-365days` durability suffix.

Use the script, not raw curl. yopad issues the deletion token **once, to the connection that
creates the pad** — any other creation path loses it permanently and the pad becomes
undeletable.

Other subcommands, all with positional arguments (same `python3 .claude/skills/managing-adrs/etherpad.py` prefix):

```bash
import <pad> <file>   # overwrite pad content
export <pad>          # print pad as markdown
delete <pad>          # token looked up in the registry
list                  # registry with days-to-expiry
```

### 4. Open the issue

```bash
GITHUB_TOKEN= gh issue create --repo operaton/operaton \
  --label "documentation:adr" \
  --title "ADR: <short title>" \
  --body "$(cat <<'EOF'
Discussion pad: <pad URL>

Following the ADR process in docs/decisions/README.md. The full draft is in the pad;
please comment there.

## Context and Problem Statement

<copied verbatim from the draft>

## Considered Options

<copied verbatim from the draft>
EOF
)"
```

`GITHUB_TOKEN=` is required: an invalid `GITHUB_TOKEN` in the environment shadows the working
keyring login. Labels are managed declaratively in `.github/labels/labels.yml` — if
`documentation:adr` is missing from the repo, report that and stop; do not run `gh label create`.

Then add the issue URL to the pad text and to the registry row.

### 5. Report

Give the user the pad URL, the issue URL, and the registry path.

## Workflow: Promote to ADR

1. `etherpad.py export <pad>` to pull the agreed content.
2. Assign the next free 4-digit number from `docs/decisions/`.
3. Write `docs/decisions/NNNN-<shortdesc>.md` with real YAML frontmatter
   (`status: "Proposed"`, `date`, `decision-makers`, `consulted`, `informed`).
4. Replace the draft notice with the real Decision Outcome, plus Consequences and Confirmation.
5. Open a PR linking the issue. Merge closes the issue.

Two core contributors other than the author must approve, and no veto may stand.

## Workflow: Review

Apply `docs/decisions/README.md` Appendix B. Check that every decision driver and considered
option is documented, consequences are honest on both sides, traceability holds (linked issues,
status, stakeholders), and external links are absolute URLs. Challenge assumptions with
project-relevant reasoning; do not review on personal preference or formatting taste.

## Common mistakes

| Mistake | Correct |
|---|---|
| Draft names a chosen option | Draft carries the verbatim draft notice; the decision is made when it is proposed |
| `curl` the pad URL to create it | `etherpad.py create` — otherwise the deletion token is lost forever |
| `-1year` durability suffix | `-365days` |
| `/export/txt` | `/export/markdown` — txt is 404 on this instance |
| YAML frontmatter in the pad | Plain list in the pad; frontmatter added at promote time |
| `docs/decisions/NNNN-*.md` written at draft time | Number assigned at PR time, after agreement |
| `gh` fails with bad credentials | Prefix with `GITHUB_TOKEN=` |
| Committing `~/.operaton/etherpad-docs.md` | Local only — it holds deletion tokens |
