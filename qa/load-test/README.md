# Load Test Module

This module is used to run a load test simulating of process executions.

## Overview

The test starts an embedded Operaton Spring Boot application, deploys BPMN/DMN
processes from the issue, sets up WireMock stubs for external HTTP services, then
runs concurrent threads repeatedly starting process instances while
monitoring JVM heap memory.

## Prerequisites

- **JDK 17+**
- All Operaton modules must be built first.

```bash
# Build all modules first (from repository root)
./mvnw clean install -DskipTests -Dskip.frontend.build=true
```

## Running the Load Test

```bash
./mvnw verify -pl qa/load-test -Pload-test
```

## Configuration

System properties can be passed via `-D` flags:

| Property                         | Default            | Description                                          |
|----------------------------------|--------------------|------------------------------------------------------|
| `loadtest.users`                 | 30                 | Number of concurrent threads                         |
| `loadtest.warmup.seconds`        | 10                 | Warmup phase duration                                |
| `loadtest.sustained.seconds`     | 60                 | Sustained load phase duration                        |
| `loadtest.memory.samples`        | 6                  | Number of heap memory samples during sustained phase |
| `loadtest.processKey`            | credit-eligibility | Process definition key to test                       |
| `loadtest.withVariablesInReturn` | true               | Include variables in start response                  |

Example with custom settings:

```bash
./mvnw verify -pl qa/load-test -Pload-test \
  -Dloadtest.users=50 \
  -Dloadtest.sustained.seconds=120
```

## Memory Configuration

The test JVM is configured with:
- `-Xmx1536m` (1.5 GB max heap, matching the issue's Docker memory limit)
- `-XX:+UseG1GC`

These are set in `pom.xml` via the `maven-failsafe-plugin` configuration.

## Test Architecture

1. **Spring Boot Application** starts with H2 in-memory database, HTTP connector, and Spin JSON plugins
2. **WireMock** serves three mock endpoints:
   - `GET /api/customers/123` — customer data
   - `GET /api/employment/termination` — rescisão check
   - `GET /api/employment/leave` — afastamento check
3. **Process variables** `BASE_URL` and `EMPLOYMENT_API_URL` point to the WireMock server
4. **Concurrent threads** simulate concurrent users calling the REST API to start process instances
5. **Memory monitoring** samples heap usage at regular intervals

## Analyzing Results

### Test Output

The test logs memory samples during execution:

```
Memory sample 1/10: 245 MB
Memory sample 2/10: 248 MB
...
Memory sample 10/10: 252 MB
```

### Assertions

The test asserts:
1. At least some process instances complete successfully
2. Final heap after GC does not exceed 2x the post-warmup baseline (or baseline + 300 MB)
3. Memory growth between first and last quarter of the sustained phase is less than 200 MB

### Enabling GC Logs

Add to the failsafe plugin `<argLine>` in `pom.xml`:

```
-Xlog:gc*:file=target/gc.log:time,uptime,level,tags
```

### Heap Dump on OOM

Add to `<argLine>`:

```
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=target/
```

### Java Flight Recorder

```bash
./mvnw verify -pl qa/load-test -Pload-test \
  -DargLine="-Xmx1536m -XX:+UseG1GC -XX:StartFlightRecording=filename=target/loadtest.jfr,duration=120s"
```

Then analyze with `jfr print target/loadtest.jfr` or JDK Mission Control.

## BPMN Process

The `credit-eligibility` process:
- Starts via REST API with JSON input variable
- Executes synchronously (no async boundaries)
- Makes 3 HTTP connector GET calls to external services
- Evaluates 3 DMN decision tables (age, fee, TOJ)
- Uses Spin JSON extensively for variable transformation
