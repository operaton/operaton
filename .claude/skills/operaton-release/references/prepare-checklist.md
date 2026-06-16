# PREPARE checklist (detailed)

Mirrors the GitHub issue template `.github/ISSUE_TEMPLATE/release_checklist.md`. Run every check; record evidence (the command output or URL) for each. Use the **Haiku** model for the mechanical scans.

Throughout, `BRANCH` = the resolved release branch, `VER` = target version (e.g. `2.1.2`), `MM` = `MAJOR.MINOR` (e.g. `2.1`).

## 1. Branch & version
- [ ] `BRANCH` exists: `git ls-remote --heads origin BRANCH`
- [ ] pom version on branch == `VER-SNAPSHOT`:
  `gh api repos/operaton/operaton/contents/pom.xml?ref=BRANCH -q .content | base64 -d | grep -m1 '<version>'`
  (mismatch → 🔴 blocker)

## 2. Build status (must be green on BRANCH)
```
gh run list --repo operaton/operaton --branch BRANCH --workflow build.yml -L 1
gh run list --repo operaton/operaton --branch BRANCH --workflow integration-build.yml -L 1
gh run list --repo operaton/operaton --branch BRANCH --workflow migration-test.yml -L 1
```
- [ ] `build.yml` latest conclusion = success
- [ ] `integration-build.yml` latest = success
- [ ] `migration-test.yml` latest = success

A red required build is a 🔴 blocker.

## 3. Docker images current on Docker Hub
For each repo `operaton`, `wildfly`, `tomcat`, check the SNAPSHOT tag was pushed recently (today/yesterday):
```
curl -s "https://hub.docker.com/v2/repositories/operaton/operaton/tags/SNAPSHOT" | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d["last_updated"])'
```
- [ ] `operaton/operaton:SNAPSHOT` last_updated recent
- [ ] `operaton/wildfly:SNAPSHOT` recent
- [ ] `operaton/tomcat:SNAPSHOT` recent

Stale images (older than the latest green build) → 🟡, note it.

## 4. Dependency / security
- [ ] No open HIGH/CRITICAL Dependabot alerts (legacy webapp deps excepted):
  `gh api repos/operaton/operaton/dependabot/alerts -X GET -f state=open -q '[.[]|select(.security_advisory.severity=="high" or .security_advisory.severity=="critical")]|length'`
- [ ] (Milestone/minor only) dependency-updates report sane — flag if Spring/Quarkus/Tomcat/Wildfly need bumping. Heavy; for patches, skip unless asked.

## 5. Label & milestone hygiene
See `references/noteworthy.md`. Produces:
- [ ] Closed issues without milestone reviewed → set milestone where applicable (raise via `gh`).
- [ ] Closed/merged PRs without milestone reviewed → set milestone.
- [ ] Merged PRs in the milestone that look noteworthy but lack the `noteworthy` label → flagged to user (don't auto-label; needs judgement).

## 6. Documentation
See `references/release-notes.md`. Type-dependent:
- **Patch (`PATCH>0`)**: existing release notes for `MM` already exist. Determine whether the bug fixes / changes in this patch need an addition. If yes, raise a PR on `operaton/documentation`.
- **Minor/Milestone (`.0` / `-Mx`)**: release notes must exist per the `ai_release_notes` template. If missing → create draft PR. Missing required `.0` notes = 🔴 blocker.
- [ ] `changelog.tpl` points to the correct `release-notes/MM_` URL.
- [ ] Server versions documented (Spring Boot/Framework, Tomcat, Wildfly, Quarkus) for `.0`.

## 7. Smoke tests
See `references/smoke-tests.md` (hybrid: browser-drive Docker images, guided manual for OS distros).

## 8. Raise PRs
For every fixable gap (release-notes additions, changelog.tpl, dependabot.yml, config), create a branch + PR against the right repo. Never push directly to `main` or a `release/*` branch. Collect every PR URL for the final report.

## 9. Confidence vote
Apply the rubric in SKILL.md. One verdict per release with blocking items, non-blocking items, PRs raised, and remaining manual work.
