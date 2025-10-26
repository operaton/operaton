#!/usr/bin/env python3

import os
import json
import subprocess
import sys
import shutil
import glob
import datetime
import xml.etree.ElementTree as ET
import pystache

# A mapping of license names to SPDX identifiers
# The license names are taken from common variations found in Maven reports
LICENSE_SPDX_MAP = {
    "The Apache Software License, Version 2.0": "Apache-2.0",
    "Apache License 2.0": "Apache-2.0",
    "Apache License, Version 2.0": "Apache-2.0",
    "Apache 2": "Apache-2.0",
    "Apache 2.0": "Apache-2.0",
    "Apache-2.0": "Apache-2.0",
    "Apache License Version 2.0": "Apache-2.0",
    "Apache License, version 2.0": "Apache-2.0",
    "The Apache License, Version 2.0": "Apache-2.0",
    "Apache License, 2.0": "Apache-2.0",
    "BSD": "BSD-3-Clause",
    "BSD-2-Clause": "BSD-2-Clause",
    "BSD 2-Clause": "BSD-2-Clause",
    "BSD-3-Clause": "BSD-3-Clause",
    "Common Development and Distribution License": "CDDL-1.0",
    "CDDL 1.1": "CDDL-1.1",
    "Eclipse Public License - v 1.0": "EPL-1.0",
    "Eclipse Public License 1.0": "EPL-1.0",
    "Eclipse Public License v2.0": "EPL-2.0",
    "Eclipse Public License v. 2.0": "EPL-2.0",
    "Eclipse Public License - v 2.0": "EPL-2.0",
    "Eclipse Public License, Version 2.0": "EPL-2.0",
    "Eclipse Public License 2.0": "EPL-2.0",
    "EDL 1.0": "EPL-2.0",
    "EPL 1.0": "EPL-1.0",
    "EPL 2.0": "EPL-2.0",
    "EPL-2.0": "EPL-2.0",
    "GNU General Public License, version 2 with the GNU Classpath Exception": "GPL-2.0-with-classpath-exception",
    "GNU General Public License, Version 2 with the Classpath Exception": "GPL-2.0-with-classpath-exception",
    "GNU Lesser General Public License v2.1 only": "LGPL-2.1-only",
    "GNU Lesser General Public License v2.1 or later": "LGPL-2.1-or-later",
    "GNU Library General Public License v2.1 or later": "LGPL-2.1-or-later",
    "GPL2 w/ CPE": "GPL-2.0-with-classpath-exception",
    "lgpl": "LGPL-2.1-only",
    "MIT": "MIT",
    "MIT-0": "MIT-0",
    "MIT license": "MIT",
    "MIT License": "MIT",
    "Modified BSD": "BSD-3-Clause",
    "MPL 1.1": "MPL-1.1",
    "MPL 2.0": "MPL-2.0",
    "Public Domain": "CC0-1.0",
    "Public Domain, per Creative Commons CC0": "CC0-1.0",
    "Unicode/ICU License": "Unicode-3.0",
    "Universal Permissive License, Version 1.0": "UPL-1.0",
}

SPDX_FILE_MAP = {
    "Apache-2.0": "apache license 2.0 - apache-2.0.txt",
    "BSD-2-Clause": "bsd-2-clause - bsd-2-clause.html",
    "BSD-3-Clause": "bsd-3-clause - license.txt",
    "CDDL-1.0": "common development and distribution license - cddl.txt",
    "CDDL-1.1": "cddl 1.1 - cddl+gpl-1.1.txt",
    "EPL-1.0": "epl 1.0 - eclipse-1.0.html",
    "EPL-2.0": "epl 2.0 - epl-2.0.html",
    "GPL-2.0-with-classpath-exception": "gnu general public license, version 2 with the classpath exception - gpl-2.0-ce.txt",
    "LGPL-2.1-only": "lgpl - lgpl.txt",
    "LGPL-2.1-or-later": "lgpl - lgpl-2.1.txt",
    "MIT": "mit - mit.html",
    "MIT-0": "mit-0 - mit-0.html",
    "MPL-1.1": "mpl 1.1 - 1.1.html",
    "MPL-2.0": "mpl 2.0 - 2.0.html",
    "CC0-1.0": "public domain - cc0-1.0.txt",
    "Unicode-3.0": "unicode_icu license - license.txt",
    "UPL-1.0": "universal permissive license, version 1.0 - upl.html"
}

LICENSES_XML_PATH = "target/generated-resources/licenses.xml"
LICENSES_DIR = "target/generated-resources/licenses"
LICENSEBOOK_DIR = "distro/license-book"

def run_maven_license_report():
    print("[INFO] Generating Maven license report...")
    subprocess.run([
        "./mvnw",
        "-Pdistro,distro-run,distro-tomcat,distro-wildfly,distro-webjar,distro-starter,distro-serverless,h2-in-memory,check-api-compatibility",
        "license:aggregate-add-third-party"
    ], check=True)

def run_npm_license_report():
    print("[INFO] Generating Node.js license report...")
    subprocess.run(["npm", "install", "license-checker"], check=True)
    subprocess.run([
        "license-checker",
        "--production",
        "--relativeLicensePath",
        "--json",
        "--out",
        "target/generated-resources/npm-licenses.json"
    ], check=True)

def normalize_license_name(name):
    return name.strip().lower()


