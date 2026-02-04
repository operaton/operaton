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
package org.operaton.bpm.engine.test.api.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.operaton.bpm.engine.test.api.runtime.util.IncrementCounterListener;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.commons.utils.cache.Cache;

import static org.operaton.bpm.engine.test.api.repository.RedeploymentTest.DEPLOYMENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
class DeleteProcessDefinitionTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  HistoryService historyService;
  RepositoryService repositoryService;
  RuntimeService runtimeService;
  ManagementService managementService;
  ProcessEngineConfigurationImpl processEngineConfiguration;
  Deployment deployment;

  @AfterEach
  void cleanUp() {
    if (deployment != null) {
      repositoryService.deleteDeployment(deployment.getId(), true);
      deployment = null;
    }
  }

  protected static final String IO_MAPPING_PROCESS_KEY = "ioMappingProcess";
  protected static final BpmnModelInstance IO_MAPPING_PROCESS = Bpmn.createExecutableProcess(IO_MAPPING_PROCESS_KEY)
    .startEvent()
    .userTask()
      .operatonOutputParameter("inputParameter", "${notExistentVariable}")
    .endEvent()
    .done();

  @Test
  void testDeleteProcessDefinitionNullId() {

    // when/then
    assertThatThrownBy(() -> repositoryService.deleteProcessDefinition(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("processDefinitionId is null");
  }

  @Test
  void testDeleteNonExistingProcessDefinition() {

    // when/then
    assertThatThrownBy(() -> repositoryService.deleteProcessDefinition("notexist"))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("No process definition found with id 'notexist': processDefinition is null");
  }

  @Test
  void testDeleteProcessDefinition() {
    // given deployment with two process definitions in one xml model file
    deployment = repositoryService.createDeployment()
            .addClasspathResource("org/operaton/bpm/engine/test/repository/twoProcesses.bpmn20.xml")
            .deploy();
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

    //when a process definition is been deleted
    repositoryService.deleteProcessDefinition(processDefinitions.get(0).getId());

    //then only one process definition should remain
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isOne();
  }

  @Test
  void testDeleteProcessDefinitionWithProcessInstance() {
    // given process definition and a process instance
    BpmnModelInstance bpmnModel = Bpmn.createExecutableProcess("process").startEvent().userTask().endEvent().done();
    deployment = repositoryService.createDeployment()
                                  .addModelInstance("process.bpmn", bpmnModel)
                                  .deploy();
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process").singleResult();
    runtimeService.createProcessInstanceByKey("process").executeWithVariablesInReturn();
    var processDefinitionId = processDefinition.getId();

    // when/then - deletion should fail since there exists a process instance
    assertThatThrownBy(() -> repositoryService.deleteProcessDefinition(processDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Deletion of process definition without cascading failed.");

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isOne();
  }

  @Test
  void testDeleteProcessDefinitionCascade() {
    // given process definition and a process instance
    BpmnModelInstance bpmnModel = Bpmn.createExecutableProcess("process").startEvent().userTask().endEvent().done();
    deployment = repositoryService.createDeployment()
                                  .addModelInstance("process.bpmn", bpmnModel)
                                  .deploy();
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process").singleResult();
    runtimeService.createProcessInstanceByKey("process").executeWithVariablesInReturn();

    //when the corresponding process definition is cascading deleted from the deployment
    repositoryService.deleteProcessDefinition(processDefinition.getId(), true);

    //then exist no process instance and no definition
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isZero();
    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_ACTIVITY.getId()) {
      assertThat(engineRule.getHistoryService().createHistoricActivityInstanceQuery().count()).isZero();
    }
  }

  @Test
  void testDeleteProcessDefinitionClearsCache() {
    // given process definition and a process instance
    BpmnModelInstance bpmnModel = Bpmn.createExecutableProcess("process").startEvent().userTask().endEvent().done();
    deployment = repositoryService.createDeployment()
                                  .addModelInstance("process.bpmn", bpmnModel)
                                  .deploy();
    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
                                                  .processDefinitionKey("process")
                                                  .singleResult()
                                                  .getId();

    DeploymentCache deploymentCache = processEngineConfiguration.getDeploymentCache();

    // ensure definitions and models are part of the cache
    assertThat(deploymentCache.getProcessDefinitionCache().get(processDefinitionId)).isNotNull();
    assertThat(deploymentCache.getBpmnModelInstanceCache().get(processDefinitionId)).isNotNull();

    repositoryService.deleteProcessDefinition(processDefinitionId, true);

    // then the definitions and models are removed from the cache
    assertThat(deploymentCache.getProcessDefinitionCache().get(processDefinitionId)).isNull();
    assertThat(deploymentCache.getBpmnModelInstanceCache().get(processDefinitionId)).isNull();
  }

  @Test
  void testDeleteProcessDefinitionAndRefillDeploymentCache() {
    // given a deployment with two process definitions in one xml model file
    deployment = repositoryService.createDeployment()
            .addClasspathResource("org/operaton/bpm/engine/test/repository/twoProcesses.bpmn20.xml")
            .deploy();
    ProcessDefinition processDefinitionOne =
        repositoryService.createProcessDefinitionQuery().processDefinitionKey("one").singleResult();
    ProcessDefinition processDefinitionTwo =
        repositoryService.createProcessDefinitionQuery().processDefinitionKey("two").singleResult();

    String idOne = processDefinitionOne.getId();
    //one is deleted from the deployment
    repositoryService.deleteProcessDefinition(idOne);

    //when clearing the deployment cache
    processEngineConfiguration.getDeploymentCache().discardProcessDefinitionCache();

    //then creating process instance from the existing process definition
    ProcessInstanceWithVariables procInst = runtimeService.createProcessInstanceByKey("two").executeWithVariablesInReturn();
    assertThat(procInst).isNotNull();
    assertThat(procInst.getProcessDefinitionId()).contains("two");

    //should refill the cache
    Cache cache = processEngineConfiguration.getDeploymentCache().getProcessDefinitionCache();
    assertThat(cache.get(processDefinitionTwo.getId())).isNotNull();
    //The deleted process definition should not be recreated after the cache is refilled
    assertThat(cache.get(processDefinitionOne.getId())).isNull();
  }

  @Test
  void testDeleteProcessDefinitionAndRedeploy() {
    // given a deployment with two process definitions in one xml model file
    deployment = repositoryService.createDeployment()
            .addClasspathResource("org/operaton/bpm/engine/test/repository/twoProcesses.bpmn20.xml")
            .deploy();

    ProcessDefinition processDefinitionOne =
        repositoryService.createProcessDefinitionQuery().processDefinitionKey("one").singleResult();

    //one is deleted from the deployment
    repositoryService.deleteProcessDefinition(processDefinitionOne.getId());

    //when the process definition is redeployed
    Deployment deployment2 = repositoryService.createDeployment()
            .name(DEPLOYMENT_NAME)
            .addDeploymentResources(deployment.getId())
            .deploy();

    //then there should exist three process definitions
    //two of the redeployment and the remaining one
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3);

    //clean up
    repositoryService.deleteDeployment(deployment2.getId(), true);
  }

  @Test
  void shouldRestorePreviousStartTimerDefinitions() {
    // given
    BpmnModelInstance processV1 = Bpmn.createExecutableProcess()
        .id("one")
        .startEvent()
        .timerWithCycle("R/PT15M")
        .userTask("aTaskName")
        .endEvent()
        .done();

    BpmnModelInstance processV2 = Bpmn.createExecutableProcess()
        .id("one")
        .startEvent()
        .endEvent()
        .done();

    testHelper.deploy(processV1);
    DeploymentWithDefinitions deploymentWithDefinitions = testHelper.deploy(processV2);

    //when
    repositoryService.deleteProcessDefinition(deploymentWithDefinitions.getDeployedProcessDefinitions().get(0).getId());

    //then
    long timerDefinitions = managementService.createJobQuery().processDefinitionKey("one").count();

    assertThat(timerDefinitions).isOne();
  }

  @Test
  void testDeleteProcessDefinitionsByNotExistingKey() {

    // when/then
    var deleteProcessDefinitionsBuilder = repositoryService.deleteProcessDefinitions()
      .byKey("no existing key")
      .withoutTenantId();
    assertThatThrownBy(deleteProcessDefinitionsBuilder::delete)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("No process definition found");
  }

  @Test
  void testDeleteProcessDefinitionsByKeyIsNull() {

    // when/then
    var deleteProcessDefinitionsBuilder = repositoryService.deleteProcessDefinitions()
      .byKey(null)
      .withoutTenantId();
    assertThatThrownBy(deleteProcessDefinitionsBuilder::delete)
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("cannot be null");

  }

  @Test
  void testDeleteProcessDefinitionsByKey() {
    // given
    for (int i = 0; i < 3; i++) {
      deployTwoProcessDefinitions();
    }

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey("processOne")
      .withoutTenantId()
      .delete();

    // then
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3L);
  }

  @Test
  void testDeleteProcessDefinitionsByKeyWithRunningProcesses() {
    // given
    for (int i = 0; i < 3; i++) {
      deployTwoProcessDefinitions();
    }
    runtimeService.startProcessInstanceByKey("processOne");
    var deleteProcessDefinitionsBuilder = repositoryService.deleteProcessDefinitions()
      .byKey("processOne")
      .withoutTenantId();

    // when/then
    assertThatThrownBy(deleteProcessDefinitionsBuilder::delete)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Deletion of process definition");
  }

  @Test
  void testDeleteProcessDefinitionsByKeyCascading() {
    // given
    for (int i = 0; i < 3; i++) {
      deployTwoProcessDefinitions();
    }

    Map<String, Object> variables = new HashMap<>();

    for (int i = 0; i < 3; i++) {
      variables.put("varName" + i, "varValue");
    }

    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("processOne", variables);
      runtimeService.startProcessInstanceByKey("processTwo", variables);
    }

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey("processOne")
      .withoutTenantId()
      .cascade()
      .delete();

    repositoryService.deleteProcessDefinitions()
      .byKey("processTwo")
      .withoutTenantId()
      .cascade()
      .delete();

    // then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isZero();
  }

  @Test
  void testDeleteProcessDefinitionsByKeyWithCustomListenersSkipped() {
    // given
    IncrementCounterListener.counter = 0;
    for (int i = 0; i < 3; i++) {
      deployTwoProcessDefinitions();
    }

    runtimeService.startProcessInstanceByKey("processOne");

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey("processOne")
      .withoutTenantId()
      .cascade()
      .skipCustomListeners()
      .delete();

    // then
    assertThat(IncrementCounterListener.counter).isZero();
  }

  @Test
  void testDeleteProcessDefinitionsByKeyWithIoMappingsSkipped() {
    // given
    testHelper.deploy(IO_MAPPING_PROCESS);
    runtimeService.startProcessInstanceByKey(IO_MAPPING_PROCESS_KEY);

    testHelper.deploy(IO_MAPPING_PROCESS);
    runtimeService.startProcessInstanceByKey(IO_MAPPING_PROCESS_KEY);

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey(IO_MAPPING_PROCESS_KEY)
      .withoutTenantId()
      .cascade()
      .skipIoMappings()
      .delete();

    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

    // then
    assertThat(processDefinitions).isEmpty();
  }

  @Test
  void testDeleteProcessDefinitionsByNotExistingIds() {
    // given
    var deleteProcessDefinitionsBuilder = repositoryService.deleteProcessDefinitions()
      .byIds("not existing", "also not existing");

    // when/then
    assertThatThrownBy(deleteProcessDefinitionsBuilder::delete)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("No process definition found");
  }

  @Test
  void testDeleteProcessDefinitionsByIdIsNull() {
    // given
    var deleteProcessDefinitionsBuilder = repositoryService.deleteProcessDefinitions()
      .byIds(null);

    // when/then
    assertThatThrownBy(deleteProcessDefinitionsBuilder::delete)
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("cannot be null");
  }

  @Test
  void testDeleteProcessDefinitionsByIds() {
    // given
    for (int i = 0; i < 3; i++) {
      deployTwoProcessDefinitions();
    }

    String[] processDefinitionIds = findProcessDefinitionIdsByKey("processOne");

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(processDefinitionIds)
      .delete();

    // then
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3L);
  }

  @Test
  void testDeleteProcessDefinitionsByIdsWithRunningProcesses() {
    // given
    for (int i = 0; i < 3; i++) {
      deployTwoProcessDefinitions();
    }
    String[] processDefinitionIds = findProcessDefinitionIdsByKey("processOne");
    runtimeService.startProcessInstanceByKey("processOne");
    var deleteProcessDefinitionsBuilder = repositoryService.deleteProcessDefinitions()
      .byIds(processDefinitionIds);

    // when/then
    assertThatThrownBy(deleteProcessDefinitionsBuilder::delete)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Deletion of process definition");

  }

  @Test
  void testDeleteProcessDefinitionsByIdsCascading() {
    // given
    for (int i = 0; i < 3; i++) {
      deployTwoProcessDefinitions();
    }
    String[] processDefinitionIdsOne = findProcessDefinitionIdsByKey("processOne");
    String[] processDefinitionIdsTwo = findProcessDefinitionIdsByKey("processTwo");
    Map<String, Object> variables = new HashMap<>();

    for (int i = 0; i < 3; i++) {
      variables.put("varName" + i, "varValue");
    }

    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("processOne", variables);
      runtimeService.startProcessInstanceByKey("processTwo", variables);
    }

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(processDefinitionIdsOne)
      .cascade()
      .delete();

    repositoryService.deleteProcessDefinitions()
      .byIds(processDefinitionIdsTwo)
      .cascade()
      .delete();

    // then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isZero();
  }

  @Test
  void testDeleteProcessDefinitionsByIdsWithCustomListenersSkipped() {
    // given
    IncrementCounterListener.counter = 0;
    for (int i = 0; i < 3; i++) {
      deployTwoProcessDefinitions();
    }
    String[] processDefinitionIds = findProcessDefinitionIdsByKey("processOne");
    runtimeService.startProcessInstanceByKey("processOne");

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(processDefinitionIds)
      .cascade()
      .skipCustomListeners()
      .delete();

    // then
    assertThat(IncrementCounterListener.counter).isZero();
  }

  @Test
  void testDeleteProcessDefinitionsByIdsWithIoMappingsSkipped() {
    // given
    testHelper.deploy(IO_MAPPING_PROCESS);
    runtimeService.startProcessInstanceByKey(IO_MAPPING_PROCESS_KEY);

    testHelper.deploy(IO_MAPPING_PROCESS);
    runtimeService.startProcessInstanceByKey(IO_MAPPING_PROCESS_KEY);

    String[] processDefinitionIds = findProcessDefinitionIdsByKey(IO_MAPPING_PROCESS_KEY);

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(processDefinitionIds)
      .cascade()
      .skipIoMappings()
      .delete();

    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

    // then
    assertThat(processDefinitions).isEmpty();
  }

  private void deployTwoProcessDefinitions() {
    testHelper.deploy(
      Bpmn.createExecutableProcess("processOne")
        .startEvent()
        .userTask()
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, IncrementCounterListener.class.getName())
        .endEvent()
        .done(),
      Bpmn.createExecutableProcess("processTwo")
        .startEvent()
        .userTask()
        .endEvent()
        .done());
  }

  private String[] findProcessDefinitionIdsByKey(String processDefinitionKey) {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey(processDefinitionKey).list();
    List<String> processDefinitionIds = new ArrayList<>();
    for (ProcessDefinition processDefinition: processDefinitions) {
      processDefinitionIds.add(processDefinition.getId());
    }

    return processDefinitionIds.toArray(new String[0]);
  }
}
