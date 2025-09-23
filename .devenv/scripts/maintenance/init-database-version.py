#!/usr/bin/env python3

import argparse
import os
import re
import shutil
import sys
from lxml import etree as ET
import datetime

DATABASES = ["h2", "db2", "mariadb", "mssql", "mysql", "oracle", "postgres"]
SELECTED_DATABASES = DATABASES.copy()

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

def error(msg):
    print(f"Error: {msg}", file=sys.stderr)
    sys.exit(1)

def validate_version(version):
    return re.match(r"^\d+\.\d+\.\d+$", version)

def get_version_without_patch(version):
    return ".".join(version.split(".")[:2])

def get_version_without_patch_without_dot(version):
    return version.replace(".", "")

def get_property_from_pom(pom_path, prop):
    tree = ET.parse(pom_path)
    root = tree.getroot()
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    for el in root.findall(".//m:properties/*", ns):
        if el.tag.endswith(prop):
            return el.text.strip()
    error(f"Property {prop} not found in {pom_path}")

def set_property_in_pom(pom_path, prop, value):
    tree = ET.parse(pom_path)
    root = tree.getroot()
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    found = False
    for el in root.findall(".//m:properties/*", ns):
        if el.tag.endswith(prop):
            el.text = value
            found = True
    if not found:
        error(f"Property {prop} not found in {pom_path}")
    tree.write(pom_path, encoding="UTF-8", xml_declaration=True, pretty_print=True)

def prompt_version(current, previous, proposed, assume_yes):
    if assume_yes:
        return proposed
    while True:
        print(f"Current database version: {current}")
        print(f"Previous database version: {previous}")
        v = input(f"Enter new database version [{proposed}]: ").strip()
        if not v:
            v = proposed
        if validate_version(v):
            return v
        print("Invalid version format. Please use semantic versioning (major.minor.patch)")

def prompt_databases(assume_yes):
    if assume_yes:
        return DATABASES.copy()
    selected = set(DATABASES)
    while True:
        print("\nAvailable databases:")
        for i, db in enumerate(DATABASES, 1):
            mark = "[*]" if db in selected else "[ ]"
            print(f"  {i}. {mark} {db}")
        inp = input("Enter number to toggle selection, or press Enter to continue: ").strip()
        if not inp:
            if selected:
                return list(selected)
            print("At least one database must be selected.")
            continue
        if inp.isdigit() and 1 <= int(inp) <= len(DATABASES):
            db = DATABASES[int(inp)-1]
            if db in selected:
                selected.remove(db)
            else:
                selected.add(db)
        else:
            print("Invalid input.")

def update_create_engine_file(db, new_version):
    path = f"engine/src/main/resources/org/operaton/bpm/engine/db/create/activiti.{db}.create.engine.sql"
    if not os.path.isfile(path):
        print(f"Warning: {path} not found")
        return
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    new_content = re.sub(
        r"values \('0', CURRENT_TIMESTAMP, '[^']*'\);",
        f"values ('0', CURRENT_TIMESTAMP, '{new_version}');",
        content
    )
    with open(path, "w", encoding="utf-8") as f:
        f.write(new_content)

def get_schema_id_from_upgrade_file(path, current_version):
    if not os.path.isfile(path):
        return None
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            m = re.search(r"values \('(\d+)', CURRENT_TIMESTAMP, '"+re.escape(current_version)+"'\);", line)
            if m:
                return int(m.group(1))
    return None

def create_upgrade_file(db, current_version, new_version, prev_vwp, cur_vwp, new_vwp):
    upgrade_dir = "engine/src/main/resources/org/operaton/bpm/engine/db/upgrade"
    prev_file = f"{upgrade_dir}/{db}_engine_{prev_vwp}_to_{cur_vwp}.sql"
    new_file = f"{upgrade_dir}/{db}_engine_{cur_vwp}_to_{new_vwp}.sql"
    schema_id = get_schema_id_from_upgrade_file(prev_file, current_version)
    if schema_id is None:
        print(f"Warning: Could not extract schema ID from {prev_file}")
        return
    new_schema_id = schema_id + 100
    year = datetime.datetime.now().year
    content = f"""--
-- Copyright {year} the Operaton contributors.
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at:
--
--     https://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

insert into ACT_GE_SCHEMA_LOG
values ('{new_schema_id}', CURRENT_TIMESTAMP, '{new_version}');
"""
    with open(new_file, "w", encoding="utf-8") as f:
        f.write(content)

def update_liquibase_changelog(cur_vwp, new_vwp, new_version):
    changelog = "engine/src/main/resources/org/operaton/bpm/engine/db/liquibase/operaton-changelog.xml"
    if not os.path.isfile(changelog):
        print(f"Error: {changelog} not found")
        return
    with open(changelog, "r", encoding="utf-8") as f:
        lines = f.readlines()
    idx = len(lines) - 1
    while idx >= 0 and not lines[idx].strip().endswith("</databaseChangeLog>"):
        idx -= 1
    if idx < 0:
        print("Error: Could not find </databaseChangeLog> in changelog")
        return
    new_changeset = f"""
  <changeSet author="Operaton" id="{cur_vwp}-to-{new_vwp}">
    <sqlFile path="upgrade/${{db.name}}_engine_{cur_vwp}_to_{new_vwp}.sql"
             encoding="UTF-8"
             relativeToChangelogFile="true"
             splitStatements="true"
             stripComments="true"/>
  </changeSet>

  <changeSet author="Operaton" id="{new_version}-tag">
    <tagDatabase tag="{new_version}"/>
  </changeSet>

"""
    lines = lines[:idx] + [new_changeset] + lines[idx:]
    with open(changelog, "w", encoding="utf-8") as f:
        f.writelines(lines)

