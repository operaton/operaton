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
import org.operaton.bpm.model.bpmn.instance.CallActivity;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonIn;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonOut;

/**
 * @author Sebastian Menski
 */
public class AbstractCallActivityBuilder<B extends AbstractCallActivityBuilder<B>> extends AbstractActivityBuilder<B, CallActivity> {

  protected AbstractCallActivityBuilder(BpmnModelInstance modelInstance, CallActivity element, Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets the called element
   *
   * @param calledElement  the process to call
   * @return the builder object
   */
  public B calledElement(String calledElement) {
    element.setCalledElement(calledElement);
    return myself;
  }

  /** operaton extensions */

  /**
   * @deprecated use operatonAsyncBefore() instead.
   *
   * Sets the operaton async attribute to true.
   *
   * @return the builder object
   */
  @Deprecated
  public B operatonAsync() {
    element.setOperatonAsyncBefore(true);
    return myself;
  }

  /**
   * @deprecated use operatonAsyncBefore(isOperatonAsyncBefore) instead
   *
   * Sets the operaton async attribute.
   *
   * @param isOperatonAsync  the async state of the task
   * @return the builder object
   */
  @Deprecated
  public B operatonAsync(boolean isOperatonAsync) {
    element.setOperatonAsyncBefore(isOperatonAsync);
    return myself;
  }

  /**
   * Sets the operaton calledElementBinding attribute
   *
   * @param operatonCalledElementBinding  the element binding to use
   * @return the builder object
   */
  public B operatonCalledElementBinding(String operatonCalledElementBinding) {
    element.setOperatonCalledElementBinding(operatonCalledElementBinding);
    return myself;
  }

  /**
   * Sets the operaton calledElementVersion attribute
   *
   * @param operatonCalledElementVersion  the element version to use
   * @return the builder object
   */
  public B operatonCalledElementVersion(String operatonCalledElementVersion) {
    element.setOperatonCalledElementVersion(operatonCalledElementVersion);
    return myself;
  }

  /**
   * Sets the operaton calledElementVersionTag attribute
   *
   * @param operatonCalledElementVersionTag  the element version to use
   * @return the builder object
   */
  public B operatonCalledElementVersionTag(String operatonCalledElementVersionTag) {
    element.setOperatonCalledElementVersionTag(operatonCalledElementVersionTag);
    return myself;
  }

  /**
   * Sets the operaton calledElementTenantId attribute
   * @param operatonCalledElementTenantId the called element tenant id
   * @return the builder object
   */
  public B operatonCalledElementTenantId(String operatonCalledElementTenantId) {
    element.setOperatonCalledElementTenantId(operatonCalledElementTenantId);
    return myself;
  }

  /**
   * Sets the operaton caseRef attribute
   *
   * @param caseRef the case to call
   * @return the builder object
   */
  public B operatonCaseRef(String caseRef) {
    element.setOperatonCaseRef(caseRef);
    return myself;
  }

  /**
   * Sets the operaton caseBinding attribute
   *
   * @param operatonCaseBinding  the case binding to use
   * @return the builder object
   */
  public B operatonCaseBinding(String operatonCaseBinding) {
    element.setOperatonCaseBinding(operatonCaseBinding);
    return myself;
  }

  /**
   * Sets the operaton caseVersion attribute
   *
   * @param operatonCaseVersion  the case version to use
   * @return the builder object
   */
  public B operatonCaseVersion(String operatonCaseVersion) {
    element.setOperatonCaseVersion(operatonCaseVersion);
    return myself;
  }

  /**
   * Sets the caseTenantId
   * @param tenantId the tenant id to set
   * @return the builder object
   */
  public B operatonCaseTenantId(String tenantId) {
    element.setOperatonCaseTenantId(tenantId);
    return myself;
  }

  /**
   * Sets a "operaton in" parameter to pass a business key from the super process instance to the sub process instance
   * @param businessKey the business key to set
   * @return the builder object
   */
  public B operatonInBusinessKey(String businessKey) {
    OperatonIn param = modelInstance.newInstance(OperatonIn.class);
    param.setOperatonBusinessKey(businessKey);
    addExtensionElement(param);
    return myself;
  }

  /**
   * Sets a "operaton in" parameter to pass a variable from the super process instance to the sub process instance
   *
   * @param source the name of variable in the super process instance
   * @param target the name of the variable in the sub process instance
   * @return the builder object
   */
  public B operatonIn(String source, String target) {
    OperatonIn param = modelInstance.newInstance(OperatonIn.class);
    param.setOperatonSource(source);
    param.setOperatonTarget(target);
    addExtensionElement(param);
    return myself;
  }

  /**
   * Sets a "operaton out" parameter to pass a variable from a sub process instance to the super process instance
   *
   * @param source the name of variable in the sub process instance
   * @param target the name of the variable in the super process instance
   * @return the builder object
   */
  public B operatonOut(String source, String target) {
    OperatonOut param = modelInstance.newInstance(OperatonOut.class);
    param.setOperatonSource(source);
    param.setOperatonTarget(target);
    addExtensionElement(param);
    return myself;
  }

  /**
   * Sets the operaton variableMappingClass attribute. It references on a class which implements the
   * {@link DelegateVariableMapping} interface.
   * Is used to delegate the variable in- and output mapping to the given class.
   *
   * @param operatonVariableMappingClass                  the class name to set
   * @return                              the builder object
   */
  @SuppressWarnings("rawtypes")
  public B operatonVariableMappingClass(Class operatonVariableMappingClass) {
    return operatonVariableMappingClass(operatonVariableMappingClass.getName());
  }

  /**
   * Sets the operaton variableMappingClass attribute. It references on a class which implements the
   * {@link DelegateVariableMapping} interface.
   * Is used to delegate the variable in- and output mapping to the given class.
   *
   * @param operatonVariableMappingClass                  the class name to set
   * @return                              the builder object
   */
  public B operatonVariableMappingClass(String fullQualifiedClassName) {
    element.setOperatonVariableMappingClass(fullQualifiedClassName);
    return myself;
  }

  /**
   * Sets the operaton variableMappingDelegateExpression attribute. The expression when is resolved
   * references to an object of a class, which implements the {@link DelegateVariableMapping} interface.
   * Is used to delegate the variable in- and output mapping to the given class.
   *
   * @param operatonVariableMappingDelegateExpression     the expression which references a delegate object
   * @return                              the builder object
   */
  public B operatonVariableMappingDelegateExpression(String operatonVariableMappingDelegateExpression) {
    element.setOperatonVariableMappingDelegateExpression(operatonVariableMappingDelegateExpression);
    return myself;
  }
}
