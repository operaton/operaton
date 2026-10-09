# ANNOUNCE

**Only after PERFORM has verified the release is live.** Every channel below is public and irreversible — always draft, show the user, and post only on explicit confirmation.

## Preconditions

- GitHub Release tag(s) live and visible
- Maven Central artifacts resolve (HTTP 200)
- Docker versioned tags on Docker Hub

## 1. Website — `operaton/operaton.org`

Two changes in the same PR:

**a) `index.html` changelog card** — update the `#changelog` section to reference the **highest** version released that day (e.g. if 1.1.4 and 2.1.2 ship together, show 2.1.2). Use the scaffold script:
```bash
python3 .devenv/scripts/release/update-website.py
```

**b) Blog post** — create `_posts/YYYY-MM-DD-operaton-X-Y-released.md` modelled on `_posts/2026-04-24-operaton-2-1-released.md`. Content: highlights from the `noteworthy`-labelled items, links to the release notes and GitHub Release.

Open as a **draft PR** on `operaton/operaton.org`. Show the user the diff before opening. Merge only on user confirmation.

## 2. Slack — `#general` in Operaton workspace

Compact `@channel` post. Template:

```
@channel 🚀 Operaton X.Y.Z is out!

Highlights:
• <noteworthy item 1>
• <noteworthy item 2>

Release notes: https://docs.operaton.org/docs/documentation/reference/release-notes/X_Y/
GitHub: https://github.com/operaton/operaton/releases/tag/vX.Y.Z
```

For patch releases (no highlights), keep it short: just the version, a one-liner ("dependency updates and bug fixes"), and the release link.

Draft → show user → post only on confirmation.

## 3. Forum — `forum.operaton.org`, "Announcements" category

Fuller markdown version of the Slack post: same highlights expanded to 2–3 sentences each, plus the changelog / release-notes link. Draft → show user → post only on confirmation.

## 4. Branch cleanup list

Run the script and hand the output to the user — **do not delete any branch yourself**:
```bash
.devenv/scripts/release/list-merged-branches.sh "<MILESTONE>"
```
Output: list of merged branches with their GitHub URLs. The human reviews and deletes as appropriate.

## 5. Housekeeping

After announcements are out:

- **`changelog.tpl`** update `changelog.tpl` URL if the minor version changed.
- **Close milestone** — `gh api repos/operaton/operaton/milestones/<N> -X PATCH -f state=closed`
- **Open next milestone** — verify the next milestone exists (e.g. 2.1.3) with a due date matching the calendar (`references/calendar.md`).
- **Calendar** — mark the released version as done in `references/calendar.md`.
- **Docs / download page** — verify https://docs.operaton.org shows the new version; verify the download page links resolve.

## Output

End ANNOUNCE with:
- Website PR URL
- Slack post URL (or confirmation it was posted)
- Forum post URL (or confirmation)
- Merged-branch list (for user to delete)
- Housekeeping items completed vs. pending
