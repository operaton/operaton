# Null-Safety in Operaton

This document provides guidance on using JSpecify null-safety annotations in the Operaton codebase.

## Overview

Operaton uses [JSpecify](https://jspecify.dev/) annotations to express nullability contracts in all public APIs. This helps:

- **Developers**: Get IDE warnings when passing null to non-null parameters
- **Documentation**: Make it explicit whether null is accepted or returned
- **Static Analysis**: Enable tools to detect potential NullPointerExceptions at compile-time

See [ADR-0001: Null Safety Strategy](decisions/0001-null-safety-strategy-for-operaton.md) for the architectural decision behind this choice.

## Current Status

All public API packages (those not in `.impl.` packages) are marked with `@NullMarked` at the package level via `package-info.java` files. This means:

- **By default**, all parameters and return values are considered non-null
- **Explicit `@Nullable`** annotations indicate where null is allowed or returned

## Using Null-Safety Annotations

### For Public APIs

When working with public APIs (packages not containing `.impl.`):

#### 1. Package-Level Annotation

All public API packages already have `@NullMarked` applied via `package-info.java`:

```java
@NullMarked
package org.operaton.bpm.engine;

import org.jspecify.annotations.NullMarked;
```

#### 2. Method Parameters

Parameters are non-null by default. Mark nullable parameters explicitly:

```java
public interface RuntimeService {
  
  // Good: variables can be null
  ProcessInstance startProcessInstanceByKey(
      String processDefinitionKey,
      @Nullable Map<String, Object> variables);
  
  // Good: all parameters are non-null (default)
  void setVariable(String executionId, String variableName, Object value);
}
```

#### 3. Return Values

Return values are non-null by default. Mark nullable returns explicitly:

```java
public interface RuntimeService {
  
  // Good: returns null if not found
  @Nullable
  ProcessInstance findProcessInstanceById(String processInstanceId);
  
  // Good: never returns null (default)
  List<ProcessInstance> findProcessInstances();
}
```

#### 4. Collections and Optionals

For collections and Optional:

```java
// Prefer: Return empty collection instead of null
List<Task> getTasks();  // Never null, may be empty

// Use Optional for single values that may be absent
Optional<Task> findTask(String taskId);

// If you must return nullable collection (legacy APIs)
@Nullable
List<Task> getTasksOrNull();
```

### For Implementation Code

Null-safety annotations in implementation packages (`.impl.`) are optional but encouraged:

```java
// Implementation package - annotations optional but helpful
package org.operaton.bpm.engine.impl.cmd;

public class StartProcessInstanceCmd implements Command<ProcessInstance> {
  
  private final String processDefinitionKey;
  @Nullable
  private final Map<String, Object> variables;
  
  public StartProcessInstanceCmd(
      String processDefinitionKey,
      @Nullable Map<String, Object> variables) {
    this.processDefinitionKey = processDefinitionKey;
    this.variables = variables;
  }
}
```

## Documentation in Javadoc

Always document nullability semantics in Javadoc, in addition to using annotations:

```java
/**
 * Starts a new process instance.
 *
 * @param processDefinitionKey the key of the process definition (must not be null)
 * @param variables process variables to set on the new instance (may be null or empty)
 * @return the started process instance (never null)
 * @throws ProcessEngineException if the process cannot be started
 */
ProcessInstance startProcessInstanceByKey(
    String processDefinitionKey,
    @Nullable Map<String, Object> variables);
```

## Common Patterns

### Pattern 1: Optional Parameters

```java
// Good: Use @Nullable for optional parameters
public Task createTask(@Nullable String taskName, @Nullable String assignee);

// Alternative: Use method overloading
public Task createTask();
public Task createTask(String taskName);
public Task createTask(String taskName, String assignee);
```

### Pattern 2: Lookup Methods

```java
// Good: Return null for "not found" semantics
@Nullable
public Task getTask(String taskId);

// Alternative: Use Optional
public Optional<Task> findTask(String taskId);

// For lists: Return empty collection, never null
public List<Task> getTasks(String processInstanceId);
```

### Pattern 3: Builder/Fluent APIs

```java
public class ProcessInstanceBuilder {
  
  // Good: Fluent APIs often accept null to mean "don't set"
  public ProcessInstanceBuilder businessKey(@Nullable String businessKey) {
    this.businessKey = businessKey;
    return this;
  }
  
  // Result should never be null
  public ProcessInstance start();
}
```

## IDE Support

### IntelliJ IDEA

IntelliJ has built-in support for JSpecify annotations (2023.2+):

1. Go to **Settings → Editor → Inspections**
2. Enable **Probable bugs → Nullability problems**
3. Configure to recognize JSpecify annotations

### Eclipse

Eclipse supports JSpecify through configuration:

1. **Window → Preferences → Java → Compiler → Errors/Warnings**
2. Enable **Null analysis**
3. Configure null annotations to recognize JSpecify

### VS Code

VS Code with Java extensions supports JSpecify:

1. Install the Java extension pack
2. The language server will automatically recognize JSpecify annotations

## Static Analysis Tools

### Error Prone

We plan to integrate Error Prone with JSpecify support in CI to catch nullability violations.

### NullAway

NullAway is another option for fast null-checking that works well with JSpecify annotations.

## Migration Guide

### Adding Null-Safety to Existing Code

When modifying existing public APIs:

1. **Identify clearly non-null cases**: Parameters that are always checked or dereferenced immediately
2. **Identify clearly nullable cases**: Parameters that are checked with `if (param == null)` or return values that can be null
3. **Add annotations gradually**: Start with the most obvious cases
4. **Document in Javadoc**: Always explain nullability behavior

Example:

```java
// Before
public Task getTask(String taskId) {
  // implementation that may return null
}

// After
/**
 * Retrieves a task by ID.
 *
 * @param taskId the ID to search for (must not be null)
 * @return the task, or null if not found
 */
@Nullable
public Task getTask(String taskId) {
  // implementation that may return null
}
```

### Handling Legacy Code

For legacy APIs where nullability is unclear:

1. **Check the implementation**: See how parameters are actually used
2. **Check the tests**: See what values tests pass in
3. **Check the documentation**: Look for existing nullability hints
4. **When in doubt**: Don't add annotations yet - mark as "TODO" for future cleanup

## Testing Null-Safety

### Test Non-Null Parameters

```java
@Test
void shouldRejectNullProcessDefinitionKey() {
  assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey(null, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("processDefinitionKey");
}
```

### Test Nullable Parameters

```java
@Test
void shouldAcceptNullVariables() {
  ProcessInstance instance = runtimeService.startProcessInstanceByKey("process", null);
  assertThat(instance).isNotNull();
}
```

### Test Nullable Returns

```java
@Test
void shouldReturnNullWhenTaskNotFound() {
  Task task = taskService.getTask("nonexistent");
  assertThat(task).isNull();
}
```

## Resources

- [JSpecify Home](https://jspecify.dev/)
- [JSpecify User Guide](https://jspecify.dev/docs/user-guide)
- [JSpecify API Documentation](https://javadoc.io/doc/org.jspecify/jspecify/latest/index.html)
- [ADR-0001: Null Safety Strategy](decisions/0001-null-safety-strategy-for-operaton.md)

## FAQ

### Q: Do I need to annotate every parameter and return value?

A: No. Only annotate when:
- The parameter can be null (`@Nullable`)
- The return value can be null (`@Nullable`)
- Otherwise, assume non-null (default in `@NullMarked` packages)

### Q: What about primitive types?

A: Primitives cannot be null, so they never need annotations.

### Q: Should I annotate implementation code?

A: It's optional but encouraged. Focus on public APIs first.

### Q: What if I'm not sure if something can be null?

A: Check the implementation, tests, and existing behavior. If unclear, leave it unmarked and add a TODO comment for future clarification.

### Q: Can I use other nullability annotations?

A: No. We standardize on JSpecify to avoid confusion and ensure tool compatibility.

### Q: What about array elements? Can they be null?

A: JSpecify 1.0 considers this an advanced topic. For now, document in Javadoc if array elements can be null.

### Q: How do I mark a field as nullable?

A: Same as parameters and returns:
```java
@Nullable
private String optionalField;
```

### Q: What about generic type parameters?

A: JSpecify 1.0 supports this, but keep it simple for now. Document complex cases in Javadoc.
