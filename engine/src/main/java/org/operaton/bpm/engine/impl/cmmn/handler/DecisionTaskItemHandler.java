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
package org.operaton.bpm.engine.impl.cmmn.handler;

import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.behavior.DmnDecisionTaskActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.core.model.BaseCallableElement;
import org.operaton.bpm.engine.impl.dmn.result.DecisionResultMapper;
import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.cmmn.instance.DecisionRefExpression;
import org.operaton.bpm.model.cmmn.instance.DecisionTask;

import static org.operaton.bpm.engine.impl.util.DecisionEvaluationUtil.getDecisionResultMapperForName;


/**
 * @author Roman Smirnov
 *
 */
public class DecisionTaskItemHandler extends CallingTaskItemHandler {

  @Override
  protected void initializeActivity(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context) {
    super.initializeActivity(element, activity, context);

    initializeResultVariable(element, activity, context);

    initializeDecisionTableResultMapper(element, activity, context);
  }

  @SuppressWarnings("unused")
  protected void initializeResultVariable(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context) {
    DecisionTask decisionTask = getDefinition(element);
    DmnDecisionTaskActivityBehavior behavior = getActivityBehavior(activity);
    String resultVariable = decisionTask.getOperatonResultVariable();
    behavior.setResultVariable(resultVariable);
  }

  @SuppressWarnings("unused")
  protected void initializeDecisionTableResultMapper(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context) {
    DecisionTask decisionTask = getDefinition(element);
    DmnDecisionTaskActivityBehavior behavior = getActivityBehavior(activity);
    String mapper = decisionTask.getOperatonMapDecisionResult();
    DecisionResultMapper decisionResultMapper = getDecisionResultMapperForName(mapper);
    behavior.setDecisionTableResultMapper(decisionResultMapper);
  }

  @Override
  protected BaseCallableElement createCallableElement() {
    return new BaseCallableElement();
  }

  @Override
  protected CmmnActivityBehavior getActivityBehavior() {
    return new DmnDecisionTaskActivityBehavior();
  }

  protected DmnDecisionTaskActivityBehavior getActivityBehavior(CmmnActivity activity) {
    return (DmnDecisionTaskActivityBehavior) activity.getActivityBehavior();
  }

  @Override
  protected String getDefinitionKey(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context) {
    DecisionTask definition = getDefinition(element);
    String decision = definition.getDecision();

    if (decision == null) {
      DecisionRefExpression decisionExpression = definition.getDecisionExpression();
      if (decisionExpression != null) {
        decision = decisionExpression.getText();
      }
    }

    return decision;
  }

  @Override
  protected String getBinding(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context) {
    DecisionTask definition = getDefinition(element);
    return definition.getOperatonDecisionBinding();
  }

  @Override
  protected String getVersion(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context) {
    DecisionTask definition = getDefinition(element);
    return definition.getOperatonDecisionVersion();
  }

  @Override
  protected String getTenantId(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context) {
    DecisionTask definition = getDefinition(element);
    return definition.getOperatonDecisionTenantId();
  }


  @Override
  protected DecisionTask getDefinition(CmmnElement element) {
    return (DecisionTask) super.getDefinition(element);
  }

}
