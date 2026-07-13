# Release notes

Release notes live in the **operaton/documentation** repo under
`docs/documentation/reference/release-notes/<MM_>/index.md` (e.g. `2_1/`, `2_2/`).
The generation procedure is defined by the issue template
`.github/ISSUE_TEMPLATE/ai_release_notes.md` in that repo — read it live before generating; the summary below may drift.

## Template essence (operaton/documentation `ai_release_notes`)
Placeholders: `{MILESTONE}` (GitHub milestone), `{VERSION_TO_RELEASE}` (`<major>.<minor>`), `{LAST_RELEASE_TAG}` (`v<major>.<minor>.0`).

Use the existing `docs/.../release-notes/2_1/index.md` as the **structure template**. Content focus:
1. Issues/PRs labelled **`noteworthy`** in `{MILESTONE}` (primary source).
2. Code diff from `{LAST_RELEASE_TAG}`..HEAD — public **and** internal API changes.
3. Notable dependency upgrades (exclude test-only).
4. Server versions: Spring Boot / Spring Framework, Tomcat, Wildfly, Quarkus.
5. Enough detail + practical examples for users to understand the impact.

## By release type

### Minor / Milestone (`X.Y.0`, `X.Y.0-Mx`)
A release-notes page for `MM` must **exist**. If absent → generate a draft and open a **PR on operaton/documentation**. Missing required `.0` notes = 🔴 blocker for that release.

Steps:
1. Resolve `{MILESTONE}` and `{LAST_RELEASE_TAG}`.
2. Gather noteworthy items (`references/noteworthy.md`).
3. Diff `{LAST_RELEASE_TAG}..HEAD` on operaton/operaton for API/dependency changes (`gh api .../compare/...` or local `git log`).
4. Read `2_1/index.md` for structure; draft `MM_/index.md`.
5. Open a draft PR; list its URL.

### Patch (`X.Y.Z`, `Z>0`)
The `MM` page already exists. Decide whether this patch needs an **addition** (typically a "Patch releases" / bugfix subsection):
1. List what changed since the last patch tag: `git log vX.Y.(Z-1)..HEAD --oneline` on the release branch, plus merged PRs in the milestone.
2. If any change is user-facing (notable fix, security fix, dependency bump), add/extend the patch section in `MM_/index.md`.
3. If nothing user-facing → record "no release-notes change needed" (not a blocker).
4. Any addition → PR on operaton/documentation; list URL.

## changelog.tpl
`.github/jreleaser/changelog.tpl` in operaton/operaton must point to the right `release-notes/MM_/` URL. If wrong, fix via PR on operaton/operaton.

## PR conventions
- Branch name e.g. `release-notes/X.Y.Z`.
- Open as **draft** unless the user asks otherwise; release notes usually want human review.
- Use the Haiku model for mechanical gathering (commit/PR lists, version extraction); use the main model for the prose and the noteworthiness judgement.
