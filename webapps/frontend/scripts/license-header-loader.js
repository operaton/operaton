/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var fs = require('fs');
var path = require('path');

const addMissingLicenseHeaders = (filePath, source) => {
  // This fix ensures windows compatibility
  // See https://github.com/camunda/camunda-bpm-platform/issues/2824
  const rowFile = filePath.replace(/\\/g, '/');
  if (
    rowFile &&
    !rowFile.endsWith('.json') &&
    !/@license|@preserve|@lic|@cc_on|^\/\**!/i.test(source)
  ) {
    let pkg = null;
    if (rowFile.includes('node_modules')) {
      pkg = rowFile.replace(
        /^(.*)node_modules\/(?:@[a-z-\d.]+\/[a-z-\d.]+)?([a-z-\d.]+)?(.*)$/,
        (match, p1, p2) => p2 || match.split('/').slice(-2)[0],
      );
    } else if (!rowFile.includes('operaton-bpm-sdk-js')) {
      pkg = rowFile.replace(
        /^(@[a-z-\d.]+\/[a-z-\d.]+)?([a-z-\d.]+)?(.*)$/,
        (match, p1, p2) => p2 || p1,
      );
    }

    if (pkg) {
      // attempt to resolve package.json via Node resolution first (handles nested deps)
      let packageJsonPath = null;
      try {
        packageJsonPath = require.resolve(`${pkg}/package.json`);
      } catch (_err) {
        // fallback candidates: top-level, nested node_modules based on resource path, and legacy operaton-bpm-webapp path
        const candidates = [];
        const topLevel = path.join(process.cwd(), 'node_modules', pkg, 'package.json');
        candidates.push(topLevel);
        // find nested node_modules segments in the resource path
        const parts = rowFile.split('node_modules');
        for (let i = 0; i < parts.length - 1; i++) {
          const base = parts.slice(0, i + 1).join('node_modules').replace(/\\/g, '/');
          const candidate = path.join(process.cwd(), base, 'node_modules', pkg, 'package.json');
          candidates.push(candidate);
        }
        // legacy fallback
        candidates.push(path.join(process.cwd(), 'node_modules', 'operaton-bpm-webapp', 'node_modules', pkg, 'package.json'));
        for (let i = 0; i < candidates.length; i++) {
          if (fs.existsSync(candidates[i])) {
            packageJsonPath = candidates[i];
            break;
          }
        }
      }

      if (!packageJsonPath) {
        console.log(`Could not resolve package.json for ${pkg}`); // eslint-disable-line
        return source;
      }

      const packageDir = path.dirname(packageJsonPath);

      let licenseInfo = null;
      try {
        licenseInfo = fs.readFileSync(path.join(packageDir, 'LICENSE'), 'utf8');
      } catch (_e) {
        try {
          licenseInfo = fs.readFileSync(path.join(packageDir, 'LICENSE.md'), 'utf8');
        } catch (_e) {
          try {
            licenseInfo = fs.readFileSync(path.join(packageDir, 'LICENSE-MIT.txt'), 'utf8');
          } catch (_e) {
            try {
              licenseInfo = fs.readFileSync(path.join(packageDir, 'LICENSE.txt'), 'utf8');
            } catch (_e) {
              console.log(`${pkg} has no license file. 🤷‍`); // eslint-disable-line
            }
          }
        }
      }

      const pkgJson = require(packageJsonPath);
      const {version, license} = pkgJson;
      if (licenseInfo) {
        return `/*!\n@license ${pkg}@${version}\n${licenseInfo}*/\n${source}`;
      } else if (license) {
        console.log(`${pkg} has a "license" property. 🤷‍`);// eslint-disable-line
        return `/*! @license ${pkg}@${version} (${license}) */\n${source}`;
      }
    }
  } else {
    return source;
  }
};

module.exports = function (source) {
  return addMissingLicenseHeaders(this.resourcePath, source);
};
