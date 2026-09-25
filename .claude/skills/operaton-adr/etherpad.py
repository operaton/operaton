#!/usr/bin/env python3
"""yopad.eu pad management for Operaton ADR drafts.

yopad.eu is an Etherpad instance with NO HTTP API (/api/1/* is 404). Two facts
drive this script's design:

  * The deletion token is handed out exactly once, over socket.io CLIENT_VARS,
    to the connection that CREATES the pad. Create the pad any other way
    (a plain GET, or POST /import) and the token is gone forever -- the pad
    then survives until it expires and nobody can delete it.
  * Engine.IO v4 speaks plain HTTP long-polling, so no websocket library is
    needed. Stdlib only.

Durability is encoded as a suffix on the pad NAME, not a parameter:
    -1day | (none) = 30 days | -365days
"""
import argparse
import datetime
import json
import os
import pathlib
import re
import sys
import time
import urllib.parse
import urllib.request
import uuid

BASE = "https://yopad.eu"
REGISTRY = pathlib.Path.home() / ".operaton" / "etherpad-docs.md"
HEADER = [
    "# Operaton Etherpad Documents",
    "",
    "Discussion pads for Operaton ADRs. The delete token is the ONLY way to remove",
    "a pad before it expires. Local file - never commit it.",
    "",
    "| Created | Expires | Pad | URL | Delete Token | Issue |",
    "| --- | --- | --- | --- | --- | --- |",
]

_opener = urllib.request.build_opener()
_opener.addheaders = [("User-Agent", "Mozilla/5.0"), ("Origin", BASE)]


def _eio(method, sid=None, body=None, timeout=20):
    q = {"EIO": "4", "transport": "polling", "t": str(time.time())}
    if sid:
        q["sid"] = sid
    req = urllib.request.Request(
        f"{BASE}/socket.io/?" + urllib.parse.urlencode(q),
        data=body.encode() if body is not None else None,
        method=method,
    )
    if body is not None:
        req.add_header("Content-Type", "text/plain;charset=UTF-8")
    return _opener.open(req, timeout=timeout).read().decode()


def _connect(pad):
    """Open a socket.io session and join the pad. Creates the pad if absent."""
    sid = json.loads(_eio("GET")[1:])["sid"]
    _eio("POST", sid, "40")
    _eio("GET", sid)
    _eio("POST", sid, "42" + json.dumps(["message", {
        "component": "pad",
        "type": "CLIENT_READY",
        "padId": pad,
        "sessionID": "null",
        "token": "t." + uuid.uuid4().hex[:20],
        "userInfo": {"colorId": None, "name": None},
    }]))
    return sid


def _pump(sid, want, tries=6):
    for _ in range(tries):
        try:
            payload = _eio("GET", sid)
        except Exception:
            return None
        for frame in payload.split("\x1e"):
            if not frame.startswith("42"):
                continue
            ev = json.loads(frame[2:])
            if len(ev) > 1 and isinstance(ev[1], dict):
                if ev[1].get("type") == want or ev[1].get("disconnect"):
                    return ev[1]
    return None


def pad_import(pad, path):
    """POST the file into the pad. Unauthenticated; ep_markdown keeps structure."""
    body, boundary = [], uuid.uuid4().hex
    data = pathlib.Path(path).read_bytes()
    body.append(f'--{boundary}\r\nContent-Disposition: form-data; name="file";'
                f' filename="draft.md"\r\nContent-Type: text/markdown\r\n\r\n'.encode())
    body.append(data)
    body.append(f"\r\n--{boundary}--\r\n".encode())
    req = urllib.request.Request(f"{BASE}/p/{urllib.parse.quote(pad)}/import",
                                 data=b"".join(body), method="POST")
    req.add_header("Content-Type", f"multipart/form-data; boundary={boundary}")
    return json.loads(_opener.open(req, timeout=30).read().decode())


def pad_export(pad):
    """Export as markdown. NOTE: /export/txt is 404 on this instance."""
    url = f"{BASE}/p/{urllib.parse.quote(pad)}/export/markdown"
    return _opener.open(url, timeout=30).read().decode()


def registry_rows():
    if not REGISTRY.exists():
        return []
    return [line for line in REGISTRY.read_text().splitlines()
            if line.startswith("| ") and "---" not in line
            and not line.startswith("| Created")]


