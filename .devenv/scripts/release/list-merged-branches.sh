#!/usr/bin/env bash
# List the branches of merged PRs in a release milestone, with links, so a human
# can review and delete them.
#
# READ-ONLY: this script never deletes anything. It only queries GitHub and prints
# a table. Deleting branches is always a human decision.
#
# Usage:
#   list-merged-branches.sh "<MILESTONE>" [--repo OWNER/REPO]
#
# Example:
#   list-merged-branches.sh "2.1.2"
#   list-merged-branches.sh "Release 2.2.0" --repo operaton/operaton
#
# Requirements: gh (authenticated with access to the repo), jq.

set -euo pipefail

REPO="operaton/operaton"
MILESTONE=""

for arg in "$@"; do
  case "$arg" in
    --repo=*) REPO="${arg#*=}" ;;
    --repo) shift; REPO="${1:-}" ;;
    --help|-h) sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    -*) echo "Unknown option: $arg" >&2; exit 2 ;;
    *) MILESTONE="$arg" ;;
  esac
done

if [[ -z "$MILESTONE" ]]; then
  echo "ERROR: milestone is required. Try --help." >&2
  exit 2
fi

command -v gh >/dev/null || { echo "ERROR: gh CLI not found." >&2; exit 1; }
command -v jq >/dev/null || { echo "ERROR: jq not found." >&2; exit 1; }

echo "Merged PRs in milestone '$MILESTONE' on $REPO" >&2
echo >&2

# All branch names that currently exist on the upstream repo (for the "still there?" column).
EXISTING="$(gh api --paginate "repos/$REPO/branches" --jq '.[].name' 2>/dev/null || true)"

branch_exists() { grep -qxF "$1" <<<"$EXISTING"; }

# Merged PRs in the milestone. headRepositoryOwner.login differs from the repo owner
# for PRs that came from a contributor fork — those branches are NOT ours to delete.
PRS="$(gh pr list --repo "$REPO" --state merged \
  --search "milestone:\"$MILESTONE\"" \
  --json number,title,headRefName,url,headRepositoryOwner -L 200)"

OWNER="${REPO%%/*}"
COUNT="$(jq 'length' <<<"$PRS")"

if [[ "$COUNT" -eq 0 ]]; then
  echo "No merged PRs found for milestone '$MILESTONE'." >&2
  echo "(Check the exact milestone title with: gh api repos/$REPO/milestones --jq '.[].title')" >&2
  exit 0
fi

echo "| PR | Title | Branch | From fork? | Still on remote? | Branch link |"
echo "|----|-------|--------|-----------|------------------|-------------|"

jq -r '.[] | [.number, .title, .headRefName, .url, .headRepositoryOwner.login] | @tsv' <<<"$PRS" \
| while IFS=$'\t' read -r num title branch url headowner; do
    if [[ "$headowner" != "$OWNER" && -n "$headowner" ]]; then
      fork="yes ($headowner)"
      exists="n/a (fork)"
      link="—"
    else
      fork="no"
      if branch_exists "$branch"; then
        exists="yes"
        link="https://github.com/$REPO/tree/$branch"
      else
        exists="no (already gone)"
        link="—"
      fi
    fi
    # Escape pipe chars in titles so the markdown table stays intact.
    title="${title//|/\\|}"
    echo "| #$num | $title | \`$branch\` | $fork | $exists | $link |"
  done

cat >&2 <<'EOF'

----------------------------------------------------------------------
This list is for a human to act on. This script does NOT delete branches.
To delete a reviewed branch yourself:
  gh api -X DELETE repos/OWNER/REPO/git/refs/heads/BRANCH
Fork branches (From fork? = yes) belong to contributors — do not delete them.
EOF
