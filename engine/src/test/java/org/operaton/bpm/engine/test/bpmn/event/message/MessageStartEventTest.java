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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Daniel Meyer
 */
class MessageStartEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RepositoryService repositoryService;
  RuntimeService runtimeService;
  TaskService taskService;

  @Test
  void testDeploymentCreatesSubscriptions() {
    String deploymentId = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
        .deploy()
        .getId();

    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

    assertThat(eventSubscriptions).hasSize(1);

    repositoryService.deleteDeployment(deploymentId);
  }

  @Test
  void testSameMessageNameFails() {
    // given
    repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
        .deploy()
        .getId();
    var deploymentBuilder = repositoryService
          .createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/otherProcessWithNewInvoiceMessage.bpmn20.xml");

    try {
      // when/then
      assertThatThrownBy(deploymentBuilder::deploy)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("there already is a message event subscription for the message with name");
    } finally {
      // clean db:
      List<org.operaton.bpm.engine.repository.Deployment> deployments = repositoryService.createDeploymentQuery().list();
      for (org.operaton.bpm.engine.repository.Deployment deployment : deployments) {
        repositoryService.deleteDeployment(deployment.getId(), true);
      }
      // Workaround for #CAM-4250: remove process definition of failed
      // deployment from deployment cache
      processEngineConfiguration.getDeploymentCache().getProcessDefinitionCache().clear();
    }
  }

  // SEE: https://app.camunda.com/jira/browse/CAM-1448
  @Test
  void testEmptyMessageNameFails() {
    // given
    var deploymentBuilder = repositoryService
          .createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/MessageStartEventTest.testEmptyMessageNameFails.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("Cannot have a message event subscription with an empty or missing name")
      .satisfies(e -> {
        ParseException pe = (ParseException) e;
        assertThat(pe.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("theStart");
      });
  }

  @Test
  void testSameMessageNameInSameProcessFails() {
    // given
    var deploymentBuilder = repositoryService
          .createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/testSameMessageNameInSameProcessFails.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot have more than one message event subscription with name 'newInvoiceMessage' for scope");
  }

  @Test
  void testUpdateProcessVersionCancelsSubscriptions() {
    String deploymentId = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
        .deploy()
        .getId();

    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

    assertThat(eventSubscriptions).hasSize(1);
    assertThat(processDefinitions).hasSize(1);

    String newDeploymentId = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
        .deploy()
        .getId();

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

    repositoryService.deleteDeployment(deploymentId);
    repositoryService.deleteDeployment(newDeploymentId);
  }

  @Deployment
  @Test
  void testSingleMessageStartEvent() {

    // using startProcessInstanceByMessage triggers the message start event

    ProcessInstance processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage");

    assertThat(processInstance.isEnded()).isFalse();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(processInstance.getProcessDefinitionKey()).isEqualTo("singleMessageStartEvent");

    // using startProcessInstanceByKey also triggers the message event, if there is a single start event

    processInstance = runtimeService.startProcessInstanceByKey("singleMessageStartEvent");

    assertThat(processInstance.isEnded()).isFalse();

    task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(processInstance.getProcessDefinitionKey()).isEqualTo("singleMessageStartEvent");
  }


  @Deployment
  @Test
  void testMessageStartEventAndNoneStartEvent() {

    // using startProcessInstanceByKey triggers the none start event

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    assertThat(processInstance.isEnded()).isFalse();

    Task task = taskService.createTaskQuery().taskDefinitionKey("taskAfterNoneStart").singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());

    // using startProcessInstanceByMessage triggers the message start event

    processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage");

    assertThat(processInstance.isEnded()).isFalse();

    task = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessageStart").singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  void testMultipleMessageStartEvents() {

    // sending newInvoiceMessage

    ProcessInstance processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage");

    assertThat(processInstance.isEnded()).isFalse();

    Task task = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessageStart").singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());

    // sending newInvoiceMessage2

    processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage2");

    assertThat(processInstance.isEnded()).isFalse();

    task = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessageStart2").singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());

    // when/then
    // starting the process using startProcessInstanceByKey is not possible:
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("has no default start activity");

  }

  @Deployment
  @Test
  void testDeployStartAndIntermediateEventWithSameMessageInSameProcess() {
    ProcessInstance pi = null;
    try {
      runtimeService.startProcessInstanceByMessage("message");
      pi = runtimeService.createProcessInstanceQuery().singleResult();
      assertThat(pi.isEnded()).isFalse();

      String deploymentId = repositoryService
          .createDeployment()
          .addClasspathResource(
              "org/operaton/bpm/engine/test/bpmn/event/message/MessageStartEventTest.testDeployStartAndIntermediateEventWithSameMessageInSameProcess.bpmn")
          .name("deployment2").deploy().getId();
      assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult()).isNotNull();
    } finally {
      // clean db:
      runtimeService.deleteProcessInstance(pi.getId(), "failure");
      List<org.operaton.bpm.engine.repository.Deployment> deployments = repositoryService.createDeploymentQuery().list();
      for (org.operaton.bpm.engine.repository.Deployment d : deployments) {
        repositoryService.deleteDeployment(d.getId(), true);
      }
      // Workaround for #CAM-4250: remove process definition of failed
      // deployment from deployment cache

      processEngineConfiguration.getDeploymentCache().getProcessDefinitionCache().clear();
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/message/MessageStartEventTest.testDeployStartAndIntermediateEventWithSameMessageDifferentProcesses.bpmn"})
  @Test
  void testDeployStartAndIntermediateEventWithSameMessageDifferentProcessesFirstStartEvent() {
    ProcessInstance pi = null;
    try {
      runtimeService.startProcessInstanceByMessage("message");
      pi = runtimeService.createProcessInstanceQuery().singleResult();
      assertThat(pi.isEnded()).isFalse();

      String deploymentId = repositoryService
          .createDeployment()
          .addClasspathResource(
              "org/operaton/bpm/engine/test/bpmn/event/message/MessageStartEventTest.testDeployStartAndIntermediateEventWithSameMessageDifferentProcesses2.bpmn")
          .name("deployment2").deploy().getId();
      assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult()).isNotNull();
    } finally {
      // clean db:
      runtimeService.deleteProcessInstance(pi.getId(), "failure");
      List<org.operaton.bpm.engine.repository.Deployment> deployments = repositoryService.createDeploymentQuery().list();
      for (org.operaton.bpm.engine.repository.Deployment d : deployments) {
        repositoryService.deleteDeployment(d.getId(), true);
      }
      // Workaround for #CAM-4250: remove process definition of failed
      // deployment from deployment cache

      processEngineConfiguration.getDeploymentCache().getProcessDefinitionCache().clear();
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/message/MessageStartEventTest.testDeployStartAndIntermediateEventWithSameMessageDifferentProcesses2.bpmn"})
  @Test
  void testDeployStartAndIntermediateEventWithSameMessageDifferentProcessesFirstIntermediateEvent() {
    ProcessInstance pi = null;
    try {
      runtimeService.startProcessInstanceByKey("Process_2");
      pi = runtimeService.createProcessInstanceQuery().singleResult();
      assertThat(pi.isEnded()).isFalse();

      String deploymentId = repositoryService
          .createDeployment()
          .addClasspathResource(
              "org/operaton/bpm/engine/test/bpmn/event/message/MessageStartEventTest.testDeployStartAndIntermediateEventWithSameMessageDifferentProcesses.bpmn")
          .name("deployment2").deploy().getId();
      assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult()).isNotNull();
    } finally {
      // clean db:
      runtimeService.deleteProcessInstance(pi.getId(), "failure");
      List<org.operaton.bpm.engine.repository.Deployment> deployments = repositoryService.createDeploymentQuery().list();
      for (org.operaton.bpm.engine.repository.Deployment d : deployments) {
        repositoryService.deleteDeployment(d.getId(), true);
      }
      // Workaround for #CAM-4250: remove process definition of failed
      // deployment from deployment cache

      processEngineConfiguration.getDeploymentCache().getProcessDefinitionCache().clear();
    }
  }

  @Test
  void testUsingExpressionWithDollarTagInMessageStartEventNameThrowsException() {

    // given
    // a process definition with a start message event that has a message name which contains an expression
    String processDefinition =
        "org/operaton/bpm/engine/test/bpmn/event/message/" +
            "MessageStartEventTest.testUsingExpressionWithDollarTagInMessageStartEventNameThrowsException.bpmn20.xml";
    var deploymentBuilder = repositoryService
          .createDeployment()
          .addClasspathResource(processDefinition);

    // when/then
    // deploying the process should throw a process engine exception with a certain message
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid message name")
      .hasMessageContaining("expressions in the message start event name are not allowed!");
  }

  @Test
  void testUsingExpressionWithHashTagInMessageStartEventNameThrowsException() {

    // given
    // a process definition with a start message event that has a message name which contains an expression
    String processDefinition =
        "org/operaton/bpm/engine/test/bpmn/event/message/" +
            "MessageStartEventTest.testUsingExpressionWithHashTagInMessageStartEventNameThrowsException.bpmn20.xml";
    var deploymentBuilder = repositoryService
          .createDeployment()
          .addClasspathResource(processDefinition);

    // when/then
    // deploying the process should throw a process engine exception with a certain message
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid message name")
      .hasMessageContaining("expressions in the message start event name are not allowed!");
  }

  //test fix CAM-10819
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/message/MessageStartEventTest.testMessageStartEventUsingCorrelationEngine.bpmn"})
  @Test
  void testMessageStartEventUsingCorrelationEngineAndLocalVariable() {

    // when
    // sending newCorrelationStartMessage using correlation engine
    ProcessInstance  processInstance = runtimeService.createMessageCorrelation("newCorrelationStartMessage")
            .setVariableLocal("var", "value")
            .correlateWithResult().getProcessInstance();

    // then
    // ensure the variable is available
    String processInstanceValue = (String) runtimeService.getVariableLocal(processInstance.getId(), "var");
    assertThat(processInstanceValue).isEqualTo("value");
  }

}
