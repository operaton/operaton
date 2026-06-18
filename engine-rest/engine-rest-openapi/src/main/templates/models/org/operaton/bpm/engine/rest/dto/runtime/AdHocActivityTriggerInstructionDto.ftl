<#--
  ~ Copyright 2026 the Operaton contributors.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at:
  ~
  ~     https://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
-->
<#macro dto_macro docsUrl="">
<@lib.dto desc = "A single ad hoc activity trigger instruction specifying which activity to trigger and optional variables.">

    <@lib.property
        name = "activityId"
        type = "string"
        desc = "The id of the ad hoc activity to trigger within the ad hoc scope."
    />

    <@lib.property
        name = "variables"
        type = "object"
        additionalProperties = true
        dto = "VariableValueDto"
        desc = "An optional JSON object containing variable key-value pairs to set in the triggered activity.
                Each key is a variable name and each value a JSON variable value object."
        last = true
    />


</@lib.dto>
</#macro>
