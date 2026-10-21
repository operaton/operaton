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

**Decide the general question, not the reported symptom.** An ADR is almost always triggered by
something concrete — a bug report, a support question, a review comment. That trigger is evidence,
not the subject. Write the Context so it states the underlying structural problem, and use the
trigger as one illustration among several. Concretely:

- The problem statement must hold even if the triggering issue had never been filed.
- Look for further instances before writing. One report usually means several occurrences; finding
  the others is what turns a bug into an architectural decision, and a single-instance ADR is
  usually a bug fix in disguise.
- Name the triggering issue once in the metadata, once where it illustrates a point, and once
  under More Information. If the issue number appears in every section, the framing is too narrow.
- Options and consequences are stated for the general rule, not for the triggering site.

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

### 2b. Link everything

An ADR is read by people who do not have the codebase open. Every factual claim must be one click
from its evidence.

**Link every reference**, without exception: issues, ADRs, specs, Javadoc, and **every Operaton
class, method, field and code snippet you name**. If you mention it, link it.

**Use a short label, not a URL, as the link text.** The label names what is being linked:

- `[DmnDecisionResultImpl.collectEntries()](…#L66)` for a method
- `[ExecutionEntity](…)` for a type
- `[issue 3713](https://github.com/operaton/operaton/issues/3713)` for an issue
- `[ADR-0001](…)` for another decision

Never paste a bare URL as the link text — long blob URLs inline destroy readability, which is the
one thing an ADR cannot afford.

**Code links are permalinks pinned to a commit, with a line anchor.** Resolve the hash first with
`git rev-parse origin/main`, confirm the files you cite are identical between that commit and your
working tree (`git diff --name-only <hash> HEAD`), and state the commit once near the top of the
draft:

```
https://github.com/operaton/operaton/blob/<short-hash>/<path>#L<line>
```

A 7-character short hash suffices. Never link to `main` or `HEAD` — line numbers rot.

**Verify every label and line number before publishing.** The label must match the declaration
that actually encloses the cited line; print each cited line and its enclosing method rather than
trusting a grep hit. A confidently wrong permalink is worse than none.

**`Good` / `Bad` / `Neutral` in the pros and cons lists are bold**: `- **Good**, because …`.

### 2c. Generate the pad variant, then apply the links

The canonical draft is GitHub-flavoured markdown. No yopad import path preserves a link, and
inline code is mangled. All of this is verified against the live instance:

| In the draft | What the import does with it |
|---|---|
| `[text](url)` | **URL silently discarded**, only `text` survives — no link |
| `<a href="…">` via HTML import | href stripped as well |
| `[text][ref]` plus definitions | definitions consumed, result has **zero** links |
| `` `code` `` | line split into an indented code block, wrecking the sentence and any list item |
| Tables | not rendered |
| A bare URL | autolinked — but then the label *is* the URL, which is what makes a draft unreadable |

Bold, italics, headings, blockquotes and bulleted lists survive intact. So **no tables anywhere in
an ADR draft** — use prose or a bulleted list with a **bold** lead-in.

Labelled links *are* possible: the editor stores them as a `url` attribute on a range of
characters. No importer creates that attribute, so it is applied in a second step:

```bash
python3 .claude/skills/operaton-adr/md2pad.py <draft.md>
# writes <draft>.pad.md (plain labels, no URLs) and <draft>.links.json (label -> url, in order)

python3 .claude/skills/operaton-adr/etherpad.py import <pad> <draft>.pad.md
python3 .claude/skills/operaton-adr/padlinks.py <pad> <draft>.links.json
```

`md2pad.py` also strips inline backticks and warns about tables, fenced blocks, reference-style
links and permalinks pointing at a branch. `padlinks.py` reports how many labels it linked and
names any it could not find — a label that spans a line break cannot be linked, so keep labels on
one line in the canonical draft.

Always run the two pad steps in that order and re-run **both** after every edit: importing
replaces the text and drops all attributes, so links must be re-applied afterwards. Never
hand-edit the pad copy; edit the canonical draft and regenerate.

Verify afterwards. The HTML export does **not** render these links, so check the stored
attributes instead of the export:

