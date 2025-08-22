# About release 1.0.0-beta-5

## New and Noteworthy

The 1.0.0-beta-5 is a continuation of our ongoing modernization efforts.

With this release we focused on:

- Completing Camunda 7.23 backport
- Enable testing against all supported databases in the CI
- Platform & dependency upgrades
- Mark deprecated code
- Completing the JUnit 5 migration
- Reduce Sonar findings

## Versions

### Java

Operaton requires **Java 17** as the minimum version.

### Camunda 7 Compatibility

This release is feature complete and compatible with [**Camunda 7.23.0
**](https://docs.camunda.org/enterprise/announcement/#camunda-platform-7-23).

### Spring

Operaton is based on:

- **Spring Boot 3.5.4** (upgrade from 3.4.4)
- **Spring Framework 6.2.10** (upgrade from 6.2.5)

### Quarkus Extension

<!-- /pom.xml -->
The Operaton Quarkus extension is based on **Quarkus 3.24.2** (upgrade from 3.20.0).

### Distributions

The Tomcat distribution is based on **Tomcat 11.0.10** (upgrade from 10.1.39).

The Wildfly distribution is based on **Wildfly 36.0.1** (upgrade from Wildfly 35.0.1).

### Standards

Operaton is compliant with the following standards:

- Jakarta EE 10
- BPMN 2.0
- DMN 1.3
- CMMN 1.1

### Code Modernization

We have continued to modernize the code base by removing deprecated code inherited from Camunda 7.
This is part of our
ongoing effort to improve the code quality and maintainability of the Operaton project.

The Sonar findings have been further addressed. Compared with 1.0.0-beta-4 the findings have been
reduced by 32 %.
Overall since 1.0.0-beta-1 the findings have been reduced by 83 %.
See the [Sonar report](https://sonarcloud.io/summary/overall?id=io.github.operaton%3Aoperaton) for
details.

### Deprecations

We have revised deprecated code inherited from Camunda 7. Since we do not want to break clients we
have only marked
deprecated methods and classes.

API that was marked deprecated before has been tagged with

```
@Deprecated(since = "1.0")
```

This is because from perspective of the Operaton code base it is deprecated with the first 1.0
release.

API that has a clear replacement has been marked for removal.

```
@Deprecated(since = "1.0", forRemoval = true)
```

It is strongly encouraged to clear usages of such API. We did not remove it yet, but will remove it
from a future version.
It will be announced to when exactly removal is planned on a detailed level.

## Testing

### Database Integration Tests

Operaton is now tested against the following databases:

<!--
  Driver version: database/pom.xml
  DB container image: OperatonXXXContainerProvider.java
  DB container tag: base class of OperatonXXXContainerProvider
-->

| Database             | DB Container Image             | DB Container Tag | Driver Version |
|----------------------|--------------------------------|------------------|----------------|
| H2                   | n/a                            | n/a              | 2.3.232        |
| PostgreSQL           | postgres                       | 9.6.12           | 42.7.7         |
| MySQL                | mysql                          | 5.7.34           | 9.3.0          |
| MariaDB              | mariadb                        | 10.3.6           | 1.7.6          |
| Oracle               | gvenzl/oracle-xe               | 18.4.0-slim      | 23.5.0.24.07   |                
| Microsoft SQL Server | mcr.microsoft.com/mssql/server | 2017-CU12        | 12.10.0.jre8   |
| DB2                  | icr.io/db2_community/db2       | 11.5.0.0a        | 12.1.2.0       |

### Database Schema Upgrade Tests

With Operaton 1.0.0-beta-5 we are running database schema upgrade tests nightly.

- Source DB Version: 7.22.0
- Target DB Version: 7.23.0

Tests are enabled for the following databases:

| Database             | DB Container Image             | DB Container Tag | Driver Version |
|----------------------|--------------------------------|------------------|----------------|
| H2                   | n/a                            | n/a              | 2.3.232        |
| PostgreSQL           | postgres                       | 17               | 42.7.7         |
| MySQL                | mysql                          | 9.2.0            | 9.3.0          |
| MariaDB              | mariadb                        | 11.8             | 3.0.7          |
| Oracle               | gvenzl/oracle-xe               | 21-faststart     | 23.5.0.24.07   |
| Microsoft SQL Server | mcr.microsoft.com/mssql/server | 2022-latest      | 12.10.0.jre8   |
| DB2                  | n/a                            | n/a              | n/a            |

## JUnit 5 migration

We have continued to migrate tests to JUnit 5. The migration is not yet complete, but we are making
steady progress.

Especially worth mentioning is that the tests of the most complex module, `engine`,
has been completed.

49/53 (+5) modules have been migrated to JUnit 5.

## Testing LDAP plugin against Testcontainer

The LDAP plugin was tested against an embedded ApacheDS LDAP server.
The used version was outdated.

Instead of the embedded server we are now using a Testcontainer based LDAP server.
The Testcontainer uses the [Osixia docker-openlap image](https://github.com/osixia/docker-openldap).

{{changelogContributors}}

## Changelog

{{changelogChanges}}
