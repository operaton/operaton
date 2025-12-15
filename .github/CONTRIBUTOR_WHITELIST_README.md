# Contributor Whitelist

## Purpose

The `contributor-whitelist.txt` file contains a list of GitHub usernames who have already contributed to the Operaton repository. This whitelist is used by the first-time contributor workflow to skip greeting messages for existing contributors.

## Format

- One GitHub username per line
- Lines starting with `#` are treated as comments and ignored
- Usernames are case-sensitive
- Empty lines are ignored

## Maintenance

### Updating the Whitelist

When new contributors make their first commit to the repository, their GitHub username should be added to this list. You can generate an updated list of all contributors using this command:

```bash
cd /path/to/operaton
git log --format='%an' --all | sort -u
```

To get GitHub usernames from commits:

```bash
# This gets the committer name from GitHub (if available in commit metadata)
git log --format='%aN' --all | sort -u
```

### Adding a Single Contributor

To add a single contributor, simply add their GitHub username on a new line in `contributor-whitelist.txt`.

Example:
```
# Existing contributors
kthoms
# New contributor
newusername
```

## How It Works

The workflow `.github/workflows/check-for-new-contributor.yml` fetches this file via the GitHub API (without requiring a checkout) and checks if the actor is in the whitelist before greeting them. This optimization:

1. Reduces unnecessary workflow runs for existing contributors
2. Avoids checkout operations when not needed
3. Centralizes the contributor list in one maintainable file
4. Allows the whitelist to be updated independently of workflow changes

## See Also

- [First-time Contributor Workflow](../workflows/check-for-new-contributor.yml)
- [Contributing Guidelines](../../CONTRIBUTING.md)
