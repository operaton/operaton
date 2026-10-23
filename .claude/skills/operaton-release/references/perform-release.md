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
3. Update the documentation. In repository operaton/documentation, update the versions by executing thd script `.devenv/scripts/update-version.py`.

## Verification (after real run)
- [ ] GitHub Release created with the tag (`gh release view vX.Y.Z --repo operaton/operaton`). For `-Mx`/`-RCx`, it should be a prerelease.
- [ ] Update the release notes: Deduplicate contributor, remove bots & LLM users. Update also the RELEASE.md artifact.
- [ ] Artifacts on **Maven Central** (e.g. check `https://repo1.maven.org/maven2/org/operaton/bpm/operaton-engine/X.Y.Z/`).
- [ ] **SQL scripts** (final releases): the release log ("Maven Build" step) prints either
  `No database changes detected since last release tag vA.B.C.` or `Database changes detected …`. It must match the
  PREPARE schema check. If the schema changed, `operaton-sql-scripts:<schema>` is on Maven Central
  (`https://repo1.maven.org/maven2/org/operaton/bpm/distro/operaton-sql-scripts/<schema>/`), and the GitHub release has
  `operaton-sql-scripts-<schema>.tar.gz`. A schema artifact can be published only once — never re-deploy it by hand
  without the user's explicit decision.
- [ ] Next development version `-SNAPSHOT` committed back to `BRANCH` (`[releng] Bump version ...`).
- [ ] **Docker** build dispatched to `operaton/operaton-docker` (the workflow notifies it)
- [ ] Watch the Docker build
- [ ] Docker image pushed to Docker Hub (check `https://hub.docker.com/r/operaton/operaton/tags`).
- [ ] PR at `operaton/operaton-helm` is open for the new Operaton release version
- [ ] PR at `operaton/operaton-helm` is merged and the chart version is live on `https://operaton.github.io/operaton-helm/index.yaml`.
- [ ] Confirm `latest` / version tags appear on Docker Hub.
- [ ] Snapshot pre-release tags cleaned up.

## Report
List: run URL + result, release URL, Maven Central status, Docker status, next-version commit. Note anything the workflow left for manual follow-up (announcements, `changelog.tpl` reset — these are post-release items, not done here).

Once the release is verified live, the post-release announcement is the **ANNOUNCE** action — see `announce.md` (website + blog, Slack, forum, branch-cleanup list, housekeeping).
