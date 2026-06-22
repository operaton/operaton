<#macro endpoint_macro docsUrl="">
{

  <@lib.endpointInfo
      id = "setExternalTaskResourceRetries"
      tag = "External Task"
      summary = "Set Retries"
      desc = "Sets the number of retries left to execute an external task by id. If retries are set to 0, an 
              incident is created." />

  "parameters" : [

    <@lib.parameter
        name = "id"
        location = "path"
        type = "string"
        required = true
        last = true
        desc = "The id of the external task to set the number of retries for."/>

  ],

  <@lib.requestBody
      mediaType = "application/json"
      dto = "RetriesDto"
      examples = ['"example-1": {
                     "summary": "PUT /external-task/anId/retries",
                     "value": {
                       "retries": 123
                     }
                   }'] />

  "responses" : {

    <@lib.response
        code = "204"
        desc = "Request successful." />

    <@lib.response
        code = "400"
        dto = "ExceptionDto"
        desc = "Returned if the retries count is negative or the external task is not active. See the
                [Introduction](${docsUrl}/reference/rest/overview/#error-handling)
                for the error response format." />

    <@lib.response
        code = "404"
        dto = "ExceptionDto"
        desc = "Returned if the external task with the given id does not exist. See the
                [Introduction](${docsUrl}/reference/rest/overview/#error-handling)
                for the error response format." />

    <@lib.errorResponses docsUrl=docsUrl last = true />

  }
}

</#macro>