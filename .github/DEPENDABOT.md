<!--
Copyright 2026 the Operaton contributors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at:

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Dependabot maintenance

[dependabot.yml](dependabot.yml) defines the update schedules and branch policies.
Prefer explicit, reviewable `ignore` rules in this file for temporary compatibility
constraints. Record their reason and remove them when the constraint is resolved.

## Jackson 2 update policy

- `main` allows Jackson 2 patch and minor updates. Major updates remain excluded
  by the general Maven policy. The switch to the unshaded FEEL engine in
  [#3342](https://github.com/operaton/operaton/pull/3342) removed the old coupling
  to FEEL's bundled Jackson Scala module.
- `release/2.1.x` retains its patch-only policy and shaded FEEL engine. Do not
  remove its general minor/major exclusions when maintaining `main`.
- `version.jackson` is a shared Maven property. The dependencies controlled by it
  include `com.fasterxml.jackson:jackson-bom` and
  `com.fasterxml.jackson.jakarta.rs:jackson-jakarta-rs-json-provider`. Use artifact
  coordinates, rather than the property name, when inspecting stored ignores.

## When an expected update is missing

Dependabot also stores ignore conditions created by PR comment commands outside
`dependabot.yml`. Removing a YAML rule does not clear those conditions. An `allow`
rule cannot override an ignore condition either.

1. Check the applicable ecosystem, directory and target branch in the YAML.
2. Inspect the latest **Dependabot Updates** run in GitHub Actions. Its job
   definition contains `source.branch` and `ignore-conditions`, including each
   condition's `dependency-name`, `version-requirement` and `source`. Look for
   `All updates for ... were ignored` in the updater output. The GitHub CLI can
   retrieve the same logs:

   ```sh
   gh run list --repo operaton/operaton --workflow "Dependabot Updates" --limit 20
   gh run view RUN_ID --repo operaton/operaton --log
   ```

3. Inspect stored conditions on a relevant Dependabot PR with
   `@dependabot show DEPENDENCY_NAME ignore conditions`. Remove one obsolete
   condition with `@dependabot unignore DEPENDENCY_NAME IGNORE_CONDITION`.
   `@dependabot unignore DEPENDENCY_NAME` clears all stored conditions for that
   artifact; use it only when all of them are obsolete. Retain unrelated
   restrictions. GitHub documents these commands in its
   [Dependabot command reference][commands].
4. Confirm that the bot acknowledged the removal. For a closed PR whose branch
   was deleted, the bot may defer creating a replacement until the next scheduled
   run. An update check can also be requested from the repository's Dependency
   graph page. Check the new run for the expected dependency and ignore state.

The Jackson 2 update in [#3907](https://github.com/operaton/operaton/pull/3907)
exposed this distinction. The old YAML exclusion had already been removed in
[September 2026][yaml-removal], but the [22 October main update job][update-job]
still contained two `> 2.21.4` conditions sourced from `@dependabot ignore command`.
They originated from [#3040][old-command] and covered both Jackson artifacts above.
Changing the YAML alone therefore did not restore those updates. Both stored
conditions were removed with targeted `unignore` commands on that PR.

[commands]: https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-pull-request-comment-commands
[yaml-removal]: https://github.com/operaton/operaton/commit/4e3723b700c8102accaf1c9482a2db75bee39862
[update-job]: https://github.com/operaton/operaton/actions/runs/39701728434
[old-command]: https://github.com/operaton/operaton/pull/3040#issuecomment-4653775360
