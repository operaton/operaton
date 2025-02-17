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
package org.operaton.bpm.engine.test.bpmn.executionlistener;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.BaseElement;
import org.operaton.bpm.model.bpmn.instance.CatchEvent;
import org.operaton.bpm.model.bpmn.instance.EndEvent;
import org.operaton.bpm.model.bpmn.instance.Event;
import org.operaton.bpm.model.bpmn.instance.ExtensionElements;
import org.operaton.bpm.model.bpmn.instance.FlowElement;
import org.operaton.bpm.model.bpmn.instance.Gateway;
import org.operaton.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.operaton.bpm.model.bpmn.instance.Message;
import org.operaton.bpm.model.bpmn.instance.MessageEventDefinition;
import org.operaton.bpm.model.bpmn.instance.ParallelGateway;
import org.operaton.bpm.model.bpmn.instance.SequenceFlow;
import org.operaton.bpm.model.bpmn.instance.StartEvent;
import org.operaton.bpm.model.bpmn.instance.Task;
import org.operaton.bpm.model.bpmn.instance.UserTask;
import org.operaton.bpm.model.xml.Model;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.junit.After;
import org.junit.Test;

/**
 * @author Sebastian Menski
 */
public class ExecutionListenerBpmnModelExecutionContextTest extends PluggableProcessEngineTest {

  private static final String PROCESS_ID = "process";
  private static final String START_ID = "start";
  private static final String SEQUENCE_FLOW_ID = "sequenceFlow";
  private static final String CATCH_EVENT_ID = "catchEvent";
  private static final String GATEWAY_ID = "gateway";
  private static final String USER_TASK_ID = "userTask";
  private static final String END_ID = "end";
  private static final String MESSAGE_ID = "messageId";
  private static final String MESSAGE_NAME = "messageName";

  private String deploymentId;

  @Test
  public void testProcessStartEvent() {
    deployAndStartTestProcess(PROCESS_ID, ExecutionListener.EVENTNAME_START);
    assertFlowElementIs(StartEvent.class);
    sendMessage();
    completeTask();
  }

  @Test
  public void testStartEventEndEvent() {
    deployAndStartTestProcess(START_ID, ExecutionListener.EVENTNAME_END);
    assertFlowElementIs(StartEvent.class);
    sendMessage();
    completeTask();
  }

  @Test
  public void testSequenceFlowTakeEvent() {
    deployAndStartTestProcess(SEQUENCE_FLOW_ID, ExecutionListener.EVENTNAME_TAKE);
    assertFlowElementIs(SequenceFlow.class);
    sendMessage();
    completeTask();
  }

  @Test
  public void testIntermediateCatchEventStartEvent() {
    deployAndStartTestProcess(CATCH_EVENT_ID, ExecutionListener.EVENTNAME_START);
    assertFlowElementIs(IntermediateCatchEvent.class);
    sendMessage();
    completeTask();
  }

  @Test
  public void testIntermediateCatchEventEndEvent() {
    deployAndStartTestProcess(CATCH_EVENT_ID, ExecutionListener.EVENTNAME_END);
    assertNotNotified();
    sendMessage();
    assertFlowElementIs(IntermediateCatchEvent.class);
    completeTask();
  }

  @Test
  public void testGatewayStartEvent() {
    deployAndStartTestProcess(GATEWAY_ID, ExecutionListener.EVENTNAME_START);
    assertNotNotified();
    sendMessage();
    assertFlowElementIs(Gateway.class);
    completeTask();
  }

  @Test
  public void testGatewayEndEvent() {
    deployAndStartTestProcess(GATEWAY_ID, ExecutionListener.EVENTNAME_END);
    assertNotNotified();
    sendMessage();
    assertFlowElementIs(ParallelGateway.class);
    completeTask();
  }

  @Test
  public void testUserTaskStartEvent() {
    deployAndStartTestProcess(USER_TASK_ID, ExecutionListener.EVENTNAME_START);
    assertNotNotified();
    sendMessage();
    assertFlowElementIs(UserTask.class);
    completeTask();
  }

