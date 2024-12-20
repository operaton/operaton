<#macro dto_macro docsUrl="">
<@lib.dto>

    <@lib.property
        name = "key"
        type = "string"
        desc = "The key of the Operaton Form." />

    <@lib.property
        name = "binding"
        type = "string"
        desc = "The binding of the Operaton Form. Can be `latest`, `deployment` or `version`." />

    <@lib.property
        name = "version"
        type = "integer"
        format = "int32"
        last = true
        desc = "The specific version of a Operaton Form. This property is only set if `binding` is `version`." />

</@lib.dto>
</#macro>