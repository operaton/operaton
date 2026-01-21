---
name:  🔙 Backport a commit
about: Backport a commit from camunda-bpm-platform to operaton
title: 'Backport "<ORIGINAL_TITLE>"'
type: task
labels: ["backport:c7"]
assignees: ''
milestones: ["1.0.0-rc-1"]
projects: ["operaton/operaton"]

---

### What needs to be done?

Backport commit [<COMMIT>](https://github.com/camunda/camunda-bpm-platform/commit/{COMMIT}) from repository [camunda-bpm-platform](https://github.com/camunda/camunda-bpm-platform).

### Adhere to the backported commit

Be as close as possible to the original commit to ease the code review, but adapt to the current code base if necessary.

The required adoptions could be:

- Namespace changes: `org.camunda.bpm` -> `org.operaton.bpm`
- JUnit 4 -> JUnit 5 migration
- JUnit 4 assertions -> AssertJ assertions

### Verify the backport

Verify carefully that the backport works as expected. This includes:

- The code compiles and tests pass. Use `./mvnw clean install` for a verification. The build must complete without errors
- If any dependency is changed, run the integration build
    - Execute `.devenv/scripts/build/build-and-run-integration-tests.sh`
    - The script must complete without errors

### Attribution
The backporting commit needs proper attribution of the original commit, including references to the backported commit, its original author and reference to the original issue.

```
Related to: https://github.com/camunda/camunda-bpm-platform/issues/{ISSUE_NR}
Backported commit: https://github.com/camunda/camunda-bpm-platform/commit/{COMMIT}
Original author: {AUTHOR_NAME} <{AUTHOR_EMAIL}>
```
