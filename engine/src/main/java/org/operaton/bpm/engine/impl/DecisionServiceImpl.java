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
package org.operaton.bpm.engine.impl;

import java.util.Map;

import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.dmn.DecisionEvaluationBuilder;
import org.operaton.bpm.engine.dmn.DecisionsEvaluationBuilder;
import org.operaton.bpm.engine.impl.dmn.DecisionEvaluationBuilderImpl;
import org.operaton.bpm.engine.impl.dmn.DecisionTableEvaluationBuilderImpl;

/**
 * @author Philipp Ossler
 */
public class DecisionServiceImpl extends ServiceImpl implements DecisionService {

  @Override
  public DmnDecisionTableResult evaluateDecisionTableById(String decisionDefinitionId, Map<String, Object> variables) {
    return evaluateDecisionTableById(decisionDefinitionId)
        .variables(variables)
        .evaluate();
  }

  @Override
  public DmnDecisionTableResult evaluateDecisionTableByKey(String decisionDefinitionKey, Map<String, Object> variables) {
    return evaluateDecisionTableByKey(decisionDefinitionKey)
        .variables(variables)
        .evaluate();
  }

  @Override
  public DmnDecisionTableResult evaluateDecisionTableByKeyAndVersion(String decisionDefinitionKey, Integer version, Map<String, Object> variables) {
    return evaluateDecisionTableByKey(decisionDefinitionKey)
        .version(version)
        .variables(variables)
        .evaluate();
  }

  @Override
  public DecisionEvaluationBuilder evaluateDecisionTableByKey(String decisionDefinitionKey) {
    return DecisionTableEvaluationBuilderImpl.evaluateDecisionTableByKey(commandExecutor, decisionDefinitionKey);
  }

  @Override
  public DecisionEvaluationBuilder evaluateDecisionTableById(String decisionDefinitionId) {
    return DecisionTableEvaluationBuilderImpl.evaluateDecisionTableById(commandExecutor, decisionDefinitionId);
  }

  @Override
  public DecisionsEvaluationBuilder evaluateDecisionByKey(String decisionDefinitionKey) {
    return DecisionEvaluationBuilderImpl.evaluateDecisionByKey(commandExecutor, decisionDefinitionKey);
  }

  @Override
  public DecisionsEvaluationBuilder evaluateDecisionById(String decisionDefinitionId) {
    return DecisionEvaluationBuilderImpl.evaluateDecisionById(commandExecutor, decisionDefinitionId);
  }

}
