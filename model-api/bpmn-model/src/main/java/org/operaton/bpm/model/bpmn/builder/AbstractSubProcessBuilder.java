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
import org.operaton.bpm.model.bpmn.instance.SubProcess;

/**
 * @author Sebastian Menski
 */
public class AbstractSubProcessBuilder<B extends AbstractSubProcessBuilder<B>> extends  AbstractActivityBuilder<B, SubProcess> {

  protected AbstractSubProcessBuilder(BpmnModelInstance modelInstance, SubProcess element, Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  public EmbeddedSubProcessBuilder embeddedSubProcess() {
    return new EmbeddedSubProcessBuilder(this);
  }

  /**
   * Sets the sub process to be triggered by an event.
   *
   * @return  the builder object
   */
  public B triggerByEvent() {
    element.setTriggeredByEvent(true);
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
  @Deprecated(forRemoval = true)
  public B operatonAsync() {
    element.setOperatonAsyncBefore(true);
    return myself;
  }

  /**
   * @deprecated use operatonAsyncBefore(isOperatonAsyncBefore) instead.
   *
   * Sets the operaton async attribute.
   *
   * @param isOperatonAsync  the async state of the task
   * @return the builder object
   */
  @Deprecated(forRemoval = true)
  public B operatonAsync(boolean isOperatonAsync) {
    element.setOperatonAsyncBefore(isOperatonAsync);
    return myself;
  }

}