def registry_add(pad, url, token, issue=""):
    REGISTRY.parent.mkdir(parents=True, exist_ok=True)
    today = datetime.date.today()
    days = 365 if pad.endswith("-365days") else 1 if pad.endswith("-1day") else 30
    expires = today + datetime.timedelta(days=days)
    if not REGISTRY.exists():
        REGISTRY.write_text("\n".join(HEADER) + "\n")
    with REGISTRY.open("a") as fh:
        fh.write(f"| {today} | {expires} | {pad} | {url} | `{token}` | {issue} |\n")
    REGISTRY.chmod(0o600)


def registry_remove(pad):
    if not REGISTRY.exists():
        return
    keep = [line for line in REGISTRY.read_text().splitlines()
            if f"| {pad} |" not in line]
    REGISTRY.write_text("\n".join(keep) + "\n")


def registry_token(pad):
    for row in registry_rows():
        cells = [c.strip() for c in row.strip("|").split("|")]
        if len(cells) > 4 and cells[2] == pad:
            return cells[4].strip("`")
    return None


def cmd_create(args):
    suffix = f"-{args.durability}" if args.durability else ""
    pad = f"Operaton-ADR-{args.shortdesc}{suffix}"
    url = f"{BASE}/p/{urllib.parse.quote(pad)}"
    sid = _connect(pad)
    cv = (_pump(sid, "CLIENT_VARS") or {}).get("data", {})
    token = cv.get("padDeletionToken")
    if not token:
        print(f"ERROR: no deletion token issued -- pad '{pad}' already existed.\n"
              f"It cannot be deleted by us. Pick a different shortdesc, or reuse "
              f"the existing pad deliberately.", file=sys.stderr)
        return 1
    if args.file:
        result = pad_import(pad, args.file)
        if result.get("code") != 0:
            print(f"ERROR: import failed: {result}", file=sys.stderr)
            print(f"Pad exists at {url} with token {token} -- record it.", file=sys.stderr)
            return 1
    registry_add(pad, url, token, args.issue or "")
    print(json.dumps({"pad": pad, "url": url, "deletionToken": token,
                      "registry": str(REGISTRY)}, indent=2))
    return 0


def cmd_delete(args):
    token = args.token or registry_token(args.pad)
    if not token:
        print(f"ERROR: no token for '{args.pad}' in {REGISTRY}", file=sys.stderr)
        return 1
    sid = _connect(args.pad)
    _pump(sid, "CLIENT_VARS")
    _eio("POST", sid, "42" + json.dumps(["message", {
        "component": "pad", "type": "COLLABROOM",
        "data": {"type": "PAD_DELETE",
                 "data": {"padId": args.pad, "deletionToken": token}}}]))
    reply = _pump(sid, "__none__", tries=2)
    ok = bool(reply and reply.get("disconnect") == "deleted")
    if ok:
        registry_remove(args.pad)
    print("deleted" if ok else f"no confirmation from server: {reply}")
    return 0 if ok else 1


def cmd_list(args):
    today = datetime.date.today()
    for row in registry_rows():
        cells = [c.strip() for c in row.strip("|").split("|")]
        if len(cells) < 4:
            continue
        try:
            left = (datetime.date.fromisoformat(cells[1]) - today).days
        except ValueError:
            left = "?"
        print(f"{cells[2]:<55} {left} days left  {cells[3]}")
    return 0


def main():
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)

    c = sub.add_parser("create", help="create pad, capture deletion token, import draft")
    c.add_argument("shortdesc", help="kebab-case, no number prefix")
    c.add_argument("--durability", default="365days", choices=["1day", "30days", "365days"])
    c.add_argument("--file", help="markdown draft to import")
    c.add_argument("--issue", default="", help="issue URL for the registry row")
    c.set_defaults(func=cmd_create)

    i = sub.add_parser("import", help="overwrite pad content from a file")
    i.add_argument("pad"); i.add_argument("file")
    i.set_defaults(func=lambda a: print(json.dumps(pad_import(a.pad, a.file))) or 0)

    e = sub.add_parser("export", help="print pad content as markdown")
    e.add_argument("pad")
    e.set_defaults(func=lambda a: print(pad_export(a.pad)) or 0)

    d = sub.add_parser("delete", help="delete pad using its token")
    d.add_argument("pad"); d.add_argument("token", nargs="?")
    d.set_defaults(func=cmd_delete)

    sub.add_parser("list", help="registry with days-to-expiry").set_defaults(func=cmd_list)

    args = p.parse_args()
    # "30days" is the instance default and is expressed by the ABSENCE of a suffix
    if getattr(args, "durability", None) == "30days":
        args.durability = ""
    sys.exit(args.func(args))


if __name__ == "__main__":
    main()
