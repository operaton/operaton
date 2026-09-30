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
import difflib
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


def is_annotation_only_removal(line):
    """True when a removed line with no matching added line is harmless to drop.

    This covers blank lines and lines that consist solely of annotation
    tokens (e.g. a lone `@Nullable` on its own line, or a jspecify import
    being deleted as cleanup). Anything else is real code being deleted
    unaccompanied by a corresponding addition, which is never annotation-only.
    """
    if not line.strip():
        return True
    if JSPECIFY_IMPORT_RE.match(line):
        return True
    return strip_annotations(line) == ''


def check_diff(diff_text):
    """Return [(path, line)] for every changed line that is not annotation-only.

    Offenders from added lines are reported as `(path, added_line)`.
    Offenders from removed lines that have no corresponding added line
    (a pure deletion of real code, e.g. a dropped null-check) are reported
    as `(path, '-' + removed_line)` -- the leading '-' marks them as
    deletions so callers/tests can distinguish the two kinds of offender.

    Lines are grouped into "chunks": a chunk is a maximal run of removed
    lines immediately followed by a maximal run of added lines. Within a
    chunk, `difflib.SequenceMatcher` aligns removed/added lines so that
    unrelated add/remove counts don't get misattributed by naive positional
    pairing. Any removed line left unmatched at the end of a chunk is
    checked on its own via `is_annotation_only_removal` -- this is what
    catches deletions with no corresponding addition, which previously were
    silently dropped.
    """
    offenders = []
    path = '?'
    removed = []
    added = []

    def flush():
        if not removed and not added:
            return
        matcher = difflib.SequenceMatcher(None, removed, added, autojunk=False)
        for tag, i1, i2, j1, j2 in matcher.get_opcodes():
            if tag == 'equal':
                continue
            rem_block = removed[i1:i2]
            add_block = added[j1:j2]
            n = min(len(rem_block), len(add_block))
            for k in range(n):
                if not is_annotation_only_line(add_block[k], rem_block[k]):
                    offenders.append((path, add_block[k].strip()))
            for extra_add in add_block[n:]:
                if not is_annotation_only_line(extra_add, ''):
                    offenders.append((path, extra_add.strip()))
            for extra_rem in rem_block[n:]:
                if not is_annotation_only_removal(extra_rem):
                    offenders.append((path, '-' + extra_rem.strip()))
        removed.clear()
        added.clear()

    for raw in diff_text.split('\n'):
        if raw.startswith('+++ b/'):
            flush()
            path = raw[6:]
            continue
        if raw.startswith('---') or raw.startswith('diff ') or raw.startswith('@@'):
            flush()
            continue
        if raw.startswith('-') and not raw.startswith('---'):
            if added:
                # A new removal after we've already seen additions starts a
                # new chunk; flush the previous one first so it isn't lost.
                flush()
            removed.append(raw[1:])
            continue
        if raw.startswith('+') and not raw.startswith('+++'):
            added.append(raw[1:])
            continue
        # Context line or anything else: end of the current chunk.
        flush()
    flush()
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
