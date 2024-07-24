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
import org.operaton.bpm.model.bpmn.instance.ErrorEventDefinition;
import org.operaton.bpm.model.bpmn.instance.ServiceTask;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonErrorEventDefinition;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractServiceTaskBuilder<B extends AbstractServiceTaskBuilder<B>> extends AbstractTaskBuilder<B, ServiceTask> {

  protected AbstractServiceTaskBuilder(BpmnModelInstance modelInstance, ServiceTask element, Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets the implementation of the build service task.
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
   * @return the build object
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
   * Sets the operaton topic attribute and the operaton type attribute to the
   * value <code>external</code. Reduces two calls to {@link #operatonType(String)} and {@link #operatonTopic(String)}.
   *
   * @param operatonTopic the topic to set
   * @return the build object
   */
  public B operatonExternalTask(String operatonTopic) {
    this.operatonType("external");
    this.operatonTopic(operatonTopic);
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

  /**
   * Creates an error event definition for this service task and returns a builder for the error event definition.
   * This is only meaningful when the {@link #operatonType(String)} attribute has the value <code>external</code>.
   *
   * @return the error event definition builder object
   */
  public OperatonErrorEventDefinitionBuilder operatonErrorEventDefinition() {
    ErrorEventDefinition operatonErrorEventDefinition = createInstance(OperatonErrorEventDefinition.class);
    addExtensionElement(operatonErrorEventDefinition);
    return new OperatonErrorEventDefinitionBuilder(modelInstance, operatonErrorEventDefinition);
  }
}
