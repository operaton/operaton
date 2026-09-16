<#macro dto_macro docsUrl="">
<@lib.dto>

    <@lib.property
        name = "deploymentId"
        type = "string"
        desc = "The id of the deployment." />

    <@lib.property
        name = "status"
        type = "string"
        desc = "The status of the delete operation. Value is `SUCCESS` or `FAILURE`." />

    <@lib.property
        name = "errorMessage"
        type = "string"
        last = true
        desc = "The error details if the delete operation failed." />

</@lib.dto>
</#macro>
