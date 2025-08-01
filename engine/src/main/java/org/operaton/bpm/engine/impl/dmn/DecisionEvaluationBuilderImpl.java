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
package org.operaton.bpm.engine.impl.dmn;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureOnlyOneNotNull;

import java.util.Map;

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.engine.dmn.DecisionsEvaluationBuilder;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.exception.dmn.DecisionDefinitionNotFoundException;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.dmn.cmd.EvaluateDecisionCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

public class DecisionEvaluationBuilderImpl implements DecisionsEvaluationBuilder {

  private static final DecisionLogger LOG = ProcessEngineLogger.DECISION_LOGGER;

  protected CommandExecutor commandExecutor;

  protected String decisionDefinitionKey;
  protected String decisionDefinitionId;

  protected Integer version;
  protected Map<String, Object> variables;

  protected String decisionDefinitionTenantId;
  protected boolean isTenantIdSet;


  public DecisionEvaluationBuilderImpl(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  @Override
  public DecisionsEvaluationBuilder variables(Map<String, Object> variables) {
    this.variables = variables;
    return this;
  }

  @Override
  public DecisionsEvaluationBuilder version(Integer version) {
    this.version = version;
    return this;
  }

  @Override
  public DecisionsEvaluationBuilder decisionDefinitionTenantId(String tenantId) {
    this.decisionDefinitionTenantId = tenantId;
    isTenantIdSet = true;
    return this;
  }

  @Override
  public DecisionsEvaluationBuilder decisionDefinitionWithoutTenantId() {
    this.decisionDefinitionTenantId = null;
    isTenantIdSet = true;
    return this;
  }

  @Override
  public DmnDecisionResult evaluate() {
     ensureOnlyOneNotNull(NotValidException.class, "either decision definition id or key must be set", decisionDefinitionId, decisionDefinitionKey);

     if (isTenantIdSet && decisionDefinitionId != null) {
       throw LOG.exceptionEvaluateDecisionDefinitionByIdAndTenantId();
     }

    try {
      return commandExecutor.execute(new EvaluateDecisionCmd(this));
    }
    catch (NullValueException e) {
      throw new NotValidException(e.getMessage(), e);
    }
    catch (DecisionDefinitionNotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    }
  }

  public static DecisionsEvaluationBuilder evaluateDecisionByKey(CommandExecutor commandExecutor, String decisionDefinitionKey) {
    DecisionEvaluationBuilderImpl builder = new DecisionEvaluationBuilderImpl(commandExecutor);
    builder.decisionDefinitionKey = decisionDefinitionKey;
    return builder;
  }

  public static DecisionsEvaluationBuilder evaluateDecisionById(CommandExecutor commandExecutor, String decisionDefinitionId) {
    DecisionEvaluationBuilderImpl builder = new DecisionEvaluationBuilderImpl(commandExecutor);
    builder.decisionDefinitionId = decisionDefinitionId;
    return builder;
  }

  // getters ////////////////////////////////////

  public String getDecisionDefinitionKey() {
    return decisionDefinitionKey;
  }

  public String getDecisionDefinitionId() {
    return decisionDefinitionId;
  }

  public Integer getVersion() {
    return version;
  }

  public Map<String, Object> getVariables() {
    return variables;
  }

  public String getDecisionDefinitionTenantId() {
    return decisionDefinitionTenantId;
  }

  public boolean isTenantIdSet() {
    return isTenantIdSet;
  }

}
