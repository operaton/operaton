/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */
class MultiTenancyProcessInstanceCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";

  protected static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected String processInstanceId;

  protected static final BpmnModelInstance ONE_TASK_PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
   .startEvent()
   .userTask("task")
   .endEvent()
   .done();

  @BeforeEach
  void init() {
    // deploy tenants
    testRule.deployForTenant(TENANT_ONE, ONE_TASK_PROCESS);

    processInstanceId = engineRule.getRuntimeService()
      .startProcessInstanceByKey(PROCESS_DEFINITION_KEY)
      .getId();

  }

  @Test
  void deleteProcessInstanceWithAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));

    engineRule.getRuntimeService().deleteProcessInstance(processInstanceId, null);

    assertThat(engineRule.getRuntimeService()
        .createProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .list()).isEmpty();
  }

  @Test
  void deleteProcessInstanceWithNoAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    RuntimeService runtimeService = engineRule.getRuntimeService();

    // when/then
    assertThatThrownBy(() -> runtimeService.deleteProcessInstance(processInstanceId, null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot delete the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void deleteProcessInstanceWithDisabledTenantCheck() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    //then
    engineRule.getRuntimeService().deleteProcessInstance(processInstanceId, null);

    assertThat(engineRule.getRuntimeService()
        .createProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .list()).isEmpty();
  }

  // modify instances
  @Test
  void modifyProcessInstanceWithAuthenticatedTenant() {

    assertThat(engineRule.getRuntimeService().getActivityInstance(processInstanceId)).isNotNull();

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // when
    engineRule.getRuntimeService()
    .createProcessInstanceModification(processInstanceId)
    .cancelAllForActivity("task")
    .execute();

    assertThat(engineRule.getRuntimeService().getActivityInstance(processInstanceId)).isNull();
  }

  @Test
  void modifyProcessInstanceWithNoAuthenticatedTenant() {

    engineRule.getIdentityService().setAuthentication("aUserId", null);
    var processInstanceModificationBuilder = engineRule.getRuntimeService()
      .createProcessInstanceModification(processInstanceId)
      .cancelAllForActivity("task");

    // when/then
    assertThatThrownBy(processInstanceModificationBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void modifyProcessInstanceWithDisabledTenantCheck() {

    assertThat(engineRule.getRuntimeService().getActivityInstance(processInstanceId)).isNotNull();

    engineRule.getIdentityService().setAuthentication("aUserId", null, List.of(TENANT_ONE));
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // when
    engineRule.getRuntimeService()
    .createProcessInstanceModification(processInstanceId)
    .cancelAllForActivity("task")
    .execute();

    assertThat(engineRule.getRuntimeService().getActivityInstance(processInstanceId)).isNull();
  }
}
