<#--
  ~ Copyright 2026 FINOS
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
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
<@lib.dto desc = "Trigger instructions for one or more ad hoc activities within an ad hoc scope.">

    <@lib.property
        name = "activities"
        type = "array"
        dto = "AdHocActivityTriggerInstructionDto"
        desc = "A collection of ad hoc activities to trigger. Each entry specifies an activity id
                and optional variables to set in that activity."
        last = true
    />


</@lib.dto>
</#macro>
