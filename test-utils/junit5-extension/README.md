# Operaton JUnit 5 / JUnit 6

JUnit 5 and JUnit 6 extension that allows you to inject a process engine into your test.

## Compatibility

This extension is compatible with both JUnit 5 and JUnit 6. Operaton provides separate artifacts with classifiers for each version:

- **JUnit 5**: Use the `junit5` classifier artifact
- **JUnit 6**: Use the `junit6` classifier artifact

## Usage

### Maven dependency

For JUnit 5, add the dependency to your pom.xml:

```xml
    <dependency>
      <groupId>org.operaton.bpm</groupId>
      <artifactId>operaton-bpm-junit5</artifactId>
      <version>7.17.0</version>
      <scope>test</scope>
    </dependency>
```

For JUnit 6 compatible engine test classes, use:

```xml
    <dependency>
      <groupId>org.operaton.bpm</groupId>
      <artifactId>operaton-engine</artifactId>
      <classifier>junit6</classifier>
      <scope>test</scope>
    </dependency>
```

### Test code
Add the annotation to your test class:

```java
    @ExtendWith(ProcessEngineExtension.class)
```

For further access provide a field where the process engine gets injected:

```java
    public ProcessEngine processEngine; 
```

Or register the extension from the builder:

```java
    @RegisterExtension
    static ProcessEngineExtension extension = ProcessEngineExtension.builder()
      .configurationResource("audithistory.operaton.cfg.xml")
      .build();
```

and access the process engine from the extension object:

```java
    RuntimeService runtimeService = extension.getProcessEngine().getRuntimeService(); 
```

If you don't want to create a configuration file, you can add a process engine, that you configure programmatically:

```java
    public ProcessEngine myProcessEngine = ProcessEngineConfiguration
        .createStandaloneInMemProcessEngineConfiguration()
        .setJdbcUrl("jdbc:h2:mem:operaton;DB_CLOSE_DELAY=1000")
        .buildProcessEngine();
    
    @RegisterExtension
    ProcessEngineExtension extension = ProcessEngineExtension
        .builder()
        .useProcessEngine(myProcessEngine)
        .build();
```
