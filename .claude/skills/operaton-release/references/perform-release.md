# PERFORM release

Drives `.github/workflows/release.yml` (which invokes JReleaser). **Only on an explicit "perform release ..." request.** Outward-facing and hard to reverse — confirm with the user at each gate.

## Preconditions (hard gates)
1. A **PREPARE** run for this version completed this session with a 🟢 GO (or 🟡 whose items the user accepts). If none was run → run PREPARE first. On 🔴 → refuse.
2. Resolve `BRANCH` + qualifier (see SKILL.md "resolve the target").
3. Working state: nothing to commit that would interfere; you are not pushing manually — the workflow does the version bump/commit.

## release.yml inputs (workflow_dispatch)
| Input | Meaning | Value |
|-------|---------|-------|
| `next_version` | next dev version (no `-SNAPSHOT`); empty = auto-compute from branch | usually leave empty |
| `preliminary_release_qualifier` | `M#` or `RC#`; only valid for `.0` versions | set for milestone/RC, else empty |
| `dry_run` | skips remote ops when true | **true first**, then false |

The released version = the branch's pom version minus `-SNAPSHOT` (+ qualifier). Auto next-version: `main` bumps minor, `release/*` bumps patch.

## Procedure
1. **Confirm** with the user: version, branch, qualifier, and that this will publish.
2. **Run:**
   ```bash
   gh workflow run release.yml --repo operaton/operaton --ref BRANCH \
     -f dry_run=false [-f preliminary_release_qualifier=M1]
   ```
   Find the run and watch it:
   ```bash
   gh run list --repo operaton/operaton --workflow release.yml -L 1
   gh run watch <run-id> --repo operaton/operaton
   ```

## Verification (after real run)
- [ ] GitHub Release created with the tag (`gh release view vX.Y.Z --repo operaton/operaton`). For `-Mx`/`-RCx`, it should be a prerelease.
- [ ] Artifacts on **Maven Central** (e.g. check `https://repo1.maven.org/maven2/org/operaton/bpm/operaton-engine/X.Y.Z/`).
- [ ] **Docker** build dispatched to `operaton/operaton-docker` (the workflow notifies it); confirm `latest` / version tags appear on Docker Hub.
- [ ] Next development version `-SNAPSHOT` committed back to `BRANCH` (`[releng] Bump version ...`).
- [ ] Snapshot pre-release tags cleaned up.

## Report
List: run URL + result, release URL, Maven Central status, Docker status, next-version commit. Note anything the workflow left for manual follow-up (announcements, `changelog.tpl` reset, `jreleaser.yml` `previousTagName` bump — these are post-release items, not done here).

Once the release is verified live, the post-release announcement is the **ANNOUNCE** action — see `announce.md` (website + blog, Slack, forum, branch-cleanup list, housekeeping).
