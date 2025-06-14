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
package org.operaton.bpm.model.bpmn.impl.instance.operaton;

import org.operaton.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonFormProperty;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonValue;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN formProperty operaton extension element
 *
 * @author Sebastian Menski
 */
public class OperatonFormPropertyImpl extends BpmnModelElementInstanceImpl implements OperatonFormProperty {

  protected static Attribute<String> operatonIdAttribute;
  protected static Attribute<String> operatonNameAttribute;
  protected static Attribute<String> operatonTypeAttribute;
  protected static Attribute<Boolean> operatonRequiredAttribute;
  protected static Attribute<Boolean> operatonReadableAttribute;
  protected static Attribute<Boolean> operatonWriteableAttribute;
  protected static Attribute<String> operatonVariableAttribute;
  protected static Attribute<String> operatonExpressionAttribute;
  protected static Attribute<String> operatonDatePatternAttribute;
  protected static Attribute<String> operatonDefaultAttribute;
  protected static ChildElementCollection<OperatonValue> operatonValueCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonFormProperty.class, OPERATON_ELEMENT_FORM_PROPERTY)
      .namespaceUri(OPERATON_NS)
      .instanceProvider(OperatonFormPropertyImpl::new);

    operatonIdAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_ID)
      .namespace(OPERATON_NS)
      .build();

    operatonNameAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_NAME)
      .namespace(OPERATON_NS)
      .build();

    operatonTypeAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_TYPE)
      .namespace(OPERATON_NS)
      .build();

    operatonRequiredAttribute = typeBuilder.booleanAttribute(OPERATON_ATTRIBUTE_REQUIRED)
      .namespace(OPERATON_NS)
      .defaultValue(false)
      .build();

    operatonReadableAttribute = typeBuilder.booleanAttribute(OPERATON_ATTRIBUTE_READABLE)
      .namespace(OPERATON_NS)
      .defaultValue(true)
      .build();

    operatonWriteableAttribute = typeBuilder.booleanAttribute(OPERATON_ATTRIBUTE_WRITEABLE)
      .namespace(OPERATON_NS)
      .defaultValue(true)
      .build();

    operatonVariableAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_VARIABLE)
      .namespace(OPERATON_NS)
      .build();

    operatonExpressionAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_EXPRESSION)
      .namespace(OPERATON_NS)
      .build();

    operatonDatePatternAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_DATE_PATTERN)
      .namespace(OPERATON_NS)
      .build();

    operatonDefaultAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_DEFAULT)
      .namespace(OPERATON_NS)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    operatonValueCollection = sequenceBuilder.elementCollection(OperatonValue.class)
      .build();

    typeBuilder.build();
  }

  public OperatonFormPropertyImpl(ModelTypeInstanceContext instanceContext) {
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
  public String getOperatonName() {
    return operatonNameAttribute.getValue(this);
  }

  @Override
  public void setOperatonName(String operatonName) {
    operatonNameAttribute.setValue(this, operatonName);
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
  public boolean isOperatonRequired() {
    return operatonRequiredAttribute.getValue(this);
  }

  @Override
  public void setOperatonRequired(boolean isOperatonRequired) {
    operatonRequiredAttribute.setValue(this, isOperatonRequired);
  }

  @Override
  public boolean isOperatonReadable() {
    return operatonReadableAttribute.getValue(this);
  }

  @Override
  public void setOperatonReadable(boolean isOperatonReadable) {
    operatonReadableAttribute.setValue(this, isOperatonReadable);
  }

  @Override
  public boolean isOperatonWriteable() {
    return operatonWriteableAttribute.getValue(this);
  }

  @Override
  public void setOperatonWriteable(boolean isOperatonWriteable) {
    operatonWriteableAttribute.setValue(this, isOperatonWriteable);
  }

  @Override
  public String getOperatonVariable() {
    return operatonVariableAttribute.getValue(this);
  }

  @Override
  public void setOperatonVariable(String operatonVariable) {
    operatonVariableAttribute.setValue(this, operatonVariable);
  }

  @Override
  public String getOperatonExpression() {
    return operatonExpressionAttribute.getValue(this);
  }

  @Override
  public void setOperatonExpression(String operatonExpression) {
    operatonExpressionAttribute.setValue(this, operatonExpression);
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
  public String getOperatonDefault() {
    return operatonDefaultAttribute.getValue(this);
  }

  @Override
  public void setOperatonDefault(String operatonDefault) {
    operatonDefaultAttribute.setValue(this, operatonDefault);
  }

  @Override
  public Collection<OperatonValue> getOperatonValues() {
    return operatonValueCollection.get(this);
  }
}
