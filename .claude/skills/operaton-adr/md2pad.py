#!/usr/bin/env python3
"""Convert a canonical ADR draft into a pad-readable variant.

yopad stores no link attributes and mangles inline code, so a draft written for
GitHub does not survive the import. Verified against the live instance:

  * [text](url)      -> the URL is discarded, only `text` remains (no link at all)
  * <a href="...">   -> same, HTML import strips the href too
  * [text][ref]      -> reference definitions are consumed, leaving zero links
  * `code`           -> the line is split and turned into an indented code block,
                        wrecking the sentence and any list item around it
  * a bare URL       -> autolinked, but then the label IS the URL

Labelled links are still possible: the editor stores them as a `url` attribute on
a range of characters. No import path creates that attribute, so this script
emits the pad text with plain labels plus a sidecar list of label/URL pairs, and
padlinks.py applies the attributes afterwards.

Usage:
    python3 md2pad.py <draft.md> [-o <draft.pad.md>]

Writes <draft>.pad.md (import this) and <draft>.links.json (feed to padlinks.py).
"""
import argparse
import json
import pathlib
import re
import sys

LINK = re.compile(r"\[([^\]\n]+)\]\((https?://[^)\s]+)\)")
INLINE_CODE = re.compile(r"`([^`\n]+)`")


def convert(text):
    """Return (pad_text, links) with links in document order."""
    links = []

    def take(match):
        label, url = match.group(1), match.group(2)
        links.append({"label": label, "url": url})
        return label

    # [label](url) -> label; padlinks.py re-attaches the URL as an attribute
    text = LINK.sub(take, text)
    # `code` -> code; backticks would split the line into a code block
    text = INLINE_CODE.sub(r"\1", text)
    return text, links


def warn(text):
    problems = []
    if re.search(r"^\s*\|.*\|\s*$", text, re.M):
        problems.append("table syntax found -- the pad cannot render tables, rewrite as prose")
    if re.search(r"^```", text, re.M):
        problems.append("fenced code block found -- keep snippets inline and short")
    if re.search(r"^\[[^\]]+\]:\s*https?://", text, re.M):
        problems.append("reference-style link definition found -- the pad drops these entirely")
    if re.search(r"/blob/(main|master|HEAD)/", text):
        problems.append("permalink points at a branch -- pin it to a commit hash")
    return problems


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("draft", help="canonical markdown draft")
    ap.add_argument("-o", "--out", help="output path (default: <draft>.pad.md)")
    args = ap.parse_args()

    src = pathlib.Path(args.draft)
    text = src.read_text()

    for problem in warn(text):
        print(f"WARNING: {problem}", file=sys.stderr)

    out = pathlib.Path(args.out) if args.out else src.with_suffix(".pad.md")
    pad_text, links = convert(text)
    out.write_text(pad_text)

    sidecar = out.with_suffix("").with_suffix(".links.json")
    sidecar.write_text(json.dumps(links, indent=1))

    for label in sorted({l["label"] for l in links if "\n" in l["label"]}):
        print(f"WARNING: label spans a line break, cannot be linked: {label}", file=sys.stderr)

    print(json.dumps({"pad": str(out), "links": str(sidecar), "count": len(links)}, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