def create_test_fixture(cur_vwpwd, new_vwpwd, current_version, new_version):
    src = f"qa/test-db-instance-migration/test-fixture-{cur_vwpwd}"
    dst = f"qa/test-db-instance-migration/test-fixture-{new_vwpwd}"
    if not os.path.isdir(src):
        print(f"Warning: Source test fixture directory not found: {src}")
        return
    if os.path.exists(dst):
        print(f"Warning: Target test fixture directory already exists: {dst}")
        return
    shutil.copytree(src, dst)
    tf_java = os.path.join(dst, "src/main/java/org/operaton/bpm/qa/upgrade/TestFixture.java")
    if os.path.isfile(tf_java):
        with open(tf_java, "r", encoding="utf-8") as f:
            content = f.read()
        content = re.sub(
            rf'public static final String ENGINE_VERSION = "{re.escape(current_version)}";',
            f'public static final String ENGINE_VERSION = "{new_version}";',
            content
        )
        with open(tf_java, "w", encoding="utf-8") as f:
            f.write(content)
    else:
        print(f"Warning: TestFixture.java not found in {dst}")
    print(f"Warning: Do not forget to add the new files at: {dst}")

def update_old_engine_pom(current_version, previous_version):
    pom = "qa/test-old-engine/pom.xml"
    if not os.path.isfile(pom):
        print(f"Warning: {pom} not found")
        return
    with open(pom, "r", encoding="utf-8") as f:
        content = f.read()
    content = re.sub(
        r"<operaton.old.engine.version>[^<]*</operaton.old.engine.version>",
        f"<operaton.old.engine.version>{previous_version}</operaton.old.engine.version>",
        content
    )
    with open(pom, "w", encoding="utf-8") as f:
        f.write(content)

def main():
    parser = argparse.ArgumentParser(description="Initialize new database version for Operaton")
    parser.add_argument("--new-version", help="Specify the new database version (semantic versioning: major.minor.patch)")
    parser.add_argument("--database", help="Comma-separated list of databases to update")
    parser.add_argument("-y", "--assume-yes", action="store_true", help="Assume yes to all prompts (non-interactive)")
    args = parser.parse_args()

    pom_path = "database/pom.xml"
    if not os.path.isfile(pom_path):
        error("This script must be run from the repository root")

    current_version = get_property_from_pom(pom_path, "operaton.dbscheme.current.version")
    previous_version = get_property_from_pom(pom_path, "operaton.dbscheme.previous.version")

    if args.new_version:
        if not validate_version(args.new_version):
            error("Invalid version format. Please use semantic versioning (major.minor.patch)")
        new_version = args.new_version
    else:
        major, minor, _ = current_version.split(".")
        proposed = f"{major}.{int(minor)+1}.0"
        new_version = prompt_version(current_version, previous_version, proposed, args.assume_yes)

    if args.database:
        selected = [db.strip() for db in args.database.split(",") if db.strip()]
        for db in selected:
            if db not in DATABASES:
                error(f"Invalid database '{db}'. Supported: {', '.join(DATABASES)}")
        selected_databases = selected
    else:
        print("\nSelect databases to update:")
        selected_databases = prompt_databases(args.assume_yes)

    cur_vwp = get_version_without_patch(current_version)
    cur_vwpwd = get_version_without_patch_without_dot(cur_vwp)
    prev_vwp = get_version_without_patch(previous_version)
    new_vwp = get_version_without_patch(new_version)
    new_vwpwd = get_version_without_patch_without_dot(new_vwp)

    print("\nSummary:")
    print(f"  Current version: {current_version}")
    print(f"  Previous version: {previous_version}")
    print(f"  New version: {new_version}")
    print(f"  Selected databases: {', '.join(selected_databases)}")
    if not args.assume_yes:
        confirm = input("Proceed with these settings? [y/N]: ").strip().lower()
        if confirm != "y":
            print("Aborted.")
            sys.exit(0)

    for db in selected_databases:
        print(f"Processing database: {db}")
        update_create_engine_file(db, new_version)
        create_upgrade_file(db, current_version, new_version, prev_vwp, cur_vwp, new_vwp)

    set_property_in_pom(pom_path, "operaton.dbscheme.current.version", new_version)
    set_property_in_pom(pom_path, "operaton.dbscheme.previous.version", current_version)
    update_liquibase_changelog(cur_vwp, new_vwp, new_version)
    create_test_fixture(cur_vwpwd, new_vwpwd, current_version, new_version)
    update_old_engine_pom(current_version, previous_version)

    print("\nDatabase version update completed successfully!")
    print("Please review the changes and commit them when ready.")

if __name__ == "__main__":
    main()
