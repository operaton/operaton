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
import org.operaton.bpm.model.bpmn.instance.CompensateEventDefinition;
import org.operaton.bpm.model.bpmn.instance.ErrorEventDefinition;
import org.operaton.bpm.model.bpmn.instance.EscalationEventDefinition;
import org.operaton.bpm.model.bpmn.instance.StartEvent;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonFormData;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonFormField;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractStartEventBuilder<B extends AbstractStartEventBuilder<B>> extends AbstractCatchEventBuilder<B, StartEvent> {

  protected AbstractStartEventBuilder(BpmnModelInstance modelInstance, StartEvent element, Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /** operaton extensions */

  /**
   * Sets the operaton async attribute to true.
   *
   * @deprecated Use {@link #operatonAsyncBefore()} instead.
   * @return the builder object
   */
  @Deprecated(forRemoval = true, since = "1.0.0-beta-1")
  public B operatonAsync() {
    element.setOperatonAsyncBefore(true);
    return myself;
  }

  /**
   * Sets the operaton async attribute.
   *
   * @deprecated Use {@link #operatonAsyncBefore(boolean)} instead.
   * @param isOperatonAsync the async state of the task
   * @return the builder object
   */
  @Deprecated(forRemoval = true, since = "1.0.0-beta-1")
  public B operatonAsync(boolean isOperatonAsync) {
    element.setOperatonAsyncBefore(isOperatonAsync);
    return myself;
  }

  /**
   * Sets the operaton form handler class attribute.
   *
   * @param operatonFormHandlerClass  the class name of the form handler
   * @return the builder object
   */
  public B operatonFormHandlerClass(String operatonFormHandlerClass) {
    element.setOperatonFormHandlerClass(operatonFormHandlerClass);
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
   * Sets the operaton initiator attribute.
   *
   * @param operatonInitiator  the initiator to set
   * @return the builder object
   */
  public B operatonInitiator(String operatonInitiator) {
    element.setOperatonInitiator(operatonInitiator);
    return myself;
  }

  /**
   * Creates a new operaton form field extension element.
   *
   * @return the builder object
   */
  public OperatonStartEventFormFieldBuilder operatonFormField() {
    OperatonFormData operatonFormData = getCreateSingleExtensionElement(OperatonFormData.class);
    OperatonFormField operatonFormField = createChild(operatonFormData, OperatonFormField.class);
    return new OperatonStartEventFormFieldBuilder(modelInstance, element, operatonFormField);
  }

  /**
   * Sets a catch all error definition.
   *
   * @return the builder object
   */
  public B error() {
    ErrorEventDefinition errorEventDefinition = createInstance(ErrorEventDefinition.class);
    element.getEventDefinitions().add(errorEventDefinition);

    return myself;
  }

  /**
   * Sets an error definition for the given error code. If already an error
   * with this code exists it will be used, otherwise a new error is created.
   *
   * @param errorCode the code of the error
   * @return the builder object
   */
  public B error(String errorCode) {
    return error(errorCode, null);
  }

  /**
   * Sets an error definition for the given error code. If already an error
   * with this code exists it will be used, otherwise a new error is created
   * with the given errorMessage.
   *
   * @param errorCode the code of the error
   * @param errorMessage the error message that is used when a new error needs
   *        to be created
   * @return the builder object
   */
  public B error(String errorCode, String errorMessage) {
    ErrorEventDefinition errorEventDefinition = createErrorEventDefinition(errorCode, errorMessage);
    element.getEventDefinitions().add(errorEventDefinition);

    return myself;
  }

  /**
   * Creates an error event definition with an unique id
   * and returns a builder for the error event definition.
   *
   * @return the error event definition builder object
   */
  public ErrorEventDefinitionBuilder errorEventDefinition(String id) {
    ErrorEventDefinition errorEventDefinition = createEmptyErrorEventDefinition();
    if (id != null) {
      errorEventDefinition.setId(id);
    }

    element.getEventDefinitions().add(errorEventDefinition);
    return new ErrorEventDefinitionBuilder(modelInstance, errorEventDefinition);
  }

  /**
   * Creates an error event definition
   * and returns a builder for the error event definition.
   *
   * @return the error event definition builder object
   */
  public ErrorEventDefinitionBuilder errorEventDefinition() {
    ErrorEventDefinition errorEventDefinition = createEmptyErrorEventDefinition();
    element.getEventDefinitions().add(errorEventDefinition);
    return new ErrorEventDefinitionBuilder(modelInstance, errorEventDefinition);
  }

  /**
   * Sets a catch all escalation definition.
   *
   * @return the builder object
   */
  public B escalation() {
    EscalationEventDefinition escalationEventDefinition = createInstance(EscalationEventDefinition.class);
    element.getEventDefinitions().add(escalationEventDefinition);

    return myself;
  }

  /**
   * Sets an escalation definition for the given escalation code. If already an escalation
   * with this code exists it will be used, otherwise a new escalation is created.
   *
   * @param escalationCode the code of the escalation
   * @return the builder object
   */
  public B escalation(String escalationCode) {
    EscalationEventDefinition escalationEventDefinition = createEscalationEventDefinition(escalationCode);
    element.getEventDefinitions().add(escalationEventDefinition);

    return myself;
  }

  /**
   * Sets a catch compensation definition.
   *
   * @return the builder object
   */
  public B compensation() {
    CompensateEventDefinition compensateEventDefinition = createCompensateEventDefinition();
    element.getEventDefinitions().add(compensateEventDefinition);

    return myself;
  }

  /**
   * Sets whether the start event is interrupting or not.
   */
  public B interrupting(boolean interrupting) {
    element.setInterrupting(interrupting);

    return myself;
  }

}
