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
package org.operaton.bpm.engine.impl.cmmn.handler;

import java.util.List;

import org.operaton.bpm.engine.impl.cmmn.behavior.ProcessOrCaseTaskActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.core.model.CallableElement;
import org.operaton.bpm.engine.impl.core.model.CallableElementParameter;
import org.operaton.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;
import org.operaton.bpm.engine.impl.el.ExpressionManager;
import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.cmmn.instance.PlanItemDefinition;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonIn;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonOut;

/**
 * @author Roman Smirnov
 *
 */
public abstract class ProcessOrCaseTaskItemHandler extends CallingTaskItemHandler {

  @Override
  protected CallableElement createCallableElement() {
    return new CallableElement();
  }

  @Override
  protected void initializeCallableElement(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context) {
    super.initializeCallableElement(element, activity, context);

    ProcessOrCaseTaskActivityBehavior behavior = (ProcessOrCaseTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();

    // inputs
    initializeInputParameter(element, activity, context, callableElement);

    // outputs
    initializeOutputParameter(element, activity, context, callableElement);
  }

  protected void initializeInputParameter(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context, CallableElement callableElement) {
    ExpressionManager expressionManager = context.getExpressionManager();

    List<OperatonIn> inputs = getInputs(element);

    for (OperatonIn input : inputs) {

      // businessKey
      String businessKey = input.getOperatonBusinessKey();
      if (businessKey != null && !businessKey.isEmpty()) {
        ParameterValueProvider businessKeyValueProvider = createParameterValueProvider(businessKey, expressionManager);
        callableElement.setBusinessKeyValueProvider(businessKeyValueProvider);

      } else {
        // create new parameter
        CallableElementParameter parameter = new CallableElementParameter();
        callableElement.addInput(parameter);

        if (input.getOperatonLocal()) {
          parameter.setReadLocal(true);
        }

        // all variables
        String variables = input.getOperatonVariables();
        if ("all".equals(variables)) {
          parameter.setAllVariables(true);
          continue;
        }

        // source/sourceExpression
        String source = input.getOperatonSource();
        if (source == null || source.isEmpty()) {
          source = input.getOperatonSourceExpression();
        }

        ParameterValueProvider sourceValueProvider = createParameterValueProvider(source, expressionManager);
        parameter.setSourceValueProvider(sourceValueProvider);

        // target
        String target = input.getOperatonTarget();
        parameter.setTarget(target);
      }
    }
  }

  protected void initializeOutputParameter(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context, CallableElement callableElement) {
    ExpressionManager expressionManager = context.getExpressionManager();

    List<OperatonOut> outputs = getOutputs(element);

    for (OperatonOut output : outputs) {

      // create new parameter
      CallableElementParameter parameter = new CallableElementParameter();
      callableElement.addOutput(parameter);

      // all variables
      String variables = output.getOperatonVariables();
      if ("all".equals(variables)) {
        parameter.setAllVariables(true);
        continue;
      }

      // source/sourceExpression
      String source = output.getOperatonSource();
      if (source == null || source.isEmpty()) {
        source = output.getOperatonSourceExpression();
      }

      ParameterValueProvider sourceValueProvider = createParameterValueProvider(source, expressionManager);
      parameter.setSourceValueProvider(sourceValueProvider);

      // target
      String target = output.getOperatonTarget();
      parameter.setTarget(target);

    }
  }

  protected List<OperatonIn> getInputs(CmmnElement element) {
    PlanItemDefinition definition = getDefinition(element);
    return queryExtensionElementsByClass(definition, OperatonIn.class);
  }

  protected List<OperatonOut> getOutputs(CmmnElement element) {
    PlanItemDefinition definition = getDefinition(element);
    return queryExtensionElementsByClass(definition, OperatonOut.class);
  }
}
