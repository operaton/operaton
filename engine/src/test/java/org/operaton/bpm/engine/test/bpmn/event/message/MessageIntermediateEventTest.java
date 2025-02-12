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
package org.operaton.bpm.engine.test.bpmn.event.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;

import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.digest._apacheCommonsCodec.Base64;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.variables.FailingJavaSerializable;
import org.operaton.bpm.engine.test.util.ProcessEngineBootstrapRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.Variables.SerializationDataFormats;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Daniel Meyer
 * @author Nico Rehwaldt
 */
public class MessageIntermediateEventTest {

  @ClassRule
  public static ProcessEngineBootstrapRule bootstrapRule = new ProcessEngineBootstrapRule(configuration ->
      configuration.setJavaSerializationFormatEnabled(true));
  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule(bootstrapRule);
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  private RuntimeService runtimeService;
  private TaskService taskService;
  private RepositoryService repositoryService;

  @Before
  public void init() {
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
    repositoryService = engineRule.getRepositoryService();
  }

  @Deployment
  @Test
  public void testSingleIntermediateMessageEvent() {

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    List<String> activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
    assertNotNull(activeActivityIds);
    assertThat(activeActivityIds).hasSize(1);
    assertTrue(activeActivityIds.contains("messageCatch"));

    String messageName = "newInvoiceMessage";
    Execution execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName(messageName)
        .singleResult();

    assertNotNull(execution);

    runtimeService.messageEventReceived(messageName, execution.getId());

    Task task = taskService.createTaskQuery()
        .singleResult();
    assertNotNull(task);
    taskService.complete(task.getId());

  }

  @Deployment
  @Test
  public void testConcurrentIntermediateMessageEvent() {

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    List<String> activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
    assertNotNull(activeActivityIds);
    assertThat(activeActivityIds).hasSize(2);
    assertTrue(activeActivityIds.contains("messageCatch1"));
    assertTrue(activeActivityIds.contains("messageCatch2"));

    String messageName = "newInvoiceMessage";
    List<Execution> executions = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName(messageName)
        .list();

    assertNotNull(executions);
    assertThat(executions).hasSize(2);

    runtimeService.messageEventReceived(messageName, executions.get(0).getId());

    Task task = taskService.createTaskQuery()
        .singleResult();
    assertNull(task);

    runtimeService.messageEventReceived(messageName, executions.get(1).getId());

    task = taskService.createTaskQuery()
        .singleResult();
    assertNotNull(task);

    taskService.complete(task.getId());
  }

  @Test
  public void testIntermediateMessageEventRedeployment() {

    // deploy version 1
    repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/MessageIntermediateEventTest.testSingleIntermediateMessageEvent.bpmn20.xml")
        .deploy();
    // now there is one process deployed
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1);

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    List<String> activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
    assertNotNull(activeActivityIds);
    assertThat(activeActivityIds).hasSize(1);
    assertTrue(activeActivityIds.contains("messageCatch"));

    // deploy version 2
    repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/MessageIntermediateEventTest.testSingleIntermediateMessageEvent.bpmn20.xml")
        .deploy();

    // now there are two versions deployed:
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);

    // assert process is still waiting in message event:
    activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
    assertNotNull(activeActivityIds);
    assertThat(activeActivityIds).hasSize(1);
    assertTrue(activeActivityIds.contains("messageCatch"));

    // delete both versions:
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  @Test
  public void testEmptyMessageNameFails() {
    var deploymentBuilder = repositoryService
          .createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/MessageIntermediateEventTest.testEmptyMessageNameFails.bpmn20.xml");
    try {
      deploymentBuilder.deploy();
      fail("exception expected");
    } catch (ParseException e) {
      assertTrue(e.getMessage().contains("Cannot have a message event subscription with an empty or missing name"));
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("messageCatch");
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/message/MessageIntermediateEventTest.testSingleIntermediateMessageEvent.bpmn20.xml")
  @Test
  public void testSetSerializedVariableValues() throws IOException, ClassNotFoundException {

    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    EventSubscription messageEventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();

    // when
    FailingJavaSerializable javaSerializable = new FailingJavaSerializable("foo");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.encodeBase64(baos.toByteArray()), engineRule.getProcessEngine());

    // then it is not possible to deserialize the object
    try {
      new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
    } catch (RuntimeException e) {
      testRule.assertTextPresent("Exception while deserializing object.", e.getMessage());
    }

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
    assertNotNull(variableTyped);
    assertFalse(variableTyped.isDeserialized());
    assertThat(variableTyped.getValueSerialized()).isEqualTo(serializedObject);
    assertThat(variableTyped.getObjectTypeName()).isEqualTo(FailingJavaSerializable.class.getName());
    assertThat(variableTyped.getSerializationDataFormat()).isEqualTo(SerializationDataFormats.JAVA.getName());
  }

  @Deployment
  @Test
  public void testExpressionInSingleIntermediateMessageEvent() {

    // given
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("foo", "bar");

    // when
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process", variables);
    List<String> activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
    assertNotNull(activeActivityIds);
    assertThat(activeActivityIds).hasSize(1);
    assertTrue(activeActivityIds.contains("messageCatch"));

    // then
    String messageName = "newInvoiceMessage-bar";
    Execution execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName(messageName)
        .singleResult();
    assertNotNull(execution);

    runtimeService.messageEventReceived(messageName, execution.getId());
    Task task = taskService.createTaskQuery()
        .singleResult();
    assertNotNull(task);
    taskService.complete(task.getId());
  }

}
