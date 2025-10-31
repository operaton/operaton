# About release 1.0.0

This is the first official release of Operaton. After forking the Camunda 7 platform in mid-2024, 
we have been working hard to modernize the code base and prepare for the 1.0.0 stable release.

Several beta releases have been published to gather feedback from the community and ensure a smooth 
transition. Multiple products have already started to use Operaton in production, and even
more are actively testing it. Major effort has been put into automated testing to ensure stability
and reliability.

From now on, Operaton is the successor for the open-source BPM platform formerly known as 
_Camunda 7 Community Edition_. The engine is already battle-tested in production environments,
and is safe to transition from existing Camunda 7 CE installations.

We are excited to embark on this new journey and look forward to building a vibrant 
community around Operaton. We welcome contributions, feedback, and collaboration from developers
worldwide. Together, we can shape the future of business process management with Operaton!

## Migration from Camunda 7 CE

Migrating from Camunda 7 Community Edition to Operaton is straightforward. Operaton 1.0 is fully 
compatible with Camunda 7.24, allowing for a seamless transition. To ease the migration process, 
ideally upgrade to Camunda 7.24 first, and then switch to Operaton 1.0.

Migration involves updating your dependencies to point to Operaton artifacts instead of Camunda ones.
We are providing a migration based on OpenRewrite to help automate this process. You can find the migration recipe
in our [Operaton Migration Repository](https://github.com/operaton/migrate-from-camunda-recipe).

## Versions

### Java

Operaton requires **Java 17** as the minimum version.

Operaton is tested and supported on **Java 17**, **Java 21**, and **Java 25**.

### Camunda 7 Compatibility

This release is feature complete and compatible with [**Camunda 7.24.0**](https://docs.camunda.org/enterprise/announcement/#camunda-platform-7-24).

### Spring

Operaton is based on:

- **Spring Boot 3.5.7**
- **Spring Framework 6.2.12**

### Quarkus Extension

<!-- /pom.xml -->
The Operaton Quarkus extension is based on **Quarkus 3.28.3**.

### Distributions

The Tomcat distribution is based on **Tomcat 11.0.12**.

The Wildfly distribution is based on **Wildfly 37.0.1**.

### Standards

Operaton is compliant with the following standards:

- Jakarta EE 10
- BPMN 2.0
- DMN 1.3
- CMMN 1.1

### Scripting

Operaton supports the following scripting languages:

| Language   | Engine             | Version  |
|------------|--------------------|----------|
| JavaScript | GraalVM JavaScript | 25.0.0   |
| Groovy     | Groovy             | 5.0.1    |
| Python     | Jython             | 2.7.4    |
| Ruby       | GraalVM Ruby       | 9.1.17.0 |

Note: The Nashorn JavaScript engine has been removed in Java 15 and is no longer supported.

**Camunda Migration Note**: If you are migrating from Camunda 7 and are using ECMAScript 5,
this might not work with the GraalVM JavaScript engine. 
Please check your JavaScript code and consider migrating them to a more recent version of JavaScript.

## Testing

### Database Integration Tests

Operaton is tested against the following databases:

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

With Operaton we are running database schema upgrade tests nightly.

- Source DB Version: 7.23.0
- Target DB Version: 7.24.0

Tests are enabled for the following databases:

| Database             | DB Container Image             | DB Container Tag | Driver Version |
|----------------------|--------------------------------|------------------|----------------|
| H2                   | n/a                            | n/a              | 2.3.232        |
| PostgreSQL           | postgres                       | 17               | 42.7.7         |
| MySQL                | mysql                          | 9.2.0            | 9.4.0          |
| MariaDB              | mariadb                        | 11.8             | 3.0.7          |
| Oracle               | gvenzl/oracle-xe               | 21-faststart     | 23.5.0.24.07   |
| Microsoft SQL Server | mcr.microsoft.com/mssql/server | 2022-latest      | 12.10.1.jre11   |
| DB2                  | n/a                            | n/a              | n/a            |


{{changelogContributors}}

## Changelog

{{changelogChanges}}
