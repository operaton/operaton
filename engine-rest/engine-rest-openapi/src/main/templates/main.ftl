<#import "/lib/utils.ftl" as lib>

<#assign docsUrl = "https://docs.operaton.org/manual/${docsVersion}">
{
  "openapi": "3.0.2",
  "info": {
    "title": "Operaton REST API",
    "description": "## Operaton REST API\n\nThe Operaton REST API is a JSON/HTTP API for interacting with the Operaton BPMN process engine.\n\n### Authentication\n\nBy default, the REST API is not secured. In production deployments, enable HTTP Basic Authentication or container-managed security. See the [Authentication](${docsUrl}/reference/rest/overview/authentication/) section for configuration options.\n\n### Authorization\n\nWhen Operaton's authorization system is enabled, most operations require the caller to hold the corresponding permission on the target resource. See the [Authorization](${docsUrl}/reference/rest/overview/authorization/) section.\n\n### Error Handling\n\nAll error responses use a consistent JSON structure:\n\n```json\n{\n  \"type\": \"RestException\",\n  \"message\": \"Human-readable description of the error.\"\n}\n```\n\nParse errors (BPMN deployment failures) use an extended `ParseExceptionDto` structure that includes line numbers and element IDs. See [Error Handling](${docsUrl}/reference/rest/overview/#error-handling).\n\n### Pagination\n\nList endpoints support pagination via `firstResult` (zero-based offset) and `maxResults` (page size). To retrieve the total count independently, use the corresponding `/count` endpoint.\n\n### Date Formats\n\nAll date and datetime values use [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) format with timezone offset: `2025-01-15T10:30:00.000+0100`.\n\n### Multi-Tenancy\n\nEndpoints that operate on tenant-aware resources accept a `tenantId` parameter. When omitted, resources for all tenants visible to the authenticated user are returned.\n\n### Variables\n\nProcess and task variables use a typed-value format:\n\n```json\n{\n  \"myVariable\": {\n    \"type\": \"String\",\n    \"value\": \"hello\",\n    \"valueInfo\": {}\n  }\n}\n```\n\nFor more details, see the [Spin data formats](${docsUrl}/reference/spin/) documentation.\n\n### Named Engines\n\nIn multi-engine setups, prefix the path with `/engine/{engineName}/` to target a specific engine. The default engine uses the path without the prefix.\n\n### Further Reading\n\n- [REST API Overview](${docsUrl}/reference/rest/overview/)\n- [User Guide](${docsUrl}/user-guide/)\n- [BPMN 2.0 Reference](${docsUrl}/reference/bpmn20/)",
    "version": "${operatonbpmVersion}",
    "license": {
      "name": "Apache License 2.0",
      "url": "https://www.apache.org/licenses/LICENSE-2.0.html"
    }
  },
  "externalDocs": {
    "description": "Find out more about Operaton Rest API",
    "url": "${docsUrl}/reference/rest/overview/"
  },
  "servers": [

  <@lib.server
      url = "http://{host}:{port}/{contextPath}"
      variables = {"host": "localhost", "port": "8080", "contextPath": "engine-rest"}
      desc = "The API server for the default process engine" />

  <@lib.server
      url = "http://{host}:{port}/{contextPath}/engine/{engineName}"
      variables = {"host": "localhost", "port": "8080", "contextPath": "engine-rest", "engineName": "default"}
      desc = "The API server for a named process engine"/>

  <@lib.server
      url = "{url}"
      variables = {"url": ""}
      desc = "The API server with a custom url"
      last = true />

  ],
  "tags": [
    {
      "name": "Authorization",
      "description": "Manages access control entries (ACEs) that grant or revoke permissions for users and groups on specific resources. Use these endpoints to create, update, delete, and query authorization rules. For an overview of the permission model, see the Authorization section of the user guide.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/authorization/"}
    },
    {
      "name": "Batch",
      "description": "Represents an asynchronous bulk operation running in the background via the job executor, such as bulk deletion of process instances or bulk retry of jobs. Endpoints provide visibility into batch progress, pause or resume execution, and retrieve historic batch records.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/batch/"}
    },
    {
      "name": "Condition",
      "description": "Provides an endpoint to trigger evaluation of condition-based start events. Send a condition variable map; the engine evaluates all condition start events and starts matching process instances.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/condition/"}
    },
    {
      "name": "Decision Definition",
      "description": "Represents a deployed DMN decision table or decision literal expression. Endpoints support listing, retrieving, evaluating, and querying the XML source of decision definitions. Decision definitions are created by deploying DMN resources.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/decision-definition/"}
    },
    {
      "name": "Decision Requirements Definition",
      "description": "Represents a DMN Decision Requirements Graph (DRG), a collection of related decision definitions with explicit dependency relationships. Endpoints support listing, retrieving, and retrieving the XML and diagram of decision requirements definitions.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/decision-requirements-definition/"}
    },
    {
      "name": "Deployment",
      "description": "Represents a single deployment operation that uploaded one or more BPMN, CMMN, or DMN resources to the engine. Endpoints support creating new deployments, listing and retrieving existing deployments, and deleting deployments. Creating a deployment parses resources and registers the contained definitions.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/deployment/"}
    },
    {
      "name": "Engine",
      "description": "Provides a single endpoint that returns the names of all process engines available in the current installation. Use this to discover available named engines for multi-engine setups.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/engine/"}
    },
    {
      "name": "Event Subscription",
      "description": "Represents a registered subscription to a BPMN event (message, signal, conditional, or compensate). Endpoints support listing and counting active event subscriptions across all process instances.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/event-subscription/"}
    },
    {
      "name": "Execution",
      "description": "Represents a pointer tracking the current position of a token within a process instance. A single process instance can have multiple concurrent executions when parallel flows are active. Endpoints support querying executions, retrieving and setting local variables on executions, and triggering message event subscriptions.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/execution/"}
    },
    {
      "name": "External Task",
      "description": "Represents a service task configured for external handling: the engine publishes the task, and an external worker process fetches, locks, and completes it. Endpoints support the full external task lifecycle: fetch-and-lock, complete, report failure, report BPMN error, extend lock, and unlock.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/external-task/"}
    },
    {
      "name": "Filter",
      "description": "Represents a saved task query that can be executed repeatedly, optionally shared with groups. Endpoints support creating, updating, deleting, and executing filters. Used primarily by the Operaton Tasklist web application.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/filter/"}
    },
    {
      "name": "Group",
      "description": "Represents a user group in Operaton's built-in identity service. Endpoints support creating, retrieving, updating, and deleting groups, and managing group membership. For LDAP-backed identity, most write operations are unavailable.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/group/"}
    },
    {
      "name": "Historic Activity Instance",
      "description": "Provides access to the audit history of individual activity instances — that is, every time a BPMN activity was entered and exited during process execution. Supports list, count, and single-record retrieval with rich filtering options.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/activity-instance/"}
    },
    {
      "name": "Historic Batch",
      "description": "Provides access to the audit history of completed or ongoing batch operations. Supports listing and counting historic batch records, and cleaning up finished batches.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/batch/"}
    },
    {
      "name": "Historic Decision Definition",
      "description": "Provides statistics about historic decision evaluations grouped by decision definition. Supports retrieving the count of historic decision instances per definition.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/decision-definition/"}
    },
    {
      "name": "Historic Decision Instance",
      "description": "Provides access to the complete audit history of every DMN decision evaluation: input values, output values, rules matched, and evaluation time. Supports list, count, single-record retrieval, and bulk deletion.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/decision-instance/"}
    },
    {
      "name": "Historic Decision Requirements Definition",
      "description": "Provides access to historic decision requirements definitions, including the count of decision instances evaluated under each requirements graph.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/decision-requirements-definition/"}
    },
    {
      "name": "Historic Detail",
      "description": "Provides access to fine-grained audit events recorded during process execution: variable updates, form fields submitted, and other detail records. Supports list, count, and single-record retrieval with extensive filter options.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/detail/"}
    },
    {
      "name": "Historic External Task Log",
      "description": "Provides access to the audit log of external task lifecycle events: creation, failure, success, deletion, and lock expiration. Supports list, count, and single-record retrieval.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/external-task-log/"}
    },
    {
      "name": "Historic Identity Link Log",
      "description": "Provides access to the history of identity link changes on tasks and process definitions: candidate user/group additions and removals, assignee changes. Supports list and count.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/identity-link-log/"}
    },
    {
      "name": "Historic Incident",
      "description": "Provides access to the audit history of incidents: when they were created, when they were resolved or deleted, and their cause. Supports list and count.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/incident/"}
    },
    {
      "name": "Historic Job Log",
      "description": "Provides access to the audit history of job execution events: creation, execution, failure, and deletion. Each entry captures the job's configuration and outcome at the time of the event. Supports list, count, and single-record retrieval including stack trace retrieval.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/job-log/"}
    },
    {
      "name": "Historic Process Definition",
      "description": "Provides statistics about process definitions based on historic data, including the number of instances in each state (active, suspended, completed, externally terminated, internally terminated). Also provides access to cleanable process instance reports.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/process-definition/"}
    },
    {
      "name": "Historic Process Instance",
      "description": "Provides access to the complete audit history of all process instances, including those that have finished. Supports list, count, single-record retrieval, deletion, and bulk variable retrieval.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/process-instance/"}
    },
    {
      "name": "Historic Task Instance",
      "description": "Provides access to the audit history of task instances: creation, assignment, completion, and deletion events. Supports list, count, and single-record retrieval with extensive filter options.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/task-instance/"}
    },
    {
      "name": "Historic User Operation Log",
      "description": "Provides access to the audit log of operations performed by users through the API (deployments, task completions, variable modifications, suspension state changes, etc.). Supports list, count, and annotation of individual log entries.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/user-operation-log/"}
    },
    {
      "name": "Historic Variable Instance",
      "description": "Provides access to the last known value of every process variable for historic process instances. Supports list, count, and retrieval of binary variable data.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history/variable-instance/"}
    },
    {
      "name": "History Cleanup",
      "description": "Manages the history cleanup job that removes historic data from the database based on configured time-to-live (TTL) settings. Endpoints support triggering cleanup, querying cleanup configuration, and retrieving cleanup job details.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/history-cleanup/"}
    },
    {
      "name": "Identity",
      "description": "Provides utility endpoints for the identity subsystem: retrieving group information for the current user and validating passwords against the configured password policy.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/identity/"}
    },
    {
      "name": "Incident",
      "description": "Represents a problem record automatically created when a job or external task exhausts its retries, or created manually. Endpoints support listing, counting, retrieving, creating, deleting, and resolving incidents.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/incident/"}
    },
    {
      "name": "Job",
      "description": "Represents an internal unit of work managed by the job executor: timer events, asynchronous continuations, and message correlations. Endpoints support listing, retrieving, executing, retrying, suspending, and configuring jobs.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/job/"}
    },
    {
      "name": "Job Definition",
      "description": "Represents the configuration of a class of jobs associated with a specific activity in a process definition. Endpoints support listing, retrieving, and modifying retry configuration, priority, and suspension state for all jobs of a given type.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/job-definition/"}
    },
    {
      "name": "Message",
      "description": "Provides a single endpoint for correlating a named BPMN message to one or more waiting process instances or start events. Used to trigger message start events and resume processes waiting at intermediate message catch events.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/message/"}
    },
    {
      "name": "Metrics",
      "description": "Provides time-series metrics about engine activity: number of activity instances, job executions, and decision evaluations. Supports aggregated queries by time range and returns sum metrics per metric name.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/metrics/"}
    },
    {
      "name": "Migration",
      "description": "Provides endpoints for migrating running process instances from one version of a process definition to another. Supports generating migration plans, validating plans, and executing migrations synchronously or asynchronously.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/migration/"}
    },
    {
      "name": "Modification",
      "description": "Provides endpoints for modifying the token flow of running process instances: starting before or after activities, and cancelling active activity instances. Supports synchronous and asynchronous (batch) execution.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/modification/"}
    },
    {
      "name": "Process Definition",
      "description": "Represents a deployed BPMN process definition — the blueprint for creating process instances. Endpoints support listing, retrieving, starting instances, retrieving XML source and diagram images, suspending and activating, deleting, and querying statistics.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/process-definition/"}
    },
    {
      "name": "Process Instance",
      "description": "Represents a single running execution of a process definition. Endpoints support listing, retrieving, deleting, modifying variables, retrieving activity instances, suspending and activating, and bulk operations via batch.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/process-instance/"}
    },
    {
      "name": "Signal",
      "description": "Provides a single endpoint for throwing a named BPMN signal event to all active signal event handlers. Signals have global broadcast semantics — all matching handlers receive the signal simultaneously.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/signal/"}
    },
    {
      "name": "Schema Log",
      "description": "Provides an endpoint to retrieve the history of database schema updates applied by Liquibase. Useful for verifying the database schema version in deployed installations.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/schema-log/"}
    },
    {
      "name": "Task",
      "description": "Represents a human task assigned to a user or candidate group. Endpoints support the full task lifecycle: listing, retrieving, creating, updating, delegating, claiming, unclaiming, completing, resolving, submitting task forms, and managing task-level variables, attachments, comments, and identity links.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/task/"}
    },
    {
      "name": "Task Attachment",
      "description": "Manages file attachments on individual tasks. Endpoints support creating, retrieving, listing, and deleting attachments, as well as retrieving attachment binary content.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/task/attachment/"}
    },
    {
      "name": "Task Comment",
      "description": "Manages text comments on individual tasks. Endpoints support creating, retrieving, listing, and deleting comments. Comments provide a human-readable audit trail of task activity.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/task/comment/"}
    },
    {
      "name": "Task Identity Link",
      "description": "Manages identity links on tasks: relationships between a task and a user or group (candidate, assignee, owner). Endpoints support adding, removing, and listing identity links.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/task/identity-links/"}
    },
    {
      "name": "Task Local Variable",
      "description": "Manages variables that are scoped exclusively to a single task (not visible to the process instance). Endpoints support listing, getting, setting, and deleting local variables, including binary and file variables.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/task/local-variables/"}
    },
    {
      "name": "Task Variable",
      "description": "Manages variables visible in the scope of a task, including variables from the enclosing process instance scope. Endpoints support listing, getting, setting, and deleting variables, including binary and file variables.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/task/variables/"}
    },
    {
      "name": "Telemetry",
      "description": "**Deprecated.** Telemetry data collection has been removed from Operaton. These endpoints are retained for backwards compatibility but return empty or no-op responses. They will be removed in a future release.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/telemetry/"}
    },
    {
      "name": "Tenant",
      "description": "Represents a tenant in a multi-tenancy deployment. Endpoints support creating, retrieving, updating, and deleting tenants, and managing tenant membership for users and groups.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/tenant/"}
    },
    {
      "name": "User",
      "description": "Represents a user in Operaton's built-in identity service. Endpoints support creating, retrieving, updating, and deleting users, managing user credentials, and unlocking locked user accounts. For LDAP-backed identity, most write operations are unavailable.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/user/"}
    },
    {
      "name": "Variable Instance",
      "description": "Represents a live process variable in a running process instance, execution, task, or case execution. Endpoints support listing, counting, and retrieving variable instances and their binary data.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/variable-instance/"}
    },
    {
      "name": "Version",
      "description": "Provides a single endpoint that returns the version number of the Operaton engine currently deployed. Useful for compatibility checks and health verification.",
      "externalDocs": {"description": "Find out more", "url": "${docsUrl}/reference/rest/version/"}
    }
  ],
  "paths": {

    <#list endpoints as path, methods>
        "${path}": {
            <#list methods as method>
                <#import "/paths${path}/${method}.ftl" as endpoint>
                "${method}":
                <@endpoint.endpoint_macro docsUrl=docsUrl/><#sep>,
            </#list>
        }<#sep>,
    </#list>

  },
  "security": [ {"basicAuth": []} ],
  "components": {
    "securitySchemes": {
      "basicAuth": {
        "type": "http",
        "scheme": "basic"
      }
    },
    "schemas": {

    <#list models as name, package>
        <#import "/models/${package}/${name}.ftl" as schema>
        "${name}": <@schema.dto_macro docsUrl=docsUrl/><#sep>,
    </#list>

    }
  }
}
