<#macro endpoint_macro docsUrl="">
  {
    <@lib.endpointInfo
        id = "updateSuspensionStateByJobIds"
        tag = "Job"
        summary = "Activate/Suspend Jobs by List of Job Ids"
        desc = "Activates or suspends jobs by the supplied list of job ids and a boolean `suspended` flag."
    />

    <@lib.requestBody
        mediaType = "application/json"
        dto = "JobActivateSuspendDto"
        examples = [
        '"example-1": {
                       "summary": "Activates or suspends jobs with the given job ids. POST `/job/suspended`",
                       "value": {
                         "jobIds": [
                           "aJobId",
                           "anotherJobId"
                         ],
                         "suspended": true
                       }
                     }'
        ]
    />

    "responses": {

      <@lib.response
          code = "200"
          desc = "Request successful."
          dto = "JobSuspensionResponse"
          array = true
          examples = ['"example-1": {
                         "summary": "Status 200 response",
                         "value": [
                           {
                             "status": "SUCCESS",
                             "errorMessage": null,
                             "jobId": "aJobId"
                           },
                           {
                             "status": "SUCCESS",
                             "errorMessage": null,
                             "jobId": "anotherJobId"
                           }
                         ]
                       }']
      />

      <@lib.response
          code = "207"
          desc = "Multi-Status: Indicates that at least one requested update has failed."
          dto = "JobSuspensionResponse"
          array = true
          examples = ['"example-1": {
                         "summary": "Status 207 response",
                         "value": [
                           {
                             "status": "FAILURE",
                             "errorMessage": "Cannot find job with id aJobId",
                             "jobId": "aJobId"
                           },
                           {
                             "status": "SUCCESS",
                             "errorMessage": null,
                             "jobId": "anotherJobId"
                           }
                         ]
                       }']
      />

      <@lib.response
          code = "400"
          dto = "ExceptionDto"
          desc = "Returned if the request parameters are invalid, for example if an empty list is provided as input."
          last = true
      />

    }

  }
</#macro>
