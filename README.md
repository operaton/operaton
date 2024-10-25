# OPERATON - The open source process engine

[![operaton manual latest](https://img.shields.io/badge/manual-latest-brown.svg)](https://docs.operaton.org/) [![License](https://img.shields.io/github/license/operaton/operaton?color=blue&logo=apache)](https://github.com/operaton/operaton/blob/main/LICENSE) [![Forum](https://img.shields.io/badge/forum-operaton-green)](https://forum.operaton.org/)

OPERATON is a native BPMN 2.0 process engine that runs inside the Java Virtual Machine. It can be embedded inside any Java application and any Runtime Container. It integrates with Java EE 6 and is a perfect match for the Spring Framework. On top of the process engine, you can choose from a stack of tools for human workflow management, operations and monitoring.

- Web Site: https://www.operaton.org/
- Getting Started: https://docs.operaton.org/
- User Forum: https://forum.operaton.org/
- Issue Tracker: https://github.com/operaton/operaton/issues

### This is a fork of the Camunda 7 BPM platform

We have not removed the old issue links and they still lead to Camunda's JIRA or the GitHub repo.

### What we plan to do and where we are going
Take a look at our [Roadmap](https://www.operaton.org/en/#roadmap)

### Want to talk to us or other people around OPERATON?
Visit our [Forum](https://forum.operaton.org)

## Components

OPERATON provides a rich set of components centered around the BPM lifecycle.

#### Process Implementation and Execution

- OPERATON Engine - The core component responsible for executing BPMN 2.0 processes.
- REST API - The REST API provides remote access to running processes.
- Spring, CDI Integration - Programming model integration that allows developers to write Java Applications that interact with running processes.

#### Process Operations

- OPERATON Engine - JMX and advanced Runtime Container Integration for process engine monitoring.
- OPERATON Cockpit - Web application tool for process operations.
- OPERATON Admin - Web application for managing users, groups, and their access permissions.

#### Human Task Management

- OPERATON Tasklist - Web application for managing and completing user tasks in the context of processes.

### Highly Integrable

Out of the box, OPERATON provides infrastructure-level integration with Java EE Application Servers and Servlet Containers.

### Embeddable

Most of the components that make up the platform can even be completely embedded inside an application. For instance, you can add the process engine and the REST API as a library to your application and assemble your custom BPM platform configuration.

## Process modelling

Operaton is fully backwards compatible to your existing BPMN-, DMN-models and Forms, which were created in Camunda Modeler for Camunda 7. You can download the Camunda Modeler [here](https://camunda.com/download/modeler/) (MIT Licence). 
Operaton removed the compatibility layer for Activiti. If you need to use Activiti models you will have to convert them (see the following [blog post](https://camunda.com/blog/2016/10/migrate-from-activiti-to-camunda/) for details).

## Contributing

Please see our [contribution guidelines](CONTRIBUTING.md) for how to raise issues and how to contribute code to our project.

## Tests

To run the tests in this repository, please see our [testing tips and tricks](TESTING.md).


## License

The source files in this repository are made available under the [Apache License Version 2.0](./LICENSE).

OPERATON uses and includes third-party dependencies published under various licenses. By downloading and using OPERATON artifacts, you agree to their terms and conditions. Refer to our [license-book.txt](./distro/license-book/src/main/resources/license-book.txt) for an overview of third-party libraries and particularly important third-party licenses we want to make you aware of.
