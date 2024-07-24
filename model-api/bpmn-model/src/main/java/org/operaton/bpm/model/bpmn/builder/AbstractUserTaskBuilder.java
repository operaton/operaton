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

import java.util.List;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.impl.BpmnModelConstants;
import org.operaton.bpm.model.bpmn.instance.TimerEventDefinition;
import org.operaton.bpm.model.bpmn.instance.UserTask;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonFormData;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonFormField;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonTaskListener;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractUserTaskBuilder<B extends AbstractUserTaskBuilder<B>> extends AbstractTaskBuilder<B, UserTask> {

  protected AbstractUserTaskBuilder(BpmnModelInstance modelInstance, UserTask element, Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets the implementation of the build user task.
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
   * Sets the operaton attribute assignee.
   *
   * @param operatonAssignee  the assignee to set
   * @return the builder object
   */
  public B operatonAssignee(String operatonAssignee) {
    element.setOperatonAssignee(operatonAssignee);
    return myself;
  }

  /**
   * Sets the operaton candidate groups attribute.
   *
   * @param operatonCandidateGroups  the candidate groups to set
   * @return the builder object
   */
  public B operatonCandidateGroups(String operatonCandidateGroups) {
    element.setOperatonCandidateGroups(operatonCandidateGroups);
    return myself;
  }

  /**
   * Sets the operaton candidate groups attribute.
   *
   * @param operatonCandidateGroups  the candidate groups to set
   * @return the builder object
   */
  public B operatonCandidateGroups(List<String> operatonCandidateGroups) {
    element.setOperatonCandidateGroupsList(operatonCandidateGroups);
    return myself;
  }

  /**
   * Sets the operaton candidate users attribute.
   *
   * @param operatonCandidateUsers  the candidate users to set
   * @return the builder object
   */
  public B operatonCandidateUsers(String operatonCandidateUsers) {
    element.setOperatonCandidateUsers(operatonCandidateUsers);
    return myself;
  }

  /**
   * Sets the operaton candidate users attribute.
   *
   * @param operatonCandidateUsers  the candidate users to set
   * @return the builder object
   */
  public B operatonCandidateUsers(List<String> operatonCandidateUsers) {
    element.setOperatonCandidateUsersList(operatonCandidateUsers);
    return myself;
  }

  /**
   * Sets the operaton due date attribute.
   *
   * @param operatonDueDate  the due date of the user task
   * @return the builder object
   */
  public B operatonDueDate(String operatonDueDate) {
    element.setOperatonDueDate(operatonDueDate);
    return myself;
  }

  /**
   * Sets the operaton follow up date attribute.
   *
   * @param operatonFollowUpDate  the follow up date of the user task
   * @return the builder object
   */
  public B operatonFollowUpDate(String operatonFollowUpDate) {
    element.setOperatonFollowUpDate(operatonFollowUpDate);
    return myself;
  }

  /**
   * Sets the operaton form handler class attribute.
   *
   * @param operatonFormHandlerClass  the class name of the form handler
   * @return the builder object
   */
  @SuppressWarnings("rawtypes")
  public B operatonFormHandlerClass(Class operatonFormHandlerClass) {
    return operatonFormHandlerClass(operatonFormHandlerClass.getName());
  }

  /**
   * Sets the operaton form handler class attribute.
   *
   * @param operatonFormHandlerClass  the class name of the form handler
   * @return the builder object
   */
  public B operatonFormHandlerClass(String fullQualifiedClassName) {
    element.setOperatonFormHandlerClass(fullQualifiedClassName);
    return myself;
  }

  /**
   * Sets the operaton form key attribute.
   *
   * @param operatonFormKey  the form key to set
   * @return the builder object
   */
  public B operatonFormKey(String operatonFormKey) {
    element.setOperatonFormKey(operatonFormKey);
    return myself;
  }

  /**
   * Sets the operaton form ref attribute.
   *
   * @param operatonFormRef the form ref to set
   * @return the builder object
   */
  public B operatonFormRef(String operatonFormRef) {
    element.setOperatonFormRef(operatonFormRef);
    return myself;
  }

  /**
   * Sets the operaton form ref binding attribute.
   *
   * @param operatonFormRef the form ref binding to set
   * @return the builder object
   */
  public B operatonFormRefBinding(String operatonFormRefBinding) {
    element.setOperatonFormRefBinding(operatonFormRefBinding);
    return myself;
  }

  /**
   * Sets the operaton form ref version attribute.
   *
   * @param operatonFormRef the form ref version to set
   * @return the builder object
   */
  public B operatonFormRefVersion(String operatonFormRefVersion) {
    element.setOperatonFormRefVersion(operatonFormRefVersion);
    return myself;
  }

  /**
   * Sets the operaton priority attribute.
   *
   * @param operatonPriority  the priority of the user task
   * @return the builder object
   */
  public B operatonPriority(String operatonPriority) {
    element.setOperatonPriority(operatonPriority);
    return myself;
  }

  /**
   * Creates a new operaton form field extension element.
   *
   * @return the builder object
   */
  public OperatonUserTaskFormFieldBuilder operatonFormField() {
    OperatonFormData operatonFormData = getCreateSingleExtensionElement(OperatonFormData.class);
    OperatonFormField operatonFormField = createChild(operatonFormData, OperatonFormField.class);
    return new OperatonUserTaskFormFieldBuilder(modelInstance, element, operatonFormField);
  }

  /**
   * Add a class based task listener with specified event name
   *
   * @param eventName - event names to listen to
   * @param fullQualifiedClassName - a string representing a class
   * @return the builder object
   */
  @SuppressWarnings("rawtypes")
  public B operatonTaskListenerClass(String eventName, Class listenerClass) {
    return operatonTaskListenerClass(eventName, listenerClass.getName());
  }

  /**
   * Add a class based task listener with specified event name
   *
   * @param eventName - event names to listen to
   * @param fullQualifiedClassName - a string representing a class
   * @return the builder object
   */
  public B operatonTaskListenerClass(String eventName, String fullQualifiedClassName) {
    OperatonTaskListener executionListener = createInstance(OperatonTaskListener.class);
    executionListener.setOperatonEvent(eventName);
    executionListener.setOperatonClass(fullQualifiedClassName);

    addExtensionElement(executionListener);

    return myself;
  }

  public B operatonTaskListenerExpression(String eventName, String expression) {
    OperatonTaskListener executionListener = createInstance(OperatonTaskListener.class);
    executionListener.setOperatonEvent(eventName);
    executionListener.setOperatonExpression(expression);

    addExtensionElement(executionListener);

    return myself;
  }

  public B operatonTaskListenerDelegateExpression(String eventName, String delegateExpression) {
    OperatonTaskListener executionListener = createInstance(OperatonTaskListener.class);
    executionListener.setOperatonEvent(eventName);
    executionListener.setOperatonDelegateExpression(delegateExpression);

    addExtensionElement(executionListener);

    return myself;
  }

  @SuppressWarnings("rawtypes")
  public B operatonTaskListenerClassTimeoutWithCycle(String id, Class listenerClass, String timerCycle) {
    return operatonTaskListenerClassTimeoutWithCycle(id, listenerClass.getName(), timerCycle);
  }

  @SuppressWarnings("rawtypes")
  public B operatonTaskListenerClassTimeoutWithDate(String id, Class listenerClass, String timerDate) {
    return operatonTaskListenerClassTimeoutWithDate(id, listenerClass.getName(), timerDate);
  }

  @SuppressWarnings("rawtypes")
  public B operatonTaskListenerClassTimeoutWithDuration(String id, Class listenerClass, String timerDuration) {
    return operatonTaskListenerClassTimeoutWithDuration(id, listenerClass.getName(), timerDuration);
  }

  public B operatonTaskListenerClassTimeoutWithCycle(String id, String fullQualifiedClassName, String timerCycle) {
    return createOperatonTaskListenerClassTimeout(id, fullQualifiedClassName, createTimeCycle(timerCycle));
  }

  public B operatonTaskListenerClassTimeoutWithDate(String id, String fullQualifiedClassName, String timerDate) {
    return createOperatonTaskListenerClassTimeout(id, fullQualifiedClassName, createTimeDate(timerDate));
  }

  public B operatonTaskListenerClassTimeoutWithDuration(String id, String fullQualifiedClassName, String timerDuration) {
    return createOperatonTaskListenerClassTimeout(id, fullQualifiedClassName, createTimeDuration(timerDuration));
  }

  public B operatonTaskListenerExpressionTimeoutWithCycle(String id, String expression, String timerCycle) {
    return createOperatonTaskListenerExpressionTimeout(id, expression, createTimeCycle(timerCycle));
  }

  public B operatonTaskListenerExpressionTimeoutWithDate(String id, String expression, String timerDate) {
    return createOperatonTaskListenerExpressionTimeout(id, expression, createTimeDate(timerDate));
  }

  public B operatonTaskListenerExpressionTimeoutWithDuration(String id, String expression, String timerDuration) {
    return createOperatonTaskListenerExpressionTimeout(id, expression, createTimeDuration(timerDuration));
  }

  public B operatonTaskListenerDelegateExpressionTimeoutWithCycle(String id, String delegateExpression, String timerCycle) {
    return createOperatonTaskListenerDelegateExpressionTimeout(id, delegateExpression, createTimeCycle(timerCycle));
  }

  public B operatonTaskListenerDelegateExpressionTimeoutWithDate(String id, String delegateExpression, String timerDate) {
    return createOperatonTaskListenerDelegateExpressionTimeout(id, delegateExpression, createTimeDate(timerDate));
  }

  public B operatonTaskListenerDelegateExpressionTimeoutWithDuration(String id, String delegateExpression, String timerDuration) {
    return createOperatonTaskListenerDelegateExpressionTimeout(id, delegateExpression, createTimeDuration(timerDuration));
  }

  protected B createOperatonTaskListenerClassTimeout(String id, String fullQualifiedClassName, TimerEventDefinition timerDefinition) {
    OperatonTaskListener executionListener = createOperatonTaskListenerTimeout(id, timerDefinition);
    executionListener.setOperatonClass(fullQualifiedClassName);
    return myself;
  }

  protected B createOperatonTaskListenerExpressionTimeout(String id, String expression, TimerEventDefinition timerDefinition) {
    OperatonTaskListener executionListener = createOperatonTaskListenerTimeout(id, timerDefinition);
    executionListener.setOperatonExpression(expression);
    return myself;
  }

  protected B createOperatonTaskListenerDelegateExpressionTimeout(String id, String delegateExpression, TimerEventDefinition timerDefinition) {
    OperatonTaskListener executionListener = createOperatonTaskListenerTimeout(id, timerDefinition);
    executionListener.setOperatonDelegateExpression(delegateExpression);
    return myself;
  }

  protected OperatonTaskListener createOperatonTaskListenerTimeout(String id, TimerEventDefinition timerDefinition) {
    OperatonTaskListener executionListener = createInstance(OperatonTaskListener.class);
    executionListener.setAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_ID, id, true);
    executionListener.setOperatonEvent("timeout");
    executionListener.addChildElement(timerDefinition);
    addExtensionElement(executionListener);
    return executionListener;
  }
}
