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
package org.operaton.bpm.model.bpmn;

import org.operaton.bpm.model.bpmn.instance.Process;
import org.operaton.bpm.model.bpmn.instance.*;
import org.operaton.bpm.model.bpmn.util.BpmnModelResource;

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastian Menski
 *
 */
class ReferenceTest extends BpmnModelTest {

  private BpmnModelInstance testBpmnModelInstance;
  private Message message;
  private MessageEventDefinition messageEventDefinition;
  private StartEvent startEvent;

  @BeforeEach
  void createModel() {
    testBpmnModelInstance = Bpmn.createEmptyModel();
    Definitions definitions = testBpmnModelInstance.newInstance(Definitions.class);
    testBpmnModelInstance.setDefinitions(definitions);

    message = testBpmnModelInstance.newInstance(Message.class);
    message.setId("message-id");
    definitions.getRootElements().add(message);

    Process process = testBpmnModelInstance.newInstance(Process.class);
    process.setId("process-id");
    definitions.getRootElements().add(process);

    startEvent = testBpmnModelInstance.newInstance(StartEvent.class);
    startEvent.setId("start-event-id");
    process.getFlowElements().add(startEvent);

    messageEventDefinition = testBpmnModelInstance.newInstance(MessageEventDefinition.class);
    messageEventDefinition.setId("msg-def-id");
    messageEventDefinition.setMessage(message);
    startEvent.getEventDefinitions().add(messageEventDefinition);

    startEvent.getEventDefinitionRefs().add(messageEventDefinition);
  }

  @Test
  void testShouldUpdateReferenceOnIdChange() {
    assertThat(messageEventDefinition.getMessage()).isEqualTo(message);
    message.setId("changed-message-id");
    assertThat(message.getId()).isEqualTo("changed-message-id");
    assertThat(messageEventDefinition.getMessage()).isEqualTo(message);

    message.setAttributeValue("id", "another-message-id", true);
    assertThat(message.getId()).isEqualTo("another-message-id");
    assertThat(messageEventDefinition.getMessage()).isEqualTo(message);
  }

  @Test
  void testShouldRemoveReferenceIfReferencingElementIsRemoved() {
    assertThat(messageEventDefinition.getMessage()).isEqualTo(message);

    Definitions definitions = testBpmnModelInstance.getDefinitions();
    definitions.getRootElements().remove(message);

    assertThat(messageEventDefinition.getId()).isEqualTo("msg-def-id");
    assertThat(messageEventDefinition.getMessage()).isNull();
  }

  @Test
  void testShouldRemoveReferenceIfReferencingAttributeIsRemoved() {
    assertThat(messageEventDefinition.getMessage()).isEqualTo(message);

    message.removeAttribute("id");

    assertThat(messageEventDefinition.getId()).isEqualTo("msg-def-id");
    assertThat(messageEventDefinition.getMessage()).isNull();
  }

  @Test
  void testShouldUpdateReferenceIfReferencingElementIsReplaced() {
    assertThat(messageEventDefinition.getMessage()).isEqualTo(message);
    Message newMessage = testBpmnModelInstance.newInstance(Message.class);
    newMessage.setId("new-message-id");

    message.replaceWithElement(newMessage);

    assertThat(messageEventDefinition.getMessage()).isEqualTo(newMessage);
  }

  @Test
  void testShouldAddMessageEventDefinitionRef() {
    Collection<EventDefinition> eventDefinitionRefs = startEvent.getEventDefinitionRefs();
    assertThat(eventDefinitionRefs)
      .isNotEmpty()
      .contains(messageEventDefinition);
  }

  @Test
  void testShouldUpdateMessageEventDefinitionRefOnIdChange() {
    Collection<EventDefinition> eventDefinitionRefs = startEvent.getEventDefinitionRefs();
    assertThat(eventDefinitionRefs).contains(messageEventDefinition);
    messageEventDefinition.setId("changed-message-event-definition-id");
    assertThat(eventDefinitionRefs).contains(messageEventDefinition);
    messageEventDefinition.setAttributeValue("id", "another-message-event-definition-id", true);
  }

  @Test
  void testShouldRemoveMessageEventDefinitionRefIfMessageEventDefinitionIsRemoved() {
    startEvent.getEventDefinitions().remove(messageEventDefinition);
    Collection<EventDefinition> eventDefinitionRefs = startEvent.getEventDefinitionRefs();
    assertThat(eventDefinitionRefs).isEmpty();
  }

  @Test
  void testShouldReplaceMessageEventDefinitionRefIfMessageEventDefinitionIsReplaced() {
    MessageEventDefinition otherMessageEventDefinition = testBpmnModelInstance.newInstance(MessageEventDefinition.class);
    otherMessageEventDefinition.setId("other-message-event-definition-id");
    Collection<EventDefinition> eventDefinitionRefs = startEvent.getEventDefinitionRefs();
    assertThat(eventDefinitionRefs).contains(messageEventDefinition);
    messageEventDefinition.replaceWithElement(otherMessageEventDefinition);
    assertThat(eventDefinitionRefs)
      .doesNotContain(messageEventDefinition)
      .contains(otherMessageEventDefinition);
  }

  @Test
  void testShouldRemoveMessageEventDefinitionRefIfIdIsRemovedOfMessageEventDefinition() {
    Collection<EventDefinition> eventDefinitionRefs = startEvent.getEventDefinitionRefs();
    assertThat(eventDefinitionRefs).contains(messageEventDefinition);
    messageEventDefinition.removeAttribute("id");
    assertThat(eventDefinitionRefs).isEmpty();
  }

  @Test
  @BpmnModelResource
  void shouldFindReferenceWithNamespace() {
    messageEventDefinition = bpmnModelInstance.getModelElementById("message-event-definition");
    message = bpmnModelInstance.getModelElementById("message-id");
    assertThat(messageEventDefinition.getMessage()).isNotNull();
    assertThat(messageEventDefinition.getMessage()).isEqualTo(message);
    message.setId("changed-message");
    assertThat(messageEventDefinition.getMessage()).isNotNull();
    assertThat(messageEventDefinition.getMessage()).isEqualTo(message);
    message.setAttributeValue("id", "again-changed-message", true);
    assertThat(messageEventDefinition.getMessage()).isNotNull();
    assertThat(messageEventDefinition.getMessage()).isEqualTo(message);

    StartEvent startEvent = bpmnModelInstance.getModelElementById("start-event");
    Collection<EventDefinition> eventDefinitionRefs = startEvent.getEventDefinitionRefs();
    assertThat(eventDefinitionRefs)
      .isNotEmpty()
      .contains(messageEventDefinition);
    messageEventDefinition.setId("changed-message-event");
    assertThat(eventDefinitionRefs)
      .isNotEmpty()
      .contains(messageEventDefinition);
    messageEventDefinition.setAttributeValue("id", "again-changed-message-event", true);
    assertThat(eventDefinitionRefs)
      .isNotEmpty()
      .contains(messageEventDefinition);

    message.removeAttribute("id");
    assertThat(messageEventDefinition.getMessage()).isNull();
    messageEventDefinition.removeAttribute("id");
    assertThat(eventDefinitionRefs).isEmpty();
  }
}
