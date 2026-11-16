<#-- Generated From File: operaton-docs-manual/public/reference/rest/history/decision-instance/post-delete/index.html -->
<#macro dto_macro docsUrl="">
<@lib.dto>
    
    <@lib.property
        name = "historicDecisionInstanceIds"
        type = "array"
        itemType = "string"
        desc = "A list of historic decision instance ids to delete."
    />

    
    <@lib.property
        name = "historicDecisionInstanceQuery"
        type = "ref"
        dto = "HistoricDecisionInstanceQueryDto"
        desc = "A historic decision instance query like the request body described by 
                [POST /history/decision-instance](#tag/Historic-Decision-Instance/operation/getHistoricDecisionInstances)."
    />

    
    <@lib.property
        name = "deleteReason"
        type = "string"
        desc = "A string with delete reason."
        last = true
    />


</@lib.dto>
</#macro>