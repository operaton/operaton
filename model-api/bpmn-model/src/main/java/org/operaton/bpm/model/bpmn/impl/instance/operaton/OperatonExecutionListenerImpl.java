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

import java.util.Collection;

import org.operaton.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonExecutionListener;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonField;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonScript;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_CLASS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_DELEGATE_EXPRESSION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_EVENT;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_EXPRESSION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ELEMENT_EXECUTION_LISTENER;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;

/**
 * The BPMN executionListener operaton extension element
 *
 * @author Sebastian Menski
 */
public class OperatonExecutionListenerImpl extends BpmnModelElementInstanceImpl implements OperatonExecutionListener {

  protected static Attribute<String> operatonEventAttribute;
  protected static Attribute<String> operatonClassAttribute;
  protected static Attribute<String> operatonExpressionAttribute;
  protected static Attribute<String> operatonDelegateExpressionAttribute;
  protected static ChildElementCollection<OperatonField> operatonFieldCollection;
  protected static ChildElement<OperatonScript> operatonScriptChild;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonExecutionListener.class, OPERATON_ELEMENT_EXECUTION_LISTENER)
      .namespaceUri(OPERATON_NS)
      .instanceProvider(instanceContext -> new OperatonExecutionListenerImpl(instanceContext));

    operatonEventAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_EVENT)
      .namespace(OPERATON_NS)
      .build();

    operatonClassAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_CLASS)
      .namespace(OPERATON_NS)
      .build();

    operatonExpressionAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_EXPRESSION)
      .namespace(OPERATON_NS)
      .build();

    operatonDelegateExpressionAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_DELEGATE_EXPRESSION)
      .namespace(OPERATON_NS)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    operatonFieldCollection = sequenceBuilder.elementCollection(OperatonField.class)
      .build();

    operatonScriptChild = sequenceBuilder.element(OperatonScript.class)
      .build();

    typeBuilder.build();
  }

  public OperatonExecutionListenerImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getOperatonEvent() {
    return operatonEventAttribute.getValue(this);
  }

  @Override
  public void setOperatonEvent(String operatonEvent) {
    operatonEventAttribute.setValue(this, operatonEvent);
  }

  @Override
  public String getOperatonClass() {
    return operatonClassAttribute.getValue(this);
  }

  @Override
  public void setOperatonClass(String operatonClass) {
    operatonClassAttribute.setValue(this, operatonClass);
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
  public String getOperatonDelegateExpression() {
    return operatonDelegateExpressionAttribute.getValue(this);
  }

  @Override
  public void setOperatonDelegateExpression(String operatonDelegateExpression) {
    operatonDelegateExpressionAttribute.setValue(this, operatonDelegateExpression);
  }

  @Override
  public Collection<OperatonField> getOperatonFields() {
    return operatonFieldCollection.get(this);
  }

  @Override
  public OperatonScript getOperatonScript() {
    return operatonScriptChild.getChild(this);
  }

  @Override
  public void setOperatonScript(OperatonScript operatonScript) {
    operatonScriptChild.setChild(this, operatonScript);
  }

}
