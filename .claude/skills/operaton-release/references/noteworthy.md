# Noteworthy & milestone hygiene

Operaton automates changelog/release-note entries from the **`noteworthy`** label (see `.github/workflows/update-changelog-noteworthy.yml`). When a PR/issue is labelled `noteworthy`, a changelog entry is generated, categorised by its other labels:

| Label on item | Changelog category |
|---------------|--------------------|
| `bug`, `fix` | fixes |
| `enhancement`, `feature` | features |
| `documentation` | documentation |
| `backport` | backports |

So a user-facing change **without** `noteworthy` silently misses the release notes. PREPARE must catch these.

## A. Items missing a milestone
The checklist links these GitHub searches — reproduce with `gh`:
```
# Closed issues with no milestone
gh search issues --repo operaton/operaton --state closed --no-milestone -L 50
# Closed PRs with no milestone
gh search prs --repo operaton/operaton --state closed --no-milestone -L 50
```
For items that clearly belong to the release being prepared, set the milestone:
```
gh issue edit <N> --repo operaton/operaton --milestone "<MILESTONE>"
gh pr edit  <N> --repo operaton/operaton --milestone "<MILESTONE>"
```
(Editing labels/milestones is a direct API edit, not a PR — that's fine and expected.)

## B. Merged PRs missing the `noteworthy` tag
List merged PRs in the target milestone, then judge which look user-facing but lack `noteworthy`:
```
gh pr list --repo operaton/operaton --state merged --search "milestone:<MILESTONE>" \
  --json number,title,labels,url -L 100
```
Heuristic (use the main model, not Haiku, for the judgement call):
- **Likely noteworthy**: new feature, behaviour change, notable bug fix, dependency upgrade users care about, API change.
- **Likely not**: internal refactor, test-only, CI/build chore, typo fix, dependency bump of test-only deps.

**Do not auto-apply `noteworthy`** — it triggers changelog automation and is a judgement call. Instead, present a table of candidates (PR, title, why it might be noteworthy) and ask the user to confirm before labelling:
```
gh pr edit <N> --repo operaton/operaton --add-label noteworthy
```

## Output of this step
A list of: milestones set, and noteworthy candidates flagged (with the user's decision). Feed confirmed-noteworthy items into the release-notes step (`references/release-notes.md`).
