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

import java.util.Collection;

import org.operaton.bpm.model.bpmn.instance.Interface;
import org.operaton.bpm.model.bpmn.instance.Operation;
import org.operaton.bpm.model.bpmn.instance.RootElement;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN interface element
 *
 * @author Sebastian Menski
 */
public class InterfaceImpl extends RootElementImpl implements Interface {

  protected static Attribute<String> nameAttribute;
  protected static Attribute<String> implementationRefAttribute;
  protected static ChildElementCollection<Operation> operationCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Interface.class, BPMN_ELEMENT_INTERFACE)
      .namespaceUri(BPMN20_NS)
      .extendsType(RootElement.class)
      .instanceProvider(InterfaceImpl::new);

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
      .required()
      .build();

    implementationRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_IMPLEMENTATION_REF)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    operationCollection = sequenceBuilder.elementCollection(Operation.class)
      .required()
      .build();

    typeBuilder.build();
  }

  public InterfaceImpl(ModelTypeInstanceContext context) {
    super(context);
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
  public Collection<Operation> getOperations() {
    return operationCollection.get(this);
  }
}
