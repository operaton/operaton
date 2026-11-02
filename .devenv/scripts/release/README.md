# Directory `release`

Helpers for the `operaton-release` skill: release checks (PREPARE) and the
**post-release announcement** phase (ANNOUNCE). Run from the repository root.

These scripts prepare and list; they never publish or delete. Posting to Slack /
forum and deleting branches stay manual, human-confirmed steps.

## `check-db-schema.py`

Report database schema changes for an upcoming release (PREPARE). Compares the
branch with the previous final release (highest `vX.Y.Z` tag below the version;
milestone/RC tags ignored) and prints the schema version before/after, the
changed files in the engine `db` directory, whether the change is version-only
or structural, and the commits with their PRs and closed issues.
**Read-only**, uses only the GitHub API through `gh` (works in shallow clones).

```bash
.devenv/scripts/release/check-db-schema.py --branch release/2.2.x --version 2.2.1
```

Exit code `0` = no schema change, `2` = schema changed (read the verdict),
`1` = error.

## `list-merged-branches.sh`

List the branches of merged PRs in a release milestone, with links, so a human
can review and delete them. **Read-only — never deletes anything.**

```bash
.devenv/scripts/release/list-merged-branches.sh "<MILESTONE>" [--repo OWNER/REPO]
```

Output is a markdown table: PR number/title, head branch, whether the PR came
from a contributor fork (those branches are not ours to delete), whether the
branch still exists on the remote, and a link. Requires `gh` and `jq`.

## `update-website.py`

Update the operaton.org marketing site for an announcement: bump the
`#changelog` card in `index.html` and scaffold a blog post under `_posts/` from
the most recent `*-released.md` post. Edits a local operaton.org checkout; does
**not** commit, push, or open a PR.

```bash
.devenv/scripts/release/update-website.py --version 2.1.2 \
  --summary "Maintenance release with bug fixes and dependency updates." \
  [--repo ~/Development/operaton/operaton.org]
```

When multiple releases ship the same day, pass the **highest** version
(`--version`); the single changelog card always shows the highest. Cover the
other lines in the blog prose. Run with `--help` for all options.