  @Test
  public void testUserTaskEndEvent() {
    deployAndStartTestProcess(USER_TASK_ID, ExecutionListener.EVENTNAME_END);
    assertNotNotified();
    sendMessage();
    completeTask();
    assertFlowElementIs(UserTask.class);
  }

  @Test
  public void testEndEventStartEvent() {
    deployAndStartTestProcess(END_ID, ExecutionListener.EVENTNAME_START);
    assertNotNotified();
    sendMessage();
    completeTask();
    assertFlowElementIs(EndEvent.class);
  }

  @Test
  public void testProcessEndEvent() {
    deployAndStartTestProcess(PROCESS_ID, ExecutionListener.EVENTNAME_END);
    assertNotNotified();
    sendMessage();
    completeTask();
    assertFlowElementIs(EndEvent.class);
  }

  private void assertNotNotified() {
    assertThat(ModelExecutionContextExecutionListener.modelInstance).isNull();
    assertThat(ModelExecutionContextExecutionListener.flowElement).isNull();
  }

  private void assertFlowElementIs(Class<? extends FlowElement> elementClass) {
    BpmnModelInstance modelInstance = ModelExecutionContextExecutionListener.modelInstance;
    assertThat(modelInstance).isNotNull();

    Model model = modelInstance.getModel();
    Collection<ModelElementInstance> events = modelInstance.getModelElementsByType(model.getType(Event.class));
    assertThat(events).hasSize(3);
    Collection<ModelElementInstance> gateways = modelInstance.getModelElementsByType(model.getType(Gateway.class));
    assertThat(gateways).hasSize(1);
    Collection<ModelElementInstance> tasks = modelInstance.getModelElementsByType(model.getType(Task.class));
    assertThat(tasks).hasSize(1);

    FlowElement flowElement = ModelExecutionContextExecutionListener.flowElement;
    assertThat(flowElement).isNotNull();
    assertThat(elementClass.isAssignableFrom(flowElement.getClass())).isTrue();
  }

  private void sendMessage() {
    runtimeService.correlateMessage(MESSAGE_NAME);
  }

  private void completeTask() {
    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);
  }

  private void deployAndStartTestProcess(String elementId, String eventName) {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(PROCESS_ID)
      .startEvent(START_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
      .intermediateCatchEvent(CATCH_EVENT_ID)
      .parallelGateway(GATEWAY_ID)
      .userTask(USER_TASK_ID)
      .endEvent(END_ID)
      .done();

    addMessageEventDefinition((CatchEvent) modelInstance.getModelElementById(CATCH_EVENT_ID));
    addExecutionListener((BaseElement) modelInstance.getModelElementById(elementId), eventName);
    deployAndStartProcess(modelInstance);
  }

  private void addMessageEventDefinition(CatchEvent catchEvent) {
    BpmnModelInstance modelInstance = (BpmnModelInstance) catchEvent.getModelInstance();
    Message message = modelInstance.newInstance(Message.class);
    message.setId(MESSAGE_ID);
    message.setName(MESSAGE_NAME);
    modelInstance.getDefinitions().addChildElement(message);
    MessageEventDefinition messageEventDefinition = modelInstance.newInstance(MessageEventDefinition.class);
    messageEventDefinition.setMessage(message);
    catchEvent.getEventDefinitions().add(messageEventDefinition);
  }

  private void addExecutionListener(BaseElement element, String eventName) {
    ExtensionElements extensionElements = element.getModelInstance().newInstance(ExtensionElements.class);
    ModelElementInstance executionListener = extensionElements.addExtensionElement(OPERATON_NS, "executionListener");
    executionListener.setAttributeValueNs(OPERATON_NS, "class", ModelExecutionContextExecutionListener.class.getName());
    executionListener.setAttributeValueNs(OPERATON_NS, "event", eventName);
    element.setExtensionElements(extensionElements);
  }

  private void deployAndStartProcess(BpmnModelInstance modelInstance) {
    deploymentId = repositoryService.createDeployment().addModelInstance("process.bpmn", modelInstance).deploy().getId();
    runtimeService.startProcessInstanceByKey(PROCESS_ID);
  }

  @After
  public void tearDown() {
    ModelExecutionContextExecutionListener.clear();
    repositoryService.deleteDeployment(deploymentId, true);
  }

}
