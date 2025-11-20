# Proposed Changes for Oracle Test Failure

## Issue Summary

Test `GetHistoricOperationLogsForOptimizeTest.getHistoricUserOperationLogs_suspendProcessDefinitionByKey` is failing on Oracle with:

```
expected: "active"
 but was: "true"
```

at line 297 of the test file.

## Root Cause Analysis

### The Problem

The test creates 4 operation log entries:
- 2 entries at timestamp `now + 2 seconds` (SUSPEND operations)
- 2 entries at timestamp `now + 4 seconds` (ACTIVATE operations)

When querying these entries, the SQL query only orders by `TIMESTAMP_` (ascending):

```sql
ORDER BY RES.TIMESTAMP_ ASC
```

**Issue**: When multiple rows have the **same timestamp**, the order of those rows is **undefined** and database-dependent. Different databases use different internal ordering mechanisms (e.g., insertion order, physical row location, etc.), which causes the test to pass on some databases but fail on Oracle.

### Expected vs Actual Order

**Expected order** (works on H2, PostgreSQL, MySQL, etc.):
1. Entry 0: SUSPEND_PROCESS_DEFINITION, newValue="suspended", PROPERTY_="suspensionState"
2. Entry 1: SUSPEND_PROCESS_DEFINITION, newValue="true", PROPERTY_="includeInstances"
3. Entry 2: ACTIVATE_PROCESS_DEFINITION, newValue="active", PROPERTY_="suspensionState"
4. Entry 3: ACTIVATE_PROCESS_DEFINITION, newValue="true", PROPERTY_="includeInstances"

**Actual order on Oracle**:
1. Entry 0: SUSPEND_PROCESS_DEFINITION, newValue="suspended", PROPERTY_="suspensionState"
2. Entry 1: SUSPEND_PROCESS_DEFINITION, newValue="true", PROPERTY_="includeInstances"
3. Entry 2: ACTIVATE_PROCESS_DEFINITION, newValue="true", PROPERTY_="includeInstances" ⚠️ **SWAPPED**
4. Entry 3: ACTIVATE_PROCESS_DEFINITION, newValue="active", PROPERTY_="suspensionState" ⚠️ **SWAPPED**

Oracle returns the two ACTIVATE entries (both at timestamp `now + 4 seconds`) in reverse order compared to other databases.

## Proposed Solution

### Option 1: Fix the SQL Query (Recommended)

Add a secondary sort field to make ordering deterministic across all databases.

**File**: `/Users/zoeller/sources/operaton/operaton-bpm-platform/engine/src/main/resources/org/operaton/bpm/engine/impl/mapping/entity/UserOperationLogEntry.xml`

**Line**: 485

**Change**:
```xml
<!-- BEFORE -->
ORDER BY RES.TIMESTAMP_ ASC

<!-- AFTER -->
ORDER BY RES.TIMESTAMP_ ASC, RES.ID_ ASC
```

**Rationale**:
- `ID_` is the primary key (unique identifier) for each operation log entry
- Adding `ID_` as a secondary sort ensures consistent, deterministic ordering across all databases
- This is a production code fix that benefits all users of the Optimize API, not just tests
- No breaking changes - the order will still be primarily by timestamp

### Option 2: Fix the Test (Alternative)

Modify the test to not rely on a specific order within entries that have the same timestamp.

**File**: `/Users/zoeller/sources/operaton/operaton-bpm-platform/engine/src/test/java/org/operaton/bpm/engine/test/api/optimize/GetHistoricOperationLogsForOptimizeTest.java`

**Lines**: 292-299

**Change**: Instead of asserting on specific indices (0, 1, 2, 3), group entries by timestamp and assert on the content within each group without assuming order.

**Rationale**:
- This would make the test more robust
- However, it doesn't fix the underlying non-deterministic behavior in the production code
- Other users of the API may also expect consistent ordering

## Recommendation

**Implement Option 1** (fix the SQL query) because:

1. It fixes the root cause in production code, not just in tests
2. Consistent ordering is a desirable property for an API
3. It's a minimal, safe change that doesn't break existing functionality
4. It benefits all users of the `getHistoricUserOperationLogs` API
5. The performance impact is negligible since we're already ordering by TIMESTAMP_ and ID_ is the primary key

## Table Structure Reference

The `ACT_HI_OP_LOG` table has the following relevant columns:

```sql
create table ACT_HI_OP_LOG (
    ID_ varchar(64) not null,           -- Primary key, unique identifier
    TIMESTAMP_ timestamp not null,       -- Current sort field
    OPERATION_TYPE_ varchar(64),         -- e.g., "SuspendProcessDefinition"
    PROPERTY_ varchar(64),               -- e.g., "suspensionState", "includeInstances"
    NEW_VALUE_ varchar(4000),            -- The value being asserted in the test
    -- ... other columns ...
    primary key (ID_)
);
```

## Files Involved

- **SQL Mapping** (needs change): `/Users/zoeller/sources/operaton/operaton-bpm-platform/engine/src/main/resources/org/operaton/bpm/engine/impl/mapping/entity/UserOperationLogEntry.xml:485`
- **Test File** (failing): `/Users/zoeller/sources/operaton/operaton-bpm-platform/engine/src/test/java/org/operaton/bpm/engine/test/api/optimize/GetHistoricOperationLogsForOptimizeTest.java:297`
- **Service Implementation**: `/Users/zoeller/sources/operaton/operaton-bpm-platform/engine/src/main/java/org/operaton/bpm/engine/impl/optimize/OptimizeManager.java:131-150`
