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

import org.operaton.bpm.model.bpmn.instance.*;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN callableElement element
 *
 * @author Daniel Meyer
 * @author Sebastian Menski
 */
public class CallableElementImpl extends RootElementImpl implements CallableElement {

  protected static Attribute<String> nameAttribute;
  protected static ElementReferenceCollection<Interface, SupportedInterfaceRef> supportedInterfaceRefCollection;
  protected static ChildElement<IoSpecification> ioSpecificationChild;
  protected static ChildElementCollection<IoBinding> ioBindingCollection;

  public static void registerType(ModelBuilder bpmnModelBuilder) {
    ModelElementTypeBuilder typeBuilder = bpmnModelBuilder.defineType(CallableElement.class, BPMN_ELEMENT_CALLABLE_ELEMENT)
      .namespaceUri(BPMN20_NS)
      .extendsType(RootElement.class)
      .instanceProvider(instanceContext -> new CallableElementImpl(instanceContext));

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    supportedInterfaceRefCollection = sequenceBuilder.elementCollection(SupportedInterfaceRef.class)
      .qNameElementReferenceCollection(Interface.class)
      .build();

    ioSpecificationChild = sequenceBuilder.element(IoSpecification.class)
      .build();

    ioBindingCollection = sequenceBuilder.elementCollection(IoBinding.class)
      .build();

    typeBuilder.build();
  }

  public CallableElementImpl(ModelTypeInstanceContext instanceContext) {
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
  public Collection<Interface> getSupportedInterfaces() {
    return supportedInterfaceRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public IoSpecification getIoSpecification() {
    return ioSpecificationChild.getChild(this);
  }

  @Override
  public void setIoSpecification(IoSpecification ioSpecification) {
    ioSpecificationChild.setChild(this, ioSpecification);
  }

  @Override
  public Collection<IoBinding> getIoBindings() {
    return ioBindingCollection.get(this);
  }

}
