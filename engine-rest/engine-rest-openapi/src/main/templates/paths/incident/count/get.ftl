<#macro endpoint_macro docsUrl="">
{

  <@lib.endpointInfo
      id = "getIncidentsCount"
      tag = "Incident"
      summary = "Count Incidents"
      desc = "Queries for the number of incidents that fulfill given parameters. Takes the same parameters as the
      [Get Incidents](${docsUrl}/reference/rest/incident/get-query/) method." />

  "parameters": [
    <#assign last = true >
    <#include "/lib/commons/incident-query-params.ftl">
  ],
  "responses" : {

    <@lib.response
        code = "200"
        dto = "CountResultDto"
        desc = "Request successful."
        examples = ['"example-1": {
                       "summary": "GET `/incident/count?processInstanceId=aProcInstId`",
                       "value":
                           {
                             "count": 2
                           }}'] />

    <@lib.response
        code = "400"
        dto = "ExceptionDto"
        desc = "Returned if some of the query parameters are invalid. See the [Introduction](${docsUrl}/reference/rest/overview/#error-handling) for the error
                response format." />

    <@lib.errorResponses docsUrl=docsUrl last = true />
    }
}

</#macro>
