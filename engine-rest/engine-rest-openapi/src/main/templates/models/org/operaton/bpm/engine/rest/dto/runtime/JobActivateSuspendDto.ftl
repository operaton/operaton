<#macro dto_macro docsUrl="">
  <@lib.dto desc = "List of job ids and the requested suspension state.">

    <@lib.property
        name = "jobIds"
        type = "array"
        itemType = "string"
        desc = "List of job ids."
    />

    <@lib.property
        name = "suspended"
        type = "boolean"
        last = true
        desc = "Supply `true` to suspend the jobs and `false` to activate them."
    />

  </@lib.dto>
</#macro>
