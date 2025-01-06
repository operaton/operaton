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
package org.operaton.bpm.model.bpmn.impl.instance.operaton;

import org.operaton.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonFormField;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonProperties;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonValidation;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonValue;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN formField operaton extension element
 *
 * @author Sebastian Menski
 */
public class OperatonFormFieldImpl extends BpmnModelElementInstanceImpl implements OperatonFormField {

  protected static Attribute<String> operatonIdAttribute;
  protected static Attribute<String> operatonLabelAttribute;
  protected static Attribute<String> operatonTypeAttribute;
  protected static Attribute<String> operatonDatePatternAttribute;
  protected static Attribute<String> operatonDefaultValueAttribute;
  protected static ChildElement<OperatonProperties> operatonPropertiesChild;
  protected static ChildElement<OperatonValidation> operatonValidationChild;
  protected static ChildElementCollection<OperatonValue> operatonValueCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonFormField.class, OPERATON_ELEMENT_FORM_FIELD)
      .namespaceUri(OPERATON_NS)
      .instanceProvider(OperatonFormFieldImpl::new);

    operatonIdAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_ID)
      .namespace(OPERATON_NS)
      .build();

    operatonLabelAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_LABEL)
      .namespace(OPERATON_NS)
      .build();

    operatonTypeAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_TYPE)
      .namespace(OPERATON_NS)
      .build();

    operatonDatePatternAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_DATE_PATTERN)
      .namespace(OPERATON_NS)
      .build();

    operatonDefaultValueAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_DEFAULT_VALUE)
      .namespace(OPERATON_NS)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    operatonPropertiesChild = sequenceBuilder.element(OperatonProperties.class)
      .build();

    operatonValidationChild = sequenceBuilder.element(OperatonValidation.class)
      .build();

    operatonValueCollection = sequenceBuilder.elementCollection(OperatonValue.class)
      .build();

    typeBuilder.build();
  }

  public OperatonFormFieldImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getOperatonId() {
    return operatonIdAttribute.getValue(this);
  }

  @Override
  public void setOperatonId(String operatonId) {
    operatonIdAttribute.setValue(this, operatonId);
  }

  @Override
  public String getOperatonLabel() {
    return operatonLabelAttribute.getValue(this);
  }

  @Override
  public void setOperatonLabel(String operatonLabel) {
    operatonLabelAttribute.setValue(this, operatonLabel);
  }

  @Override
  public String getOperatonType() {
    return operatonTypeAttribute.getValue(this);
  }

  @Override
  public void setOperatonType(String operatonType) {
    operatonTypeAttribute.setValue(this, operatonType);
  }

  @Override
  public String getOperatonDatePattern() {
    return operatonDatePatternAttribute.getValue(this);
  }

  @Override
  public void setOperatonDatePattern(String operatonDatePattern) {
    operatonDatePatternAttribute.setValue(this, operatonDatePattern);
  }

  @Override
  public String getOperatonDefaultValue() {
    return operatonDefaultValueAttribute.getValue(this);
  }

  @Override
  public void setOperatonDefaultValue(String operatonDefaultValue) {
    operatonDefaultValueAttribute.setValue(this, operatonDefaultValue);
  }

  @Override
  public OperatonProperties getOperatonProperties() {
    return operatonPropertiesChild.getChild(this);
  }

  @Override
  public void setOperatonProperties(OperatonProperties operatonProperties) {
    operatonPropertiesChild.setChild(this, operatonProperties);
  }

  @Override
  public OperatonValidation getOperatonValidation() {
    return operatonValidationChild.getChild(this);
  }

  @Override
  public void setOperatonValidation(OperatonValidation operatonValidation) {
    operatonValidationChild.setChild(this, operatonValidation);
  }

  @Override
  public Collection<OperatonValue> getOperatonValues() {
    return operatonValueCollection.get(this);
  }
}
