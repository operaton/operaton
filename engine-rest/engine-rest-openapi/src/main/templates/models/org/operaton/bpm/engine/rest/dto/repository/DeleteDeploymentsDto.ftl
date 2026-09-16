<#macro dto_macro docsUrl="">
<@lib.dto>

    <@lib.property
        name = "deploymentIds"
        type = "array"
        desc = "The list of deployment ids to be deleted." />

    <@lib.property
        name = "cascade"
        type = "boolean"
        desc = "A flag indicating if all process instances, historic process instances and jobs for this deployment should be deleted." />

    <@lib.property
        name = "skipCustomListeners"
        type = "boolean"
        desc = "A flag indicating if only the built-in ExecutionListeners should be notified with the end event." />

    <@lib.property
        name = "skipIoMappings"
        type = "boolean"
        last = true
        desc = "A flag indicating if all input/output mappings should not be invoked." />

</@lib.dto>
</#macro>
