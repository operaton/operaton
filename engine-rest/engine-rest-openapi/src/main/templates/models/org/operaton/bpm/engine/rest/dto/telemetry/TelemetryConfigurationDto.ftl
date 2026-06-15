<#macro dto_macro docsUrl="">
<@lib.dto>

    <@lib.property
        name = "enableTelemetry"
        type = "boolean"
        last = true
        desc = "Deprecated. Telemetry sending has been removed; this value is retained only for backwards compatibility and is always false when read."/>

</@lib.dto>

</#macro>
