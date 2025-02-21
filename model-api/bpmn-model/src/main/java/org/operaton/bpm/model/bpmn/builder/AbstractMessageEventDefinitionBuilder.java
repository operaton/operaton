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
import org.operaton.bpm.model.bpmn.instance.Event;
import org.operaton.bpm.model.bpmn.instance.MessageEventDefinition;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */

public abstract class AbstractMessageEventDefinitionBuilder<B extends AbstractMessageEventDefinitionBuilder<B>> extends AbstractRootElementBuilder<B, MessageEventDefinition>{

  protected AbstractMessageEventDefinitionBuilder(BpmnModelInstance modelInstance, MessageEventDefinition element, Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets the message attribute.
   *
   * @param message the message for the message event definition
   * @return the builder object
   */
  public B message(String message) {
    element.setMessage(findMessageForName(message));
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
   * Finishes the building of a message event definition.
   *
   * @param <T>
   * @return the parent event builder
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public <T extends AbstractFlowNodeBuilder> T messageEventDefinitionDone() {
    return (T) ((Event) element.getParentElement()).builder();
  }
}
