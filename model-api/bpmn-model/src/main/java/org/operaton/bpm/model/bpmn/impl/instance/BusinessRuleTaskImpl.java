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

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_IMPLEMENTATION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_BUSINESS_RULE_TASK;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.CAMUNDA_ATTRIBUTE_CLASS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.CAMUNDA_ATTRIBUTE_DECISION_REF;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.CAMUNDA_ATTRIBUTE_DECISION_REF_BINDING;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.CAMUNDA_ATTRIBUTE_DECISION_REF_VERSION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.CAMUNDA_ATTRIBUTE_DELEGATE_EXPRESSION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.CAMUNDA_ATTRIBUTE_MAP_DECISION_RESULT;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.CAMUNDA_ATTRIBUTE_RESULT_VARIABLE;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.CAMUNDA_ATTRIBUTE_TOPIC;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.CAMUNDA_ATTRIBUTE_TYPE;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.BusinessRuleTaskBuilder;
import org.operaton.bpm.model.bpmn.instance.BusinessRuleTask;
import org.operaton.bpm.model.bpmn.instance.Rendering;
import org.operaton.bpm.model.bpmn.instance.Task;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;

/**
 * The BPMN businessRuleTask element
 *
 * @author Sebastian Menski
 */
public class BusinessRuleTaskImpl extends TaskImpl implements BusinessRuleTask {

  protected static Attribute<String> implementationAttribute;
  protected static ChildElementCollection<Rendering> renderingCollection;

  /** operaton extensions */

  protected static Attribute<String> operatonClassAttribute;
  protected static Attribute<String> operatonDelegateExpressionAttribute;
  protected static Attribute<String> operatonExpressionAttribute;
  protected static Attribute<String> operatonResultVariableAttribute;
  protected static Attribute<String> operatonTopicAttribute;
  protected static Attribute<String> operatonTypeAttribute;
  protected static Attribute<String> operatonDecisionRefAttribute;
  protected static Attribute<String> operatonDecisionRefBindingAttribute;
  protected static Attribute<String> operatonDecisionRefVersionAttribute;
  protected static Attribute<String> operatonDecisionRefVersionTagAttribute;
  protected static Attribute<String> operatonDecisionRefTenantIdAttribute;
  protected static Attribute<String> operatonMapDecisionResultAttribute;
  protected static Attribute<String> operatonTaskPriorityAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(BusinessRuleTask.class, BPMN_ELEMENT_BUSINESS_RULE_TASK)
      .namespaceUri(BPMN20_NS)
      .extendsType(Task.class)
      .instanceProvider(new ModelTypeInstanceProvider<BusinessRuleTask>() {
        public BusinessRuleTask newInstance(ModelTypeInstanceContext instanceContext) {
          return new BusinessRuleTaskImpl(instanceContext);
        }
      });

    implementationAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_IMPLEMENTATION)
      .defaultValue("##unspecified")
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

    operatonDecisionRefAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_DECISION_REF)
      .namespace(OPERATON_NS)
      .build();

    operatonDecisionRefBindingAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_DECISION_REF_BINDING)
      .namespace(OPERATON_NS)
      .build();

    operatonDecisionRefVersionAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_DECISION_REF_VERSION)
      .namespace(OPERATON_NS)
      .build();

    operatonDecisionRefVersionTagAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_DECISION_REF_VERSION_TAG)
        .namespace(OPERATON_NS)
        .build();

    operatonDecisionRefTenantIdAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_DECISION_REF_TENANT_ID)
      .namespace(OPERATON_NS)
      .build();

    operatonMapDecisionResultAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_MAP_DECISION_RESULT)
        .namespace(OPERATON_NS)
        .build();

    operatonTaskPriorityAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_TASK_PRIORITY)
      .namespace(OPERATON_NS)
      .build();

    typeBuilder.build();
  }

  public BusinessRuleTaskImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public BusinessRuleTaskBuilder builder() {
    return new BusinessRuleTaskBuilder((BpmnModelInstance) modelInstance, this);
  }

  public String getImplementation() {
    return implementationAttribute.getValue(this);
  }

  public void setImplementation(String implementation) {
    implementationAttribute.setValue(this, implementation);
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

  public String getOperatonDecisionRef() {
    return operatonDecisionRefAttribute.getValue(this);
  }

  public void setOperatonDecisionRef(String operatonDecisionRef) {
    operatonDecisionRefAttribute.setValue(this, operatonDecisionRef);
  }

  public String getOperatonDecisionRefBinding() {
    return operatonDecisionRefBindingAttribute.getValue(this);
  }

  public void setOperatonDecisionRefBinding(String operatonDecisionRefBinding) {
    operatonDecisionRefBindingAttribute.setValue(this, operatonDecisionRefBinding);
  }

  public String getOperatonDecisionRefVersion() {
    return operatonDecisionRefVersionAttribute.getValue(this);
  }

  public void setOperatonDecisionRefVersion(String operatonDecisionRefVersion) {
    operatonDecisionRefVersionAttribute.setValue(this, operatonDecisionRefVersion);
  }

  public String getOperatonDecisionRefVersionTag() {
    return operatonDecisionRefVersionTagAttribute.getValue(this);
  }

  public void setOperatonDecisionRefVersionTag(String operatonDecisionRefVersionTag) {
    operatonDecisionRefVersionTagAttribute.setValue(this, operatonDecisionRefVersionTag);
  }

  @Override
  public String getOperatonMapDecisionResult() {
    return operatonMapDecisionResultAttribute.getValue(this);
  }

  @Override
  public void setOperatonMapDecisionResult(String operatonMapDecisionResult) {
    operatonMapDecisionResultAttribute.setValue(this, operatonMapDecisionResult);
  }

  public String getOperatonDecisionRefTenantId() {
    return operatonDecisionRefTenantIdAttribute.getValue(this);
  }

  public void setOperatonDecisionRefTenantId(String tenantId) {
    operatonDecisionRefTenantIdAttribute.setValue(this, tenantId);
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