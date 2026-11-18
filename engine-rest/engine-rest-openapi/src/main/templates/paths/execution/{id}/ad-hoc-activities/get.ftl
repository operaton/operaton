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
      id = "getStartableAdHocActivities"
      tag = "Execution"
      summary = "Get Startable Ad-Hoc Activities"
      desc = "Returns the activities that can currently be triggered in an active ad-hoc subprocess execution. The result observes the BPMN ordering of the ad-hoc subprocess."
  />

  "parameters" : [

      <@lib.parameter
          name = "id"
          location = "path"
          type = "string"
          required = true
          desc = "The id of the execution containing the ad-hoc scope."
          last = true
      />

  ],

  "responses": {

    <@lib.response
        code = "200"
        dto = "AdHocActivityDto"
        array = true
        desc = "Request successful."
        examples = ['"example-1": {
                       "summary": "GET `/execution/{id}/ad-hoc-activities`",
                       "value": [
                         {
                           "activityId": "taskA",
                           "activityName": "Task A",
                           "activityType": "userTask"
                         }
                       ]
                     }']
    />

    <@lib.response
        code = "400"
        dto = "ExceptionDto"
        desc = "The execution id is invalid or the execution is not an ad-hoc subprocess."
    />

    <@lib.response
        code = "500"
        dto = "ExceptionDto"
        desc = "An internal server error occurred while retrieving the startable ad-hoc activities."
        last = true
    />

  }

}
</#macro>
