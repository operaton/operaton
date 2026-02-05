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
package org.operaton.bpm.engine.test.bpmn.event.conditional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

class ConditionalStartEventTest {

  private static final String SINGLE_CONDITIONAL_START_EVENT_XML = "org/operaton/bpm/engine/test/bpmn/event/conditional/ConditionalStartEventTest.testSingleConditionalStartEvent.bpmn20.xml";
  private static final String SINGLE_CONDITIONAL_XML = "org/operaton/bpm/engine/test/bpmn/event/conditional/ConditionalStartEventTest.testSingleConditionalStartEvent1.bpmn20.xml";
  private static final String TRUE_CONDITION_START_XML = "org/operaton/bpm/engine/test/bpmn/event/conditional/ConditionalStartEventTest.testStartInstanceWithTrueConditionalStartEvent.bpmn20.xml";
  private static final String TWO_EQUAL_CONDITIONAL_START_EVENT_XML = "org/operaton/bpm/engine/test/bpmn/event/conditional/ConditionalStartEventTest.testTwoEqualConditionalStartEvent.bpmn20.xml";
  private static final String MULTIPLE_CONDITION_XML = "org/operaton/bpm/engine/test/bpmn/event/conditional/ConditionalStartEventTest.testMultipleCondition.bpmn20.xml";
  private static final String START_INSTANCE_WITH_VARIABLE_NAME_XML = "org/operaton/bpm/engine/test/bpmn/event/conditional/ConditionalStartEventTest.testStartInstanceWithVariableName.bpmn20.xml";
  private static final String ONE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";

  private static final String MULTIPLE_CONDITIONS = "multipleConditions";
  private static final String TRUE_CONDITION_PROCESS = "trueConditionProcess";
  private static final String CONDITIONAL_EVENT_PROCESS = "conditionalEventProcess";

