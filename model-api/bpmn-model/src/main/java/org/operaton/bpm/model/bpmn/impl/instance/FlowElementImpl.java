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
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN flowElement element
 *
 * @author Daniel Meyer
 * @author Sebastian Menski
 */
public abstract class FlowElementImpl extends BaseElementImpl implements FlowElement {

  protected static Attribute<String> nameAttribute;
  protected static ChildElement<Auditing> auditingChild;
  protected static ChildElement<Monitoring> monitoringChild;
  protected static ElementReferenceCollection<CategoryValue, CategoryValueRef> categoryValueRefCollection;

  public static void registerType(ModelBuilder modelBuilder) {

    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowElement.class, BPMN_ELEMENT_FLOW_ELEMENT)
      .namespaceUri(BPMN20_NS)
      .extendsType(BaseElement.class)
      .abstractType();

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    auditingChild = sequenceBuilder.element(Auditing.class)
      .build();

    monitoringChild = sequenceBuilder.element(Monitoring.class)
      .build();

    categoryValueRefCollection = sequenceBuilder.elementCollection(CategoryValueRef.class)
      .qNameElementReferenceCollection(CategoryValue.class)
      .build();

    typeBuilder.build();
  }

  protected FlowElementImpl(ModelTypeInstanceContext context) {
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
  public Auditing getAuditing() {
    return auditingChild.getChild(this);
  }

  @Override
  public void setAuditing(Auditing auditing) {
    auditingChild.setChild(this, auditing);
  }

  @Override
  public Monitoring getMonitoring() {
    return monitoringChild.getChild(this);
  }

  @Override
  public void setMonitoring(Monitoring monitoring) {
    monitoringChild.setChild(this, monitoring);
  }

  @Override
  public Collection<CategoryValue> getCategoryValueRefs() {
    return categoryValueRefCollection.getReferenceTargetElements(this);
  }
}
