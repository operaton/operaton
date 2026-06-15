<#macro endpoint_macro docsUrl="">
{

  <@lib.endpointInfo
      id = "configureTelemetry"
      tag = "Telemetry"
      deprecated = true
      summary = "Configure Telemetry"
      desc = "Deprecated: The sending telemetry feature is removed. This endpoint is retained for backwards compatibility and ignores the requested value." />

  "parameters" : [],

  <@lib.requestBody
      mediaType = "application/json"
      dto = "TelemetryConfigurationDto"
      examples = ['"examle-1": {
                     "summary": "POST /telemetry/configuration",
                     "description": "The content of the Request Body",
                     "value": {
                         "enableTelemetry": false
                       }
                     }'] />

  "responses" : {

    <@lib.response
        code = "204"
        desc = "Request successful." />

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
