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
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonExpression;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonField;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonString;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;
import static org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

/**
 * The BPMN field operaton extension element
 *
 * @author Sebastian Menski
 */
public class OperatonFieldImpl extends BpmnModelElementInstanceImpl implements OperatonField {

  protected static Attribute<String> operatonNameAttribute;
  protected static Attribute<String> operatonExpressionAttribute;
  protected static Attribute<String> operatonStringValueAttribute;
  protected static ChildElement<OperatonExpression> operatonExpressionChild;
  protected static ChildElement<OperatonString> operatonStringChild;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonField.class, OPERATON_ELEMENT_FIELD)
      .namespaceUri(OPERATON_NS)
      .instanceProvider(new ModelTypeInstanceProvider<OperatonField>() {
        public OperatonField newInstance(ModelTypeInstanceContext instanceContext) {
          return new OperatonFieldImpl(instanceContext);
        }
      });

    operatonNameAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_NAME)
      .namespace(OPERATON_NS)
      .build();

    operatonExpressionAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_EXPRESSION)
      .namespace(OPERATON_NS)
      .build();

    operatonStringValueAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_STRING_VALUE)
      .namespace(OPERATON_NS)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    operatonExpressionChild = sequenceBuilder.element(OperatonExpression.class)
      .build();

    operatonStringChild = sequenceBuilder.element(OperatonString.class)
      .build();

    typeBuilder.build();
  }

  public OperatonFieldImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public String getOperatonName() {
    return operatonNameAttribute.getValue(this);
  }

  public void setOperatonName(String operatonName) {
    operatonNameAttribute.setValue(this, operatonName);
  }

  public String getOperatonExpression() {
    return operatonExpressionAttribute.getValue(this);
  }

  public void setOperatonExpression(String operatonExpression) {
    operatonExpressionAttribute.setValue(this, operatonExpression);
  }

  public String getOperatonStringValue() {
    return operatonStringValueAttribute.getValue(this);
  }

  public void setOperatonStringValue(String operatonStringValue) {
    operatonStringValueAttribute.setValue(this, operatonStringValue);
  }

  public OperatonString getOperatonString() {
    return operatonStringChild.getChild(this);
  }

  public void setOperatonString(OperatonString operatonString) {
    operatonStringChild.setChild(this, operatonString);
  }

  public OperatonExpression getOperatonExpressionChild() {
    return operatonExpressionChild.getChild(this);
  }

  public void setOperatonExpressionChild(OperatonExpression operatonExpression) {
    operatonExpressionChild.setChild(this, operatonExpression);
  }
}
