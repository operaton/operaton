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
package org.operaton.bpm.model.bpmn;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.bpmn.instance.*;
import org.operaton.bpm.model.bpmn.instance.Process;
import org.operaton.bpm.model.bpmn.util.BpmnModelResource;
import org.operaton.bpm.model.xml.ModelParseException;
import org.operaton.bpm.model.xml.ModelReferenceException;
import org.operaton.bpm.model.xml.ModelValidationException;
import org.operaton.bpm.model.xml.impl.util.IoUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.XML_SCHEMA_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.XPATH_NS;

/**
 * @author Daniel Meyer
 *
 */
public class DefinitionsTest extends BpmnModelTest {

  @Test
  @BpmnModelResource
  public void shouldImportEmptyDefinitions() {

    Definitions definitions = bpmnModelInstance.getDefinitions();
    assertThat(definitions).isNotNull();

    // provided in file
    assertThat(definitions.getTargetNamespace()).isEqualTo("http://operaton.org/test");

    // defaults provided in Schema
    assertThat(definitions.getExpressionLanguage()).isEqualTo(XPATH_NS);
    assertThat(definitions.getTypeLanguage()).isEqualTo(XML_SCHEMA_NS);

    // not provided in file
    assertThat(definitions.getExporter()).isNull();
    assertThat(definitions.getExporterVersion()).isNull();
    assertThat(definitions.getId()).isNull();
    assertThat(definitions.getName()).isNull();

    // has no imports
    assertThat(definitions.getImports()).isEmpty();
  }

  @Test
  public void shouldNotImportWrongOrderedSequence() {
    assertThatThrownBy(() -> Bpmn.readModelFromStream(getClass().getResourceAsStream("DefinitionsTest.shouldNotImportWrongOrderedSequence.bpmn")))
            .isInstanceOf(ModelParseException.class);
  }

  @Test
  public void shouldAddChildElementsInCorrectOrder() {
    // create an empty model
    BpmnModelInstance bpmnModelInstance = Bpmn.createEmptyModel();

    // add definitions
    Definitions definitions = bpmnModelInstance.newInstance(Definitions.class);
    definitions.setTargetNamespace("Examples");
    bpmnModelInstance.setDefinitions(definitions);

    // create a Process element and add it to the definitions
    Process process = bpmnModelInstance.newInstance(Process.class);
    process.setId("some-process-id");
    definitions.getRootElements().add(process);

    // create an Import element and add it to the definitions
    Import importElement = bpmnModelInstance.newInstance(Import.class);
    importElement.setNamespace("Imports");
    importElement.setLocation("here");
    importElement.setImportType("example");
    definitions.getImports().add(importElement);

    // create another Process element and add it to the definitions
    process = bpmnModelInstance.newInstance(Process.class);
    process.setId("another-process-id");
    definitions.getRootElements().add(process);

    // create another Import element and add it to the definitions
    importElement = bpmnModelInstance.newInstance(Import.class);
    importElement.setNamespace("Imports");
    importElement.setLocation("there");
    importElement.setImportType("example");
    definitions.getImports().add(importElement);

    // validate model
    try {
      Bpmn.validateModel(bpmnModelInstance);
    }
    catch (ModelValidationException e) {
      Assertions.fail();
    }
  }

  @Test
  @BpmnModelResource
  public void shouldNotAffectComments() throws IOException {
    Definitions definitions = bpmnModelInstance.getDefinitions();
    assertThat(definitions).isNotNull();

    // create another Process element and add it to the definitions
    Process process = bpmnModelInstance.newInstance(Process.class);
    process.setId("another-process-id");
    definitions.getRootElements().add(process);

    // create another Import element and add it to the definitions
    Import importElement = bpmnModelInstance.newInstance(Import.class);
    importElement.setNamespace("Imports");
    importElement.setLocation("there");
    importElement.setImportType("example");
    definitions.getImports().add(importElement);

    // validate model
    Bpmn.validateModel(bpmnModelInstance);

    // convert the model to the XML string representation
    OutputStream outputStream = new ByteArrayOutputStream();
    Bpmn.writeModelToStream(outputStream, bpmnModelInstance);
    InputStream inputStream = IoUtil.convertOutputStreamToInputStream(outputStream);
    String modelString = IoUtil.getStringFromInputStream(inputStream);
    IoUtil.closeSilently(outputStream);
    IoUtil.closeSilently(inputStream);

    // read test process from file as string
    inputStream = getClass().getResourceAsStream("DefinitionsTest.shouldNotAffectCommentsResult.bpmn");
    String fileString = IoUtil.getStringFromInputStream(inputStream);
    IoUtil.closeSilently(inputStream);

    // compare strings
    assertThat(modelString).endsWith(fileString);
  }

