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
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonInputOutput;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonInputParameter;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonOutputParameter;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractEventBuilder<B extends  AbstractEventBuilder<B, E>, E extends Event> extends AbstractFlowNodeBuilder<B, E> {

  protected AbstractEventBuilder(BpmnModelInstance modelInstance, E element, Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Creates a new operaton input parameter extension element with the
   * given name and value.
   *
   * @param name the name of the input parameter
   * @param value the value of the input parameter
   * @return the builder object
   */
  public B operatonInputParameter(String name, String value) {
    OperatonInputOutput operatonInputOutput = getCreateSingleExtensionElement(OperatonInputOutput.class);

    OperatonInputParameter operatonInputParameter = createChild(operatonInputOutput, OperatonInputParameter.class);
    operatonInputParameter.setOperatonName(name);
    operatonInputParameter.setTextContent(value);

    return myself;
  }

  /**
   * Creates a new operaton output parameter extension element with the
   * given name and value.
   *
   * @param name the name of the output parameter
   * @param value the value of the output parameter
   * @return the builder object
   */
  public B operatonOutputParameter(String name, String value) {
    OperatonInputOutput operatonInputOutput = getCreateSingleExtensionElement(OperatonInputOutput.class);

    OperatonOutputParameter operatonOutputParameter = createChild(operatonInputOutput, OperatonOutputParameter.class);
    operatonOutputParameter.setOperatonName(name);
    operatonOutputParameter.setTextContent(value);

    return myself;
  }

}
