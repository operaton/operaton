# About release 1.1.0

## New and Noteworthy

The 1.1.0 release is the first feature release after Operaton's go-live.

With this release we focused on:

- Completing Camunda 7.24 backport
- Modernizing the code base
- Java 25 Support
- Upgrade to Wildfly 37

## Versions

### Java

Operaton requires **Java 17** as the minimum version.

Operaton is tested and supported on **Java 17**, **Java 21**, and **Java 25**.

### Camunda 7 Compatibility

This release is feature complete and API-compatible with [**Camunda 7.24**](https://docs.camunda.org/enterprise/announcement/#camunda-platform-7-24).

### Spring

Operaton is based on:

- **Spring Boot 3.5.7**
- **Spring Framework 6.2.12**

### Quarkus Extension

<!-- /pom.xml -->
The Operaton Quarkus extension is based on **Quarkus 3.28.3**.

### Distributions

The Tomcat distribution is based on **Tomcat 11.0.14**.

The Wildfly distribution is based on **Wildfly 37.0.1**.

### Standards Compliance

Operaton is compliant with the following standards:

- Jakarta EE 10
- BPMN 2.0
- DMN 1.3
- CMMN 1.1

### Scripting Languages

Operaton supports the following scripting languages:

| Language   | Engine             | Version  |
|------------|--------------------|----------|
| JavaScript | GraalVM JavaScript | 25.0.0   |
| Groovy     | Groovy             | 5.0.1    |
| Python     | Jython             | 2.7.4    |
| Ruby       | GraalVM Ruby       | 9.1.17.0 |

**Critical Migration Note on JavaScript**: The legacy Nashorn JavaScript engine has been removed 
(as of Java 15) and is no longer supported. 
Operaton now uses the high-performance **GraalVM JavaScript** engine. 
If you are migrating from Camunda 7 and are using **ECMAScript 5 (or older)**, 
your scripts might require updates to comply with modern JavaScript standards. 
Please check your existing JavaScript code.

**Camunda Migration Note**: If you are migrating from Camunda 7 and are using ECMAScript 5,
this might not work with the GraalVM JavaScript engine. 
Please check your JavaScript code and consider migrating them to a more recent version of JavaScript.

## 🧪 Testing and Quality Assurance

### Database Integration Tests

Operaton is thoroughly tested against the following production-grade databases, 
ensuring compatibility with modern driver and container versions:

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
