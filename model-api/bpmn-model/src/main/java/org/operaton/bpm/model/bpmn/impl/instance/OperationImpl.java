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
package org.operaton.bpm.model.bpmn.impl.instance;

import org.operaton.bpm.model.bpmn.instance.BaseElement;
import org.operaton.bpm.model.bpmn.instance.Error;
import org.operaton.bpm.model.bpmn.instance.Message;
import org.operaton.bpm.model.bpmn.instance.Operation;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.ElementReference;
import org.operaton.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN operation element
 *
 * @author Sebastian Menski
 */
public class OperationImpl extends BaseElementImpl implements Operation {

  protected static Attribute<String> nameAttribute;
  protected static Attribute<String> implementationRefAttribute;
  protected static ElementReference<Message, InMessageRef> inMessageRefChild;
  protected static ElementReference<Message, OutMessageRef> outMessageRefChild;
  protected static ElementReferenceCollection<Error, ErrorRef> errorRefCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Operation.class, BPMN_ELEMENT_OPERATION)
      .namespaceUri(BPMN20_NS)
      .extendsType(BaseElement.class)
      .instanceProvider(OperationImpl::new);

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
      .required()
      .build();

    implementationRefAttribute = typeBuilder.stringAttribute(BPMN_ELEMENT_IMPLEMENTATION_REF)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    inMessageRefChild = sequenceBuilder.element(InMessageRef.class)
      .required()
      .qNameElementReference(Message.class)
      .build();

    outMessageRefChild = sequenceBuilder.element(OutMessageRef.class)
      .qNameElementReference(Message.class)
      .build();

    errorRefCollection = sequenceBuilder.elementCollection(ErrorRef.class)
      .qNameElementReferenceCollection(Error.class)
      .build();

    typeBuilder.build();
  }

  public OperationImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getName() {
    return nameAttribute.getValue(this);
  }

  @Override
  public void setName(String name) {
    nameAttribute.setValue(this, name);
  }

  @Override
  public String getImplementationRef() {
    return implementationRefAttribute.getValue(this);
  }

  @Override
  public void setImplementationRef(String implementationRef) {
    implementationRefAttribute.setValue(this, implementationRef);
  }

  @Override
  public Message getInMessage() {
    return inMessageRefChild.getReferenceTargetElement(this);
  }

  @Override
  public void setInMessage(Message message) {
    inMessageRefChild.setReferenceTargetElement(this, message);
  }

  @Override
  public Message getOutMessage() {
    return outMessageRefChild.getReferenceTargetElement(this);
  }

  @Override
  public void setOutMessage(Message message) {
    outMessageRefChild.setReferenceTargetElement(this, message);
  }

  @Override
  public Collection<Error> getErrors() {
    return errorRefCollection.getReferenceTargetElements(this);
  }
}