  private static final BpmnModelInstance MODEL_WITHOUT_CONDITION = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS)
      .startEvent()
      .userTask()
      .endEvent()
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RepositoryService repositoryService;
  RuntimeService runtimeService;

  @Test
  @Deployment(resources = SINGLE_CONDITIONAL_START_EVENT_XML)
  void testDeploymentCreatesSubscriptions() {
    // given a deployed process
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().processDefinitionKey(CONDITIONAL_EVENT_PROCESS).singleResult().getId();

    // when
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

    // then
    assertThat(eventSubscriptions).hasSize(1);
    EventSubscriptionEntity conditionalEventSubscription = (EventSubscriptionEntity) eventSubscriptions.get(0);
    assertThat(conditionalEventSubscription.getEventType()).isEqualTo(EventType.CONDITONAL.name());
    assertThat(conditionalEventSubscription.getConfiguration()).isEqualTo(processDefinitionId);
    assertThat(conditionalEventSubscription.getEventName()).isNull();
    assertThat(conditionalEventSubscription.getExecutionId()).isNull();
    assertThat(conditionalEventSubscription.getProcessInstanceId()).isNull();
  }

  @Test
  @Deployment(resources = SINGLE_CONDITIONAL_START_EVENT_XML)
  void testUpdateProcessVersionCancelsSubscriptions() {
    // given a deployed process
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

    assertThat(eventSubscriptions).hasSize(1);
    assertThat(processDefinitions).hasSize(1);

    // when
    testRule.deploy(SINGLE_CONDITIONAL_START_EVENT_XML);

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
  @Deployment(resources = SINGLE_CONDITIONAL_START_EVENT_XML)
  void testEventSubscriptionAfterDeleteLatestProcessVersion() {
    // given a deployed process
    ProcessDefinition processDefinitionV1 = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinitionV1).isNotNull();

    // deploy second version of the process
    String deploymentId = testRule.deploy(SINGLE_CONDITIONAL_XML).getId();

    // when
    repositoryService.deleteDeployment(deploymentId, true);

    // then
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(CONDITIONAL_EVENT_PROCESS).singleResult();
    assertThat(processDefinition.getId()).isEqualTo(processDefinitionV1.getId());

    EventSubscriptionEntity eventSubscription = (EventSubscriptionEntity) runtimeService.createEventSubscriptionQuery().singleResult();
    assertThat(eventSubscription).isNotNull();
    assertThat(eventSubscription.getConfiguration()).isEqualTo(processDefinitionV1.getId());
  }

  @Test
  @Deployment(resources = SINGLE_CONDITIONAL_START_EVENT_XML)
  void testStartInstanceAfterDeleteLatestProcessVersionByIds() {
    // given a deployed process

    // deploy second version of the process
    DeploymentWithDefinitions deployment = testRule.deploy(SINGLE_CONDITIONAL_XML);
    ProcessDefinition processDefinition = deployment.getDeployedProcessDefinitions().get(0);

    // delete it
    repositoryService.deleteProcessDefinitions()
      .byIds(processDefinition.getId())
      .delete();

    // when
    List<ProcessInstance> conditionInstances = runtimeService
        .createConditionEvaluation()
        .setVariable("foo", 1)
        .evaluateStartConditions();

    // then
    assertThat(conditionInstances).hasSize(1);
    assertThat(conditionInstances.get(0)).isNotNull();
  }

  @Test
  @Deployment(resources = SINGLE_CONDITIONAL_START_EVENT_XML)
  void testStartInstanceAfterDeleteLatestProcessVersion() {
    // given a deployed process

    // deploy second version of the process
    String deploymentId = testRule.deploy(SINGLE_CONDITIONAL_XML).getId();
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();

    // delete it
    repositoryService.deleteDeployment(deployment.getId(), true);

    // when
    List<ProcessInstance> conditionInstances = runtimeService
        .createConditionEvaluation()
        .setVariable("foo", 1)
        .evaluateStartConditions();

    // then
    assertThat(conditionInstances).hasSize(1);
    assertThat(conditionInstances.get(0)).isNotNull();
  }

  @Test
  void testVersionWithoutConditionAfterDeleteLatestProcessVersionWithCondition() {
    // given a process
    testRule.deploy(MODEL_WITHOUT_CONDITION);

    // deploy second version of the process
    String deploymentId = testRule.deploy(SINGLE_CONDITIONAL_XML).getId();
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();

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
    testRule.deploy(SINGLE_CONDITIONAL_XML);
    testRule.deploy(SINGLE_CONDITIONAL_XML);
    testRule.deploy(SINGLE_CONDITIONAL_XML);

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey(CONDITIONAL_EVENT_PROCESS)
      .delete();

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isZero();
  }

  @Test
  void testSubscriptionsWhenDeletingGroupsProcessDefinitionsByIds() {
    // given
    String processDefId1 = deployProcess(SINGLE_CONDITIONAL_XML);
    String processDefId2 = deployProcess(SINGLE_CONDITIONAL_XML);
    String processDefId3 = deployModel(MODEL_WITHOUT_CONDITION); // with the same process definition key

    String processDefId4 = deployProcess(TRUE_CONDITION_START_XML);
    String processDefId5 = deployProcess(TRUE_CONDITION_START_XML);
    String processDefId6 = deployProcess(TRUE_CONDITION_START_XML);

    // two versions of a process without conditional start event
    String processDefId7 = deployProcess(ONE_TASK_PROCESS);
    @SuppressWarnings("unused")
    String processDefId8 = deployProcess(ONE_TASK_PROCESS);

    // assume
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isOne();

    // when
    repositoryService.deleteProcessDefinitions()
        .byIds(processDefId4, processDefId6, processDefId3, processDefId2, processDefId7)
      .delete();

    // then
    List<EventSubscription> list = runtimeService.createEventSubscriptionQuery().list();
    assertThat(list).hasSize(2);
    for (EventSubscription eventSubscription : list) {
      EventSubscriptionEntity eventSubscriptionEntity = (EventSubscriptionEntity) eventSubscription;
      if (!eventSubscriptionEntity.getConfiguration().equals(processDefId1)
       && !eventSubscriptionEntity.getConfiguration().equals(processDefId5)) {
        fail("This process definition '%s' and the respective event subscription should not exist.".formatted(eventSubscriptionEntity.getConfiguration()));
      }
    }
  }

  @Test
  void testSubscriptionsWhenDeletingProcessDefinitionsInOneTransactionByIdOrdered() {
    // given
    String definitionId1 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId2 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId3 = deployProcess(SINGLE_CONDITIONAL_XML);

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
    String definitionId1 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId2 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId3 = deployProcess(SINGLE_CONDITIONAL_XML);

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
    String definitionId1 = deployModel(MODEL_WITHOUT_CONDITION);
    String definitionId2 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId3 = deployProcess(SINGLE_CONDITIONAL_XML);

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
    String definitionId1 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId2 = deployModel(MODEL_WITHOUT_CONDITION);
    String definitionId3 = deployProcess(SINGLE_CONDITIONAL_XML);

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
    String definitionId1 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId2 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId3 = deployModel(MODEL_WITHOUT_CONDITION);

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
    String definitionId1 = deployModel(MODEL_WITHOUT_CONDITION);
    String definitionId2 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId3 = deployProcess(SINGLE_CONDITIONAL_XML);

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
    String definitionId1 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId2 = deployModel(MODEL_WITHOUT_CONDITION);
    String definitionId3 = deployProcess(SINGLE_CONDITIONAL_XML);

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
    String definitionId1 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId2 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId3 = deployModel(MODEL_WITHOUT_CONDITION);

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
    String definitionId1 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId2 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId3 = deployProcess(SINGLE_CONDITIONAL_XML);

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

    String definitionId1 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId2 = deployProcess(SINGLE_CONDITIONAL_XML);
    String definitionId3 = deployProcess(SINGLE_CONDITIONAL_XML); //we're deleting version 3, but as version 2 is already deleted, we must subscribe version 1

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(definitionId2, definitionId3)
      .delete();

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isOne();
    assertThat(((EventSubscriptionEntity) runtimeService.createEventSubscriptionQuery().singleResult()).getConfiguration()).isEqualTo(definitionId1);
  }

  @Test
  void testDeploymentOfTwoEqualConditionalStartEvent() {
    // when/then
    assertThatThrownBy(() -> testRule.deploy(TWO_EQUAL_CONDITIONAL_START_EVENT_XML))
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("Cannot have more than one conditional event subscription with the same condition '${variable == 1}'")
      .satisfies(e -> {
        var parseException = (ParseException) e;
        assertThat(parseException.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("StartEvent_2");
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
        assertThat(eventSubscriptions).isEmpty();
      });
  }

  @Test
  @Deployment
  void testStartInstanceWithTrueConditionalStartEvent() {
    // given a deployed process

    // when
    List<ProcessInstance> conditionInstances = runtimeService
        .createConditionEvaluation()
        .evaluateStartConditions();

    // then
    assertThat(conditionInstances).hasSize(1);

    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey(TRUE_CONDITION_PROCESS).list();
    assertThat(processInstances).hasSize(1);

    assertThat(processInstances.get(0).getProcessDefinitionKey()).isEqualTo(TRUE_CONDITION_PROCESS);
    assertThat(conditionInstances.get(0).getId()).isEqualTo(processInstances.get(0).getId());
  }

  @Test
  @Deployment(resources = SINGLE_CONDITIONAL_START_EVENT_XML)
  void testStartInstanceWithVariableCondition() {
    // given a deployed process

    // when
    List<ProcessInstance> instances = runtimeService
        .createConditionEvaluation()
        .setVariable("foo", 1)
        .evaluateStartConditions();

    // then
    assertThat(instances).hasSize(1);

    VariableInstance vars = runtimeService.createVariableInstanceQuery().singleResult();
    assertThat(instances.get(0).getId()).isEqualTo(vars.getProcessInstanceId());
    assertThat(vars.getValue()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = SINGLE_CONDITIONAL_START_EVENT_XML)
  void testStartInstanceWithTransientVariableCondition() {
    // given a deployed process
    VariableMap variableMap = Variables.createVariables()
        .putValueTyped("foo", Variables.integerValue(1, true));

    // when
    List<ProcessInstance> instances = runtimeService
        .createConditionEvaluation()
        .setVariables(variableMap)
        .evaluateStartConditions();

    // then
    assertThat(instances).hasSize(1);

    VariableInstance vars = runtimeService.createVariableInstanceQuery().singleResult();
    assertThat(vars).isNull();
  }

  @Test
  @Deployment(resources = SINGLE_CONDITIONAL_START_EVENT_XML)
  void testStartInstanceWithoutResult() {
    // given a deployed process

    // when
    List<ProcessInstance> processes = runtimeService
      .createConditionEvaluation()
      .setVariable("foo", 0)
      .evaluateStartConditions();

    assertThat(processes).isEmpty();

    assertThat(runtimeService.createVariableInstanceQuery().singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(CONDITIONAL_EVENT_PROCESS).singleResult()).isNull();
  }

  @Test
  @Deployment(resources = MULTIPLE_CONDITION_XML)
  void testStartInstanceWithMultipleConditions() {
    // given a deployed process with three conditional start events
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

    assertThat(eventSubscriptions).hasSize(3);
    for (EventSubscription eventSubscription : eventSubscriptions) {
      assertThat(eventSubscription.getEventType()).isEqualTo(EventType.CONDITONAL.name());
    }

    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("foo", 1);
    variableMap.put("bar", true);

    // when
    List<ProcessInstance> resultInstances = runtimeService
        .createConditionEvaluation()
        .setVariables(variableMap)
        .evaluateStartConditions();

    // then
    assertThat(resultInstances).hasSize(2);

    List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().processDefinitionKey(MULTIPLE_CONDITIONS).list();
    assertThat(instances).hasSize(2);
  }

  @Test
  @Deployment(resources = {SINGLE_CONDITIONAL_START_EVENT_XML,
      MULTIPLE_CONDITION_XML,
      TRUE_CONDITION_START_XML})
  void testStartInstanceWithMultipleSubscriptions() {
    // given three deployed processes
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

    assertThat(eventSubscriptions).hasSize(5);

    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("foo", 1);
    variableMap.put("bar", true);

    // when
    List<ProcessInstance> instances = runtimeService
        .createConditionEvaluation()
        .setVariables(variableMap)
        .evaluateStartConditions();

    // then
    assertThat(instances).hasSize(4);
  }

  @Test
  @Deployment(resources = {SINGLE_CONDITIONAL_START_EVENT_XML,
      MULTIPLE_CONDITION_XML,
      TRUE_CONDITION_START_XML})
  void testStartInstanceWithMultipleSubscriptionsWithoutProvidingAllVariables() {
    // given three deployed processes
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

    assertThat(eventSubscriptions).hasSize(5);

    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("foo", 1);

    // when, it should not throw PropertyNotFoundException
    List<ProcessInstance> instances = runtimeService
        .createConditionEvaluation()
        .setVariables(variableMap)
        .evaluateStartConditions();

    // then
    assertThat(instances).hasSize(3);
  }

  @Test
  @Deployment(resources = {SINGLE_CONDITIONAL_START_EVENT_XML, MULTIPLE_CONDITION_XML})
  void testStartInstanceWithBusinessKey() {
    // given two deployed processes
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

    assertThat(eventSubscriptions).hasSize(4);

    // when
    List<ProcessInstance> instances = runtimeService
        .createConditionEvaluation()
        .setVariable("foo", 1)
        .processInstanceBusinessKey("humuhumunukunukuapua")
        .evaluateStartConditions();

    // then
    assertThat(instances).hasSize(2);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("humuhumunukunukuapua").count()).isEqualTo(2);
  }

  @Test
  @Deployment(resources = {SINGLE_CONDITIONAL_START_EVENT_XML, TRUE_CONDITION_START_XML})
  void testStartInstanceByProcessDefinitionId() {
    // given two deployed processes
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

    assertThat(eventSubscriptions).hasSize(2);

    String processDefinitionId = repositoryService.createProcessDefinitionQuery().processDefinitionKey(TRUE_CONDITION_PROCESS).singleResult().getId();

    // when
    List<ProcessInstance> instances = runtimeService
        .createConditionEvaluation()
        .setVariable("foo", 1)
        .processDefinitionId(processDefinitionId)
        .evaluateStartConditions();

    // then
    assertThat(instances).hasSize(1);
    assertThat(instances.get(0).getProcessDefinitionId()).isEqualTo(processDefinitionId);
  }

  @Test
  @Deployment(resources = {SINGLE_CONDITIONAL_START_EVENT_XML, MULTIPLE_CONDITION_XML})
  void testStartInstanceByProcessDefinitionFirstVersion() {
    // given two deployed processes
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().processDefinitionKey(CONDITIONAL_EVENT_PROCESS).singleResult().getId();

    // assume
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
    assertThat(eventSubscriptions).hasSize(4);

    // when deploy another version
    testRule.deploy(SINGLE_CONDITIONAL_START_EVENT_XML);

    List<ProcessInstance> instances = runtimeService
        .createConditionEvaluation()
        .setVariable("foo", 1)
        .processDefinitionId(processDefinitionId)
        .evaluateStartConditions();

    // then
    assertThat(instances).hasSize(1);
    assertThat(instances.get(0).getProcessDefinitionId()).isEqualTo(processDefinitionId);
  }

  @Test
  @Deployment(resources = {SINGLE_CONDITIONAL_START_EVENT_XML, TRUE_CONDITION_START_XML})
  void testStartInstanceByNonExistingProcessDefinitionId() {
    // given two deployed processes
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

    assertThat(eventSubscriptions).hasSize(2);

    var conditionEvaluationBuilder = runtimeService
      .createConditionEvaluation()
      .setVariable("foo", 1)
      .processDefinitionId("nonExistingId");

    // when/then
    assertThatThrownBy(conditionEvaluationBuilder::evaluateStartConditions)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no deployed process definition found with id 'nonExistingId': processDefinition is null");
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testStartInstanceByProcessDefinitionIdWithoutCondition() {
    // given deployed process without conditional start event
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult().getId();

    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

    assertThat(eventSubscriptions).isEmpty();

    var conditionEvaluationBuilder = runtimeService
      .createConditionEvaluation()
      .processDefinitionId(processDefinitionId);

    // when/then
    assertThatThrownBy(conditionEvaluationBuilder::evaluateStartConditions)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Process definition with id '%s' does not declare conditional start event".formatted(processDefinitionId));
  }

  @Test
  @Deployment
  void testStartInstanceWithVariableName() {
    // given deployed process
    // ${true} variableName="foo"

    // assume
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
    assertThat(eventSubscriptions).hasSize(1);

    // when
    List<ProcessInstance> instances = runtimeService
        .createConditionEvaluation()
        .setVariable("foo", true)
        .evaluateStartConditions();

    // then
    assertThat(instances).hasSize(1);
  }

  @Test
  @Deployment(resources = START_INSTANCE_WITH_VARIABLE_NAME_XML)
  void testStartInstanceWithVariableNameNotFullfilled() {
    // given deployed process
    // ${true} variableName="foo"

    // assume
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
    assertThat(eventSubscriptions).hasSize(1);

    // when
    List<ProcessInstance> instances = runtimeService
        .createConditionEvaluation()
        .evaluateStartConditions();

    // then
    assertThat(instances).isEmpty();
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
