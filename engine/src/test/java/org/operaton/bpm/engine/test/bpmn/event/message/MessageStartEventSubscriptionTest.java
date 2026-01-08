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
package org.operaton.bpm.engine.test.bpmn.event.message;

import java.util.List;

import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

class MessageStartEventSubscriptionTest {

  private static final String SINGLE_MESSAGE_START_EVENT_XML = "org/operaton/bpm/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml";
  private static final String ONE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";
  private static final String MESSAGE_EVENT_PROCESS = "singleMessageStartEvent";

  private static final BpmnModelInstance MODEL_WITHOUT_MESSAGE = Bpmn.createExecutableProcess(MESSAGE_EVENT_PROCESS)
      .startEvent()
      .userTask()
      .endEvent()
      .done();

  private static final BpmnModelInstance MODEL = Bpmn.createExecutableProcess("another")
      .startEvent()
      .message("anotherMessage")
      .userTask()
      .endEvent()
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RepositoryService repositoryService;
  RuntimeService runtimeService;
  TaskService taskService;

  @Test
  void testUpdateProcessVersionCancelsSubscriptions() {
    testRule.deploy(SINGLE_MESSAGE_START_EVENT_XML);
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

    assertThat(eventSubscriptions).hasSize(1);
    assertThat(processDefinitions).hasSize(1);

    // when
    testRule.deploy(SINGLE_MESSAGE_START_EVENT_XML);

    // then
    List<EventSubscription> newEventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
    List<ProcessDefinition> newProcessDefinitions = repositoryService.createProcessDefinitionQuery().list();

    assertThat(newEventSubscriptions).hasSize(1);
    assertThat(newProcessDefinitions).hasSize(2);
    for (ProcessDefinition processDefinition : newProcessDefinitions) {
      if (processDefinition.getVersion() == 1) {
        for (EventSubscription subscription : newEventSubscriptions) {
          EventSubscriptionEntity subscriptionEntity = (EventSubscriptionEntity) subscription;
          assertThat(processDefinition.getId()).isNotEqualTo(subscriptionEntity.getConfiguration());
        }
      } else {
        for (EventSubscription subscription : newEventSubscriptions) {
          EventSubscriptionEntity subscriptionEntity = (EventSubscriptionEntity) subscription;
          assertThat(processDefinition.getId()).isEqualTo(subscriptionEntity.getConfiguration());
        }
      }
    }
    assertThat(newEventSubscriptions).isNotEqualTo(eventSubscriptions);
  }

  @Test
  void testEventSubscriptionAfterDeleteLatestProcessVersion() {
    // given a deployed process
    testRule.deploy(SINGLE_MESSAGE_START_EVENT_XML);
    ProcessDefinition processDefinitionV1 = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinitionV1).isNotNull();

    // deploy second version of the process
    String deploymentId = testRule.deploy(SINGLE_MESSAGE_START_EVENT_XML).getId();

    // when
    repositoryService.deleteDeployment(deploymentId, true);

    // then
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(MESSAGE_EVENT_PROCESS).singleResult();
    assertThat(processDefinition.getId()).isEqualTo(processDefinitionV1.getId());

