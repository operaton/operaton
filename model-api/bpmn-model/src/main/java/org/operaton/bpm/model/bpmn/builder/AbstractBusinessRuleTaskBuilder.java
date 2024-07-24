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
package org.operaton.bpm.model.bpmn.builder;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.BusinessRuleTask;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractBusinessRuleTaskBuilder<B extends AbstractBusinessRuleTaskBuilder<B>> extends AbstractTaskBuilder<B, BusinessRuleTask> {

  protected AbstractBusinessRuleTaskBuilder(BpmnModelInstance modelInstance, BusinessRuleTask element, Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets the implementation of the business rule task.
   *
   * @param implementation  the implementation to set
   * @return the builder object
   */
  public B implementation(String implementation) {
    element.setImplementation(implementation);
    return myself;
  }

  /** operaton extensions */

  /**
   * Sets the operaton class attribute.
   *
   * @param operatonClass  the class name to set
   * @return the builder object
   */
  @SuppressWarnings("rawtypes")
  public B operatonClass(Class operatonClass) {
    return operatonClass(operatonClass.getName());
  }

  /**
   * Sets the operaton class attribute.
   *
   * @param operatonClass  the class name to set
   * @return the builder object
   */
  public B operatonClass(String fullQualifiedClassName) {
    element.setOperatonClass(fullQualifiedClassName);
    return myself;
  }

  /**
   * Sets the operaton delegateExpression attribute.
   *
   * @param operatonExpression  the delegateExpression to set
   * @return the builder object
   */
  public B operatonDelegateExpression(String operatonExpression) {
    element.setOperatonDelegateExpression(operatonExpression);
    return myself;
  }

  /**
   * Sets the operaton expression attribute.
   *
   * @param operatonExpression  the expression to set
   * @return the builder object
   */
  public B operatonExpression(String operatonExpression) {
    element.setOperatonExpression(operatonExpression);
    return myself;
  }

  /**
   * Sets the operaton resultVariable attribute.
   *
   * @param operatonResultVariable  the name of the process variable
   * @return the builder object
   */
  public B operatonResultVariable(String operatonResultVariable) {
    element.setOperatonResultVariable(operatonResultVariable);
    return myself;
  }

  /**
   * Sets the operaton topic attribute. This is only meaningful when
   * the {@link #operatonType(String)} attribute has the value <code>external</code>.
   *
   * @param operatonTopic the topic to set
   * @return the builder object
   */
  public B operatonTopic(String operatonTopic) {
    element.setOperatonTopic(operatonTopic);
    return myself;
  }

  /**
   * Sets the operaton type attribute.
   *
   * @param operatonType  the type of the service task
   * @return the builder object
   */
  public B operatonType(String operatonType) {
    element.setOperatonType(operatonType);
    return myself;
  }

  /**
   * Sets the operaton decisionRef attribute.
   *
   * @param operatonDecisionRef the decisionRef to set
   * @return the builder object
   */
  public B operatonDecisionRef(String operatonDecisionRef) {
    element.setOperatonDecisionRef(operatonDecisionRef);
    return myself;
  }

  /**
   * Sets the operaton decisionRefBinding attribute.
   *
   * @param operatonDecisionRefBinding the decisionRefBinding to set
   * @return the builder object
   */
  public B operatonDecisionRefBinding(String operatonDecisionRefBinding) {
    element.setOperatonDecisionRefBinding(operatonDecisionRefBinding);
    return myself;
  }

  /**
   * Sets the operaton decisionRefVersion attribute.
   *
   * @param operatonDecisionRefVersion the decisionRefVersion to set
   * @return the builder object
   */
  public B operatonDecisionRefVersion(String operatonDecisionRefVersion) {
    element.setOperatonDecisionRefVersion(operatonDecisionRefVersion);
    return myself;
  }

  /**
   * Sets the operaton decisionRefVersionTag attribute.
   *
   * @param operatonDecisionRefVersionTag the decisionRefVersionTag to set
   * @return the builder object
   */
  public B operatonDecisionRefVersionTag(String operatonDecisionRefVersionTag) {
    element.setOperatonDecisionRefVersionTag(operatonDecisionRefVersionTag);
    return myself;
  }

  /**
   * Sets the operaton decisionRefTenantId attribute.
   *
   * @param decisionRefTenantId the decisionRefTenantId to set
   * @return the builder object
   */
  public B operatonDecisionRefTenantId(String decisionRefTenantId) {
    element.setOperatonDecisionRefTenantId(decisionRefTenantId);
    return myself;
  }

  /**
   * Set the operaton mapDecisionResult attribute.
   *
   * @param operatonMapDecisionResult the mapper for the decision result to set
   * @return the builder object
   */
  public B operatonMapDecisionResult(String operatonMapDecisionResult) {
    element.setOperatonMapDecisionResult(operatonMapDecisionResult);
    return myself;
  }

  /**
   * Sets the operaton task priority attribute. This is only meaningful when
   * the {@link #operatonType(String)} attribute has the value <code>external</code>.
   *
   *
   * @param taskPriority the priority for the external task
   * @return the builder object
   */
  public B operatonTaskPriority(String taskPriority) {
    element.setOperatonTaskPriority(taskPriority);
    return myself;
  }
}
