<#--
  ~ Copyright 2026 FINOS
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
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
      id = "completeAdHocSubProcess"
      tag = "Execution"
      summary = "Complete Ad-Hoc Subprocess"
      desc = "Completes an active ad-hoc subprocess execution by id. The request fails if any inner activity is currently active. Optional variables can be set before completion."
  />

  "parameters" : [

      <@lib.parameter
          name = "id"
          location = "path"
          type = "string"
          required = true
          desc = "The id of the ad-hoc subprocess execution to complete."
          last = true
      />

  ],

  <@lib.requestBody
      mediaType = "application/json"
      dto = "AdHocSubProcessCompletionDto"
            examples = ['"empty-payload": {
                     "summary": "POST `/execution/{id}/ad-hoc-activities/complete`",
                     "value": {}
                                     }',
                                    '"with-variables": {
                                         "summary": "POST `/execution/{id}/ad-hoc-activities/complete` with variables",
                                         "value": {
                                             "variables": {
                                                 "completionReason": {
                                                     "value": "manual",
                                                     "type": "String"
                                                 }
                                             }
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
        desc = "The execution id is invalid, the execution is not an ad-hoc subprocess, or active child activities exist."
    />

    <@lib.response
        code = "500"
        dto = "ExceptionDto"
        desc = "An internal server error occurred while completing the ad-hoc subprocess."
        last = true
    />

  }

}
</#macro>
