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
Derives JSpecify nullability evidence for DbEntity implementors.

For each mapped field of an entity it reports the MyBatis column, the
owning table, how many of the seven DDL dialects declare that column
NOT NULL, how many call sites pass null to the setter, and the resulting
annotation decision.

A column counts as NOT NULL for a dialect when its definition says so or
when it is named in that table's primary key clause: SQL forbids a null
primary key, and several dialects rely on that rather than spelling it out.
"""

import argparse
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

DIALECTS = ['h2', 'postgres', 'oracle', 'mysql', 'mssql', 'db2', 'mariadb']

CREATE_DIR = 'engine/src/main/resources/org/operaton/bpm/engine/db/create'
UPGRADE_DIR = 'engine/src/main/resources/org/operaton/bpm/engine/db/upgrade'
MAPPING_DIR = ('engine/src/main/resources/org/operaton/bpm/engine/impl'
               '/mapping/entity')
JAVA_DIR = 'engine/src/main/java'

# Table body ends at a line starting with ")" followed by anything up to ";".
# mysql/mariadb close with ") ENGINE=InnoDB DEFAULT CHARSET=utf8 ...;".
TABLE_RE = re.compile(
    r'create\s+table\s+(\w+)\s*\((.*?)\n\s*\)[^;]*;', re.S | re.I)
COLUMN_RE = re.compile(r'^([A-Z][A-Z0-9_]*_)\s+\S+', re.I)
NOT_NULL_RE = re.compile(r'not\s+null', re.I)
PRIMARY_KEY_RE = re.compile(r'primary\s+key\s*\(([^)]*)\)', re.I)


@dataclass
class ResultMap:
    entity: str
    table: Optional[str] = None
    columns: dict = field(default_factory=dict)
    # {COLUMN: constructor parameter name or None}, in declaration order
    constructor_columns: dict = field(default_factory=dict)
    reference_count: int = 0


def parse_ddl(path):
    """Return {TABLE: {COLUMN: is_not_null}} for one DDL file."""
    text = path.read_text(errors='ignore')
    tables = {}
    for match in TABLE_RE.finditer(text):
        name = match.group(1).upper()
        body = match.group(2)
        pk_match = PRIMARY_KEY_RE.search(body)
        pk = ({c.strip().upper() for c in pk_match.group(1).split(',')}
              if pk_match else set())
        columns = {}
        for line in body.split('\n'):
            line = line.strip().rstrip(',')
            col_match = COLUMN_RE.match(line)
            if not col_match:
                continue
            column = col_match.group(1).upper()
            columns[column] = bool(NOT_NULL_RE.search(line)) or column in pk
        tables.setdefault(name, {}).update(columns)
    return tables


def ddl_index(root):
    """Return {dialect: {TABLE: {COLUMN: is_not_null}}}."""
    base = Path(root) / CREATE_DIR
    index = {}
    for dialect in DIALECTS:
        merged = {}
        for path in sorted(base.glob(f'activiti.{dialect}.create.*.sql')):
            for table, columns in parse_ddl(path).items():
                merged.setdefault(table, {}).update(columns)
        index[dialect] = merged
    return index


def notnull_dialect_count(index, table, column):
    return sum(1 for d in DIALECTS
               if index[d].get(table, {}).get(column) is True)


def column_is_known(index, table, column):
    return any(column in index[d].get(table, {}) for d in DIALECTS)


def upgrade_relaxations(root):
    """Return {(TABLE, COLUMN)} that an upgrade script makes nullable."""
    base = Path(root) / UPGRADE_DIR
    relaxed = set()
    pattern = re.compile(
        r'alter\s+table\s+(\w+)\s+(?:alter|modify)\s+(?:column\s+)?(\w+)\b'
        r'(?![^;]*not\s+null)[^;]*(?:null|drop\s+not\s+null)', re.I)
    for path in sorted(base.glob('*.sql')):
        for match in pattern.finditer(path.read_text(errors='ignore')):
            relaxed.add((match.group(1).upper(), match.group(2).upper()))
    return relaxed


def parse_result_maps(path):
    """Return {entity_fqcn: ResultMap} for one mapper XML."""
    text = path.read_text(errors='ignore')
    maps = {}
    insert_match = re.search(r'insert\s+into\s+(?:\$\{prefix\})?(\w+)', text, re.I)
    table = insert_match.group(1).upper() if insert_match else None

    for match in re.finditer(
            r'<resultMap\b([^>]*)>(.*?)</resultMap>', text, re.S):
        attrs, body = match.group(1), match.group(2)
        type_match = re.search(r'type="([^"]+)"', attrs)
        id_match = re.search(r'id="([^"]+)"', attrs)
        if not type_match:
            continue
        entity = type_match.group(1)
        result_map = maps.setdefault(entity, ResultMap(entity=entity))
        result_map.table = result_map.table or table

        constructor = re.search(r'<constructor\b(.*?)</constructor>', body, re.S)
        if constructor:
            for arg in re.finditer(r'<(?:idArg|arg)\b([^>]*)>', constructor.group(1)):
                column_match = re.search(r'column="([^"]+)"', arg.group(1))
                if not column_match:
                    continue
                name_match = re.search(r'name="([^"]+)"', arg.group(1))
                result_map.constructor_columns[column_match.group(1).upper()] = (
                    name_match.group(1) if name_match else None)

        outside = re.sub(r'<constructor\b.*?</constructor>', '', body, flags=re.S)
        outside = re.sub(r'<discriminator\b.*?</discriminator>', '',
                         outside, flags=re.S)
        outside = re.sub(r'<discriminator\b[^>]*/>', '', outside)
        for entry in re.finditer(
                r'<(?:id|result)\b[^>]*property="([^"]+)"[^>]*'
                r'column="([^"]+)"', outside):
            result_map.columns[entry.group(1)] = entry.group(2).upper()

        if id_match:
            result_map.reference_count = len(
                re.findall(rf'"{re.escape(id_match.group(1))}"', text))
    return maps


def setter_name(prop):
    return 'set' + prop[0].upper() + prop[1:]


def count_null_callers(root, prop):
    """Count occurrences of setProp(null) across engine main sources."""
    needle = setter_name(prop) + '(null)'
    total = 0
    for path in (Path(root) / JAVA_DIR).rglob('*.java'):
        total += path.read_text(errors='ignore').count(needle)
    return total


def find_result_map(root, entity_simple_name):
    for path in sorted((Path(root) / MAPPING_DIR).glob('*.xml')):
        maps = parse_result_maps(path)
        for fqcn, result_map in maps.items():
            if fqcn.rsplit('.', 1)[-1] == entity_simple_name:
                return result_map
    return None


def evidence_for(root, entity_simple_name):
    result_map = find_result_map(root, entity_simple_name)
    if result_map is None:
        return []
    index = ddl_index(root)
    relaxed = upgrade_relaxations(root)
    table = result_map.table

    def ddl_evidence(column):
        known = table and column_is_known(index, table, column)
        count = notnull_dialect_count(index, table, column) if known else 0
        flags = [] if known else ['PROJECTION']
        if result_map.reference_count > 1:
            flags.append('REUSED_RESULTMAP')
        if (table, column) in relaxed:
            flags.append('UPGRADE_RELAXED')
        return count, flags

    rows = []
    # Constructor arguments have no setter, so only the DDL decides.
    for column, name in result_map.constructor_columns.items():
        count, flags = ddl_evidence(column)
        flags.insert(0, 'CONSTRUCTOR')
        decision = ('CONSTRUCTOR_NONNULL'
                    if count == 7 and 'UPGRADE_RELAXED' not in flags
                    else 'NULLABLE')
        rows.append((entity_simple_name, name or f'<constructor:{column}>',
                     column, table or '', f'{count}/7', 0, '|'.join(flags),
                     decision))
    for prop, column in sorted(result_map.columns.items()):
        count, flags = ddl_evidence(column)
        callers = count_null_callers(root, prop)
        decision = ('SETTER_NONNULL'
                    if count == 7 and callers == 0
                    and 'UPGRADE_RELAXED' not in flags
                    else 'NULLABLE')
        rows.append((entity_simple_name, prop, column, table or '',
                     f'{count}/7', callers, '|'.join(flags), decision))
    return rows


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('entities', nargs='+',
                        help='simple class names, e.g. JobEntity')
    parser.add_argument('--root', default='.',
                        help='repository root (default: current directory)')
    args = parser.parse_args(argv)

    print('entity,property,column,table,notnull_dialects,null_callers,'
          'flags,decision')
    missing = []
    for entity in args.entities:
        rows = evidence_for(args.root, entity)
        if not rows:
            missing.append(entity)
        for row in rows:
            print(','.join(str(cell) for cell in row))
    for entity in missing:
        print(f'# no result map found for {entity}', file=sys.stderr)
    return 0


if __name__ == '__main__':
    sys.exit(main())
