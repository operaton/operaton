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

import org.operaton.bpm.model.bpmn.instance.Escalation;
import org.operaton.bpm.model.bpmn.instance.ItemDefinition;
import org.operaton.bpm.model.bpmn.instance.RootElement;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN escalation element
 *
 * @author Sebastian Menski
 */
public class EscalationImpl extends RootElementImpl implements Escalation {

  protected static Attribute<String> nameAttribute;
  protected static Attribute<String> escalationCodeAttribute;
  protected static AttributeReference<ItemDefinition> structureRefAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Escalation.class, BPMN_ELEMENT_ESCALATION)
      .namespaceUri(BPMN20_NS)
      .extendsType(RootElement.class)
      .instanceProvider(instanceContext -> new EscalationImpl(instanceContext));

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
      .build();

    escalationCodeAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ESCALATION_CODE)
      .build();

    structureRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_STRUCTURE_REF)
      .qNameAttributeReference(ItemDefinition.class)
      .build();

    typeBuilder.build();
  }

  public EscalationImpl(ModelTypeInstanceContext context) {
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
  public String getEscalationCode() {
    return escalationCodeAttribute.getValue(this);
  }

  @Override
  public void setEscalationCode(String escalationCode) {
    escalationCodeAttribute.setValue(this, escalationCode);
  }

  @Override
  public ItemDefinition getStructure() {
    return structureRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setStructure(ItemDefinition structure) {
    structureRefAttribute.setReferenceTargetElement(this, structure);
  }

}