```bash
python3 - <<'EOF'
import importlib.util
spec = importlib.util.spec_from_file_location("ep", ".claude/skills/operaton-adr/etherpad.py")
ep = importlib.util.module_from_spec(spec); spec.loader.exec_module(ep)
ccv = ep._pump(ep._connect("<pad>"), "CLIENT_VARS")["data"]["collab_client_vars"]
pool = ccv["apool"]["numToAttrib"]
text = ccv["initialAttributedText"]["text"]
print("url attributes:", sum(1 for v in pool.values() if v[0] == "url"))
print("bare URLs left in text:", text.count("https://"))   # expect 0
EOF
```

### 3. Create the pad

```bash
python3 .claude/skills/operaton-adr/etherpad.py create <shortdesc> --file <draft>.pad.md
python3 .claude/skills/operaton-adr/padlinks.py <pad> <draft>.links.json   # links are a separate step
```

Prints the URL and deletion token and records both in `~/.operaton/etherpad-docs.md`.
`<shortdesc>` is kebab-case with no number prefix; the script adds the `Operaton-ADR-` prefix
and the `-365days` durability suffix.

Use the script, not raw curl. yopad issues the deletion token **once, to the connection that
creates the pad** — any other creation path loses it permanently and the pad becomes
undeletable.

yopad's namespace is global and shared with the whole internet. If `create` reports that the pad
already existed, the name is taken by someone else and **cannot** be managed by us; pick a more
specific `<shortdesc>` (appending the issue number works well) rather than reusing it.

Other subcommands, all with positional arguments (same `python3 .claude/skills/operaton-adr/etherpad.py` prefix):

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

The issue body is rendered by GitHub, not by the pad, so normal markdown (tables, `[text](url)`
links, inline code) is fine there — but keep the copied sections textually identical to the pad
apart from that formatting, so the two do not drift.

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
   (`status: "Proposed"`, `date`, `decision-makers`, `consulted`, `informed`). The file is now
   Start from the **canonical** draft, not the pad export: the canonical file already has the
   labelled links and inline code that GitHub renders, and the pad copy has lost them. Use the pad
   export only to pick up edits made during the discussion.
4. Replace the draft notice with the real Decision Outcome, plus Consequences and Confirmation.
5. Open a PR linking the issue. Merge closes the issue.

Two core contributors other than the author must approve, and no veto may stand.

## Workflow: Review

Apply `docs/decisions/README.md` Appendix B. Check that every decision driver and considered
option is documented, consequences are honest on both sides, traceability holds (linked issues,
status, stakeholders), and external links are absolute URLs. Challenge assumptions with
project-relevant reasoning; do not review on personal preference or formatting taste.

Also check the draft against steps 2, 2b and 2c: the decision is stated generally rather than as
a fix for the triggering issue; every class, method and issue mentioned is linked; links use short
labels and commit-pinned permalinks rather than branches; there are no tables; and the pros and
cons use bold **Good** / **Bad** / **Neutral**.

## Common mistakes

| Mistake | Correct |
|---|---|
| Draft names a chosen option | Draft carries the verbatim draft notice; the decision is made when it is proposed |
| ADR framed around the triggering issue | Decide the general question; the issue is one illustration |
| Only one instance of the problem cited | Search for the others first — one instance is a bug fix, not an ADR |
| Tables in the draft | Prose or bulleted lists — the pad cannot render tables |
| Hand-editing the pad copy | Edit the canonical draft, regenerate with `md2pad.py` |
| Re-importing and forgetting `padlinks.py` | Import replaces the text and drops every link attribute — always re-apply |
| Judging pad links by the HTML export | The export omits them; check the `url` entries in the attribute pool |
| Bare URL used as the link text | Short label: `[ExecutionEntity.getExecutions()](…#L704)` |
| Class, method or issue named without a link | Every mention gets a link so the reader can navigate |
| Permalink to `main`/`HEAD`, or an unverified line number | Pin to a commit hash; print the cited line and its enclosing declaration |
| Link label naming a method that does not enclose the cited line | Verify the label against the declaration above the line |
| Plain `Good,` / `Bad,` in pros and cons | `- **Good**, because …` |
| `curl` the pad URL to create it | `etherpad.py create` — otherwise the deletion token is lost forever |
| `-1year` durability suffix | `-365days` |
| `/export/txt` | `/export/markdown` — txt is 404 on this instance |
| YAML frontmatter in the pad | Plain list in the pad; frontmatter added at promote time |
| `docs/decisions/NNNN-*.md` written at draft time | Number assigned at PR time, after agreement |
| `gh` fails with bad credentials | Prefix with `GITHUB_TOKEN=` |
| Committing `~/.operaton/etherpad-docs.md` | Local only — it holds deletion tokens |
