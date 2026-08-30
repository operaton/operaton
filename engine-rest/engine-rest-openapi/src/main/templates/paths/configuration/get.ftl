<#macro endpoint_macro docsUrl="">
{
  <@lib.endpointInfo
      id = "getProcessEngineConfiguration"
      tag = "Configuration"
      summary = "Get process engine configuration"
      desc = "Retrieves selected configuration values of the process engine." />

  "responses" : {
    <@lib.response
        code = "200"
        dto = "ProcessEngineConfigurationDto"
        desc = "Request successful."
        examples = ['"example-1": {
                       "summary": "Status 200 Response",
                       "description": "The response content of a status 200",
                       "value": {
                           "engineName": "default",
                           "historyLevel": "full",
                           "authorizationEnabled": false,
                           "enablePasswordPolicy": false
                         }
                     }'] />

    <@lib.errorResponses docsUrl=docsUrl last = true />
  }
}
</#macro>
