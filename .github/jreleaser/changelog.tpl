# About release 1.0.0-rc-1

## New and Noteworthy

The 1.0.0-rc-1 release is the first release candidate for the 1.0.0 stable release.

With this release we focused on:

- Completing Camunda 7.24 backport
- Modernizing the code base

## Versions

### Java

Operaton requires **Java 17** as the minimum version.

Operaton is tested and supported on **Java 17**, **Java 21**, and **Java 25**.

### Camunda 7 Compatibility

This release is feature complete and compatible with [**Camunda 7.24.0**](https://docs.camunda.org/enterprise/announcement/#camunda-platform-7-24).

### Spring

Operaton is based on:

- **Spring Boot 3.5.5** (upgrade from 3.4.4)
- **Spring Framework 6.2.10** (upgrade from 6.2.5)

### Quarkus Extension

<!-- /pom.xml -->
The Operaton Quarkus extension is based on **Quarkus 3.28.0** (upgrade from 3.26.3).

### Distributions

The Tomcat distribution is based on **Tomcat 11.0.10** (upgrade from 10.1.39).

The Wildfly distribution is based on **Wildfly 36.0.1** (upgrade from Wildfly 35.0.1).

### Standards

Operaton is compliant with the following standards:

- Jakarta EE 10
- BPMN 2.0
- DMN 1.3
- CMMN 1.1


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
| MySQL                | mysql                          | 5.7.34           | 9.4.0          |
| MariaDB              | mariadb                        | 10.3.6           | 1.8.0          |
| Oracle               | gvenzl/oracle-xe               | 18.4.0-slim      | 23.9.0.25.07   |                
| Microsoft SQL Server | mcr.microsoft.com/mssql/server | 2017-CU12        | 12.10.1.jre11   |
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

## Customizable Model Singletons

BPMN, CMMN, and DMN singletons are now loaded via factories discovered with `ServiceLoader`, allowing custom implementations.
To override, create an implementation of desired factory and register it in corresponding file in `META-INF/services`.


{{changelogContributors}}

## Changelog

{{changelogChanges}}
