# Operaton External Task Client (Java)


[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.operaton.bpm/operaton-external-task-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.operaton.bpm/operaton-external-task-client)

> Are you looking for the Spring Boot External Task Client? This way please: [Spring Boot External Task Client](../../spring-boot-starter/starter-client)

The **Operaton External Task Client (Java)** allows to set up remote Service Tasks for your workflow.

* [Quick Start](https://docs.operaton.org/get-started/quick-start/)
* [Documentation](https://docs.operaton.org/manual/develop/user-guide/ext-client/)
* [Examples](https://github.com/operaton/operaton/tree/master/clients/java)

## Features
* Complete External Tasks
* Extend the lock duration of External Tasks
* Unlock External Tasks
* Report BPMN errors as well as failures
* Share primitive and object typed process variables with the Workflow Engine


## Configuration options
* The client can be configured with the fluent api of the [ExternalTaskClientBuilder](client/src/main/java/org/operaton/bpm/client/ExternalTaskClientBuilder.java).
* The topic subscription can be configured with the fluent api of the [TopicSubscriptionBuilder](client/src/main/java/org/operaton/bpm/client/topic/TopicSubscriptionBuilder.java).

## Prerequisites
* Java (supported version by the used Operaton)
* Operaton

## Maven coordinates
The following Maven coordinate needs to be added to the projects `pom.xml`:
```xml
<dependency>
  <groupId>org.operaton.bpm</groupId>
  <artifactId>operaton-external-task-client</artifactId>
  <version>${version}</version>
</dependency>
```

## Testing

### Run a single test

```bash
./mvnw verify -f clients/java/client -Dfailsafe.includes="**/PaSerializationIT.java"
```

### Server Logs

The Tomcat instance logs can be found at 
`clients/java/client/target/operaton-tomcat/server/apache-tomcat-10.1.39/logs/catalina.out`

### Remote Debugging

To enable remote debugging of server instance started by cargo, activate the `debug-cargo` profile:

```bash
./mvnw verify -f clients/java/client -Dfailsafe.includes="**/PaSerializationIT.java" -Pdebug-cargo
```

Then connect to the remote debugger on port `5055`. For IntelliJ IDEA, you can run the following configuration:
`.devenv/ide/idea/run/external-task-client_cargo-remote-debug.run.xml`

## Contributing

Have a look at our [contribution guide](https://github.com/operaton/operaton/blob/main/CONTRIBUTING.md) for how to contribute to this repository.


## License
The source files in this repository are made available under the [Apache License Version 2.0](./LICENSE).
