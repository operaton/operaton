#!/usr/bin/env python3
"""Check that a release branch integrates the latest Spring Boot / Spring Framework patch.

Operaton usually releases the day after Spring ships its monthly patch releases, so
the branch is expected to carry the newest patch of the Spring Boot and Spring
Framework lines it is built on. Prints a markdown report:

  - version on the branch (`version.spring-boot` / `version.spring` in pom.xml)
  - latest GA patch of the same MAJOR.MINOR line on Maven Central, with its
    publication date
  - latest GA version overall (a newer minor is informational for patch releases)
  - open PRs against the branch that bump Spring (e.g. Dependabot)
  - a verdict

READ-ONLY: reads the branch through `gh` and Maven Central over HTTPS.

Usage:
  check-spring-versions.py --branch release/2.2.x [--repo OWNER/REPO]

Exit codes: 0 latest patches integrated, 2 a newer patch exists (see verdict), 1 error.
"""
import argparse
import base64
import json
import re
import subprocess
import sys
import urllib.request
import xml.etree.ElementTree as ET
from email.utils import parsedate_to_datetime

CENTRAL = "https://repo1.maven.org/maven2"
ARTIFACTS = {
    "Spring Boot": ("version.spring-boot", "org/springframework/boot", "spring-boot"),
    "Spring Framework": ("version.spring", "org/springframework", "spring-core"),
}
GA_RE = re.compile(r"\d+\.\d+\.\d+")


def gh(*args):
    out = subprocess.run(["gh", *args], check=True, capture_output=True, text=True)
    return out.stdout


def pom_properties(repo, branch):
    data = json.loads(gh("api", f"repos/{repo}/contents/pom.xml?ref={branch}"))
    return base64.b64decode(data["content"]).decode()


def property_value(pom, name):
    m = re.search(rf"<{re.escape(name)}>([^<]+)</{re.escape(name)}>", pom)
    return m.group(1).strip() if m else None


def version_tuple(v):
    return tuple(int(x) for x in v.split("."))


def ga_versions(group_path, artifact):
    with urllib.request.urlopen(f"{CENTRAL}/{group_path}/{artifact}/maven-metadata.xml", timeout=30) as r:
        root = ET.fromstring(r.read())
    return [v.text for v in root.iter("version") if v.text and GA_RE.fullmatch(v.text)]


def published(group_path, artifact, version):
    req = urllib.request.Request(f"{CENTRAL}/{group_path}/{artifact}/{version}/{artifact}-{version}.pom",
                                 method="HEAD")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return parsedate_to_datetime(r.headers["Last-Modified"]).date().isoformat()
    except Exception:  # date is informational only
        return "?"


def open_spring_prs(repo, branch):
    out = gh("pr", "list", "--repo", repo, "--base", branch, "--state", "open", "--search", "spring in:title",
             "--json", "number,title,url")
    return json.loads(out)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--branch", required=True)
    p.add_argument("--repo", default="operaton/operaton")
    a = p.parse_args()

    try:
        pom = pom_properties(a.repo, a.branch)
    except subprocess.CalledProcessError as e:
        print(f"error: cannot read pom.xml on {a.branch}: {e.stderr.strip()}", file=sys.stderr)
        return 1

    print(f"## Spring versions: {a.branch}\n")
    print("| Library | Branch | Latest patch (same line) | Published | Latest GA | Status |")
    print("|---|---|---|---|---|---|")
    outdated, newer_minor = [], []
    for name, (prop, group_path, artifact) in ARTIFACTS.items():
        current = property_value(pom, prop)
        if not current or not GA_RE.fullmatch(current):
            print(f"error: property {prop} not found or not a GA version on {a.branch}: {current}", file=sys.stderr)
            return 1
        try:
            versions = ga_versions(group_path, artifact)
        except Exception as e:
            print(f"error: cannot read Maven Central metadata for {artifact}: {e}", file=sys.stderr)
            return 1
        line = current.rsplit(".", 1)[0]
        latest_patch = max((v for v in versions if v.rsplit(".", 1)[0] == line), key=version_tuple)
        latest_ga = max(versions, key=version_tuple)
        if version_tuple(latest_patch) > version_tuple(current):
            status = "❌ newer patch"
            outdated.append(f"{name} {current} → {latest_patch}")
        else:
            status = "✅ latest"
        if latest_ga.rsplit(".", 1)[0] != line:
            newer_minor.append(f"{name} {latest_ga}")
        print(f"| {name} | `{current}` | `{latest_patch}` | {published(group_path, artifact, latest_patch)} "
              f"| `{latest_ga}` | {status} |")

    prs = open_spring_prs(a.repo, a.branch)
    if prs:
        print("\nOpen PRs against the branch mentioning Spring:")
        for pr in prs:
            print(f"- #{pr['number']} {pr['title']} — {pr['url']}")

    if newer_minor:
        print(f"\nNewer minor line available (informational, not for patch releases): {', '.join(newer_minor)}")

    if outdated:
        print(f"\n**Verdict:** newer Spring patch not integrated: {'; '.join(outdated)}. "
              "Merge the bump (Dependabot PR or own PR) into the branch before releasing.")
        return 2
    print("\n**Verdict:** latest Spring Boot and Spring Framework patches integrated.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
