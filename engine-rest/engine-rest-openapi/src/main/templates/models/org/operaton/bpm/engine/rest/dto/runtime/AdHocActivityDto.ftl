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
<@lib.dto desc = "An activity that can currently be triggered in an active ad-hoc subprocess.">

    <@lib.property
        name = "activityId"
        type = "string"
        desc = "The BPMN activity id."/>

    <@lib.property
        name = "activityName"
        type = "string"
        desc = "The BPMN activity name."/>

    <@lib.property
        name = "activityType"
        type = "string"
        desc = "The BPMN activity type, for example `userTask`."
        last = true/>

</@lib.dto>
</#macro>
