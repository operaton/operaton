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
package org.operaton.bpm.engine.impl.dmn.invocation;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.delegate.DelegateInvocation;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionEntity;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.variable.context.VariableContext;

/**
 * {@link DelegateInvocation} invoking a {@link DecisionDefinition}
 * in a given {@link VariableContext}.
 *
 * <p>
 * The DmnEngine instance is resolved from the Context.
 * </p>
 *
 * <p>
 * The invocation result is a {@link DmnDecisionResult}.
 * </p>
 *
 * <p>
 * The target of the invocation is the {@link DecisionDefinition}.
 * </p>
 *
 * @author Daniel Meyer
 *
 */
public class DecisionInvocation extends DelegateInvocation {

  protected DecisionDefinition decisionDefinition;
  protected VariableContext variableContext;

  public DecisionInvocation(DecisionDefinition decisionDefinition, VariableContext variableContext) {
    super(null, (DecisionDefinitionEntity) decisionDefinition);
    this.decisionDefinition = decisionDefinition;
    this.variableContext = variableContext;
  }

  @Override
  protected void invoke() {
    final DmnEngine dmnEngine = Context.getProcessEngineConfiguration()
      .getDmnEngine();

    invocationResult = dmnEngine.evaluateDecision((DmnDecision) decisionDefinition, variableContext);
  }

  @Override
  public DmnDecisionResult getInvocationResult() {
    return (DmnDecisionResult) super.getInvocationResult();
  }

  public DecisionDefinition getDecisionDefinition() {
    return decisionDefinition;
  }

}
