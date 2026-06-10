<#macro dto_macro docsUrl="">
  <@lib.dto desc = "Result of a bulk job deletion for a single job.">

    <@lib.property
        name = "jobId"
        type = "string"
        desc = "The id of the job."
    />

    <@lib.property
        name = "status"
        type = "string"
        desc = "The status of the deletion operation."
    />

    <@lib.property
        name = "errorMessage"
        type = "string"
        last = true
        desc = "The error details related to a deletion operation failure."
    />

  </@lib.dto>
</#macro>