def postprocess_licenses_xml():
    if not os.path.exists(LICENSES_XML_PATH):
        print(f"[WARN] {LICENSES_XML_PATH} not found, skipping postprocessing.")
        return

    print("[INFO] Postprocessing licenses.xml and renaming license files to SPDX names...")
    tree = ET.parse(LICENSES_XML_PATH)
    root = tree.getroot()
    renamed = {}

    for lic in root.findall(".//license"):
        name = lic.findtext("name")
        file_elem = lic.find("file")
        if name and file_elem is not None:
            spdx_id = LICENSE_SPDX_MAP.get(name)
            if spdx_id is None and name in LICENSE_SPDX_MAP.values():
                spdx_id = name
            print(f"[DEBUG] {name} -> {spdx_id}")
            if spdx_id:
                lic.find("name").text = spdx_id
                # Rename file if not already done
                #src = os.path.join(LICENSES_DIR, orig_file)
                #dst = os.path.join(LICENSES_DIR, spdx_id)
                #if os.path.exists(src) and not os.path.exists(dst):
                #    shutil.move(src, dst)
                #    renamed[orig_file] = spdx_id

    tree.write(LICENSES_XML_PATH, encoding="utf-8", xml_declaration=True)
    if renamed:
        print(f"[INFO] Renamed license files: {renamed}")
    else:
        print("[INFO] No license files needed renaming.")


def copy_license_book_to_dist():
    """Copy the content of target/generated-resources/license-book to LICENSEBOOK_DIR/target/generated-resources."""
    src = "target/generated-resources/license-book"
    dst = os.path.join(LICENSEBOOK_DIR, "target/generated-resources/")
    if not os.path.exists(src):
        print(f"[WARN] Source directory {src} does not exist, nothing to copy.")
        return
    print(f"[INFO] Copying license book from {src} to {dst} ...")
    if os.path.exists(dst):
        shutil.rmtree(dst)
    shutil.copytree(src, dst)
    for f in ['npm-licenses.json', 'licenses.xml']:
        shutil.copy2(os.path.join("target/generated-resources", f), os.path.join(dst, f))
    print("[INFO] License book copied successfully.")

def read_dependencies_from_xml(xml_path):
    tree = ET.parse(xml_path)
    root = tree.getroot()
    dependencies = []
    for dep in root.findall(".//dependency"):
        group_id = dep.findtext("groupId")
        artifact_id = dep.findtext("artifactId")
        version = dep.findtext("version")
        licenses = dep.findall('.//license/name')
        license_names = [l.text for l in licenses]
        license_str = ', '.join(license_names).replace('\n', ' ').replace('\r', ' ')
        licenses = []
        for lic in dep.findall(".//license"):
            name = lic.findtext("name")
            if name:
                licenses.append(name)
        dependencies.append({
            "groupId": group_id,
            "artifactId": artifact_id,
            "version": version,
            "licenses": license_str
        })
    return dependencies

def generate_license_book():
    # read licenses from files
    licenses_dir = 'distro/license-book/src/main/resources/licenses'
    licenses = {}
    for file_path in glob.glob(os.path.join(licenses_dir, '*.txt')):
        key = os.path.splitext(os.path.basename(file_path))[0]
        with open(file_path, 'r', encoding='utf-8') as f:
            licenses[key] = {'license_name': key, 'license_text': f.read()}

    print(f"[INFO] Loaded {len(licenses)} npm libraries for the license book.")

    mvn_dependencies = read_dependencies_from_xml('distro/license-book/target/generated-resources/licenses.xml')
    print(f"[INFO] Loaded {len(mvn_dependencies)} Maven dependencies for the license book.")

    # get project version from pom.xml
    pom_path = 'distro/license-book/pom.xml'
    tree = ET.parse(pom_path)
    root = tree.getroot()
    parent = root.find('{http://maven.apache.org/POM/4.0.0}parent')
    version = parent.find('{http://maven.apache.org/POM/4.0.0}version')
    version_str = version.text if version is not None else 'unknown'

    # read main template
    tpl_path = 'distro/license-book/src/main/mustache/license-book.tpl'
    with open(tpl_path, 'r', encoding='utf-8') as tpl_file:
        template = tpl_file.read()

    renderer = get_renderer_with_partials()

    json_path="distro/license-book/target/generated-resources/npm-licenses.json"
    with open(json_path, 'r', encoding='utf-8') as json_file:
        npm_licenses = json.load(json_file)
    for library, data in npm_licenses.items():
        data['library'] = library.split('@')[0]
        data['name'] = "X " + library
        data['version'] = library.split('@')[1]

    # define variables
    license_ids=sorted(licenses.keys(), key=lambda k: k.lower())
    date_str = datetime.datetime.now().strftime('%Y-%m-%d')
    npm_licenses = sorted(npm_licenses.values(), key=lambda k: k['library'])
    output = renderer.render(template, {
        'version': version_str,
        'date': date_str,
        'license_ids': license_ids,
        'licenses': licenses.values(),
        'npm_licenses': npm_licenses,
        'mvn_dependencies': mvn_dependencies
    })

    # write output file
    os.makedirs('distro/license-book/target/generated-resources', exist_ok=True)
    out_path = 'distro/license-book/target/generated-resources/license-book.md'
    with open(out_path, 'w', encoding='utf-8') as out_file:
        out_file.write(output)

def get_renderer_with_partials():
    partials = {}
    tpl_dir = 'distro/license-book/src/main/mustache/'

    for fname in os.listdir(tpl_dir):
        if fname.endswith('.tpl'):
            key = fname[:-4]
            with open(os.path.join(tpl_dir, fname), encoding='utf-8') as f:
                partials[key] = f.read()

    renderer = pystache.Renderer(partials=partials)
    return renderer

def main():
    #run_maven_license_report()
    #run_npm_license_report()
    #postprocess_licenses_xml()
    #copy_license_book_to_dist()
    generate_license_book()
    print("[INFO] License reports generated and postprocessed. You can now process them to create the license book.")

if __name__ == "__main__":
    main()
