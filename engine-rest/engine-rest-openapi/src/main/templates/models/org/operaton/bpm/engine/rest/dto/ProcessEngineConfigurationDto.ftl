<#macro dto_macro docsUrl="">
<@lib.dto>

    <@lib.property
        name = "engineName"
        type = "string"
        desc = "The name of the process engine." />

    <@lib.property
        name = "historyLevel"
        type = "string"
        desc = "The history level of the process engine, for example `full`, `audit`, or `none`." />

    <@lib.property
        name = "authorizationEnabled"
        type = "boolean"
        desc = "Whether authorization is enabled on the process engine." />

    <@lib.property
        name = "enablePasswordPolicy"
        type = "boolean"
        last = true
        desc = "Whether the password policy is enabled on the process engine." />

</@lib.dto>
</#macro>
