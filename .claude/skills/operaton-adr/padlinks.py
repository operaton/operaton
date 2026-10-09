#!/usr/bin/env python3
"""Apply real hyperlinks to an already-imported yopad pad.

Background: yopad supports labelled links -- the editor stores them as a `url`
attribute on a range of characters -- but NO import path creates them. Markdown
`[text](url)` loses the URL, HTML `<a href>` is stripped, reference-style
definitions are dropped. Only a bare URL is autolinked, and then the label is the
URL itself, which is what makes a draft unreadable.

This script closes the gap: import the pad with plain labels (see md2pad.py),
then apply one `url` attribute per label with a single changeset, so the pad
reads exactly like the canonical markdown.

Usage:
    python3 padlinks.py <pad> <links.json>

<links.json> is the sidecar written by md2pad.py: a list of {"label", "url"}
objects in document order.
"""
import argparse
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from etherpad import _connect, _eio, _pump  # noqa: E402


def b36(n):
    if n == 0:
        return "0"
    digits = "0123456789abcdefghijklmnopqrstuvwxyz"
    out = ""
    while n:
        n, r = divmod(n, 36)
        out = digits[r] + out
    return out


def keep(seg):
    """Encode a '=' op for `seg`. An op carrying newlines must end on one."""
    if not seg:
        return ""
    idx = seg.rfind("\n")
    if idx == -1:
        return "=" + b36(len(seg))
    head, tail = seg[:idx + 1], seg[idx + 1:]
    ops = "|" + b36(head.count("\n")) + "=" + b36(len(head))
    if tail:
        ops += "=" + b36(len(tail))
    return ops


def build_changeset(text, spans):
    """spans: list of (start, end, attrib_number), non-overlapping, sorted.

    The trailing keep op is deliberately omitted: a changeset that keeps text all
    the way to the document's final newline is rejected with `badChangeset`.
    Trailing keeps are implicit, so leaving it out is both correct and required.
    """
    ops, pos = [], 0
    for start, end, num in spans:
        ops.append(keep(text[pos:start]))
        ops.append("*" + b36(num) + "=" + b36(end - start))
        pos = end
    return "Z:" + b36(len(text)) + ">0" + "".join(ops) + "$"


def locate(text, links):
    """Find each label in document order. Returns spans and the misses."""
    spans, misses, cursor = [], [], 0
    for item in links:
        label = item["label"]
        at = text.find(label, cursor)
        if at == -1:  # fall back to a global search; wrapping may reorder nothing, but be safe
            at = text.find(label)
        if at == -1 or "\n" in label:
            misses.append(label)
            continue
        spans.append((at, at + len(label), item["url"]))
        cursor = at + len(label)
    spans.sort()
    deduped = []
    for span in spans:
        if not deduped or span[0] >= deduped[-1][1]:
            deduped.append(span)
    return deduped, misses


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pad")
    ap.add_argument("links", help="sidecar JSON from md2pad.py")
    args = ap.parse_args()

    links = json.loads(pathlib.Path(args.links).read_text())

    sid = _connect(args.pad)
    cv = (_pump(sid, "CLIENT_VARS") or {}).get("data", {})
    ccv = cv.get("collab_client_vars", {})
    text = ccv.get("initialAttributedText", {}).get("text", "")
    rev = ccv.get("rev", 0)
    if not text:
        print("ERROR: could not read pad text -- is the pad empty?", file=sys.stderr)
        return 1

    located, misses = locate(text, links)
    for label in misses:
        print(f"WARNING: label not found in pad, left unlinked: {label}", file=sys.stderr)

    # one attribute pool entry per distinct URL
    urls, pool = {}, {}
    spans = []
    for start, end, url in located:
        if url not in urls:
            urls[url] = len(urls)
            pool[str(urls[url])] = ["url", url]
        spans.append((start, end, urls[url]))

    if not spans:
        print("nothing to link")
        return 1

    changeset = build_changeset(text, spans)
    _eio("POST", sid, "42" + json.dumps(["message", {
        "component": "pad", "type": "COLLABROOM",
        "data": {"type": "USER_CHANGES", "baseRev": rev, "changeset": changeset,
                 "apool": {"numToAttrib": pool, "nextNum": len(pool)}}}]))

    for _ in range(3):
        try:
            if "badChangeset" in _eio("GET", sid):
                print(f"ERROR: server rejected the changeset:\n{changeset[:200]}", file=sys.stderr)
                return 1
        except Exception:
            break

    print(json.dumps({"linked": len(spans), "distinctUrls": len(pool),
                      "unlinked": misses}, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
