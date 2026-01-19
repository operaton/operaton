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
package org.operaton.bpm.model.bpmn.instance;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.impl.instance.Incoming;
import org.operaton.bpm.model.bpmn.impl.instance.Outgoing;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastian Menski
 */
class FlowNodeTest extends BpmnModelElementInstanceTest {

  @Override
  public TypeAssumption getTypeAssumption() {
    return new TypeAssumption(FlowElement.class, true);
  }

  @Override
  public Collection<ChildElementAssumption> getChildElementAssumptions() {
    return List.of(
      new ChildElementAssumption(Incoming.class),
      new ChildElementAssumption(Outgoing.class)
    );
  }

  @Override
  public Collection<AttributeAssumption> getAttributesAssumptions() {
    return List.of(
      new AttributeAssumption(OPERATON_NS, "asyncAfter", false, false, false),
      new AttributeAssumption(OPERATON_NS, "asyncBefore", false, false, false),
      new AttributeAssumption(OPERATON_NS, "exclusive", false, false, true),
      new AttributeAssumption(OPERATON_NS, "jobPriority")
    );
  }

  @Test
  void testUpdateIncomingOutgoingChildElements() {
    BpmnModelInstance modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("test")
      .endEvent()
      .done();

    // save current incoming and outgoing sequence flows
    UserTask userTask = modelInstance.getModelElementById("test");
    Collection<SequenceFlow> incoming = userTask.getIncoming();
    Collection<SequenceFlow> outgoing = userTask.getOutgoing();

    // create a new service task
    ServiceTask serviceTask = modelInstance.newInstance(ServiceTask.class);
    serviceTask.setId("new");

    // replace the user task with the new service task
    userTask.replaceWithElement(serviceTask);

    // assert that the new service task has the same incoming and outgoing sequence flows
    assertThat(serviceTask.getIncoming()).containsExactlyElementsOf(incoming);
    assertThat(serviceTask.getOutgoing()).containsExactlyElementsOf(outgoing);
  }

  @Test
  void testOperatonAsyncBefore() {
    Task task = modelInstance.newInstance(Task.class);
    assertThat(task.isOperatonAsyncBefore()).isFalse();

    task.setOperatonAsyncBefore(true);
    assertThat(task.isOperatonAsyncBefore()).isTrue();
  }

  @Test
  void testOperatonAsyncAfter() {
    Task task = modelInstance.newInstance(Task.class);
    assertThat(task.isOperatonAsyncAfter()).isFalse();

    task.setOperatonAsyncAfter(true);
    assertThat(task.isOperatonAsyncAfter()).isTrue();
  }

  @Test
  void testOperatonAsyncAfterAndBefore() {
    Task task = modelInstance.newInstance(Task.class);

    assertThat(task.isOperatonAsyncAfter()).isFalse();
    assertThat(task.isOperatonAsyncBefore()).isFalse();

    task.setOperatonAsyncBefore(true);

    assertThat(task.isOperatonAsyncAfter()).isFalse();
    assertThat(task.isOperatonAsyncBefore()).isTrue();

    task.setOperatonAsyncAfter(true);

    assertThat(task.isOperatonAsyncAfter()).isTrue();
    assertThat(task.isOperatonAsyncBefore()).isTrue();

    task.setOperatonAsyncBefore(false);

    assertThat(task.isOperatonAsyncAfter()).isTrue();
    assertThat(task.isOperatonAsyncBefore()).isFalse();
  }

  @Test
  void testOperatonExclusive() {
    Task task = modelInstance.newInstance(Task.class);

    assertThat(task.isOperatonExclusive()).isTrue();

    task.setOperatonExclusive(false);

    assertThat(task.isOperatonExclusive()).isFalse();
  }

  @Test
  void testOperatonJobPriority() {
    Task task = modelInstance.newInstance(Task.class);
    assertThat(task.getOperatonJobPriority()).isNull();

    task.setOperatonJobPriority("15");

    assertThat(task.getOperatonJobPriority()).isEqualTo("15");
  }
}
