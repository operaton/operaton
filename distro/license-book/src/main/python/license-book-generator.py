#!/usr/bin/env python3

# Copyright 2025 the Operaton contributors.
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

import os
import json
import subprocess
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
    "Apache-2.0 WITH LLVM-exception": "Apache-2.0-with-LLVM-exception",
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
    "The GNU General Public License (GPL), Version 2, With Classpath Exception": "GPL-2.0-with-classpath-exception",
    "GNU Lesser General Public License v2.1 only": "LGPL-2.1-only",
    "GNU Lesser General Public License v2.1 or later": "LGPL-2.1-or-later",
    "GNU Library General Public License v2.1 or later": "LGPL-2.1-or-later",
    "GNU Lesser General Public License": "LGPL-2.1-only",
    "GPL2 w/ CPE": "GPL-2.0-with-classpath-exception",
    "Indiana University Extreme! Lab Software License 1.1.1": "IndianaUniversity-1.1.1",
    "lgpl": "LGPL-2.1-only",
    "MIT": "MIT",
    "MIT-0": "MIT-0",
    "MIT-X11": "X11",
    "MIT license": "MIT",
    "MIT License": "MIT",
    "Modified BSD": "BSD-3-Clause",
    "MPL 1.1": "MPL-1.1",
    "MPL 2.0": "MPL-2.0",
    "Public Domain": "CC0-1.0",
    "Public Domain, per Creative Commons CC0": "CC0-1.0",
    "Unicode/ICU License": "Unicode-3.0",
    "Universal Permissive License, Version 1.0": "UPL-1.0",
    "W3C license": "W3C-19990505",
    "jQuery license": "JQUERY",
    "Apache License, Version 2.0 and Common Development And Distribution License (CDDL) Version 1.0 and Eclipse Public License - v 2.0": "(Apache-2.0 AND CDDL-1.0 AND EPL-2.0)",
    "Fabric3 License": "FABRIC3",
    "OFL-1.1": "OFL-1.1",
    "SIL": "OFL-1.1",
    "SEE-LICENSE-IN-LICENSE": "bpmn.io"
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

LIBNAME_LICENSE_OVERRIDES = {
    "requirejs": "MIT",
    "dom4": "MIT",
    "component-events": "MIT",
    "component-props": "MIT",
    "indexof": "MIT",
}


LICENSES_XML_PATH = "target/generated-resources/licenses.xml"
LICENSES_DIR = "target/generated-resources/licenses"
LICENSEBOOK_DIR = "distro/license-book"
SBOM_DIR = "target/sbom"
SBOM_FILES = ['operaton-modules.cyclonedx-json.sbom', 'operaton-webapps.cyclonedx-json.sbom']
CMD_BUILD_SBOM = ".devenv/scripts/build/build-sbom.sh"

def run_sbom_generation():
    need_generation = False
    for f in SBOM_FILES:
        if not os.path.exists(os.path.join(SBOM_DIR, f)):
            need_generation = True

    if not need_generation:
        print("[INFO] SBOMs already exist, skipping generation.")
        print(f"[INFO]   Execute command {CMD_BUILD_SBOM} or delete {SBOM_DIR} to regenerate.")
        return

    print("[INFO] Generating SBOMs...")
    with open('distro/license-book/target/license-book-generator.out', 'a') as out_file:
        subprocess.run([".devenv/scripts/build/build-sbom.sh"], check=True, stdout=out_file, stderr=out_file)

def read_sbom(sbom_path):
    with open(sbom_path, 'r', encoding='utf-8') as f:
        sbom = json.load(f)

    dependencies = []
    components = sbom.get('components', [])
    for comp in components:
        purl = comp.get('purl', '')
        group = comp.get('group', '')
        name = comp.get('name', '')

        if group.startswith('org.operaton'):
            continue
        if name == 'package-lock.json':
            continue

        licenses_info = comp.get('licenses', [])
        license_names = []
        for lic in licenses_info:
            lic_name = lic.get('license', {}).get('id', '')
            if not lic_name:
                lic_name = lic.get('expression', {})
            if not lic_name:
                lic_name = lic.get('license', {}).get('name', '')
                if lic_name:
                    lic_name = LICENSE_SPDX_MAP.get(lic_name)
            if not lic_name or lic_name == '':
                print(f"[DEBUG] Unknown license {lic}")
                lic_name = 'UNKNOWN'
            if lic_name:
                license_names.append(lic_name)
        if not license_names:
            license_names.append(LIBNAME_LICENSE_OVERRIDES.get(name, 'UNKNOWN'))

        license_str = ', '.join(license_names).replace('\n', ' ').replace('\r', ' ')
        dependencies.append({
            "purl": purl,
            "group": group,
            "name": name,
            "version": comp.get('version', ''),
            "description": comp.get('description', ''),
            "licenses": license_str
        })
    result = sorted(dependencies, key=lambda k: k['licenses']+':'+k['group']+':'+k['name'])
    return result

def generate_license_book():
    # read licenses from files
    licenses_dir = 'distro/license-book/src/main/resources/licenses'
    licenses = {}
    for file_path in glob.glob(os.path.join(licenses_dir, '*.txt')):
        key = os.path.splitext(os.path.basename(file_path))[0]
        with open(file_path, 'r', encoding='utf-8') as f:
            licenses[key] = {'license_name': key, 'license_name_lower': key.lower(), 'license_text': f.read()}

    print(f"[INFO] Loaded {len(licenses)} licenses for the license book.")

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

    mvn_dependencies = read_sbom(SBOM_DIR + '/operaton-modules.cyclonedx-json.sbom')
    print(f"[INFO] Loaded {len(mvn_dependencies)} Maven dependencies for the license book.")

    npm_dependencies = read_sbom(SBOM_DIR + '/operaton-webapps.cyclonedx-json.sbom')
    print(f"[INFO] Loaded {len(npm_dependencies)} NPM dependencies for the license book.")

    # define variables
    license_ids = sorted(
        [{"id": k, "id_lower": k.lower()} for k in licenses.keys()],
        key=lambda x: x["id_lower"]
    )

    date_str = datetime.datetime.now().strftime('%Y-%m-%d')
    renderer = get_renderer_with_partials()
    output = renderer.render(template, {
        'version': version_str,
        'date': date_str,
        'license_ids': license_ids,
        'licenses': licenses.values(),
        'npm_dependencies': npm_dependencies,
        'mvn_dependencies': mvn_dependencies
    })

    # write output file
    os.makedirs('distro/license-book/target/generated-resources', exist_ok=True)
    out_path = 'distro/license-book/src/main/resources/LICENSE_BOOK.md'
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
    print("[INFO] Generating license book...")
    run_sbom_generation()
    generate_license_book()
    print("[INFO] Done.")

if __name__ == "__main__":
    main()
