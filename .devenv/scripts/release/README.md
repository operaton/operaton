# Directory `release`

Helpers for the **post-release announcement** phase (the ANNOUNCE action of the
`operaton-release` skill). Run from the repository root.

These scripts prepare and list; they never publish or delete. Posting to Slack /
forum and deleting branches stay manual, human-confirmed steps.

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
