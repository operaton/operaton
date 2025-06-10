/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.history.useroperationlog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
@ExtendWith(ProcessEngineExtension.class)
class UserOperationLogProcessInstanceTest {

  IdentityService identityService;
  RuntimeService runtimeService;
  RepositoryService repositoryService;
  FormService formService;
  HistoryService historyService;

  @BeforeEach
  void setup() {
    identityService.setAuthenticatedUserId("testUser");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void shouldProduceUserOperationLogStartProcessInstanceByKey() {
    // when
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // then
    UserOperationLogEntry userOperationLog = historyService.createUserOperationLogQuery().singleResult();
    assertThat(userOperationLog.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);
    assertThat(userOperationLog.getProcessInstanceId()).isEqualTo(instance.getId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void shouldProduceUserOperationLogStartProcessInstanceById() {
    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // when
    ProcessInstance instance = runtimeService.startProcessInstanceById(processDefinition.getId());

    // then
    UserOperationLogEntry userOperationLog = historyService.createUserOperationLogQuery().singleResult();
    assertThat(userOperationLog.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);
    assertThat(userOperationLog.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
    assertThat(userOperationLog.getProcessInstanceId()).isEqualTo(instance.getId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void shouldProduceUserOperationLogStartProcessInstanceAtActivity() {
    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // when
    ProcessInstance instance = runtimeService.createProcessInstanceById(processDefinition.getId()).startBeforeActivity("theTask").execute();

    // then
    UserOperationLogEntry userOperationLog = historyService.createUserOperationLogQuery().singleResult();
    assertThat(userOperationLog.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);
    assertThat(userOperationLog.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
    assertThat(userOperationLog.getProcessInstanceId()).isEqualTo(instance.getId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcessWithStartForm.bpmn20.xml"})
  void shouldProduceUserOperationLogStartProcessInstanceBySubmitStartForm() {
    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    Map<String, Object> properties = new HashMap<>();
    properties.put("itemName", "apple");
    properties.put("amount", 5);

    // when
    ProcessInstance instance = formService.submitStartForm(processDefinition.getId(), properties);

    // then
    UserOperationLogEntry userOperationLog = historyService.createUserOperationLogQuery().singleResult();
    assertThat(userOperationLog.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);
    assertThat(userOperationLog.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
    assertThat(userOperationLog.getProcessInstanceId()).isEqualTo(instance.getId());
  }

}
