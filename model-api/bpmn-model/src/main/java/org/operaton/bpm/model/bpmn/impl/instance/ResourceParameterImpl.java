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

import org.operaton.bpm.model.bpmn.instance.BaseElement;
import org.operaton.bpm.model.bpmn.instance.ItemDefinition;
import org.operaton.bpm.model.bpmn.instance.ResourceParameter;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN resourceParameter element
 *
 * @author Sebastian Menski
 */
public class ResourceParameterImpl extends BaseElementImpl implements ResourceParameter {

  protected static Attribute<String> nameAttribute;
  protected static AttributeReference<ItemDefinition> typeAttribute;
  protected static Attribute<Boolean> isRequiredAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ResourceParameter.class, BPMN_ELEMENT_RESOURCE_PARAMETER)
      .namespaceUri(BPMN20_NS)
      .extendsType(BaseElement.class)
      .instanceProvider(ResourceParameterImpl::new);

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
      .build();

    typeAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_TYPE)
      .qNameAttributeReference(ItemDefinition.class)
      .build();

    isRequiredAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_IS_REQUIRED)
      .build();

    typeBuilder.build();
  }

  public ResourceParameterImpl(ModelTypeInstanceContext instanceContext) {
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
  public ItemDefinition getType() {
    return typeAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setType(ItemDefinition type) {
    typeAttribute.setReferenceTargetElement(this, type);
  }

  @Override
  public boolean isRequired() {
    return isRequiredAttribute.getValue(this);
  }

  @Override
  public void setRequired(boolean isRequired) {
   isRequiredAttribute.setValue(this, isRequired);
  }
}
