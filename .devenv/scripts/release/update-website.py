#!/usr/bin/env python3
"""Update the operaton.org marketing site for a release announcement.

Two things, both optional:
  1. Bump the ``#changelog`` card in ``index.html`` to the released version
     (tag link, "Version X.Y.Z" text, and the one-line summary).
  2. Scaffold a blog post under ``_posts/`` from the most recent existing
     ``*-released.md`` post, so the prose just needs filling in.

It edits files in a local operaton.org checkout. It does NOT commit, push, or
open a PR — review the diff, then open the PR against operaton/operaton.org
yourself.

When several releases ship the same day, pass the HIGHEST version as --version
(the single changelog card always shows the highest); cover the other lines in
the blog prose.

Usage:
  update-website.py --version 2.1.2 [options]

Options:
  --version X.Y.Z       Released version (highest, if multiple). Required.
  --summary "text"      One-line summary for the #changelog card. If omitted,
                        a TODO marker is inserted for you to edit.
  --title "text"        H2 title for the blog post. Default derived from version.
  --date YYYY-MM-DD     Blog post date / filename prefix. Default: today.
  --repo PATH           Path to the operaton.org checkout.
                        Default: ~/Development/operaton/operaton.org
  --no-changelog        Skip the index.html changelog card update.
  --no-blog             Skip the blog post scaffold.
  --help                Show this help and exit.
"""
import argparse
import datetime
import os
import re
import sys
from pathlib import Path

DEFAULT_REPO = Path.home() / "Development" / "operaton" / "operaton.org"
TAG_BASE = "https://github.com/operaton/operaton/releases/tag/v"


def parse_version(v):
    m = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)", v)
    if not m:
        sys.exit(f"ERROR: --version must be MAJOR.MINOR.PATCH, got {v!r}")
    return tuple(int(x) for x in m.groups())


def update_changelog(index_path, version, summary):
    text = index_path.read_text()
    anchor = text.find("#changelog")
    if anchor == -1:
        sys.exit(f"ERROR: no '#changelog' marker found in {index_path}")
    head, tail = text[:anchor], text[anchor:]

    # The only 'releases/tag/v...' link in the file is the changelog card.
    new_tail, n_url = re.subn(
        r"releases/tag/v[^\"']*", f"releases/tag/v{version}", tail, count=1
    )
    new_tail, n_ver = re.subn(
        r"(>\s*)Version\s+[\w.\-]+(\s*<)", rf"\g<1>Version {version}\g<2>",
        new_tail, count=1,
    )
    if not (n_url and n_ver):
        sys.exit("ERROR: could not locate the changelog card link/version to update.")

    if summary is not None:
        new_tail, n_sum = re.subn(
            r"(</header>\s*<p>\s*)(.*?)(\s*</p>)",
            lambda m: m.group(1) + summary + m.group(3),
            new_tail, count=1, flags=re.DOTALL,
        )
        if not n_sum:
            print("WARN: changelog summary paragraph not found; left unchanged.",
                  file=sys.stderr)

    index_path.write_text(head + new_tail)
    note = "version + tag" + (" + summary" if summary is not None else
                              " (summary left as-is — edit the <p> by hand)")
    print(f"✓ index.html #changelog card → Version {version} ({note})")


def read_frontmatter(post_path):
    text = post_path.read_text()
    m = re.match(r"^---\n(.*?)\n---\n", text, flags=re.DOTALL)
    return m.group(1) if m else "layout: post\nauthor: The Operaton Team"


def latest_released_post(posts_dir):
    candidates = sorted(posts_dir.glob("*-released.md"))
    if candidates:
        return candidates[-1]
    allposts = sorted(posts_dir.glob("*.md"))
    return allposts[-1] if allposts else None


def scaffold_blog(posts_dir, version, date_str, title):
    major, minor, patch = parse_version(version)
    slug = f"operaton-{major}-{minor}-released" if patch == 0 \
        else f"operaton-{major}-{minor}-{patch}-released"
    out = posts_dir / f"{date_str}-{slug}.md"
    if out.exists():
        print(f"WARN: {out.name} already exists — not overwriting.", file=sys.stderr)
        return

    ref = latest_released_post(posts_dir)
    frontmatter = read_frontmatter(ref) if ref else \
        "layout: post\nauthor: The Operaton Team"
    ref_note = f" (frontmatter copied from {ref.name})" if ref else ""

    notes_url = (f"https://docs.operaton.org/docs/documentation/reference/"
                 f"release-notes/{major}_{minor}/")
    kind = "patch release" if patch else "minor release"

    body = f"""---
{frontmatter}
---

## {title}

<!-- TODO intro: 2–3 sentences. What is this {kind}, what does it build on,
     and the headline change. Mention schema/REST-API compatibility if relevant. -->

### Key Features & Improvements

<!-- TODO: one numbered subsection per noteworthy item, sourced from the
     release notes and `noteworthy`-labelled PRs. For a patch release keep this
     short — just the notable fixes / security / dependency bumps. -->

#### 1. <Feature>
<!-- TODO -->

### Migration Notes

<!-- TODO: drop-in upgrade? any changed internal (.impl.) APIs? Link release notes. -->

### Get Started Today!

You can find the full release notes and migration guide in our [documentation]({notes_url}).

*   **Download:** Get the latest binaries from our [Downloads page](https://operaton.org/download).
*   **Forum:** Have questions or feedback? Join the discussion on our [forum](https://forum.operaton.org).

Thank you to all contributors and community members who continue to help shape Operaton. Every bug report, pull request, and forum discussion makes a difference!

*The Operaton Team*
"""
    out.write_text(body)
    print(f"✓ blog post scaffolded: _posts/{out.name}{ref_note}")
    print(f"  → fill in the TODO sections from {notes_url}")


def main():
    p = argparse.ArgumentParser(add_help=False)
    p.add_argument("--version", required=False)
    p.add_argument("--summary")
    p.add_argument("--title")
    p.add_argument("--date")
    p.add_argument("--repo", default=str(DEFAULT_REPO))
    p.add_argument("--no-changelog", action="store_true")
    p.add_argument("--no-blog", action="store_true")
    p.add_argument("--help", "-h", action="store_true")
    args = p.parse_args()

    if args.help or not args.version:
        print(__doc__)
        sys.exit(0 if args.help else 2)

    parse_version(args.version)  # validate early
    repo = Path(os.path.expanduser(args.repo))
    index_path = repo / "index.html"
    posts_dir = repo / "_posts"
    if not index_path.exists():
        sys.exit(f"ERROR: {index_path} not found. Is --repo correct?")

    date_str = args.date or datetime.date.today().isoformat()
    title = args.title or f"Operaton {args.version} Released"

    if not args.no_changelog:
        update_changelog(index_path, args.version, args.summary)
    if not args.no_blog:
        scaffold_blog(posts_dir, args.version, date_str, title)

    print()
    print("Next steps (do these yourself, after reviewing):")
    print(f"  cd {repo}")
    print("  git switch -c announce/" + args.version)
    print("  git diff                      # review the changelog + new post")
    print("  # edit the blog post prose, then commit and open a PR:")
    print("  gh pr create --repo operaton/operaton.org --base main")


if __name__ == "__main__":
    main()
