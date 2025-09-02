<#macro endpoint_macro docsUrl="">
{

  <@lib.endpointInfo
      id = "getTask"
      tag = "Task"
      summary = "Get"
      desc = "Retrieves a task by id." />

  "parameters" : [

    <@lib.parameter
        name = "id"
        location = "path"
        type = "string"
        required = true
        desc = "The id of the task to be retrieved."/>

    <@lib.parameter
        name = "withCommentAttachmentInfo"
        location = "query"
        type = "boolean"
        defaultValue = "false"
        desc = "Check if task has attachments and/or comments. Value may only be `true`, as
               `false` is the default behavior.
               Adding the filter will do additional attachment and comments queries to the database,
               it might slow down the query in case of tables having high volume of data.
               This param is not considered for count queries" />

    <@lib.parameter
        name = "withTaskVariablesInReturn"
        location = "query"
        type = "boolean"
        defaultValue = "false"
        desc = "Indicates if all the variables visible from task should be retrieved. A variable is visible from the task
               if it is a local task variable or declared in a parent scope of the task. Value may only be `true`, as
               `false` is the default behavior." />

    <@lib.parameter
        name = "withTaskLocalVariablesInReturn"
        location = "query"
        type = "boolean"
        defaultValue = "false"
        last = true
        desc = "Indicates if all the local variables visible from task should be retrieved. Value may only be `true`, as
               `false` is the default behavior." />

  ],

  "responses" : {

    <@lib.response
        code = "200"
        dto = "TaskWithAttachmentAndCommentDto"
        desc = "Request successful."
        examples = ['"example-1": {
                       "summary": "GET /task/anId Response",
                       "value": {
                         "id":"anId",
                         "name":"aName",
                         "assignee":"anAssignee",
                         "created":"2013-01-23T13:42:42.000+0200",
                         "due":"2013-01-23T13:49:42.576+0200",
                         "followUp":"2013-01-23T13:44:42.437+0200",
                         "delegationState":"RESOLVED",
                         "description":"aDescription",
                         "executionId":"anExecution",
                         "owner":"anOwner",
                         "parentTaskId":"aParentId",
                         "priority":42,
                         "processDefinitionId":"aProcDefId",
                         "processInstanceId":"aProcInstId",
                         "caseDefinitionId":"aCaseDefId",
                         "caseInstanceId":"aCaseInstId",
                         "caseExecutionId":"aCaseExecution",
                         "taskDefinitionKey":"aTaskDefinitionKey",
                         "suspended": false,
                         "formKey":"aFormKey",
                         "operatonFormRef":{
                           "key": "aOperatonFormKey",
                           "binding": "version",
                           "version": 2
                         },
                         "tenantId":"aTenantId",
			 "taskState": "aTaskState"
                       }
                     }',
        '"example-2": {
                       "summary": "GET /task/anId?withCommentAttachmentInfo=true Response",
                       "value": [
                         {
                           "id": "349fffa8-6571-11e7-9a44-d6940f5ef88d",
                           "name": "Approve Invoice",
                           "assignee": "John Munda",
                           "created": "2017-07-10T15:10:54.670+0200",
                           "due": "2017-07-17T15:10:54.670+0200",
                           "followUp": null,
                           "lastUpdated": "2017-07-17T15:10:54.670+0200",
                           "delegationState": null,
                           "description": "Approve the invoice (or not).",
                           "executionId": "349f8a5c-6571-11e7-9a44-d6940f5ef88d",
                           "owner": null,
                           "parentTaskId": null,
                           "priority": 50,
                           "processDefinitionId": "invoice:1:2c8d8057-6571-11e7-9a44-d6940f5ef88d",
                           "processInstanceId": "349f8a5c-6571-11e7-9a44-d6940f5ef88d",
                           "taskDefinitionKey": "approveInvoice",
                           "caseExecutionId": null,
                           "caseInstanceId": null,
                           "caseDefinitionId": null,
                           "suspended": false,
                           "formKey": "embedded:app:develop/invoice-forms/approve-invoice.html",
                           "tenantId": null,
                           "taskState": "aTaskState",
                           "attachment":false,
                           "comment":false
                         }
                       ]
                     }',
        '"example-3": {
                       "summary": "GET /task/anId?withCommentAttachmentInfo=true&withTaskVariablesInReturn=true
                       Response",
                       "value": [
                         {
                           "id": "349fffa8-6571-11e7-9a44-d6940f5ef88d",
                           "name": "Approve Invoice",
                           "assignee": "John Munda",
                           "created": "2017-07-10T15:10:54.670+0200",
                           "due": "2017-07-17T15:10:54.670+0200",
                           "followUp": null,
                           "lastUpdated": "2017-07-17T15:10:54.670+0200",
                           "delegationState": null,
                           "description": "Approve the invoice (or not).",
                           "executionId": "349f8a5c-6571-11e7-9a44-d6940f5ef88d",
                           "owner": null,
                           "parentTaskId": null,
                           "priority": 50,
                           "processDefinitionId": "invoice:1:2c8d8057-6571-11e7-9a44-d6940f5ef88d",
                           "processInstanceId": "349f8a5c-6571-11e7-9a44-d6940f5ef88d",
                           "taskDefinitionKey": "approveInvoice",
                           "caseExecutionId": null,
                           "caseInstanceId": null,
                           "caseDefinitionId": null,
                           "suspended": false,
                           "formKey": "embedded:app:develop/invoice-forms/approve-invoice.html",
                           "tenantId": null,
                           "taskState": "aTaskState",
                           "variables": {
                             "aVariableKey": {
                               "type": "String",
                               "value": "aVariableValue",
                               "valueInfo": {}
                             },
                             "anotherVariableKey": {
                               "type": "String",
                               "value": "anotherVariableValue",
                               "valueInfo": {}
                             }
			                     },
                           "attachment":false,
                           "comment":false
                         }
                       ]
                     }',
        '"example-4": {
                       "summary": "GET /task/anId?withCommentAttachmentInfo=true&withTaskLocalVariablesInReturn=true
                       Response",
                       "value": [
                         {
                           "id": "349fffa8-6571-11e7-9a44-d6940f5ef88d",
                           "name": "Approve Invoice",
                           "assignee": "John Munda",
                           "created": "2017-07-10T15:10:54.670+0200",
                           "due": "2017-07-17T15:10:54.670+0200",
                           "followUp": null,
                           "lastUpdated": "2017-07-17T15:10:54.670+0200",
                           "delegationState": null,
                           "description": "Approve the invoice (or not).",
                           "executionId": "349f8a5c-6571-11e7-9a44-d6940f5ef88d",
                           "owner": null,
                           "parentTaskId": null,
                           "priority": 50,
                           "processDefinitionId": "invoice:1:2c8d8057-6571-11e7-9a44-d6940f5ef88d",
                           "processInstanceId": "349f8a5c-6571-11e7-9a44-d6940f5ef88d",
                           "taskDefinitionKey": "approveInvoice",
                           "caseExecutionId": null,
                           "caseInstanceId": null,
                           "caseDefinitionId": null,
                           "suspended": false,
                           "formKey": "embedded:app:develop/invoice-forms/approve-invoice.html",
                           "tenantId": null,
                           "taskState": "aTaskState",
                           "variables": {
                             "anotherVariableKey": {
                               "type": "String",
                               "value": "anotherVariableValue",
                               "valueInfo": {}
                             }
			                     },
                           "attachment":false,
                           "comment":false
                         }
                       ]
                     }'] />

    <@lib.response
        code = "404"
        dto = "ExceptionDto"
        last = true
        desc = "Task with given id does not exist. See the
                [Introduction](${docsUrl}/reference/rest/overview/#error-handling)
                for the error response format." />

  }
}

</#macro>
