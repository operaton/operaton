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

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.ServiceTaskBuilder;
import org.operaton.bpm.model.bpmn.instance.Operation;
import org.operaton.bpm.model.bpmn.instance.ServiceTask;
import org.operaton.bpm.model.bpmn.instance.Task;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN serviceTask element
 *
 * @author Sebastian Menski
 */
public class ServiceTaskImpl extends TaskImpl implements ServiceTask {

  protected static Attribute<String> implementationAttribute;
  protected static AttributeReference<Operation> operationRefAttribute;

  /** operaton extensions */

  protected static Attribute<String> operatonClassAttribute;
  protected static Attribute<String> operatonDelegateExpressionAttribute;
  protected static Attribute<String> operatonExpressionAttribute;
  protected static Attribute<String> operatonResultVariableAttribute;
  protected static Attribute<String> operatonTopicAttribute;
  protected static Attribute<String> operatonTypeAttribute;
  protected static Attribute<String> operatonTaskPriorityAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ServiceTask.class, BPMN_ELEMENT_SERVICE_TASK)
      .namespaceUri(BPMN20_NS)
      .extendsType(Task.class)
      .instanceProvider(ServiceTaskImpl::new);

    implementationAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_IMPLEMENTATION)
      .defaultValue("##WebService")
      .build();

    operationRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_OPERATION_REF)
      .qNameAttributeReference(Operation.class)
      .build();

    /** operaton extensions */

    operatonClassAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_CLASS)
      .namespace(OPERATON_NS)
      .build();

    operatonDelegateExpressionAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_DELEGATE_EXPRESSION)
      .namespace(OPERATON_NS)
      .build();

    operatonExpressionAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_EXPRESSION)
      .namespace(OPERATON_NS)
      .build();

    operatonResultVariableAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_RESULT_VARIABLE)
      .namespace(OPERATON_NS)
      .build();

    operatonTopicAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_TOPIC)
        .namespace(OPERATON_NS)
        .build();

    operatonTypeAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_TYPE)
      .namespace(OPERATON_NS)
      .build();
    
    operatonTaskPriorityAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_TASK_PRIORITY)
      .namespace(OPERATON_NS)
      .build();

    typeBuilder.build();
  }

  public ServiceTaskImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public ServiceTaskBuilder builder() {
    return new ServiceTaskBuilder((BpmnModelInstance) modelInstance, this);
  }

  @Override
  public String getImplementation() {
    return implementationAttribute.getValue(this);
  }

  @Override
  public void setImplementation(String implementation) {
    implementationAttribute.setValue(this, implementation);
  }

  @Override
  public Operation getOperation() {
    return operationRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setOperation(Operation operation) {
    operationRefAttribute.setReferenceTargetElement(this, operation);
  }

  /** operaton extensions */

  @Override
  public String getOperatonClass() {
    return operatonClassAttribute.getValue(this);
  }

  @Override
  public void setOperatonClass(String operatonClass) {
    operatonClassAttribute.setValue(this, operatonClass);
  }

  @Override
  public String getOperatonDelegateExpression() {
    return operatonDelegateExpressionAttribute.getValue(this);
  }

  @Override
  public void setOperatonDelegateExpression(String operatonExpression) {
    operatonDelegateExpressionAttribute.setValue(this, operatonExpression);
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
  public String getOperatonResultVariable() {
    return operatonResultVariableAttribute.getValue(this);
  }

  @Override
  public void setOperatonResultVariable(String operatonResultVariable) {
    operatonResultVariableAttribute.setValue(this, operatonResultVariable);
  }

  @Override
  public String getOperatonTopic() {
    return operatonTopicAttribute.getValue(this);
  }

  @Override
  public void setOperatonTopic(String operatonTopic) {
    operatonTopicAttribute.setValue(this, operatonTopic);
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
  public String getOperatonTaskPriority() {
    return operatonTaskPriorityAttribute.getValue(this);    
  }

  @Override
  public void setOperatonTaskPriority(String taskPriority) {
    operatonTaskPriorityAttribute.setValue(this, taskPriority);
  }
}
