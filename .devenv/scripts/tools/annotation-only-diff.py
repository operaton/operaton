#!/usr/bin/env python3
# Copyright 2026 the Operaton contributors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at:
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""
Proves a diff contains only JSpecify annotations and their imports.

A nullability refactor must not change runtime behavior. This guard
compares each added line against its removed counterpart with all
annotation tokens stripped: if the remainder is unchanged, the line is
annotation-only. Added imports of jspecify types are allowed outright.
"""

import argparse
import re
import subprocess
import sys

ANNOTATION_RE = re.compile(
    r'@(?:NullMarked|NullUnmarked|Nullable|NonNull)\s*')
JSPECIFY_IMPORT_RE = re.compile(
    r'^\s*import\s+(?:static\s+)?org\.jspecify\.annotations\.\w+\s*;\s*$')


def strip_annotations(line):
    return re.sub(r'\s+', ' ', ANNOTATION_RE.sub('', line)).strip()


def is_annotation_only_line(added, removed):
    """True when `added` differs from `removed` only by annotations."""
    if JSPECIFY_IMPORT_RE.match(added):
        return True
    return strip_annotations(added) == strip_annotations(removed)


def widens_parameter(parent_signature, child_signature):
    """True when the child adds @Nullable to a parameter the parent left non-null."""
    return (strip_annotations(parent_signature) == strip_annotations(child_signature)
            and '@Nullable' in child_signature
            and '@Nullable' not in parent_signature)


def check_diff(diff_text):
    """Return [(path, line)] for every added line that is not annotation-only."""
    offenders = []
    path = '?'
    removed = []
    for raw in diff_text.split('\n'):
        if raw.startswith('+++ b/'):
            path = raw[6:]
            removed = []
            continue
        if raw.startswith('---') or raw.startswith('diff ') or raw.startswith('@@'):
            removed = []
            continue
        if raw.startswith('-') and not raw.startswith('---'):
            removed.append(raw[1:])
            continue
        if raw.startswith('+'):
            added = raw[1:]
            if not added.strip():
                continue
            candidate = removed.pop(0) if removed else ''
            if not is_annotation_only_line(added, candidate):
                offenders.append((path, added.strip()))
    return offenders


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('range', nargs='?', default='HEAD',
                        help='git range or revision to diff (default: HEAD)')
    args = parser.parse_args(argv)

    diff = subprocess.run(
        ['git', 'diff', '-U0', args.range + '^', args.range],
        capture_output=True, text=True, check=True).stdout
    offenders = check_diff(diff)
    for path, line in offenders:
        print(f'{path}: {line}')
    if offenders:
        print(f'\n{len(offenders)} non-annotation change(s) found.',
              file=sys.stderr)
        return 1
    print('Diff is annotation-only.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
