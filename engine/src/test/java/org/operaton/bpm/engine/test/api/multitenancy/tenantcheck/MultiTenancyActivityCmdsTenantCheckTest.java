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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
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
class MultiTenancyActivityCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";

  protected static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";

  protected static final BpmnModelInstance ONE_TASK_PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
    .startEvent()
    .userTask()
    .endEvent()
    .done();

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected String processInstanceId;

  private IdentityService identityService;
  private RuntimeService runtimeService;

  @BeforeEach
  void init() {

    testRule.deployForTenant(TENANT_ONE, ONE_TASK_PROCESS);

    processInstanceId = engineRule.getRuntimeService()
      .startProcessInstanceByKey(PROCESS_DEFINITION_KEY)
      .getId();

    runtimeService = engineRule.getRuntimeService();
    identityService = engineRule.getIdentityService();
  }

  @Test
  void getActivityInstanceWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // then
    assertThat(engineRule.getRuntimeService().getActivityInstance(processInstanceId)).isNotNull();
  }

  @Test
  void getActivityInstanceWithNoAuthenticatedTenant() {
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> runtimeService.getActivityInstance(processInstanceId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void getActivityInstanceWithDisabledTenantCheck() {
    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    assertThat(engineRule.getRuntimeService().getActivityInstance(processInstanceId)).isNotNull();
  }

  // get active activity id
  @Test
  void getActivityIdsWithAuthenticatedTenant() {
    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // then
    assertThat(engineRule.getRuntimeService().getActiveActivityIds(processInstanceId)).hasSize(1);

  }

  @Test
  void getActivityIdsWithNoAuthenticatedTenant() {
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> runtimeService.getActiveActivityIds(processInstanceId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void getActivityIdsWithDisabledTenantCheck() {
    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    assertThat(engineRule.getRuntimeService().getActiveActivityIds(processInstanceId)).hasSize(1);

  }

}
