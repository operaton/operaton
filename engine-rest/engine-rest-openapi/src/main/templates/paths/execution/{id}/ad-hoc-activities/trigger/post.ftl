<#--
  ~ Copyright 2026 the Operaton contributors.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at:
  ~
  ~     https://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
-->
<#macro endpoint_macro docsUrl="">
{
  <@lib.endpointInfo
      id = "triggerAdHocActivities"
      tag = "Execution"
      summary = "Trigger Ad Hoc Activities"
      desc = "Manually triggers one or more ad hoc activities within an ad hoc scope.
              Allows specifying activities to activate and optional variables to set in each activity."
  />

  "parameters" : [

      <@lib.parameter
          name = "id"
          location = "path"
          type = "string"
          required = true
          desc = "The id of the execution containing the ad hoc scope."
          last = true
      />

  ],

  <@lib.requestBody
      mediaType = "application/json"
      dto = "AdHocActivitiesTriggerDto"
      examples = ['"example-1": {
                     "summary": "POST `/execution/{id}/ad-hoc-activities/trigger`",
                     "value": {
                       "activities": [
                         {
                           "activityId": "taskA",
                           "variables": {
                             "priority": {
                               "value": "high",
                               "type": "String"
                             }
                           }
                         },
                         {
                           "activityId": "taskB"
                         }
                       ]
                     }
                   }']
  />

  "responses": {

    <@lib.response
        code = "204"
        desc = "Request successful. This method returns no content."
    />

    <@lib.response
        code = "400"
        dto = "ExceptionDto"
        desc = "The request was invalid. For example, if the execution is not waiting in an ad hoc scope,
                or if the specified activity id is not found in the ad hoc scope.
                See the [Introduction](${docsUrl}/reference/rest/overview/#error-handling) for the error response format."
    />

    <@lib.response
        code = "500"
        dto = "ExceptionDto"
        desc = "An internal server error occurred while processing the request.
                See the [Introduction](${docsUrl}/reference/rest/overview/#error-handling) for the error response format."
        last = true
    />

  }

}
</#macro>