  @Test
  public void shouldAddMessageAndMessageEventDefinition() {
    // create empty model
    BpmnModelInstance bpmnModelInstance = Bpmn.createEmptyModel();

    // add definitions to model
    Definitions definitions = bpmnModelInstance.newInstance(Definitions.class);
    definitions.setTargetNamespace("Examples");
    bpmnModelInstance.setDefinitions(definitions);

    // create and add message
    Message message = bpmnModelInstance.newInstance(Message.class);
    message.setId("start-message-id");
    definitions.getRootElements().add(message);

    // create and add message event definition
    MessageEventDefinition messageEventDefinition = bpmnModelInstance.newInstance(MessageEventDefinition.class);
    messageEventDefinition.setId("message-event-def-id");
    messageEventDefinition.setMessage(message);
    definitions.getRootElements().add(messageEventDefinition);

    // test if message was set correctly
    Message setMessage = messageEventDefinition.getMessage();
    assertThat(setMessage).isEqualTo(message);

    // add process
    Process process = bpmnModelInstance.newInstance(Process.class);
    process.setId("messageEventDefinition");
    definitions.getRootElements().add(process);

    // add start event
    StartEvent startEvent = bpmnModelInstance.newInstance(StartEvent.class);
    startEvent.setId("theStart");
    process.getFlowElements().add(startEvent);

    // create and add message event definition to start event
    MessageEventDefinition startEventMessageEventDefinition = bpmnModelInstance.newInstance(MessageEventDefinition.class);
    startEventMessageEventDefinition.setMessage(message);
    startEvent.getEventDefinitions().add(startEventMessageEventDefinition);

    // create another message but do not add it
    Message anotherMessage = bpmnModelInstance.newInstance(Message.class);
    anotherMessage.setId("another-message-id");

    // create a message event definition and try to add last create message
    MessageEventDefinition anotherMessageEventDefinition = bpmnModelInstance.newInstance(MessageEventDefinition.class);
    final MessageEventDefinition finalMessageEventDefinition = anotherMessageEventDefinition;
    assertThatThrownBy(() -> finalMessageEventDefinition.setMessage(anotherMessage))
            .isInstanceOf(ModelReferenceException.class)
            .withFailMessage("Message should not be added to message event definition, cause it is not part of the model");

    // first add message to model than to event definition
    definitions.getRootElements().add(anotherMessage);
    anotherMessageEventDefinition.setMessage(anotherMessage);
    startEvent.getEventDefinitions().add(anotherMessageEventDefinition);

    // message event definition and add message by id to it
    anotherMessageEventDefinition = bpmnModelInstance.newInstance(MessageEventDefinition.class);
    startEvent.getEventDefinitions().add(anotherMessageEventDefinition);

    // validate model
    Bpmn.validateModel(bpmnModelInstance);
  }

  @Test
  public void shouldAddParentChildElementInCorrectOrder() {
    // create empty model
    BpmnModelInstance bpmnModelInstance = Bpmn.createEmptyModel();

    // add definitions to model
    Definitions definitions = bpmnModelInstance.newInstance(Definitions.class);
    definitions.setTargetNamespace("Examples");
    bpmnModelInstance.setDefinitions(definitions);

    // add process
    Process process = bpmnModelInstance.newInstance(Process.class);
    process.setId("messageEventDefinition");
    definitions.getRootElements().add(process);

    // add start event
    StartEvent startEvent = bpmnModelInstance.newInstance(StartEvent.class);
    startEvent.setId("theStart");
    process.getFlowElements().add(startEvent);

    // create and add message
    Message message = bpmnModelInstance.newInstance(Message.class);
    message.setId("start-message-id");
    definitions.getRootElements().add(message);

    // add message event definition to start event
    MessageEventDefinition startEventMessageEventDefinition = bpmnModelInstance.newInstance(MessageEventDefinition.class);
    startEventMessageEventDefinition.setMessage(message);
    startEvent.getEventDefinitions().add(startEventMessageEventDefinition);

    // add property after message event definition
    Property property = bpmnModelInstance.newInstance(Property.class);
    startEvent.getProperties().add(property);

    // finally add an extensions element
    ExtensionElements extensionElements = bpmnModelInstance.newInstance(ExtensionElements.class);
    process.setExtensionElements(extensionElements);

    // validate model
    Bpmn.validateModel(bpmnModelInstance);
  }
}
