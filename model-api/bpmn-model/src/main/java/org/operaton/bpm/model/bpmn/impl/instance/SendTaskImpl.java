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
import org.operaton.bpm.model.bpmn.builder.SendTaskBuilder;
import org.operaton.bpm.model.bpmn.instance.Message;
import org.operaton.bpm.model.bpmn.instance.Operation;
import org.operaton.bpm.model.bpmn.instance.SendTask;
import org.operaton.bpm.model.bpmn.instance.Task;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;
import static org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

/**
 * The BPMN sendTask element
 *
 * @author Sebastian Menski
 */
public class SendTaskImpl extends TaskImpl implements SendTask {

  protected static Attribute<String> implementationAttribute;
  protected static AttributeReference<Message> messageRefAttribute;
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
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(SendTask.class, BPMN_ELEMENT_SEND_TASK)
      .namespaceUri(BPMN20_NS)
      .extendsType(Task.class)
      .instanceProvider(new ModelTypeInstanceProvider<SendTask>() {
        public SendTask newInstance(ModelTypeInstanceContext instanceContext) {
          return new SendTaskImpl(instanceContext);
        }
      });

    implementationAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_IMPLEMENTATION)
      .defaultValue("##WebService")
      .build();

    messageRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_MESSAGE_REF)
      .qNameAttributeReference(Message.class)
      .build();

    operationRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_OPERATION_REF)
      .qNameAttributeReference(Operation.class)
      .build();

    /** operaton extensions */

    operatonClassAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_CLASS)
      .namespace(OPERATON_NS)
      .build();

    operatonDelegateExpressionAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_DELEGATE_EXPRESSION)
      .namespace(OPERATON_NS)
      .build();

    operatonExpressionAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_EXPRESSION)
      .namespace(OPERATON_NS)
      .build();

    operatonResultVariableAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_RESULT_VARIABLE)
      .namespace(OPERATON_NS)
      .build();

    operatonTopicAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_TOPIC)
        .namespace(OPERATON_NS)
        .build();

    operatonTypeAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_TYPE)
      .namespace(OPERATON_NS)
      .build();
    
    operatonTaskPriorityAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_TASK_PRIORITY)
      .namespace(OPERATON_NS)
      .build();

    typeBuilder.build();
  }

  public SendTaskImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  public SendTaskBuilder builder() {
    return new SendTaskBuilder((BpmnModelInstance) modelInstance, this);
  }

  public String getImplementation() {
    return implementationAttribute.getValue(this);
  }

  public void setImplementation(String implementation) {
    implementationAttribute.setValue(this, implementation);
  }

  public Message getMessage() {
    return messageRefAttribute.getReferenceTargetElement(this);
  }

  public void setMessage(Message message) {
    messageRefAttribute.setReferenceTargetElement(this, message);
  }

  public Operation getOperation() {
    return operationRefAttribute.getReferenceTargetElement(this);
  }

  public void setOperation(Operation operation) {
    operationRefAttribute.setReferenceTargetElement(this, operation);
  }

  /** operaton extensions */

  public String getOperatonClass() {
    return operatonClassAttribute.getValue(this);
  }

  public void setOperatonClass(String operatonClass) {
    operatonClassAttribute.setValue(this, operatonClass);
  }

  public String getOperatonDelegateExpression() {
    return operatonDelegateExpressionAttribute.getValue(this);
  }

  public void setOperatonDelegateExpression(String operatonExpression) {
    operatonDelegateExpressionAttribute.setValue(this, operatonExpression);
  }

  public String getOperatonExpression() {
    return operatonExpressionAttribute.getValue(this);
  }

  public void setOperatonExpression(String operatonExpression) {
    operatonExpressionAttribute.setValue(this, operatonExpression);
  }

  public String getOperatonResultVariable() {
    return operatonResultVariableAttribute.getValue(this);
  }

  public void setOperatonResultVariable(String operatonResultVariable) {
    operatonResultVariableAttribute.setValue(this, operatonResultVariable);
  }

  public String getOperatonTopic() {
    return operatonTopicAttribute.getValue(this);
  }

  public void setOperatonTopic(String operatonTopic) {
    operatonTopicAttribute.setValue(this, operatonTopic);
  }

  public String getOperatonType() {
    return operatonTypeAttribute.getValue(this);
  }

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
