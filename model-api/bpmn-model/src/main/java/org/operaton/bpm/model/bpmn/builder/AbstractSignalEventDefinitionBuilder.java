/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.model.bpmn.builder;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.SignalEventDefinition;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonIn;

/**
 * @author Nikola Koevski
 */
public abstract class AbstractSignalEventDefinitionBuilder<B extends AbstractSignalEventDefinitionBuilder<B>> extends AbstractRootElementBuilder<B, SignalEventDefinition> {

  protected AbstractSignalEventDefinitionBuilder(BpmnModelInstance modelInstance, SignalEventDefinition element, Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets a "operaton:in" parameter to pass a variable from the signal-throwing
   * process instance to the signal-catching process instance
   *
   * @param source the name of the variable in the signal-throwing process instance
   * @param target the name of the variable in the signal-catching process instance
   * @return the builder object
   */
  public B operatonInSourceTarget(String source, String target) {
    OperatonIn param = modelInstance.newInstance(OperatonIn.class);

    param.setOperatonSource(source);
    param.setOperatonTarget(target);

    addExtensionElement(param);

    return myself;
  }

  /**
   * Sets a "operaton:in" parameter to pass an expression from the signal-throwing
   * process instance to a variable in the signal-catching process instance
   *
   * @param sourceExpression the expression in the signal-throwing process instance
   * @param target the name of the variable in the signal-catching process instance
   * @return the builder object
   */
  public B operatonInSourceExpressionTarget(String sourceExpression, String target) {
    OperatonIn param = modelInstance.newInstance(OperatonIn.class);

    param.setOperatonSourceExpression(sourceExpression);
    param.setOperatonTarget(target);

    addExtensionElement(param);

    return myself;
  }

  /**
   * Sets a "operaton:in" parameter to pass the business key from the signal-throwing
   * process instance to the signal-catching process instance
   *
   * @param businessKey the business key string or expression of the signal-throwing process instance
   * @return the builder object
   */
  public B operatonInBusinessKey(String businessKey) {
    OperatonIn param = modelInstance.newInstance(OperatonIn.class);

    param.setOperatonBusinessKey(businessKey);

    addExtensionElement(param);

    return myself;
  }

  /**
   * Sets a "operaton:in" parameter to pass all the process variables of the
   * signal-throwing process instance to the signal-catching process instance
   *
   * @param variables a String flag to declare that all of the signal-throwing process-instance variables should be passed
   * @param local a Boolean flag to declare that only the local variables should be passed
   * @return the builder object
   */
  public B operatonInAllVariables(String variables, boolean local) {
    OperatonIn param = modelInstance.newInstance(OperatonIn.class);

    param.setOperatonVariables(variables);

    if (local) {
      param.setOperatonLocal(local);
    }

    addExtensionElement(param);

    return myself;
  }

  /**
   * Sets a "operaton:in" parameter to pass all the process variables of the
   * signal-throwing process instance to the signal-catching process instance
   *
   * @param variables a String flag to declare that all of the signal-throwing process-instance variables should be passed
   * @return the builder object
   */
  public B operatonInAllVariables(String variables) {
    return operatonInAllVariables(variables, false);
  }
}
