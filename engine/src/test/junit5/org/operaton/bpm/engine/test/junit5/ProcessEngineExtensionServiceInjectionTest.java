/*
 * Copyright 2025 the Operaton contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.operaton.bpm.engine.test.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.*;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ProcessEngineExtension.class)
class ProcessEngineExtensionServiceInjectionTest {
    ProcessEngine processEngine;
    // this is to check that injection will set _all_ fields with the target type
    ProcessEngine processEngine2;
    ProcessEngineConfiguration processEngineConfiguration;
    ProcessEngineConfigurationImpl processEngineConfigurationImpl;
    RepositoryService repositoryService;
    RuntimeService runtimeService;
    TaskService taskService;
    HistoryService historyService;
    IdentityService identityService;
    ManagementService managementService;
    FormService formService;
    FilterService filterService;
    AuthorizationService authorizationService;
    CaseService caseService;
    ExternalTaskService externalTaskService;
    DecisionService decisionService;

    @Test
    void extensionInjectsServiceInstances () {
        assertThat(processEngine).isNotNull();
        assertThat(processEngine2).isNotNull();
        assertThat(processEngine).isSameAs(processEngine2);

        assertThat(processEngineConfiguration).isNotNull();
        assertThat(processEngineConfigurationImpl).isNotNull();

        assertThat(repositoryService).isNotNull();
        assertThat(runtimeService).isNotNull();
        assertThat(taskService).isNotNull();
        assertThat(historyService).isNotNull();
        assertThat(identityService).isNotNull();
        assertThat(managementService).isNotNull();
        assertThat(formService).isNotNull();
        assertThat(filterService).isNotNull();
        assertThat(authorizationService).isNotNull();
        assertThat(caseService).isNotNull();
        assertThat(externalTaskService).isNotNull();
        assertThat(decisionService).isNotNull();
    }
}
