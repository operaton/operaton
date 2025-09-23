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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class ProcessInstanceTerminationCascadeStateTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine engine;
  RepositoryService repositoryService;
  RuntimeService runtimeService;
  HistoryService historyService;

  boolean externallyTerminated;

  @BeforeEach
  void init() {
    prepareDeployment();
  }

  protected void prepareDeployment() {
    BpmnModelInstance callee = Bpmn.createExecutableProcess("subProcess").startEvent().userTask("userTask").endEvent().done();
    BpmnModelInstance caller = Bpmn.createExecutableProcess("process").startEvent().callActivity("callActivity").calledElement("subProcess").endEvent().done();

    testRule.deploy(caller, callee);
  }

  @AfterEach
  void teardown() {
    List<HistoricProcessInstance> processes = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance historicProcessInstance : processes) {
      historyService.deleteHistoricProcessInstance(historicProcessInstance.getId());
    }

    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    for (Deployment deployment : deployments) {
      repositoryService.deleteDeployment(deployment.getId());
    }
  }

  @Test
  void shouldCascadeStateFromSubprocessUpDeletion() {
    // given
    runtimeService.startProcessInstanceByKey("process");
    ProcessInstance subProcess = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess").singleResult();
    externallyTerminated = true;

    // when
    runtimeService.deleteProcessInstance(subProcess.getId(), "test", false, externallyTerminated);

    // then
    assertHistoricProcessInstances();
  }

  @Test
  void shouldNotCascadeStateFromSubprocessUpDeletion() {
    // given
    runtimeService.startProcessInstanceByKey("process");
    ProcessInstance subProcess = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess").singleResult();
    externallyTerminated = false;

    // when
    runtimeService.deleteProcessInstance(subProcess.getId(), "test", false, externallyTerminated);

    // then
    assertHistoricProcessInstances();
  }

  @Test
  void shouldCascadeStateFromProcessDownDeletion() {
    // given
    runtimeService.startProcessInstanceByKey("process");
    ProcessInstance process = runtimeService.createProcessInstanceQuery().processDefinitionKey("process").singleResult();
    externallyTerminated = true;

    // when
    runtimeService.deleteProcessInstance(process.getId(), "test", false, externallyTerminated);

    // then
    assertHistoricProcessInstances();
  }

  @Test
  void shouldNotCascadeStateFromProcessDownDeletion() {
    // given
    runtimeService.startProcessInstanceByKey("process");
    ProcessInstance process = runtimeService.createProcessInstanceQuery().processDefinitionKey("process").singleResult();
    externallyTerminated = false;

    // when
    runtimeService.deleteProcessInstance(process.getId(), "test", false, externallyTerminated);

    // then
    assertHistoricProcessInstances();
  }

  @Test
  void shouldNotCascadeStateFromSubprocessUpCancelation() {
    // given
    runtimeService.startProcessInstanceByKey("process");
    ProcessInstance subProcess = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess").singleResult();
    ActivityInstance activityInstance = runtimeService.getActivityInstance(subProcess.getId());
    externallyTerminated = false;

    // when
    runtimeService.createProcessInstanceModification(subProcess.getId()).cancellationSourceExternal(externallyTerminated).cancelActivityInstance(activityInstance.getId()).execute();

    // then
    assertHistoricProcessInstances();
  }

  @Test
  void shouldNotCascadeStateFromProcessDownCancelation() {
    // given
    runtimeService.startProcessInstanceByKey("process");
    ProcessInstance process = runtimeService.createProcessInstanceQuery().processDefinitionKey("process").singleResult();
    ActivityInstance activityInstance = runtimeService.getActivityInstance(process.getId());
    externallyTerminated = false;

    // when
    runtimeService.createProcessInstanceModification(process.getId()).cancellationSourceExternal(externallyTerminated).cancelActivityInstance(activityInstance.getId()).execute();

    // then
    assertHistoricProcessInstances();
  }

  @Test
  void shouldCascadeStateFromSubprocessUpCancelation() {
    // given
    runtimeService.startProcessInstanceByKey("process");
    ProcessInstance subProcess = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess").singleResult();
    ActivityInstance activityInstance = runtimeService.getActivityInstance(subProcess.getId());
    externallyTerminated = true;

    // when
    runtimeService.createProcessInstanceModification(subProcess.getId()).cancellationSourceExternal(externallyTerminated).cancelActivityInstance(activityInstance.getId()).execute();

    // then
    assertHistoricProcessInstances();
  }

  @Test
  void shouldCascadeStateFromProcessDownCancelation() {
    // given
    runtimeService.startProcessInstanceByKey("process");
    ProcessInstance process = runtimeService.createProcessInstanceQuery().processDefinitionKey("process").singleResult();
    ActivityInstance activityInstance = runtimeService.getActivityInstance(process.getId());
    externallyTerminated = true;

    // when
    runtimeService.createProcessInstanceModification(process.getId()).cancellationSourceExternal(externallyTerminated).cancelActivityInstance(activityInstance.getId()).execute();

    // then
    assertHistoricProcessInstances();
  }

  protected void assertHistoricProcessInstances() {
    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();
    assertThat(historicProcessInstances).hasSize(2);
    for (HistoricProcessInstance historicProcessInstance : historicProcessInstances) {
      assertThat(historicProcessInstance.getState())
          .isEqualTo(externallyTerminated ? HistoricProcessInstance.STATE_EXTERNALLY_TERMINATED : HistoricProcessInstance.STATE_INTERNALLY_TERMINATED);
    }
  }
}