    EventSubscriptionEntity eventSubscription = (EventSubscriptionEntity) runtimeService.createEventSubscriptionQuery().singleResult();
    assertThat(eventSubscription).isNotNull();
    assertThat(eventSubscription.getConfiguration()).isEqualTo(processDefinitionV1.getId());
  }

  @Test
  void testStartInstanceAfterDeleteLatestProcessVersionByIds() {
    // given a deployed process
    testRule.deploy(SINGLE_MESSAGE_START_EVENT_XML);
    // deploy second version of the process
    DeploymentWithDefinitions deployment = testRule.deploy(SINGLE_MESSAGE_START_EVENT_XML);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);

    // delete it
    repositoryService.deleteProcessDefinitions()
      .byIds(processDefinition.getId())
      .delete();

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage");

    // then
    assertThat(processInstance.isEnded()).isFalse();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId());

    ProcessInstance completedInstance = runtimeService
        .createProcessInstanceQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    if (completedInstance != null) {
      throw new AssertionFailedError("Expected finished process instance '" + completedInstance + "' but it was still in the db");
    }
  }

  @Test
  void testStartInstanceAfterDeleteLatestProcessVersion() {
    // given a deployed process
    testRule.deploy(SINGLE_MESSAGE_START_EVENT_XML);
    // deploy second version of the process
    String deploymentId = testRule.deploy(SINGLE_MESSAGE_START_EVENT_XML).getId();
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();

    // delete it
    repositoryService.deleteDeployment(deployment.getId(), true);

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("singleMessageStartEvent");

    assertThat(processInstance.isEnded()).isFalse();

    Task  task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId());

    ProcessInstance completedInstance = runtimeService
        .createProcessInstanceQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    if (completedInstance != null) {
      throw new AssertionFailedError("Expected finished process instance '" + completedInstance + "' but it was still in the db");
    }
  }

  @Test
  void testVersionWithoutConditionAfterDeleteLatestProcessVersionWithCondition() {
    // given a process
    testRule.deploy(MODEL_WITHOUT_MESSAGE);

    // deploy second version of the process
    String deploymentId = testRule.deploy(SINGLE_MESSAGE_START_EVENT_XML).getId();
    var deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();

    // delete it
    repositoryService.deleteDeployment(deployment.getId(), true);
    var conditionEvaluationBuilder = runtimeService
      .createConditionEvaluation()
      .setVariable("foo", 1);

    // when/then
    assertThatThrownBy(conditionEvaluationBuilder::evaluateStartConditions)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No subscriptions were found during evaluation of the conditional start events.");
  }

  @Test
  void testSubscriptionsWhenDeletingProcessDefinitionsInOneTransactionByKeys() {
    // given three versions of the process
    testRule.deploy(SINGLE_MESSAGE_START_EVENT_XML);
    testRule.deploy(SINGLE_MESSAGE_START_EVENT_XML);
    testRule.deploy(SINGLE_MESSAGE_START_EVENT_XML);

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey(MESSAGE_EVENT_PROCESS)
      .delete();

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();
  }

  @Test
  void testSubscriptionsWhenDeletingGroupsProcessDefinitionsByIds() {
    // given
    String processDefId11 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String processDefId12 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String processDefId13 = testRule.deployAndGetDefinition(MODEL_WITHOUT_MESSAGE).getId();

    String processDefId21 = deployModel(MODEL);
    String processDefId22 = deployModel(MODEL);
    String processDefId23 = deployModel(MODEL);

    String processDefId31 = deployProcess(ONE_TASK_PROCESS);
    @SuppressWarnings("unused")
    String processDefId32 = deployProcess(ONE_TASK_PROCESS);

    // assume
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isOne();

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(processDefId21,processDefId23,processDefId13,
          processDefId12,processDefId31)
      .delete();

    // then
    List<EventSubscription> list = runtimeService.createEventSubscriptionQuery().list();
    assertThat(list).hasSize(2);
    for (EventSubscription eventSubscription : list) {
      EventSubscriptionEntity eventSubscriptionEntity = (EventSubscriptionEntity) eventSubscription;
      if (!eventSubscriptionEntity.getConfiguration().equals(processDefId11) && !eventSubscriptionEntity.getConfiguration().equals(processDefId22)) {
        fail("This process definition '%s' and the respective event subscription should not exist.".formatted(eventSubscriptionEntity.getConfiguration()));
      }
    }
  }

  @Test
  void testSubscriptionsWhenDeletingProcessDefinitionsInOneTransactionByIdOrdered() {
    // given
    String definitionId1 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId2 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId3 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);

    // when
    repositoryService.deleteProcessDefinitions()
        .byIds(definitionId1, definitionId2, definitionId3)
        .delete();

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();
  }

  @Test
  void testSubscriptionsWhenDeletingProcessDefinitionsInOneTransactionByIdReverseOrder() {
    // given
    String definitionId1 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId2 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId3 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);

    // when
    repositoryService.deleteProcessDefinitions()
        .byIds(definitionId3, definitionId2, definitionId1)
        .delete();

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();
  }

  @Test
  void testMixedSubscriptionsWhenDeletingProcessDefinitionsInOneTransactionById1() {
    // given first version without condition
    String definitionId1 = deployModel(MODEL_WITHOUT_MESSAGE);
    String definitionId2 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId3 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);

    // when
    repositoryService.deleteProcessDefinitions()
        .byIds(definitionId1, definitionId2, definitionId3)
        .delete();

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();
  }

  @Test
  void testMixedSubscriptionsWhenDeletingProcessDefinitionsInOneTransactionById2() {
    // given second version without condition
    String definitionId1 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId2 = deployModel(MODEL_WITHOUT_MESSAGE);
    String definitionId3 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);

    // when
    repositoryService.deleteProcessDefinitions()
        .byIds(definitionId1, definitionId2, definitionId3)
        .delete();

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();
  }

  @Test
  void testMixedSubscriptionsWhenDeletingProcessDefinitionsInOneTransactionById3() {
    // given third version without condition
    String definitionId1 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId2 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId3 = deployModel(MODEL_WITHOUT_MESSAGE);

    // when
    repositoryService.deleteProcessDefinitions()
        .byIds(definitionId1, definitionId2, definitionId3)
        .delete();

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();
  }

  @Test
  void testMixedSubscriptionsWhenDeletingTwoProcessDefinitionsInOneTransaction1() {
    // given first version without condition
    String definitionId1 = deployModel(MODEL_WITHOUT_MESSAGE);
    String definitionId2 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId3 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);

    // when
    repositoryService.deleteProcessDefinitions()
        .byIds(definitionId2, definitionId3)
        .delete();

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().singleResult().getId()).isEqualTo(definitionId1);
  }

  @Test
  void testMixedSubscriptionsWhenDeletingTwoProcessDefinitionsInOneTransaction2() {
    // given second version without condition
    String definitionId1 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId2 = deployModel(MODEL_WITHOUT_MESSAGE);
    String definitionId3 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);

    // when
    repositoryService.deleteProcessDefinitions()
        .byIds(definitionId2, definitionId3)
        .delete();

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isOne();
    assertThat(((EventSubscriptionEntity) runtimeService.createEventSubscriptionQuery().singleResult()).getConfiguration()).isEqualTo(definitionId1);
  }

  @Test
  void testMixedSubscriptionsWhenDeletingTwoProcessDefinitionsInOneTransaction3() {
    // given third version without condition
    String definitionId1 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId2 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId3 = deployModel(MODEL_WITHOUT_MESSAGE);

    // when
    repositoryService.deleteProcessDefinitions()
        .byIds(definitionId2, definitionId3)
        .delete();

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isOne();
    assertThat(((EventSubscriptionEntity) runtimeService.createEventSubscriptionQuery().singleResult()).getConfiguration()).isEqualTo(definitionId1);
  }

  /**
   * Tests the case, when no new subscription is needed, as it is not the latest version, that is being deleted.
   */
  @Test
  void testDeleteNotLatestVersion() {
    @SuppressWarnings("unused")
    String definitionId1 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId2 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId3 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(definitionId2)
      .delete();

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isOne();
    assertThat(((EventSubscriptionEntity) runtimeService.createEventSubscriptionQuery().singleResult()).getConfiguration()).isEqualTo(definitionId3);
  }

  /**
   * Tests the case when the previous of the previous version will be needed.
   */
  @Test
  void testSubscribePreviousPreviousVersion() {

    String definitionId1 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId2 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML);
    String definitionId3 = deployProcess(SINGLE_MESSAGE_START_EVENT_XML); //we're deleting version 3, but as version 2 is already deleted, we must subscribe version 1

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(definitionId2, definitionId3)
      .delete();

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isOne();
    assertThat(((EventSubscriptionEntity) runtimeService.createEventSubscriptionQuery().singleResult()).getConfiguration()).isEqualTo(definitionId1);
  }

  protected String deployProcess(String resourcePath) {
    List<ProcessDefinition> deployedProcessDefinitions = testRule.deploy(resourcePath).getDeployedProcessDefinitions();
    assertThat(deployedProcessDefinitions).hasSize(1);
    return deployedProcessDefinitions.get(0).getId();
  }

  protected String deployModel(BpmnModelInstance model) {
    List<ProcessDefinition> deployedProcessDefinitions = testRule.deploy(model).getDeployedProcessDefinitions();
    assertThat(deployedProcessDefinitions).hasSize(1);
    return deployedProcessDefinitions.get(0).getId();
  }
}
