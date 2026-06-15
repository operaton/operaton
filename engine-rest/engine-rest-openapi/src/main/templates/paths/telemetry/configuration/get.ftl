<#macro endpoint_macro docsUrl="">
{

  <@lib.endpointInfo
      id = "getTelemetryConfiguration"
      tag = "Telemetry"
      deprecated = true
      summary = "Fetch Telemetry Configuration"
      desc = "Deprecated: The sending telemetry feature is removed. This endpoint is retained for backwards compatibility and always reports telemetry as disabled." />

  "parameters" : [],

  "responses" : {

    <@lib.response
        code = "200"
        dto = "TelemetryConfigurationDto"
        desc = "Request successful."
        examples = ['"example-1": {
                       "summary": "Status 200 Response",
                       "description": "The Response content of a status 200",
                       "value": {
                           "enableTelemetry": false
                         }
                     }'] />

    <@lib.response
        code = "401"
        dto = "ExceptionDto"
        desc = "If the user who perform the operation is not a <b>operaton-admin</b> user." />

    <@lib.response
        code = "500"
        dto = "ExceptionDto"
        last = true
        desc = "An internal server error occurred.
                See the [Introduction](${docsUrl}/reference/rest/overview/#error-handling)
                for the error response format." />


  }
}
</#macro>
