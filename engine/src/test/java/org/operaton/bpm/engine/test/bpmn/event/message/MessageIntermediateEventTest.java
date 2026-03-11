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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.variables.FailingJavaSerializable;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.Variables.SerializationDataFormats;
import org.operaton.bpm.engine.variable.value.ObjectValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Daniel Meyer
 * @author Nico Rehwaldt
 */
class MessageIntermediateEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(config -> config.setJavaSerializationFormatEnabled(true))
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  RepositoryService repositoryService;

  @Deployment
  @Test
  void testSingleIntermediateMessageEvent() {

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    List<String> activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
    assertThat(activeActivityIds)
            .isNotNull()
            .hasSize(1)
            .contains("messageCatch");

    String messageName = "newInvoiceMessage";
    Execution execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName(messageName)
        .singleResult();

    assertThat(execution).isNotNull();

    runtimeService.messageEventReceived(messageName, execution.getId());

    Task task = taskService.createTaskQuery()
        .singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

  }

  @Deployment
  @Test
  void testConcurrentIntermediateMessageEvent() {

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    List<String> activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
    assertThat(activeActivityIds)
            .isNotNull()
            .hasSize(2)
            .contains("messageCatch1")
            .contains("messageCatch2");

    String messageName = "newInvoiceMessage";
    List<Execution> executions = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName(messageName)
        .list();

    assertThat(executions)
            .isNotNull()
            .hasSize(2);

    runtimeService.messageEventReceived(messageName, executions.get(0).getId());

    Task task = taskService.createTaskQuery()
        .singleResult();
    assertThat(task).isNull();

    runtimeService.messageEventReceived(messageName, executions.get(1).getId());

    task = taskService.createTaskQuery()
        .singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId());
  }

  @Test
  void testIntermediateMessageEventRedeployment() {

    // deploy version 1
    repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/MessageIntermediateEventTest.testSingleIntermediateMessageEvent.bpmn20.xml")
        .deploy();
    // now there is one process deployed
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isOne();

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    List<String> activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
    assertThat(activeActivityIds)
            .isNotNull()
            .hasSize(1)
            .contains("messageCatch");

    // deploy version 2
    repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/MessageIntermediateEventTest.testSingleIntermediateMessageEvent.bpmn20.xml")
        .deploy();

    // now there are two versions deployed:
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);

    // assert process is still waiting in message event:
    activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
    assertThat(activeActivityIds)
            .isNotNull()
            .hasSize(1)
            .contains("messageCatch");

    // delete both versions:
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  @Test
  void testEmptyMessageNameFails() {
    // given
    var deploymentBuilder = repositoryService
          .createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/MessageIntermediateEventTest.testEmptyMessageNameFails.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("Cannot have a message event subscription with an empty or missing name")
      .satisfies(e -> {
        ParseException pe = (ParseException) e;
        assertThat(pe.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("messageCatch");
      });
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/message/MessageIntermediateEventTest.testSingleIntermediateMessageEvent.bpmn20.xml")
  @Test
  void testSetSerializedVariableValues() throws Exception {

    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    EventSubscription messageEventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();

    // when
    FailingJavaSerializable javaSerializable = new FailingJavaSerializable("foo");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.getEncoder().encode(baos.toByteArray()), engineRule.getProcessEngine());

    // then it is not possible to deserialize the object
    var objectInputStream = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    assertThatThrownBy(objectInputStream::readObject)
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("Exception while deserializing object.");

    // but it can be set as a variable when delivering a message:
    runtimeService
        .messageEventReceived(
            "newInvoiceMessage",
            messageEventSubscription.getExecutionId(),
            Variables.createVariables().putValueTyped("var",
                Variables
                    .serializedObjectValue(serializedObject)
                    .objectTypeName(FailingJavaSerializable.class.getName())
                    .serializationDataFormat(SerializationDataFormats.JAVA)
                    .create()));

    // then
    ObjectValue variableTyped = runtimeService.getVariableTyped(processInstance.getId(), "var", false);
    assertThat(variableTyped).isNotNull();
    assertThat(variableTyped.isDeserialized()).isFalse();
    assertThat(variableTyped.getValueSerialized()).isEqualTo(serializedObject);
    assertThat(variableTyped.getObjectTypeName()).isEqualTo(FailingJavaSerializable.class.getName());
    assertThat(variableTyped.getSerializationDataFormat()).isEqualTo(SerializationDataFormats.JAVA.getName());
  }

  @Deployment
  @Test
  void testExpressionInSingleIntermediateMessageEvent() {

    // given
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("foo", "bar");

    // when
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process", variables);
    List<String> activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
    assertThat(activeActivityIds)
            .isNotNull()
            .hasSize(1)
            .contains("messageCatch");

    // then
    String messageName = "newInvoiceMessage-bar";
    Execution execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName(messageName)
        .singleResult();
    assertThat(execution).isNotNull();

    runtimeService.messageEventReceived(messageName, execution.getId());
    Task task = taskService.createTaskQuery()
        .singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());
  }

}
