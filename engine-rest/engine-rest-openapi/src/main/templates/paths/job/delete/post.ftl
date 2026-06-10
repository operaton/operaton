<#macro endpoint_macro docsUrl="">
  {
    <@lib.endpointInfo
        id = "deleteByJobIds"
        tag = "Job"
        summary = "Delete Jobs by List of Job Ids"
        desc = "Deletes jobs by the supplied list of job ids."
    />

    <@lib.requestBody
        mediaType = "application/json"
        dto = "JobDeletionDto"
        examples = [
        '"example-1": {
                       "summary": "Delete jobs with the given job ids. POST `/job/delete`",
                       "value": {
                         "jobIds": [
                           "aJobId",
                           "anotherJobId"
                         ]
                       }
                     }'
        ]
    />

    "responses": {

      <@lib.response
          code = "200"
          desc = "Request successful."
          dto = "JobDeletionResponse"
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
          desc = "Multi-Status: Indicates that at least one requested deletion has failed."
          dto = "JobDeletionResponse"
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
