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
package org.operaton.bpm.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class IncidentUserOperationLogTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  HistoryService historyService;
  IdentityService identityService;

  @Test
  void shouldLogIncidentCreation() {
    // given
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    assertThat(historyService.createUserOperationLogQuery().count()).isZero();

    // when
    Incident incident = doAuthenticated(() -> runtimeService.createIncident("foo", processInstance.getId(), "aa", "bar"));

    // then
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(2L);

    UserOperationLogEntry entry = historyService.createUserOperationLogQuery().property("incidentType").singleResult();
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE_INCIDENT);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    assertThat(entry.getOrgValue()).isNull();
    assertThat(entry.getNewValue()).isEqualTo("foo");
    assertThat(entry.getExecutionId()).isNull();
    assertThat(entry.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(entry.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(entry.getProcessDefinitionKey()).isEqualTo("Process");

    entry = historyService.createUserOperationLogQuery().property("configuration").singleResult();
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE_INCIDENT);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    assertThat(entry.getOrgValue()).isNull();
    assertThat(entry.getNewValue()).isEqualTo(incident.getConfiguration());
    assertThat(entry.getExecutionId()).isNull();
    assertThat(entry.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(entry.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(entry.getProcessDefinitionKey()).isEqualTo("Process");
  }

  @Test
  void shouldNotLogIncidentCreationFailure() {
    // given
    assertThat(historyService.createUserOperationLogQuery().count()).isZero();

    // when/then
    assertThatThrownBy(() -> runtimeService.createIncident("foo", null, "userTask1", "bar"))
      .isInstanceOf(BadUserRequestException.class);

    assertThat(historyService.createUserOperationLogQuery().count()).isZero();
  }

  @Test
  void shouldLogIncidentResolution() {
    // given
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    Incident incident = runtimeService.createIncident("foo", processInstance.getId(), "userTask1", "bar");
    assertThat(historyService.createUserOperationLogQuery().count()).isZero();

    // when
    doAuthenticated(() -> runtimeService.resolveIncident(incident.getId()));

    // then
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(1L);
    UserOperationLogEntry entry = historyService.createUserOperationLogQuery().singleResult();
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_RESOLVE);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    assertThat(entry.getProperty()).isEqualTo("incidentId");
    assertThat(entry.getOrgValue()).isNull();
    assertThat(entry.getNewValue()).isEqualTo(incident.getId());
    assertThat(entry.getExecutionId()).isNull();
    assertThat(entry.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(entry.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(entry.getProcessDefinitionKey()).isEqualTo("Process");
  }

  @Test
  void shouldNotLogIncidentResolutionFailure() {
    // given
    assertThat(historyService.createUserOperationLogQuery().count()).isZero();

    // when/then
    assertThatThrownBy(() -> runtimeService.resolveIncident("foo"))
      .isInstanceOf(NotFoundException.class);

    assertThat(historyService.createUserOperationLogQuery().count()).isZero();
  }

  @Test
  void shouldLogSetAnnotationToIncident() {
    // given
    String annotation = "my annotation";
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    Incident incident = runtimeService.createIncident("foo", processInstance.getId(), "userTask1", "bar");

    // assume
    assertThat(historyService.createUserOperationLogQuery().count()).isZero();

    // when
    doAuthenticated(() -> runtimeService.setAnnotationForIncidentById(incident.getId(), annotation));

    // then
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(1L);
    UserOperationLogEntry entry = historyService.createUserOperationLogQuery().singleResult();
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_ANNOTATION);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.INCIDENT);
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    assertThat(entry.getProperty()).isEqualTo("incidentId");
    assertThat(entry.getOrgValue()).isNull();
    assertThat(entry.getNewValue()).isEqualTo(incident.getId());
  }

  @Test
  void shouldLogClearAnnotationToIncident() {
    // given
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");
    Incident incident = runtimeService.createIncident("foo", processInstance.getId(), "userTask1", "bar");

    // assume
    assertThat(historyService.createUserOperationLogQuery().count()).isZero();

    // when
    doAuthenticated(() -> runtimeService.clearAnnotationForIncidentById(incident.getId()));

    // then
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(1L);
    UserOperationLogEntry entry = historyService.createUserOperationLogQuery().singleResult();
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CLEAR_ANNOTATION);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.INCIDENT);
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    assertThat(entry.getProperty()).isEqualTo("incidentId");
    assertThat(entry.getOrgValue()).isNull();
    assertThat(entry.getNewValue()).isEqualTo(incident.getId());
  }

  protected <T extends Object> T doAuthenticated(Supplier<T> action) {
    identityService.setAuthenticatedUserId("userId");
    T result = action.get();
    identityService.clearAuthentication();
    return result;
  }

  protected void doAuthenticated(Runnable action) {
    identityService.setAuthenticatedUserId("userId");
    action.run();
    identityService.clearAuthentication();
  }
}
