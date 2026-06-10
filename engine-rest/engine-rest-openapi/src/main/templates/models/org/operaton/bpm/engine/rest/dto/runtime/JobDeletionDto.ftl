<#macro dto_macro docsUrl="">
  <@lib.dto desc = "List of job ids to delete.">

    <@lib.property
        name = "jobIds"
        type = "array"
        itemType = "string"
        last = true
        desc = "List of job ids for deletion."
    />

  </@lib.dto>
</#macro>
