---
name: operaton-release
description: Use when preparing, performing, or announcing an Operaton release. Triggers include "prepare release X.Y.Z", "prepare the upcoming releases", "perform release X.Y.Z", "X.Y.Z-Mx", and "announce release X.Y.Z" / "announce the release(s)". Use for release pre-flight checks, build/Docker-image/documentation status, noteworthy-label and milestone hygiene, distribution smoke tests, release notes, go/no-go confidence votes, driving the release.yml workflow, and post-release announcements (website, blog, Slack, forum) plus branch-cleanup listing.
---

# Operaton Release

Three actions over the Operaton release process, sharing one knowledge base.

| Action | Trigger phrases | What it does | Releases anything? |
|--------|-----------------|--------------|--------------------|
| **PREPARE** | "prepare release X.Y.Z", "prepare the upcoming releases" | Pre-flight checks → fixes via PRs → **go/no-go confidence vote** | **NEVER** |
| **PERFORM** | "perform release X.Y.Z", "perform release X.Y.Z-Mx" | Dispatches `release.yml` (dry-run first), then verifies | Yes (gated) |
| **ANNOUNCE** | "announce release X.Y.Z", "announce the release(s)" | Post-release: website + blog PR, Slack, forum, branch-cleanup list, housekeeping | No (outward-facing; gated by confirmation) |
move
## The Iron Guardrail

**PREPARE must NEVER trigger a release.** It must not run `gh workflow run release.yml`, `mvnw ... deploy`, `jreleaser`, push tags, or anything that publishes. PREPARE only reads state and raises PRs for documentation/label/config fixes. If you catch yourself about to dispatch the release workflow during a "prepare" request — STOP. The user must explicitly say "perform".

PERFORM is outward-facing and hard to reverse. Always dry-run first, always confirm with the user before the real run.

## First step (both actions): resolve the target

1. **Determine version(s).**
   - Explicit ("prepare release 2.1.2") → use it.
   - "the upcoming releases" → read the calendar (`references/calendar.md`), take events dated from today within the next ~14 days.
2. **Resolve branch + type** for each version `MAJOR.MINOR.PATCH[-QUALIFIER]`:
   - `PATCH > 0` → **patch release**, branch `release/MAJOR.MINOR.x` (must exist).
   - `PATCH == 0`, no qualifier → **minor release**, branch `release/MAJOR.MINOR.x` if it exists, else `main`.
   - Qualifier `-M#` / `-RC#` → **milestone/preliminary release** of a `.0`, from `main` (or its release branch if one exists). Pass as `preliminary_release_qualifier`.
3. **Sanity check:** the branch's `pom.xml` version must equal the target base version + `-SNAPSHOT`. Mismatch = blocker, report it.

For multiple releases, process each independently and give a **separate confidence vote per release**.

## PREPARE workflow

Work the checklist in `references/prepare-checklist.md`. Create a TodoWrite item per release per major check. Summary of phases:

1. **Branch & version** — resolve as above; confirm pom version.
2. **Build status** — `build.yml`, `integration-build.yml`, `migration-test.yml` green on the branch (`gh run list`).
3. **Docker images** — `operaton/operaton`, `operaton/wildfly`, `operaton/tomcat` SNAPSHOT images current on Docker Hub.
4. **Dependencies** — no open HIGH/CRITICAL Dependabot alerts (except legacy webapp deps).
5. **Label & milestone hygiene** — closed issues/PRs without milestone; merged PRs in the milestone that look noteworthy but lack the `noteworthy` label. See `references/noteworthy.md`.
6. **Documentation** — release notes correct for the type. See `references/release-notes.md`.
7. **Smoke tests** — hybrid. See `references/smoke-tests.md`.
8. **Raise PRs** for every fixable gap (docs, labels, config). Collect links.
9. **Confidence vote** — see rubric below.

Use the **Haiku** model (via `Agent` with `model: haiku`) for the mechanical sub-tasks: scanning issues/PRs for missing milestones/labels, parsing build-run lists, formatting checklists, diffing release-note sections. Reserve the main model for judgement (noteworthiness, the confidence vote, release-note prose).

### Confidence vote rubric (per release)

End every PREPARE run with an explicit verdict:

- 🟢 **GO** — all blocking checks pass; only non-blocking polish remains.
- 🟡 **CONDITIONAL** — releasable after listed fixes; name each item and whether a PR was raised.
- 🔴 **NO-GO** — a blocking check fails (build red, version mismatch, missing required release notes for a `.0`, open HIGH/CRITICAL alert). State exactly what blocks it.

Always list: blocking items, non-blocking items, PRs raised (with links), and the remaining manual work the human must do.

## PERFORM workflow

See `references/perform-release.md`. In short:

1. **Require a green PREPARE.** If none was run this session, run PREPARE first. Refuse on 🔴.
2. **Confirm** the version, branch, and qualifier with the user explicitly.
3. **Dry run:** `gh workflow run release.yml --ref <branch> -f dry_run=true [-f preliminary_release_qualifier=M1]`. Monitor to green.
4. **Confirm again**, then real run with `-f dry_run=false`.
5. **Verify:** GitHub release created, Maven Central artifacts present, `operaton-docker` build dispatched / `latest` tag updated, next `-SNAPSHOT` version committed.
6. **Report** run URLs and verification results.

## ANNOUNCE workflow

See `references/announce.md`. **Only after PERFORM verified the release is live.** Every channel is public and irreversible — draft, show the user, post only on explicit confirmation. In short:

1. **Website** (`operaton/operaton.org`, PR) — update the `index.html` `#changelog` card to the **highest** version released that day; write a blog post in `_posts/` modelled on `2026-04-24-operaton-2-1-released.md`. Scaffolds via `.devenv/scripts/release/update-website.py`.
2. **Slack** `general` — compact `@channel` post with highlights + release-notes link. Draft → confirm → post.
3. **Forum** (forum.operaton.org, "Announcement" category) — fuller markdown version. Draft → confirm → post.
4. **Branch cleanup** — `.devenv/scripts/release/list-merged-branches.sh "<MILESTONE>"` lists merged branches with links. **List only — you MUST NOT delete any branch.** Hand the list to the user.
5. **Housekeeping** — `jreleaser` `previousTagName` / `changelog.tpl`, close milestone + open next, calendar, docs/download verification, optional wider social.

## Output

PREPARE and PERFORM end with a structured report: per-release status, confidence vote (PREPARE) or verification results (PERFORM), and a flat list of every PR raised with its URL. ANNOUNCE ends with: links to the website PR, the Slack/forum posts (or drafts awaiting the user), and the merged-branch list for the user to delete.

## Red flags — STOP

- About to `gh workflow run release.yml` during a "prepare" request → **STOP**, that's PERFORM.
- About to give a 🟢 GO without having actually run the build/image/doc checks → verify first, evidence before assertion.
- About to run PERFORM with `dry_run=false` without a prior dry-run or user confirmation → STOP.
- Branch pom version ≠ target version → blocker, do not proceed.
- About to delete a branch during ANNOUNCE (`git push origin --delete`, `git branch -D`) → **STOP.** List branches only; the human deletes them.
- About to post to Slack `@channel` or the forum without showing the user the draft first → STOP, draft and confirm.
- About to announce a release whose GitHub release / Maven Central / release-notes page is not yet live → STOP, verify the links resolve first.
