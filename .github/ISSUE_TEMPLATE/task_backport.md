---
name:  📋 Backport a commit
about: A simple task suitable for new contributors
title: 'Backport "<ORIGINAL_TITLE>"'
type: task
labels: ["backport:c7"]
assignees: ''
milestones: ["1.0.0-rc-1"]
projects: ["operaton/operaton"]

---

### What needs to be done?

Backport commit [<COMMIT>](https://github.com/camunda/camunda-bpm-platform/commit/<COMMIT>) from repository [camunda-bpm-platform](https://github.com/camunda/camunda-bpm-platform).

### Adhere to the backported commit

Be as close as possible to the original commit to ease the code review, but adapt to the current code base if necessary.

The required adoptions could be:

- namespace changes: `org.camunda.bpm` -> `org.operaton.bpm`
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
Related to {REPOSISTORY}issues/{ISSUE}

Backported commit {HASH} from repository {REPOSITORY_URL}.
Original author: {AUTHOR_NAME} <{AUTHOR_EMAIL}>
```

### Contribution

If you want to work on this task, just state it in a comment. You don't have to wait until the task is assigned to you.
First come, first serve! Avoid starting work on this task when someone else has already claimed it.
Please start soon with the task when you signaled that you want to work on it and be responsive to questions or feedback.
We also do our best to review and merge your PR in a timely manner.

Are you a first-time contributor? We are happy to see you here! 
Just state in a comment of your PR "I confirm that my contribution and following ones comply with the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 
and the [Code of Conduct](https://github.com/operaton/operaton/blob/main/CODE_OF_CONDUCT.md).
Consider adding a star ⭐️ to the [repository](https://github.com/operaton/operaton) if you like it, we appreciate that.

Contributors will be added to the release notes of the next release.
