<#macro endpoint_macro docsUrl="">
{
  <@lib.endpointInfo
      id = "getDecisionDefinitionsCount"
      tag = "Decision Definition"
      summary = "Count Decision Definitions"
      desc = "Requests the number of decision definitions that fulfill the query criteria.
              Takes the same filtering parameters as the
              [Get Decision Definition](${docsUrl}/reference/rest/decision-definition/get-query/) method." />

  "parameters" : [

    <#assign requestMethod="GET"/>
    <#include "/lib/commons/decision-definition.ftl" >

    <@lib.parameters
        object = params
        last = true />

  ],
  "responses" : {
    <@lib.response
        code = "200"
        dto = "CountResultDto"
        desc = "Request successful."
        examples = ['"example-1": {
                       "summary": "Status 200 response",
                       "description": "Response for GET `/decision-definition/count?key=dish-decision&version=2`",
                       "value": {
                            "count": 1
                         }
                     }'] />

    <@lib.response
        code = "400"
        dto = "ExceptionDto"
        desc = "Returned if some of the query parameters are invalid.
                See the [Introduction](${docsUrl}/reference/rest/overview/#error-handling) for the error response format."/>

    <@lib.errorResponses docsUrl=docsUrl last = true />
  }
}
</#macro>