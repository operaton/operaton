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
- [ ] Confirm version tags appear on Docker Hub, and `latest` points at the **highest** version released (see "Docker `latest` with several releases on one day").
- [ ] **Helm chart** published (see below): chart PR on `operaton/operaton-helm` opened, merged, and the new chart version listed in `https://operaton.github.io/operaton-helm/index.yaml`.
- [ ] Snapshot pre-release tags cleaned up.

## Docker `latest` with several releases on one day

Rule: when releases of different lines run on the same day (e.g. patch `2.1.6` and minor `2.2.0`), the Docker `latest`
tags must end up on the **highest** version only.

How tagging works: `release.yml` (post_release → "Notify Docker Repository") dispatches `build-test-and-publish.yml` on
`operaton/operaton-docker` with `version` and `snapshot=false`. Per image and Java variant, `release.sh` adds `latest`
when the version is higher than the highest release tag on Docker Hub **at check time** (`should_tag_latest`), then
pushes. The check and the push are not atomic: if the lower version checks before the higher one is pushed and pushes
after it, `latest` ends on the lower version. A lower-version push that lands *before* the higher one is harmless.

`release.yml` passes `tag_latest` to `operaton-docker` (operaton/operaton-docker#50, operaton/operaton#3941): `false`
when a newer `release/A.B.x` branch exists, otherwise `auto`. With that in place on the releasing branch, an older-line
release never tags `latest`; check the "Notify Docker Repository" log for `Newer release branch … not tagging 'latest'`.
Branches whose `release.yml` predates it (check: `git grep -n tag_latest origin/<BRANCH> -- .github/workflows/release.yml`)
still race — use the procedure below. Run the digest check in step 4 in every case.

Procedure when releasing several lines on the same day:
1. Dispatch in ascending version order (lower line first) — releases may run in parallel.
2. Watch both `operaton-docker` runs (`gh run list --repo operaton/operaton-docker --workflow build-test-and-publish.yml -L 5`);
   map each to its release via the time of the release run's "Notify Docker Repository" step.
3. If the higher version dispatches Docker while the lower version's Docker run is still in progress — or before it —
   cancel the lower version's Docker run (`gh run cancel <id> --repo operaton/operaton-docker`) and re-dispatch it once
   the higher version's Docker run has finished:
   ```bash
   gh workflow run build-test-and-publish.yml --repo operaton/operaton-docker -f version=X.Y.Z -f snapshot=false -f tag_latest=false
   ```
   (Omit `-f tag_latest=false` if operaton-docker#50 is not merged yet; the `latest` check then sees the higher version
   and skips `latest`.)
4. Verify for `operaton`, `tomcat`, `wildfly`: every `latest*` tag has the same digest as the matching
   `<HIGHER>*` tag:
   ```bash
   for r in operaton tomcat wildfly; do curl -s "https://hub.docker.com/v2/repositories/operaton/$r/tags?page_size=100" | python3 -c 'import json,sys; t={x["name"]:x.get("digest") for x in json.load(sys.stdin)["results"]}; v=sys.argv[2]; [print(sys.argv[1], n, "OK" if d==t.get(v+n[6:]) else "MISMATCH vs "+v+n[6:]) for n,d in sorted(t.items()) if n.startswith("latest")]' "$r" HIGHER; done
   ```

Re-dispatching after the fact does **not** repair a wrong `latest`: `release.sh` exits with "Not pushing already
released image" when the version tag exists. Repair needs a manual re-tag with Docker Hub push rights, e.g.
`docker buildx imagetools create -t operaton/<image>:latest operaton/<image>:<HIGHER>` (per Java variant) — ask the user.

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
5. Verify:
   ```bash
   git grep -n 'X.Y.0-SNAPSHOT' -- '*pom.xml' '*package.json' '*package-lock.json' \
     | grep -v '<operaton.version.current>'                                         # must be empty
   grep release-notes .github/jreleaser/changelog.tpl                               # new MAJOR_MINOR
   ```
   Expected leftover: `<operaton.version.current>X.Y.0-SNAPSHOT</operaton.version.current>` in the newest
   `qa/test-db-instance-migration/test-fixture-*/pom.xml`. It records the version that created the DB fixture and is set by
   `finalize-qa-db-fixture.sh` during the release — leave it unchanged. The script's closing
   `⚠️ The following files still contain the old version:` list includes that file for the same reason.
   Any other hit is a miss: fix it, re-run the check, and report it.
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
