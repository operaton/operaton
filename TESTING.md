# Operaton Testing Guidelines

* [Best Practices for Writing Test Cases](#best-practices-for-writing-test-cases)
* [Running Integration Tests](#running-integration-tests)
* [Limiting the Number of Engine Unit Tests](#limiting-the-number-of-engine-unit-tests)

---

## Best Practices for Writing Test Cases

* **Use JUnit 5**, do not write JUnit 4 tests
* **Leverage the `ProcessEngineExtension`**: In the `operaton-engine` project, if you need a process engine object, use the `org.operaton.bpm.engine.test.extension.ProcessEngineExtension`. This extension integrates seamlessly with JUnit 5 and ensures that the process engine object is reused across test cases and that certain integrity checks are performed after every test. Example:

  ```java
  public class MyProcessEngineTest {
    // inject ProcessEngine and engine services
    ProcessEngine processEngine;
    RuntimeService runtimeService;

    @RegisterExtension
    static ProcessEngineExtension extension = ProcessEngineExtension.builder()
        .configurationResource("my-process-engine-config.xml")
        .build();

    @Test
    void testThings() {
      // Use the injected ProcessEngine instance
      assertThat(processEngine).isNotNull();

      //...
    }
  }
  ```

* Use the JUnit 5 extensions in your `pom.xml`
 
  ```xml
  <dependency>
    <groupId>org.operaton.bpm</groupId>
    <artifactId>operaton-bpm-junit5</artifactId>
    <scope>test</scope>
  </dependency>
  ```
* **Custom ProcessEngine Configuration**: If you need a process engine with custom configuration, use the `ProcessEngineExtension.builder()` method to specify configuration options, as shown in the example above.
* **Avoid Static Rules and Inheritance**: Instead of extending a base class, favor composition by leveraging ProcessEngineExtension and dependency injection. This approach is cleaner and more flexible in JUnit 5.
* **Consider to use AssertJ**: AssertJ allows a fluent style for writing test assertions. You will find its usage in Operaton's tests as reference.  
* **Mock when possible**: Use Mockito when you want to test your logic and you could mock away used services. Unit tests are much faster than tests that require a running engine. 
  Consider to make methods under test package-private, so that they can't be accessed outside, but a unit test which is located usually in the same package can do. 
  
## Running Integration Tests

The integration test suites are located under qa/. Each server runtime we support has its own folder (e.g., XX-runtime). These projects are responsible for configuring a runtime container distribution (e.g., Apache Tomcat, WildFly) for integration testing.

The actual integration tests are located in the following modules:

* `qa/integration-tests-engine`: Contains tests for integrating the process engine within a runtime container. For example, ensuring that the Job Executor Service works as expected in a Jakarta EE Container or that CDI request contexts span multiple EJB invocations.
* `qa/integration-tests-webapps`: Tests the Operaton web applications inside runtime containers. These tests run in a client/server setting, where the web application is deployed to the runtime container, and the tests interact with it via HTTP requests.

To run the integration tests:

1. Perform a full install build:

  ```shell
  mvn clean install
  ```

2. Navigate to the `qa` folder.

## Maven Profiles

You can configure the build using Maven profiles:

* **Runtime containers & environments**: `tomcat`, `wildfly`
* **Testsuite**: `engine-integration`, `webapps-integration`
* **Database**: `h2`, `h2-xa`, `db2`, `sqlserver`, `oracle`, `postgresql`, `postgresql-xa`, `mysql` (Only `h2` and `postgresql` are supported for engine-integration tests)

In order to configure the build, compose the profiles for runtime container, testsuite, database. 

Example:

```shell
mvn clean install -Pengine-integration,wildfly,h2
```

If you want to test against an XA database, just add the corresponding XA database profile to the mvn cmdline above. 

Example:

```shell
mvn clean install -Pengine-integration,wildfly,postgresql,postgresql-xa
```

You can select multiple testsuites but only a single database and a single runtime container. This is valid:

```shell
mvn clean install -Pengine-integration,webapps-integration,tomcat,postgresql
```

There is a special profile for the WildFly Application Servers:

* WildFly Domain mode: `mvn clean install -Pengine-integration,h2,wildfly-domain`

# No Maven? No problem!

This project provides a [Maven Wrapper](https://github.com/takari/maven-wrapper). This feature is useful for developers
to build and test the project with the same version that Operaton uses. It's also useful for developers who don't want
to install Maven at all. By executing the `./mvnw` script (Unix), or `./mvnw.cmd` script (Windows), a Maven distro will be 
downloaded and installed in the `$USER_HOME/.m2/wrapper/dists` folder of the system. You can check the download URL in
the [.mvn/wrapper/maven-wrapper.properties](.mvn/wrapper/maven-wrapper.properties) file.

The Maven Wrapper requires Maven commands to be executed from the root of the project. As the Operaton project
is a multi-module (Maven Reactor) project, this is also a good best practice to apply.

To build the whole project, or just a module, one of the following commands may be executed:

```shell
# build the whole project
./mvnw clean install

# build the engine module
./mvnw clean install -f engine/pom.xml

# run the rolling-update IT tests with the H2 database
./mvnw verify -f qa/test-db-rolling-update/pom.xml -Prolling-update,h2
```

> Note: Above the `mvn -f` command line option is recommended over the `mvn -pl` option. The reason is that `-pl` will
build only the specified module, and will ignore any sub-modules that it might contain (unless the `-amd` option is also
added). As the Operaton project has a multi-tiered module hierarchy (e.g. the [qa](qa) module has modules of 
it's own), the `mvn -f` command option is simpler. 

## What about database technology X in environment Y?

To make a statement regarding Operaton support, we need to understand if technology X is one of the technologies we already support or different technology. 
Several databases may share the same or a similar name, but they can still be different technologies: 
For example, IBM DB2 z/OS behaves quite differently from IBM DB2 on Linux, Unix, Windows. 
Amazon Aurora Postgres is different from a standard Postgres.

If you want to make sure that a given database works well with the Operaton, you can run the test suite against this database.

In the [`database/pom.xml`](./database/pom.xml) file, several database profiles are defined with a matching database driver.

To run the test suite against a given database, select the `database` profile and your desired database profile and provide the connection parameters:

Example
```
mvn test -Pdatabase,postgresql -Ddatabase.url=jdbc:postgresql:pgdb -Ddatabase.username=pguser -Ddatabase.password=pgpassword
```

## Testing a Operaton-supported Database with Testcontainers

It is also possible to use [Testcontainers](https://testcontainers.com/) to run the test suite against a given database. 
To ensure that your database Docker image can be used this way, please perform the following steps:

1. Ensure that your Docker image is compatible with Testcontainers;
1. Provide the repository name of your Docker image in the [testcontainers.properties](./test-utils/testcontainers/src/main/resources/testcontainers.properties) file;
   * If you use a private Docker repository, please include it in the Docker image name (e.g. private.registry.org/postgres)
1. In the [`database/pom.xml`](./database/pom.xml) file, check out the `database.tc.url` property to ensure that 
   the Docker tags match.
1. Make sure that the `testcontainers` profile is added to your Maven `settings.xml` (you can find it [here](settings/maven/nexus-settings.xml)).

At the moment, Testcontainers can be used with the Operaton-supported versions of the following databases. 
Please make sure that the database image is configured with the proper isolation-level:
* PostgreSQL
* MariaDB
* MySQL
* MS-SQL 2017/2019 

To execute the process engine test suite with a certain database (e.g. PostgreSQL), you should call Maven in the 
engine directory with
```shell
mvn clean test -Ppostgresql,testcontainers
```

# Limiting the Number of Engine Unit Tests

Due to the fact that the number of unit tests in the Operaton engine increases daily and that you might just want to test a certain subset of tests the `maven-surefire-plugin` is configured in a way that you can include/exclude certain packages in your tests.

There are two properties that can be used for that: ``test.includes`` and ``test.excludes``

When using the includes only the packages listed will be include and with excludes the other way around.
For example calling Maven in the engine directory with
```shell
mvn clean test -Dtest.includes=bpmn
```
will test all packages that contain "bpmn". This will include e.g. ``*test.bpmn*`` and ``*api.bpmn*``. 
If you want to limit this further you have to get more concrete. Additionally, you can combine certain packages with a pipe:
```shell
mvn clean test -Dtest.includes=bpmn|cmmn
```
will execute all bpmn and cmmn tests.

The same works for excludes. Also, you can combine both:
```shell
mvn clean test -Dtest.includes=bpmn -Dtest.excludes=bpmn.async
```
Please note that excludes take precedence over includes.

To make it easier for you we created some profiles with predefined in- and excludes:
- `testBpmn`
- `testCmmn`
- `testBpmnCmmn`
- `testExceptBpmn`
- `testExceptCmmn`
- `testExceptBpmnCmmn`

So simply call
```shell
mvn clean test -PtestExceptBpmn
```
and all the bpmn testcases won't bother you any longer.
