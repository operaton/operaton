## New and Noteworthy

This Operaton release brings major platform & dependency updates. 

It is a **breaking** release for clients integrating Operaton in Spring based applications.

Clients that integrate Operaton via the REST API or database schema do not need to take any action.

### Spring Boot 4 & Spring Framework 7

Operaton 2.0.0 is now based on **Spring Boot 4** and **Spring Framework 7**.

This Spring Boot 4 upgrade is a major driver for the major release and is a **breaking change**.
Clients integrating Operaton into their Spring-based applications need to upgrade their Spring dependencies accordingly.

The support for Spring Boot 3 and Spring Framework 6 has been dropped in this release.

The module structure is unchanged. For clients this means that if the a module from
group `org.operaton.bpm.springboot` is used, using this module with Spring Boot 4 is now required.

### Jakarta EE 11

Operaton 2.0.0 is now compliant with **Jakarta EE 11**.

### JUnit 6 Support

Operaton 2.0 provides support for **JUnit 6** for testing. In order to use JUnit 6 compliant
extensions, use this dependency:

```xml
    <dependency>
      <groupId>org.operaton.bpm</groupId>
      <artifactId>operaton-engine</artifactId>
      <scope>test</scope>
      <classifier>junit6</classifier>
    </dependency>
```

The extension class names remain unchanged, e.g., `org.operaton.bpm.engine.test.junit5.ProcessEngineExtension`

Note that the package name still contains `junit5` for backward compatibility reasons.
This may change for the final Operaton 2.0 release.

### API

### Database Schema

Operaton 2.0 does not introduce changes to the database schema since 1.1.

The database schema remains version 7.24.

### REST API

Operaton 2.0.0 does not introduce changes to the REST API since 1.1.

### Removed & Changed APIs

The following APIs have been removed or changed in a breaking way:

<!-- Evaluate clirr reports (part of Integration Tests build artifacts) and list relevant changes here -->

- Module `operaton-xml-model`:
  - `org.operaton.bpm.model.xml.ModelInstance`:
    - Method added: `public org.operaton.bpm.model.xml.ModelInstance copy()`
- Module `operaton-bpmn-model`:
  - `org.operaton.bpm.model.bpmn.BpmnModelInstance`:
    - Method added: `public org.operaton.bpm.model.bpmn.BpmnModelInstance copy()`
    - Method added: `public org.operaton.bpm.model.xml.ModelInstance copy()` added
  - `org.operaton.bpm.model.bpmn.instance.dc.Font`:
    - Method added: `public void setUnderline(boolean)`
- Module `operaton-cmmn-model`:
  - `org.operaton.bpm.model.cmmn.CmmnModelInstance`:
    - Method added: `public org.operaton.bpm.model.cmmn.CmmnModelInstance copy()`
    - Method added: `public org.operaton.bpm.model.xml.ModelInstance copy()`
- Module `operaton-dmn-model`:
  - `org.operaton.bpm.model.dmn.DmnModelInstance`:
    - Method added: `public org.operaton.bpm.model.dmn.DmnModelInstance copy()`
    - Method added: `public org.operaton.bpm.model.xml.ModelInstance copy()`
- Module `operaton-spin-core`:
  - `org.operaton.spin.scripting.SpinScriptEnv`:
    - Removed field: `extensions`## Versions & Compatibility

### Java

Operaton requires **Java 17** as the minimum version.

Operaton is tested and supported on **Java 17**, **Java 21**, and **Java 25**.

### Camunda 7 Compatibility

This release is feature complete and API-compatible with [**Camunda 7.24**](https://docs.camunda.org/enterprise/announcement/#camunda-platform-7-24).

### Spring

Operaton is based on:

- **Spring Boot 4.0**
- **Spring Framework 7.0**

### Quarkus Extension

<!-- /pom.xml -->
The Operaton Quarkus extension is based on **Quarkus 3.30**.

**Note**: The final Operaton Quarkus extension will be based on Quarkus 3.35 LTS.

### Distributions

The Tomcat distribution is based on **Tomcat 11**.

The Wildfly distribution is based on **Wildfly 38**.

### Standards Compliance

Operaton is compliant with the following standards:

- Jakarta EE 11
- BPMN 2.0
- DMN 1.3
- CMMN 1.1

### Scripting Languages

Operaton supports the following scripting languages:

| Language   | Engine             | Version  |
|------------|--------------------|----------|
| JavaScript | GraalVM JavaScript | 25.0.0   |
| Groovy     | Groovy             | 5.0.3    |
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

### JUnit

Operaton supports the following JUnit versions for testing:

| JUnit Version | Supported Since Operaton Version |
|---------------|----------------------------------|
| JUnit 6       | 1.1.0                            |
| JUnit 5       | 1.0.0                            |
| JUnit 4       | 1.0.0                            |

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
| H2                   | n/a                            | n/a              | 2.3.240        |
| PostgreSQL           | postgres                       | 9.6.12           | 42.7.9         |
| MySQL                | mysql                          | 5.7.34           | 9.5.0          |
| MariaDB              | mariadb                        | 10.3.6           | 3.5.7          |
| Oracle               | gvenzl/oracle-xe               | 18.4.0-slim      | 23.26.0.0.0   |                
| Microsoft SQL Server | mcr.microsoft.com/mssql/server | 2017-CU12        | 13.2.1.jre11  |
| DB2                  | icr.io/db2_community/db2       | 11.5.0.0a        | 12.1.3.0       |

### Database Schema Upgrade Tests

With Operaton we are running database schema upgrade tests nightly.

- Source DB Version: 7.23.0
- Target DB Version: 7.24.0

{{changelogContributors}}

## Changelog

{{changelogChanges}}
