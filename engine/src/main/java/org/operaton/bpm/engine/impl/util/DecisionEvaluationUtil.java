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
package org.operaton.bpm.engine.impl.util;

import org.jspecify.annotations.Nullable;

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableResultImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.core.model.BaseCallableElement;
import org.operaton.bpm.engine.impl.core.variable.scope.AbstractVariableScope;
import org.operaton.bpm.engine.impl.dmn.invocation.DecisionInvocation;
import org.operaton.bpm.engine.impl.dmn.invocation.VariableScopeContext;
import org.operaton.bpm.engine.impl.dmn.result.CollectEntriesDecisionResultMapper;
import org.operaton.bpm.engine.impl.dmn.result.DecisionResultMapper;
import org.operaton.bpm.engine.impl.dmn.result.ResultListDecisionTableResultMapper;
import org.operaton.bpm.engine.impl.dmn.result.SingleEntryDecisionResultMapper;
import org.operaton.bpm.engine.impl.dmn.result.SingleResultDecisionResultMapper;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static java.util.Objects.requireNonNull;

/**
 * @author Roman Smirnov
 *
 */
public final class DecisionEvaluationUtil {

  public static final String DECISION_RESULT_VARIABLE = "decisionResult";

  private DecisionEvaluationUtil() {
  }

  public static @Nullable DecisionResultMapper getDecisionResultMapperForName(@Nullable String mapDecisionResult) {
    if ("singleEntry".equals(mapDecisionResult)) {
      return new SingleEntryDecisionResultMapper();

    }
    else if ("singleResult".equals(mapDecisionResult)) {
      return new SingleResultDecisionResultMapper();

    }
    else if ("collectEntries".equals(mapDecisionResult)) {
      return new CollectEntriesDecisionResultMapper();

    }
    else if ("resultList".equals(mapDecisionResult) || mapDecisionResult == null) {
      return new ResultListDecisionTableResultMapper();

    }
    else {
      return null;
    }
  }

  public static void evaluateDecision(AbstractVariableScope execution,
      String defaultTenantId,
      BaseCallableElement callableElement,
      @Nullable String resultVariable,
      @Nullable DecisionResultMapper decisionResultMapper) throws Exception {

    DecisionDefinition decisionDefinition = resolveDecisionDefinition(callableElement, execution, defaultTenantId);
    EnsureUtil.ensureNotNull("No decision definition found for " + callableElement, "decisionDefinition", decisionDefinition);
    DecisionInvocation invocation = createInvocation(decisionDefinition, execution);

    invoke(invocation);

    DmnDecisionResult result = invocation.getInvocationResult();
    if (result != null) {
      TypedValue typedValue = Variables.untypedValue(result, true);
      execution.setVariableLocal(DECISION_RESULT_VARIABLE, typedValue);

      if (resultVariable != null && decisionResultMapper != null) {
        Object mappedDecisionResult = decisionResultMapper.mapDecisionResult(result);
        execution.setVariable(resultVariable, mappedDecisionResult);
      }
    }
  }

  public static @Nullable DmnDecisionResult evaluateDecision(DecisionDefinition decisionDefinition, VariableMap variables) throws Exception {
    DecisionInvocation invocation = createInvocation(decisionDefinition, variables);
    invoke(invocation);
    return invocation.getInvocationResult();
  }

  public static DmnDecisionTableResult evaluateDecisionTable(DecisionDefinition decisionDefinition, VariableMap variables) throws Exception {
    // doesn't throw an exception if the decision definition is not implemented as decision table
    DmnDecisionResult decisionResult = evaluateDecision(decisionDefinition, variables);
    EnsureUtil.ensureNotNull(DECISION_RESULT_VARIABLE, decisionResult);
    requireNonNull(decisionResult);
    return DmnDecisionTableResultImpl.wrap(decisionResult);
  }

  private static void invoke(DecisionInvocation invocation) throws Exception {
    Context.getProcessEngineConfiguration()
      .getDelegateInterceptor()
      .handleInvocation(invocation);
  }

  private static DecisionInvocation createInvocation(DecisionDefinition decisionDefinition, VariableMap variables) {
    return createInvocation(decisionDefinition, variables.asVariableContext());
  }

  private static DecisionInvocation createInvocation(DecisionDefinition decisionDefinition, AbstractVariableScope variableScope) {
    return createInvocation(decisionDefinition, VariableScopeContext.wrap(variableScope));
  }

  private static DecisionInvocation createInvocation(DecisionDefinition decisionDefinition, VariableContext variableContext) {
    return new DecisionInvocation(decisionDefinition, variableContext);
  }

  private static @Nullable DecisionDefinition resolveDecisionDefinition(BaseCallableElement callableElement, AbstractVariableScope execution, String defaultTenantId) {
    return CallableElementUtil.getDecisionDefinitionToCall(execution, defaultTenantId, callableElement);
  }
}
