---
name:  🚀 Release Checklist
about: A checklist for preparing a new release
title: 'Release 1.x.x'
type: task
labels: ["build"]
assignees: 'kthoms'
projects: ["operaton/operaton"]

---

**What needs to be done?**

This issue is for managing the release TODO.

## Documentation

- [ ] [changelog.tpl](https://github.com/operaton/operaton/blob/main/.github/jreleaser/changelog.tpl) updated to point to the release
- [release notes](https://docs.operaton.org/docs/documentation/reference/release-notes/TODO) updated with:
    - [ ] Spring Boot / Spring Framework version
    - [ ] Tomcat version
    - [ ] Wildfly version
    - [ ] Quarkus version
    - [ ] New & noteworthy changes documented
- [ ] [Closed issues without milestones](https://github.com/operaton/operaton/issues?q=is%3Aissue%20state%3Aclosed%20no%3Amilestone) checked: Current milestone set if applicable
- [ ] [Closed PRs without milestone](https://github.com/operaton/operaton/pulls?q=is%3Apr+is%3Aclosed+no%3Amilestone) checked: Current milestone set if applicable

## Dependency Check

- [ ] [Check Dependabot alerts](https://github.com/operaton/operaton/security/dependabot): No alert with [severity HIGH or CRITICAL](https://github.com/operaton/operaton/security/dependabot?q=is%3Aopen+severity%3Ahigh%2Ccritical) are open (except for old webapp dependencies)
- [ ] Dependency Upgrade Report checked
    - Execute
      ```./mvnw versions:dependency-updates-aggregate-report```
    - Open `target/reports/dependency-updates-aggregate-report.html`
    - Check that no important dependency has to be upgraded (e.g. Spring)

## Build Check

- [ ] [Build on main](https://github.com/operaton/operaton/actions/workflows/build.yml?query=branch%3Amain) is green
- [ ] [Nightly integration build](https://github.com/operaton/operaton/actions/workflows/integration-build.yml?query=branch%3Amain) is green
- [ ] [Database Migration Tests build](https://github.com/operaton/operaton/actions/workflows/migration-test.yml?query=branch%3Amain) is green
- [ ] [Images on DockerHub](https://hub.docker.com/u/operaton) are current (last updated today/1 day)

## Distributions Check

### Download
- [ ] Download distributions from [releases](https://github.com/operaton/operaton/releases)
    - Download date: `TODO`

### Manual Check

_For each_ distribution perform these manual test steps:

- Tasklist
    - Open [Tasklist](http://localhost:8080/operaton/app/tasklist)
    - Login page appears
    - Login with `demo`/ `demo` succeeds, Tasklist opens with 2 "Assign Reviewer" tasks
    - Click on one task
    - Task form is shown
    - Enter a reviewer "John", click on "Complete"
    - Task is removed from "My Tasks" (1 remaining)
    - Click on "John's Tasks"
    - 1 "Review Invoice" task is open
- Cockpit
    - Open [Cockpit](http://localhost:8080/operaton/app/cockpit)
    - Click on _Running Process Instances_
    - Click on one Process Instance
    - Edit some variable and save. The message "The variable '<VAR>' has been changed successfully." appears.

### Tomcat Distribution

- [ ] Tomcat startup
    - Run `./start.sh`
        - Reported Tomcat version: `TODO`
    - [ ] Browser opens http://localhost:8080/operaton-welcome/index.html
    - [ ] ` ps -ef |grep catalina` shows Tomcat process is running
    - [ ] Server successfully started
    - [ ] `grep "ENGINE-08048" -r --include=catalina.out` shows `Operaton sucessfully started at 'Apache Tomcat/<TOMCAT_VERSION>`
    - [ ] `grep "ENGINE-08050" -r --include=catalina.out` shows `InvoiceProcessApplication successfully deployed`
- [ ] Manual Webapp tests checked
- - Run `./shutdown.sh`

### Wildfly Distribution
- [ ] Wildfly startup
    - Run `./start.sh`
        - Reported Wildfly version: `TODO`
    - [ ] Browser opens http://localhost:8080/operaton-welcome/index.html
- [ ] Manual Webapp tests checked


### Docker Images

- [ ] Operaton Standalone
    - [ ] Image started successfully
```
docker rm -f operaton 2>/dev/null || true
docker pull operaton/operaton:SNAPSHOT
docker run --name operaton -p 8080:8080 operaton/operaton:SNAPSHOT
```
- [ ] Apps are working: Perform test steps from "Distributions"

- [ ] Operaton Wildfly
    - [ ] Image started successfully
```
docker rm -f operaton-wildfly 2>/dev/null || true                                   
docker pull operaton/wildfly:SNAPSHOT
docker run --name operaton-wildfly -p 8080:8080 operaton/wildfly:SNAPSHOT
```
- [ ] Apps are working: Perform test steps from "Distributions"

- [ ] Operaton Tomcat
    - [ ] Image started successfully
```
docker rm -f operaton-tomcat 2>/dev/null || true                                   
docker pull operaton/tomcat:SNAPSHOT
docker run --name operaton-tomcat -p 8080:8080 operaton/tomcat:SNAPSHOT
```
- [ ] Apps are working: Perform test steps from "Distributions"

**Release Build**

- [ ] Release Build executed
- [ ] Artifacts are available on Maven Central
- [ ] Docker Images are available with tag `latest`
- [ ] Generated Release documentation is checked and updated

**Post Release**
- [ ] Announcements
    - [ ] Release announced on LinkedIn
    - [ ] Release announced in [Forum](https://forum.operaton.org/c/announcements/4)
    - [ ] Release announced in Slack
- [ ] [`changelog.tpl`](https://github.com/operaton/operaton/blob/main/.github/jreleaser/changelog.tpl) emptied for next release
- [ ] [`jreleaser.yml`](https://github.com/operaton/operaton/blob/main/jreleaser.yml): `previousTagName` updated
 
