---
name:  📦 Dependency Upgrade
about: A task to upgrade a dependency in the project
title: 'Upgrade <DEP_NAME> to y.y.y'
type: task
labels: ["dependencies"]
assignees: ''
projects: ["operaton/operaton"]

---

**Dependency to upgrade**

Upgrade [<DependencyName> to <y.y.y>](<link_to_release)

Current version: `<x.x.x>`

**Impact**

- Only test dependency: <YES,NO>
<!-- For test dependencies: n/a -->
- Affected distros: <n/a,Tomcat,Wildfly,Standalone>

**Verify**

Run a full build with:
```
./mvnw clean verify
```

Listing the project dependencies should only reference the upgraded dependency version. Execute:
```bash
./.devenv/scripts/tools/list-unique-dependencies.sh
```

Alternatively, you can use the `./mvnw dependency:tree > dependencies.txt` command to see the dependency tree 
and ensure that the upgraded dependency is correctly included.

**Contribution**

If you want to work on this task, just state it in a comment. You don't have to wait until the task is assigned to you.
First come, first serve! Avoid starting work on this task when someone else has already claimed it. 

Are you a first-time contributor? We are happy to see you here! 
Just state in a comment of your PR "I confirm that my contribution and following ones comply with the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 
and the [Code of Conduct](https://github.com/operaton/operaton/blob/main/CODE_OF_CONDUCT.md)"

Contributors will be added to the release notes of the next release.
