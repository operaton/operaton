# Backport Notes

## Camunda Commit 320e9f6dd9 - Revert unnecessary changes for migrator

**Status**: No changes required

**Analysis**:
The Camunda commit 320e9f6dd9 reverted afterId functionality from the following query implementations:
- HistoricActivityInstanceQueryImpl.java
- HistoricIncidentQueryImpl.java  
- HistoricProcessInstanceQueryImpl.java
- HistoricTaskInstanceQueryImpl.java
- ProcessDefinitionQueryImpl.java
- ProcessInstanceQueryImpl.java
- Corresponding XML mapping files
- QueryByIdAfterTest.java

**Findings**:
Investigation of the Operaton codebase shows that these files do NOT currently contain the afterId functionality that was reverted in Camunda. This indicates that either:
1. The original afterId functionality for Historic and Process queries was never backported to Operaton, or
2. It was already reverted in a previous backport

**Note**: The current PR #1215 adds afterId functionality to different queries (FilterQuery, UserQuery, TaskQuery) which are separate from and unrelated to the functionality that was reverted in Camunda.

**Conclusion**: 
No code changes are required for this backport as the functionality to be reverted does not exist in the current Operaton codebase.