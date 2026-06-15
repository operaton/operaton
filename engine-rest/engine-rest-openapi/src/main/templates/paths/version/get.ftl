<#macro endpoint_macro docsUrl="">
{
  <@lib.endpointInfo
      id = "getRestAPIVersion"
      tag = "Version"
      summary = "Get Rest API version"
      desc = "Retrieves the version of the Rest API." />

  "responses" : {
    <@lib.response
        code = "200"
        dto = "VersionDto"
        desc = "Request successful."
        examples = ['"example-1": {
                       "summary": "Status 200 Response",
                       "description": "The Response content of a status 200",
                       "value": {
                           "version": "7.13.0"
                         }
                     }'] />

    <@lib.errorResponses docsUrl=docsUrl last = true />
  }
}
</#macro>