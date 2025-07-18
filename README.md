# Operaton - The open source process engine

![build status](https://github.com/operaton/operaton/actions/workflows/build.yml/badge.svg?branch=main)
[![sonarqube](https://img.shields.io/sonar/tests/operaton_operaton?server=https%3A%2F%2Fsonarcloud.io&logo=sonarcloud)](https://sonarcloud.io/project/overview?id=operaton_operaton)
[![Maven Central Version](https://img.shields.io/maven-central/v/org.operaton.bpm/operaton-bom-root?color=blue&logo=apachemaven)](https://central.sonatype.com/search?q=org.operaton)

![](https://tokei.rs/b1/github/operaton/operaton?label=Files&category=files)
![](https://tokei.rs/b1/github/operaton/operaton?label=LOC&category=code)

[![operaton manual latest](https://img.shields.io/badge/manual-latest-brown.svg)](https://docs.operaton.org/)
[![License](https://img.shields.io/github/license/operaton/operaton?color=blue&logo=apache)](https://github.com/operaton/operaton/blob/main/LICENSE)


[![Forum](https://img.shields.io/badge/forum-Operaton-green)](https://forum.operaton.org/)
[![Slack](https://img.shields.io/badge/chat-Slack-purple)](https://join.slack.com/t/operaton/shared_invite/zt-39cabj835-eLMK1VKAJx~kXf_qO8gaUQ)

Operaton is a native BPMN 2.0 process engine that runs inside the Java Virtual Machine. It can be embedded inside any Java application and any Runtime Container. It integrates with Spring, Spring Boot, Quarkus and is a perfect match for Jakarta EE. On top of the process engine, you can choose from a stack of tools for human workflow management, operations and monitoring.

- Web Site: https://operaton.org/
- Getting Started: https://docs.operaton.org/
- User Forum: https://forum.operaton.org/
- Issue Tracker: https://github.com/operaton/operaton/issues

### This is a fork of the Camunda 7 BPM platform

We have not removed the old issue links and they still lead to Camunda's JIRA or the GitHub repo.

### What we plan to do and where we are going
Take a look at our [Roadmap](https://operaton.org/roadmap)

### Want to talk to us or other people around Operaton?
Visit our [Forum](https://forum.operaton.org)

## Building
Prerequisites:

JDK 17 or newer - check `java -version`

You can use the Maven Wrapper script to execute the build. The script downloads and installs (if necessary) the required Maven version to `~/.m2/wrapper` and runs it from there.

On Linux and MacOS, run
```shell
./mvnw
```

On Windows, run
```shell
mvnw
```

Alternatively, you can use the your own Maven installation (minimal version: 3.9.0) Wrapper and execute
```shell
mvn
```

For a faster build you can add `-DskipTests` to skip test execution and `-Dskip.frontend.build=true` to skip the build of the webapps.

## Get it!

Get the latest release from the [Releases page](https://github.com/operaton/operaton/releases).

To get the latest stable build visit the [Early Access release page]([https://github.com/operaton/operaton/actions/workflows/nighly-build.yml?query=branch%3Amain+event%3Aschedule+is%3Asuccess++](https://github.com/operaton/operaton/releases/tag/early-access)), click on _Assets_ and download the desired package.

## About Operaton

### Components

Operaton provides a rich set of components centered around the BPM lifecycle.

#### Process Implementation and Execution

- Operaton Engine - The core component responsible for executing BPMN 2.0 processes.
- REST API - The REST API provides remote access to running processes.
- Spring, CDI Integration - Programming model integration that allows developers to write Java Applications that interact with running processes.

#### Process Operations

- Operaton Engine - JMX and advanced Runtime Container Integration for process engine monitoring.
- Operaton Cockpit - Web application tool for process operations.
- Operaton Admin - Web application for managing users, groups, and their access permissions.

#### Human Task Management

- Operaton Tasklist - Web application for managing and completing user tasks in the context of processes.

### Highly Integrable

Out of the box, Operaton provides infrastructure-level integration with Java EE Application Servers and Servlet Containers.

### Embeddable

Most of the components that make up the platform can even be completely embedded inside an application. For instance, you can add the process engine and the REST API as a library to your application and assemble your custom BPM platform configuration.

### Process modelling

Operaton is fully backwards compatible to your existing BPMN-, DMN-models and Forms, which were created in Camunda Modeler for Camunda 7. You can download the Camunda Modeler [here](https://camunda.com/download/modeler/) (MIT Licence). 

## Documentation

The documentation is currently under construction. Currently, you can use the [Camunda 7 Manual](https://docs.camunda.org/manual/7.22/) as a reference.
Since Operaton is a fork of Camunda 7, most of the documentation is still valid. We will provide a new manual soon.

## Architectural Decisions

For insights into our architectural decisions and the reasoning behind them, see our [Architectural Decision Records (ADRs)](docs/decisions/). These documents provide context for key technical choices and help contributors understand the project's design principles.

## Contributing

Please see our [contribution guidelines](CONTRIBUTING.md) for how to raise issues and how to contribute code to our project.

## Tests

To run the tests in this repository, please see our [testing tips and tricks](TESTING.md).

## Prerequisites

Java 17 or higher is required.

## License

The source files in this repository are made available under the [Apache License Version 2.0](./LICENSE).

Operaton uses and includes third-party dependencies published under various licenses. By downloading and using Operaton artifacts, you agree to their terms and conditions. Refer to our [license-book.txt](./distro/license-book/src/main/resources/license-book.txt) for an overview of third-party libraries and particularly important third-party licenses we want to make you aware of.

## Contributors

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->
