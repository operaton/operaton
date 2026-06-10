<#macro dto_macro docsUrl="">
<@lib.dto>

    <@lib.property
        name = "key"
        type = "string"
        deprecated = true
        desc = "The key of the Camunda Form. Deprecated compatibility alias for `operatonFormRef.key`." />

    <@lib.property
        name = "binding"
        type = "string"
        deprecated = true
        desc = "The binding of the Camunda Form. Deprecated compatibility alias for `operatonFormRef.binding`." />

    <@lib.property
        name = "version"
        type = "integer"
        format = "int32"
        deprecated = true
        last = true
        desc = "The specific version of a Camunda Form. Deprecated compatibility alias for `operatonFormRef.version`." />

</@lib.dto>
</#macro>
