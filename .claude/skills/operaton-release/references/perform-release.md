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
- [ ] Next development version `-SNAPSHOT` committed back to `BRANCH` (`[releng] Bump version ...`).
- [ ] **`main` version bumped** — REQUIRED when `X.Y.0` (no qualifier) was released from `release/X.Y.x`. See "Bump `main` after a minor release" below.
- [ ] **Docker** build dispatched to `operaton/operaton-docker` (the workflow notifies it)
- [ ] Watch the Docker build
- [ ] Docker image pushed to Docker Hub (check `https://hub.docker.com/r/operaton/operaton/tags`).
- [ ] Confirm `latest` / version tags appear on Docker Hub.
- [ ] **Helm chart** published (see below): chart PR on `operaton/operaton-helm` opened, merged, and the new chart version listed in `https://operaton.github.io/operaton-helm/index.yaml`.
- [ ] Snapshot pre-release tags cleaned up.

## Bump `main` after a minor release

`release.yml` only bumps the branch it ran on. Releasing `X.Y.0` from `release/X.Y.x` moves that branch to
`X.Y.1-SNAPSHOT`; `main` stays at `X.Y.0-SNAPSHOT` and must be moved to the next minor or major version by hand.

| Released | From | `main` action |
|----------|------|---------------|
| `X.Y.0` | `release/X.Y.x` | bump `main` (this section) |
| `X.Y.0` | `main` (no release branch) | none — `release.yml` already bumped `main` to `X.(Y+1).0-SNAPSHOT` |
| `X.Y.Z` (Z>0), `-Mx`, `-RCx` | any | none |

The target of `main` is always `X.(Y+1).0-SNAPSHOT` or `(X+1).0.0-SNAPSHOT` — never `X.Y.1-SNAPSHOT` (patch versions belong to `release/X.Y.x`).

1. Check `main`'s version:
   `gh api "repos/operaton/operaton/contents/pom.xml?ref=main" -q .content | base64 -d | grep -m1 '<version>'`.
   If it is already above `X.Y.0-SNAPSHOT`, skip this section.
2. **Ask the user** (AskUserQuestion) for the next version: `X.(Y+1).0-SNAPSHOT` (next minor, recommended default) or
   `(X+1).0.0-SNAPSHOT` (next major). Do not pick on your own.
3. Branch from `origin/main` (e.g. `chore/bump-version-X.(Y+1).0`), use a worktree if the current checkout is busy.
4. From the repository root run:
   ```bash
   .github/scripts/set-project-version.sh <NEXT>-SNAPSHOT
   ```
   It sets all reactor poms (`versions:set`), `package.json` + `package-lock.json` (needs `npm`), the non-reactor
   `distro/run/**` poms, and points `.github/jreleaser/changelog.tpl` at `release-notes/<NEXT_MAJOR>_<NEXT_MINOR>/`.
5. Verify — the script ends with `⚠️ The following files still contain the old version:` if anything was missed:
   ```bash
   git grep -l 'X.Y.0-SNAPSHOT' -- '*pom.xml' '*package.json' '*package-lock.json'   # must be empty
   grep release-notes .github/jreleaser/changelog.tpl                                 # new MAJOR_MINOR
   ```
   If files are listed, fix them, re-run the `git grep`, and report the miss (the script should not leave any).
6. Commit `chore(release): bump project version to <NEXT>-SNAPSHOT` and open a PR against `main`. Never push to `main`
   directly.
7. Make sure the GitHub milestone for `<NEXT>` (e.g. `2.3.0`) exists.

## Helm chart publishing (automated)

Final releases (not `-Mx`/`-RCx`, not dry runs) publish the Helm chart automatically:

1. `release.yml` step **Notify Helm Chart Repository** dispatches
   [`update-operaton-release.yaml`](https://github.com/operaton/operaton-helm/blob/main/.github/workflows/update-operaton-release.yaml)
   on `operaton/operaton-helm` with `operaton_version=X.Y.Z` (token secret `HELM_ACTION_DISPATCH_TOKEN`).
2. That workflow waits (max. 45 min) for `operaton/operaton:X.Y.Z` on Docker Hub, computes the chart version, sets
   `version`/`appVersion` in `charts/operaton/Chart.yaml` and `image.tag` in `values.yaml`, and opens a PR
   `release: Release chart version <C> for Operaton X.Y.Z` (branch `update-operaton-release/X.Y.Z`).
3. **Manual:** review and merge that PR. The push to `main` runs `test-and-release.yaml` (chart-releaser), which creates the
   `operaton-<C>` GitHub release and updates `index.yaml`.

Chart version rules: `X.Y.0` → minor bump of the current chart version (`1.1.0` → `1.2.0`). `X.Y.Z` (Z>0) → patch bump of
the chart that shipped `appVersion X.Y.(Z-1)`; if none exists, falls back to a minor bump.

Verify:
```bash
gh run list --repo operaton/operaton-helm --workflow update-operaton-release.yaml -L 1
gh pr list --repo operaton/operaton-helm --search "Operaton X.Y.Z in:title"
curl -s https://operaton.github.io/operaton-helm/index.yaml | grep -A1 'appVersion: X.Y.Z'
```

Caveats:
- The Helm step exists on `main` and branches cut from it (`release/2.2.x` onward). Older release branches (e.g.
  `release/2.1.x`) lack it — dispatch manually if a chart is wanted:
  `gh workflow run update-operaton-release.yaml --repo operaton/operaton-helm -f operaton_version=X.Y.Z`.
- The chart PR always targets the helm repo's `main`. Releasing a patch of an **older** line after a newer minor (e.g.
  2.1.6 after 2.2.0) would reset `main`'s `Chart.yaml` to the older `appVersion`. Merge the older line's chart PR first,
  or skip the chart for the older line.
- If the dispatch fails (HTTP ≠ 204, e.g. expired `HELM_ACTION_DISPATCH_TOKEN`), the `post_release` job stops **before**
  "Delete snapshot release", "Set next development version" and "Commit changes". Do not re-run the whole release: dispatch
  the Helm workflow manually, then re-run only the failed `post_release` job after fixing the token.

## Report
List: run URL + result, release URL, Maven Central status, Docker status, next-version commit. Note anything the workflow left for manual follow-up (announcements, `changelog.tpl` reset — these are post-release items, not done here).

Once the release is verified live, the post-release announcement is the **ANNOUNCE** action — see `announce.md` (website + blog, Slack, forum, branch-cleanup list, housekeeping).
