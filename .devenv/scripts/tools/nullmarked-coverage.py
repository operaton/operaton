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
Evaluates @NullMarked (JSpecify) completion rate across Maven modules.

A class counts as covered if:
  - it (or its package-info.java, or the module's module-info.java) carries
    @NullMarked, and
  - it does not itself carry @NullUnmarked (which opts a class back out).

Writes one detailed report per module to <module>/target/nullmarked-report.txt,
listing every uncovered file, and prints an overall summary to stdout.
"""

import argparse
import configparser
import fnmatch
import re
import sys
from pathlib import Path

DEFAULT_CONFIG_NAME = 'nullmarked-coverage.cfg'
DEFAULT_SORT_BY = 'Rate:DESC'

SORT_COLUMNS = {
    'module': lambda row: str(row[0]).lower(),
    'classes': lambda row: row[1],
    'covered': lambda row: row[2],
    'rate': lambda row: row[3],
}


def parse_sort_spec(spec):
    """'Module:ASC,Rate:DESC' -> [('module', False), ('rate', True)] (reverse flags)."""
    criteria = []
    for part in spec.split(','):
        part = part.strip()
        if not part:
            continue
        if ':' in part:
            column, direction = part.split(':', 1)
        else:
            column, direction = part, 'ASC'
        column = column.strip().lower()
        direction = direction.strip().upper()
        if column not in SORT_COLUMNS:
            raise ValueError(f"Unknown sort column '{column}'. Valid columns: "
                              f"{', '.join(sorted(SORT_COLUMNS))}")
        if direction not in ('ASC', 'DESC'):
            raise ValueError(f"Unknown sort direction '{direction}'. Use ASC or DESC.")
        criteria.append((column, direction == 'DESC'))
    return criteria


def sort_summary(summary, criteria):
    # Stable sort: apply criteria from least to most significant.
    for column, reverse in reversed(criteria):
        summary.sort(key=SORT_COLUMNS[column], reverse=reverse)
    return summary

NULLMARKED_RE = re.compile(r'@(?:[\w.]+\.)?NullMarked\b')
NULLUNMARKED_RE = re.compile(r'@(?:[\w.]+\.)?NullUnmarked\b')
TOP_LEVEL_TYPE_RE = re.compile(
    r'^\s*(?:public\s+|final\s+|abstract\s+|sealed\s+|non-sealed\s+|strictfp\s+)*'
    r'(?:class|interface|enum|record|@interface)\s+\w'
)
LINE_COMMENT_RE = re.compile(r'//.*')
BLOCK_COMMENT_RE = re.compile(r'/\*.*?\*/', re.DOTALL)

EXCLUDED_DIR_NAMES = {'target', '.git', 'node_modules', 'build'}


def strip_comments(text):
    text = BLOCK_COMMENT_RE.sub('', text)
    text = LINE_COMMENT_RE.sub('', text)
    return text


def load_config(config_path):
    """Returns (exclude_patterns, include_tests, sort_by) from an INI-style config file."""
    if config_path is None or not config_path.is_file():
        return [], False, DEFAULT_SORT_BY

    parser = configparser.ConfigParser()
    parser.read(config_path, encoding='utf-8')

    patterns = []
    if parser.has_option('exclude', 'modules'):
        raw = parser.get('exclude', 'modules')
        patterns = [line.strip() for line in raw.splitlines() if line.strip()]

    include_tests = False
    if parser.has_option('options', 'include_tests'):
        include_tests = parser.getboolean('options', 'include_tests')

    sort_by = DEFAULT_SORT_BY
    if parser.has_option('options', 'sort_by'):
        sort_by = parser.get('options', 'sort_by')

    return patterns, include_tests, sort_by


def is_excluded(module_rel, patterns):
    module_rel_str = str(module_rel)
    return any(fnmatch.fnmatch(module_rel_str, pattern) for pattern in patterns)


def find_modules(root, exclude_patterns=()):
    """Maven modules: directories with a pom.xml and a src/main/java tree."""
    modules = []
    for pom in root.rglob('pom.xml'):
        if any(part in EXCLUDED_DIR_NAMES for part in pom.parts):
            continue
        module_dir = pom.parent
        if not (module_dir / 'src' / 'main' / 'java').is_dir():
            continue
        if is_excluded(module_dir.relative_to(root), exclude_patterns):
            continue
        modules.append(module_dir)
    return sorted(modules)


def module_is_null_marked(module_dir):
    for module_info in module_dir.rglob('module-info.java'):
        if any(part in EXCLUDED_DIR_NAMES for part in module_info.parts):
            continue
        text = strip_comments(module_info.read_text(encoding='utf-8', errors='replace'))
        if NULLMARKED_RE.search(text):
            return True
    return False


def is_top_level_type_marked(text):
    """@NullMarked / @NullUnmarked found before the first top-level type keyword."""
    lines = text.splitlines()
    marked = False
    unmarked = False
    for line in lines:
        if TOP_LEVEL_TYPE_RE.match(line):
            break
        if NULLMARKED_RE.search(line):
            marked = True
        if NULLUNMARKED_RE.search(line):
            unmarked = True
    return marked, unmarked


def analyze_module(module_dir, include_tests):
    source_roots = [module_dir / 'src' / 'main' / 'java']
    if include_tests:
        source_roots.append(module_dir / 'src' / 'test' / 'java')

    module_marked = module_is_null_marked(module_dir)

    package_marked_cache = {}

    def is_package_marked(package_dir):
        if package_dir not in package_marked_cache:
            package_info = package_dir / 'package-info.java'
            marked = False
            if package_info.is_file():
                text = strip_comments(package_info.read_text(encoding='utf-8', errors='replace'))
                marked = bool(NULLMARKED_RE.search(text))
            package_marked_cache[package_dir] = marked
        return package_marked_cache[package_dir]

    covered = []
    uncovered = []

    for source_root in source_roots:
        if not source_root.is_dir():
            continue
        for java_file in sorted(source_root.rglob('*.java')):
            if any(part in EXCLUDED_DIR_NAMES for part in java_file.parts):
                continue
            if java_file.name in ('package-info.java', 'module-info.java'):
                continue

            text = strip_comments(java_file.read_text(encoding='utf-8', errors='replace'))
            class_marked, class_unmarked = is_top_level_type_marked(text)
            package_marked = is_package_marked(java_file.parent)

            rel = java_file.relative_to(module_dir)
            if class_unmarked:
                uncovered.append((rel, '@NullUnmarked on class'))
            elif class_marked or package_marked or module_marked:
                covered.append(rel)
            else:
                uncovered.append((rel, 'no @NullMarked (class/package/module)'))

    return covered, uncovered


def write_module_report(module_dir, covered, uncovered):
    target_dir = module_dir / 'target'
    target_dir.mkdir(parents=True, exist_ok=True)
    report_path = target_dir / 'nullmarked-report.txt'

    total = len(covered) + len(uncovered)
    rate = (len(covered) / total * 100) if total else 100.0

    lines = []
    lines.append(f"NullMarked coverage report for module: {module_dir.name}")
    lines.append(f"Module path: {module_dir}")
    lines.append('')
    lines.append(f"Total classes:    {total}")
    lines.append(f"Covered classes:  {len(covered)}")
    lines.append(f"Uncovered classes: {len(uncovered)}")
    lines.append(f"Completion rate:  {rate:.1f}%")
    lines.append('')

    if uncovered:
        lines.append("Uncovered files:")
        for rel, reason in sorted(uncovered, key=lambda item: str(item[0])):
            lines.append(f"  - {rel}  [{reason}]")
    else:
        lines.append("All classes covered.")

    report_path.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    return report_path, total, rate


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', default='.', help='Repository root (default: current directory)')
    parser.add_argument('--config', default=None,
                         help=f'Path to config file (default: {DEFAULT_CONFIG_NAME} next to this script)')
    parser.add_argument('--exclude', action='append', default=[],
                         help='Wildcard pattern (fnmatch) for module paths to exclude, e.g. "qa/*". '
                              'Repeatable; merged with the config file exclusions.')
    parser.add_argument('--include-tests', action='store_true',
                         help='Also evaluate src/test/java sources (overrides config)')
    parser.add_argument('--sort-by', default=None,
                         help='Comma-separated sort spec, e.g. "Module:ASC,Rate:DESC". '
                              'Columns: Module, Classes, Covered, Rate. Direction defaults to ASC. '
                              f'Overrides config (default: {DEFAULT_SORT_BY}).')
    args = parser.parse_args()

    root = Path(args.root).resolve()
    config_path = Path(args.config).resolve() if args.config else Path(__file__).with_name(DEFAULT_CONFIG_NAME)
    config_excludes, config_include_tests, config_sort_by = load_config(config_path)

    exclude_patterns = config_excludes + args.exclude
    include_tests = args.include_tests or config_include_tests

    try:
        sort_criteria = parse_sort_spec(args.sort_by or config_sort_by)
    except ValueError as exc:
        print(f"Invalid --sort-by: {exc}", file=sys.stderr)
        return 1

    modules = find_modules(root, exclude_patterns)

    if not modules:
        print(f"No Maven modules with src/main/java found under {root}", file=sys.stderr)
        return 1

    summary = []
    for module_dir in modules:
        covered, uncovered = analyze_module(module_dir, include_tests)
        total = len(covered) + len(uncovered)
        if total == 0:
            continue
        report_path, total, rate = write_module_report(module_dir, covered, uncovered)
        summary.append((module_dir.relative_to(root), total, len(covered), rate, report_path))

    sort_summary(summary, sort_criteria)

    grand_total = sum(row[1] for row in summary)
    grand_covered = sum(row[2] for row in summary)
    grand_rate = (grand_covered / grand_total * 100) if grand_total else 100.0

    print(f"{'Module':<55} {'Classes':>8} {'Covered':>8} {'Rate':>7}")
    print('-' * 82)
    for module_rel, total, covered_count, rate, report_path in summary:
        print(f"{str(module_rel):<55} {total:>8} {covered_count:>8} {rate:>6.1f}%")
    print('-' * 82)
    print(f"{'TOTAL':<55} {grand_total:>8} {grand_covered:>8} {grand_rate:>6.1f}%")
    print()
    print(f"Detailed per-module reports written to <module>/target/nullmarked-report.txt")

    return 0


if __name__ == '__main__':
    sys.exit(main())
