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
package org.operaton.bpm.model.bpmn.impl.instance;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.ReceiveTaskBuilder;
import org.operaton.bpm.model.bpmn.instance.Message;
import org.operaton.bpm.model.bpmn.instance.Operation;
import org.operaton.bpm.model.bpmn.instance.ReceiveTask;
import org.operaton.bpm.model.bpmn.instance.Task;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN receiveTask element
 *
 * @author Sebastian Menski
 */
public class ReceiveTaskImpl extends TaskImpl implements ReceiveTask {

  protected static Attribute<String> implementationAttribute;
  protected static Attribute<Boolean> instantiateAttribute;
  protected static AttributeReference<Message> messageRefAttribute;
  protected static AttributeReference<Operation> operationRefAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ReceiveTask.class, BPMN_ELEMENT_RECEIVE_TASK)
      .namespaceUri(BPMN20_NS)
      .extendsType(Task.class)
      .instanceProvider(instanceContext -> new ReceiveTaskImpl(instanceContext));

    implementationAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_IMPLEMENTATION)
      .defaultValue("##WebService")
      .build();

    instantiateAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_INSTANTIATE)
      .defaultValue(false)
      .build();

    messageRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_MESSAGE_REF)
      .qNameAttributeReference(Message.class)
      .build();

    operationRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_OPERATION_REF)
      .qNameAttributeReference(Operation.class)
      .build();

    typeBuilder.build();
  }

  public ReceiveTaskImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public ReceiveTaskBuilder builder() {
    return new ReceiveTaskBuilder((BpmnModelInstance) modelInstance, this);
  }

  @Override
  public String getImplementation() {
    return implementationAttribute.getValue(this);
  }

  @Override
  public void setImplementation(String implementation) {
    implementationAttribute.setValue(this, implementation);
  }

  @Override
  public boolean instantiate() {
    return instantiateAttribute.getValue(this);
  }

  @Override
  public void setInstantiate(boolean instantiate) {
    instantiateAttribute.setValue(this, instantiate);
  }

  @Override
  public Message getMessage() {
    return messageRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setMessage(Message message) {
    messageRefAttribute.setReferenceTargetElement(this, message);
  }

  @Override
  public Operation getOperation() {
    return operationRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setOperation(Operation operation) {
    operationRefAttribute.setReferenceTargetElement(this, operation);
  }
}
